package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.enums.automation.AwaitMode;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.RconCommandStep;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.NotifyStep;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.DiscordWebhookStep;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import de.swiftbyte.gmc.common.entity.RconCommand;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class RconCommandStepExecutor implements AutomationStepExecutor<RconCommandStep> {
    private final ServerService serverService;
    @Override public AutomationStepType getType() { return AutomationStepType.RCON_COMMAND; }
    @Override public void execute(Automation automation, AutomationRun run, RconCommandStep step) {
        var state = run.getCurrentStep(); if (state == null) return;

        // Validate command
        if (step.getCommand() == null || step.getCommand().isBlank()) {
            // Mark all active targets failed due to missing command
            for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) continue;
                state.putStatus(e.getKey(), AutomationStepStatus.FAILED, "command_missing", Instant.now());
            }
            return;
        }

        // Decide await behavior
        AwaitMode mode = step.getAwaitMode() == null ? AwaitMode.AUTO : step.getAwaitMode();
        String varName = step.getVar();
        boolean hasVar = varName != null && !varName.isBlank();
        boolean shouldAwait = switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> hasVar && willBeUsedLater(automation, run, varName);
        };

        if (!shouldAwait) {
            // Fire-and-forget for all active targets
            for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) continue;
                String targetId = e.getKey();
                try {
                    var server = serverService.getOrThrow(targetId);
                    RconCommand cmd = new RconCommand();
                    cmd.setCommand(step.getCommand());
                    cmd.setTimestamp(Instant.now());
                    serverService.rconCommand(server, cmd);
                    state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
                } catch (Exception ex) {
                    state.putStatus(targetId, AutomationStepStatus.FAILED, ex.getMessage(), Instant.now());
                }
            }
            return;
        }

        // Await responses concurrently and capture under vars[varName][serverId]
        Map<String, String> captured = new HashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Long timeoutMs = step.getTimeoutMs() == null ? null : step.getTimeoutMs().longValue();

        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) continue;
            String targetId = e.getKey();
            CompletableFuture<Void> f = CompletableFuture.supplyAsync(() -> {
                var server = serverService.getOrThrow(targetId);
                RconCommand cmd = new RconCommand();
                cmd.setCommand(step.getCommand());
                cmd.setTimestamp(Instant.now());
                return serverService.rconCommandAwait(server, cmd, timeoutMs);
            }).handle((ServerRconResponsePacket resp, Throwable err) -> {
                if (err == null && resp != null) {
                    String response = resp.getCommand() != null ? resp.getCommand().getResponse() : null;
                    captured.put(targetId, response);
                    state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
                } else {
                    String msg;
                    if (err instanceof CompletionException ce && ce.getCause() != null) msg = ce.getCause().getMessage();
                    else msg = err == null ? "unknown_error" : err.getMessage();
                    state.putStatus(targetId, AutomationStepStatus.FAILED, msg, Instant.now());
                }
                return null;
            });
            futures.add(f);
        }

        // Block once for all targets
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            // Persist captured vars into the run for later template rendering
            if (hasVar) {
                if (run.getVars() == null) run.setVars(new HashMap<>());
                run.getVars().put(varName, captured);
            }
        }
    }

    private boolean willBeUsedLater(Automation automation, AutomationRun run, String varName) {
        if (automation == null || automation.getWorkflow() == null) return false;
        var current = run.getCurrentStep();
        if (current == null || current.getWorkflowIndex() == null) return false;
        int from = current.getWorkflowIndex() + 1;
        String needle1 = "outputs.rcon('" + varName + "')";
        String needle1d = "outputs.rcon(\"" + varName + "\")";
        String needle2 = "{" + needle1; // explicit template start pattern
        String needle2d = "{" + needle1d;
        for (int i = from; i < automation.getWorkflow().size(); i++) {
            var step = automation.getWorkflow().get(i);
            // Check known template-carrying steps
            if (step instanceof NotifyStep ns) {
                if (containsAny(ns.getTitle(), needle1, needle2, needle1d, needle2d)
                        || containsAny(ns.getMessage(), needle1, needle2, needle1d, needle2d)) return true;
            } else if (step instanceof DiscordWebhookStep ds) {
                if (containsAny(ds.getContent(), needle1, needle2, needle1d, needle2d)) return true;
                var e = ds.getEmbed();
                if (e != null) {
                    if (containsAny(e.getTitle(), needle1, needle2, needle1d, needle2d)
                            || containsAny(e.getDescription(), needle1, needle2, needle1d, needle2d)
                            || containsAny(e.getAuthorName(), needle1, needle2, needle1d, needle2d)
                            || containsAny(e.getAuthorUrl(), needle1, needle2, needle1d, needle2d)
                            || containsAny(e.getAuthorIconUrl(), needle1, needle2, needle1d, needle2d)) return true;
                }
            }
        }
        return false;
    }

    private boolean containsVar(String s, String needle) {
        return s != null && s.contains(needle);
    }

    private boolean containsAny(String s, String... needles) {
        if (s == null) return false;
        for (String n : needles) if (n != null && s.contains(n)) return true;
        return false;
    }
}

package de.swiftbyte.gmc.backend.service.automation;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.DynamicTargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.ExplicitTargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.TargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.*;
import de.swiftbyte.gmc.backend.events.server.ServerStateChangedEvent;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationRunUpdatedPacket;
import de.swiftbyte.gmc.backend.repository.AutomationRepository;
import de.swiftbyte.gmc.backend.repository.AutomationRunRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.util.AutomationRunUtility;
import de.swiftbyte.gmc.backend.util.AutomationUtility;
import de.swiftbyte.gmc.backend.service.automation.steps.AutomationStepExecutorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;

@Slf4j
@Service
public class AutomationRunEngine {

    @Autowired
    @Qualifier("automation_run_executor")
    private ThreadPoolTaskExecutor  executor;

    @Autowired
    private AutomationRunRepository runRepository;
    @Autowired
    private AutomationRepository automationRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    // Removed serverRepository usage in favor of a query utility

    @Autowired
    private StompSender stomp;

    @Autowired
    private TeamRepository teamRepository;



    public void startRun(AutomationRun run) {
        Automation automation = automationRepository.findById(run.getAutomationId()).orElseThrow();
        run.setStatus(AutomationRunStatus.RUNNING);
        if (run.getStartedAt() == null) {
            run.setStartedAt(Instant.now());
        }
        // Only resolve targets if none were preset (e.g., manual override)
        if (run.getTargetIds() == null || run.getTargetIds().isEmpty()) {
            HashMap<String, Boolean> map = new HashMap<>();
            for (String id : resolveTargets(automation)) {
                map.put(id, Boolean.FALSE);
            }
            run.setTargetIds(map);
        }

        // Assume initial step is set by the AutomationEngine at run creation

        // Initialize per-target status for the current step with QUEUED for all targets,
        // except for explicit timed waits where perTargetStatus may remain null.
        try {
            int idx = run.getCurrentStep() != null ? (run.getCurrentStep().getWorkflowIndex() != null ? run.getCurrentStep().getWorkflowIndex() : -1) : -1;
            AutomationStep step = (idx >= 0 && automation.getWorkflow() != null && idx < automation.getWorkflow().size())
                    ? automation.getWorkflow().get(idx) : null;
            if (step == null || (step.getType() != AutomationStepType.TIMED_WAIT)) {
                if (run.getCurrentStep().getPerTargetStatus() == null) {
                    run.getCurrentStep().setPerTargetStatus(new HashMap<>());
                }
                for (String targetId : run.getTargetIds().keySet()) {
                    if (Boolean.TRUE.equals(run.getTargetIds().get(targetId))) continue; // skipped globally
                    AutomationRun.StepState.TargetStatus ts = new AutomationRun.StepState.TargetStatus();
                    ts.setState(AutomationStepStatus.QUEUED);
                    run.getCurrentStep().getPerTargetStatus().putIfAbsent(targetId, ts);
                }
            }
        } catch (Exception ignored) {}

        // Mark the beginning of the first step
        if (run.getCurrentStep().getStartedAt() == null) {
            run.getCurrentStep().setStartedAt(Instant.now());
        }

        updateRun(run);
        // Immediately start processing the first step
        processStep(automation, run);
    }

    


    public void processStep(Automation automation, AutomationRun run) {
        if (automation == null || run == null) return;
        if (automation.getWorkflow() == null) return;
        if (run.getCurrentStep() == null || run.getCurrentStep().getWorkflowIndex() == null) return;

        int stepIndex = run.getCurrentStep().getWorkflowIndex();
        if (stepIndex < 0 || stepIndex >= automation.getWorkflow().size()) return;

        AutomationStep step = automation.getWorkflow().get(stepIndex);
        AutomationRun.StepState state = run.getCurrentStep();

        // Cancellation requested at step boundary -> cancel immediately (like timeouts)
        if (Boolean.TRUE.equals(run.getCancelRequested())) {
            cancelCurrentStepAndRun(run, state);
            return;
        }

        // Run-level timeout enforcement at the beginning of the step
        if (isRunTimedOut(automation, run)) {
            timeoutCurrentStepAndRun(automation, run, state);
            return;
        }

        // Pre-mark already skipped targets for this step to keep step completeness consistent
        if (state.getPerTargetStatus() == null) state.setPerTargetStatus(new HashMap<>());
        for (String targetId : run.getTargetIds().keySet()) {
            if (Boolean.TRUE.equals(run.getTargetIds().get(targetId))) {
                AutomationStepStatus existing = state.getStateOf(targetId);
                if (!isTerminal(existing)) {
                    state.putStatus(targetId, AutomationStepStatus.SKIPPED, null, Instant.now());
                }
            }
        }

        // Delegate execution to the per-step executor implementation
        // (one executor per step type). Executors are responsible for
        // updating per-target statuses appropriately.
        stepExecutors.execute(automation, run, step);

        // Persist updates from handlers, then try to advance if ready.
        updateRun(run);
        advanceIfReady(automation, run);
    }

    private boolean isRunTimedOut(Automation automation, AutomationRun run) {
        Integer runTimeoutSec = automation.getRunTimeoutSec();
        if (runTimeoutSec == null || runTimeoutSec <= 0) return false;
        if (run.getStartedAt() == null) return false;
        Instant deadline = run.getStartedAt().plusSeconds(runTimeoutSec);
        return Instant.now().isAfter(deadline);
    }

    private boolean isStepTimedOut(Automation automation, AutomationRun run) {
        if (run.getCurrentStep() == null || run.getCurrentStep().getStartedAt() == null) return false;
        Integer idx = run.getCurrentStep().getWorkflowIndex();
        if (idx == null) return false;
        if (automation.getWorkflow() == null || idx < 0 || idx >= automation.getWorkflow().size()) return false;
        AutomationStep s = automation.getWorkflow().get(idx);
        Integer stepTimeoutSec = s != null ? s.getTimeoutSec() : null;
        if (stepTimeoutSec == null || stepTimeoutSec <= 0) return false;
        Instant deadline = run.getCurrentStep().getStartedAt().plusSeconds(stepTimeoutSec);
        return Instant.now().isAfter(deadline);
    }

    private void timeoutCurrentStepAndRun(Automation automation, AutomationRun run, AutomationRun.StepState state) {
        if (state != null) {
            for (String targetId : run.getTargetIds().keySet()) {
                AutomationRun.StepState.TargetStatus ts = new AutomationRun.StepState.TargetStatus();
                ts.setState(AutomationStepStatus.FAILED);
                ts.setMessage("timeout");
                ts.setCompletedAt(Instant.now());
                state.getPerTargetStatus().put(targetId, ts);
            }
            if (state.getFinishedAt() == null) state.setFinishedAt(Instant.now());
            if (run.getStepHistory() != null) run.getStepHistory().add(state);
        }
        run.setStatus(AutomationRunStatus.TIMEOUTED);
        run.setFinishedAt(Instant.now());
        run.setCurrentStep(null);
        updateRun(run);
        try {
            String title = "Automation run timed out";
            String msg = String.format("Automation '%s' (id=%s) run %s timed out.",
                    automation != null ? automation.getName() : run.getAutomationId(),
                    run.getAutomationId(),
                    run.getId());
            notificationService.create(run.getTeamId(),
                    "AUTOMATION_RUN_TIMEOUTED",
                    NotificationSeverity.WARNING,
                    title,
                    msg,
                    NotificationSource.BACKEND);
        } catch (Exception ignored) { }
    }

    /**
     * Advances the run to the next workflow step if the current step is complete for all targets.
     * Returns true if the run was advanced or completed; false otherwise.
     */
    public void advanceIfReady(Automation automation, AutomationRun run) {
        if (automation == null || run == null) return;
        if (automation.getWorkflow() == null) return;
        if (run.getStatus() != AutomationRunStatus.RUNNING) return;
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null || state.getWorkflowIndex() == null) return;

        boolean allDone = run.getTargetIds().keySet().stream()
                .allMatch(tid -> isTerminal(state.getStateOf(tid)));

        if (!allDone) return;

        // Record the completed step in history
        if (state.getFinishedAt() == null) {
            state.setFinishedAt(Instant.now());
        }
        if (run.getStepHistory() != null) {
            run.getStepHistory().add(state);
        }

        // If cancellation requested, do not advance; cancel run after completing this step
        if (Boolean.TRUE.equals(run.getCancelRequested())) {
            run.setStatus(AutomationRunStatus.CANCELED);
            run.setFinishedAt(Instant.now());
            updateRun(run);
            try {
                String title = "Automation run canceled";
                String msg = String.format("Automation '%s' (id=%s) run %s was canceled.",
                        automation.getName(), automation.getId(), run.getId());
                notificationService.create(run.getTeamId(),
                        "AUTOMATION_RUN_CANCELED",
                        NotificationSeverity.INFO,
                        title,
                        msg,
                        NotificationSource.BACKEND);
            } catch (Exception ignored) { }
            return;
        }

        // If all non-skipped targets failed in this step, end the run as FAILED
        List<String> activeBefore = run.getTargetIds().entrySet().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        boolean allFailed = !activeBefore.isEmpty() && activeBefore.stream()
                .allMatch(tid -> state.getStateOf(tid) == AutomationStepStatus.FAILED);
        if (allFailed) {
            run.setStatus(AutomationRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setCurrentStep(null);
            updateRun(run);
            try {
                String title = "Automation run failed";
                String msg = String.format("Automation '%s' (id=%s) run %s failed at step %d.",
                        automation.getName(), automation.getId(), run.getId(), state.getWorkflowIndex());
                notificationService.create(run.getTeamId(),
                        "AUTOMATION_RUN_FAILED",
                        NotificationSeverity.WARNING,
                        title,
                        msg,
                        NotificationSource.BACKEND);
            } catch (Exception ignored) { }
            return;
        }

        // Mark any non-succeeded targets as skipped for subsequent steps
        for (String tid : run.getTargetIds().keySet()) {
            AutomationStepStatus st = state.getStateOf(tid);
            if (st != AutomationStepStatus.SUCCEEDED) {
                run.getTargetIds().put(tid, Boolean.TRUE);
            }
        }

        // Last-step check (zero-based index)
        if (state.getWorkflowIndex() != null && run.getWorkflowSize() != null
                && state.getWorkflowIndex().equals(run.getWorkflowSize() - 1)) {
            runCompleted(automation, run);
            return;
        }

        int nextIndex = state.getWorkflowIndex() + 1;
        AutomationRun.StepState nextState = AutomationUtility.buildStepStateFor(automation, nextIndex);
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                nextState.putStatus(e.getKey(), AutomationStepStatus.QUEUED);
            }
        }
        // Mark the beginning of the next step
        nextState.setStartedAt(Instant.now());
        run.setCurrentStep(nextState);
        updateRun(run);
        // Immediately process the next step if the run is still active
        if (run.getStatus() == AutomationRunStatus.RUNNING) {
            processStep(automation, run);
        }
    }

    /**
     * Centralized place to persist run updates and later emit STOMP notifications.
     */
    public AutomationRun updateRun(AutomationRun run) {
        AutomationRun saved = runRepository.save(run);
        try {
            AutomationRunUpdatedPacket pkt = new AutomationRunUpdatedPacket();
            pkt.setRun(saved);
            stomp.sendToTeam(teamRepository.findById(saved.getTeamId()).orElseThrow(), "/automation-run/update", pkt);
        } catch (Exception ignored) { }
        return saved;
    }

    private List<String> resolveTargets(Automation automation) {
        TargetSelector selector = automation.getTargets();
        if (selector == null) {
            return List.of();
        }

        if (selector instanceof ExplicitTargetSelector explicit) {
            return List.copyOf(explicit.getIds());
        }

        if (selector instanceof DynamicTargetSelector dynamic) {
            Criteria team = Criteria.where("teamId").is(automation.getTeamId());
            Criteria dyn = AutomationRunUtility.buildDynamicSelectorCriteria(dynamic);
            Query query = new Query(new Criteria().andOperator(team, dyn));

            // Fetch only IDs directly from Mongo without materializing full entities
            return mongoTemplate.findDistinct(query, "_id", GameServer.class, String.class);
        }

        // Unknown selector type -> no targets
        return List.of();
    }

    private void runCompleted(Automation automation, AutomationRun run) {
        boolean anyFailed = run.getCurrentStep().getPerTargetStatus().keySet().stream()
                .anyMatch(tid -> run.getCurrentStep().getStateOf(tid) == AutomationStepStatus.FAILED);
        run.setStatus(anyFailed ? AutomationRunStatus.FAILED : AutomationRunStatus.SUCCEEDED);
        run.setFinishedAt(Instant.now());
        run.setCurrentStep(null);
        updateRun(run);
        try {
            boolean failed = anyFailed;
            String title = failed ? "Automation run failed" : "Automation run succeeded";
            String msg;
            if (failed) {
                msg = String.format("Automation '%s' (id=%s) run %s completed with failures.",
                        automation.getName(), automation.getId(), run.getId());
            } else {
                msg = String.format("Automation '%s' (id=%s) run %s completed successfully.",
                        automation.getName(), automation.getId(), run.getId());
            }
            notificationService.create(run.getTeamId(),
                    failed ? "AUTOMATION_RUN_FAILED" : "AUTOMATION_RUN_SUCCEEDED",
                    failed ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
                    title,
                    msg,
                    NotificationSource.BACKEND);
        } catch (Exception ignored) { }
    }

    private boolean isTerminal(AutomationStepStatus status) {
        if (status == null) return false;
        return switch (status) {
            case SUCCEEDED, FAILED, SKIPPED -> true;
            default -> false;
        };
    }

    // Per-step logic handled by dedicated executors via AutomationStepExecutorRegistry

    // TIMED_WAIT and STATE_WAIT handling moved to dedicated executors

    @Autowired
    private AutomationStepExecutorRegistry stepExecutors;

    @Autowired
    private NotificationService notificationService;

    // --- Scheduled scanner: wake timed waits ---
    @Scheduled(fixedRate = 30_000)
    public void scanDueTimedWaits() {
        // Loop and claim work items; execute processing via automation run executor
        while (true) {
            AutomationRun claimed = claimOneDueTimedWait();
            if (claimed == null) break;
            executor.execute(() -> processTimedWaitWake(claimed));
        }
    }

    private AutomationRun claimOneDueTimedWait() {
        Instant now = Instant.now();
        Query query = AutomationUtility.dueTimedWaitClaimQuery(now).limit(1);
        Update update = new Update().set("currentStep.leaseUntil", now.plusSeconds(5));
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), AutomationRun.class);
    }

    private void processTimedWaitWake(AutomationRun run) {
        try {
            Automation automation = automationRepository.findById(run.getAutomationId()).orElse(null);
            if (automation == null) return;
            AutomationRun.StepState stepState = run.getCurrentStep();
            if (stepState == null) return;
            if (Boolean.TRUE.equals(run.getCancelRequested())) {
                cancelCurrentStepAndRun(run, stepState);
                return;
            }
            // Enforce run or step timeout before completing wait
            if (isRunTimedOut(automation, run) || isStepTimedOut(automation, run)) {
                timeoutCurrentStepAndRun(automation, run, stepState);
                return;
            }
            // Complete the timed wait by succeeding all non-skipped targets
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) continue;
                stepState.putStatus(e.getKey(), AutomationStepStatus.SUCCEEDED, null, Instant.now());
            }
            // Clear wake and lease; switch back to RUNNING so next step processes
            stepState.setWakeAt(null);
            stepState.setLeaseUntil(null);
            run.setStatus(AutomationRunStatus.RUNNING);
            updateRun(run);
            advanceIfReady(automation, run);
        } catch (Exception e) {
            log.warn("Failed processing timed wait wake for run {}: {}", run.getId(), e.getMessage(), e);
        }
    }

    // --- Event-driven wake: server state change ---
    @Async
    @EventListener(ServerStateChangedEvent.class)
    public void onServerStateChanged(ServerStateChangedEvent event) {
        String serverId = event.getServer().getId();
        String teamId = event.getServer().getTeamId();
        // Direct atomic writes without leases; multiple events may occur within seconds
        while (true) {
            Query query = AutomationUtility.stateWaitEventQuery(event.getNewState(), serverId, teamId).limit(1);

            Map<String, Object> value = new HashMap<>();
            value.put("state", AutomationStepStatus.SUCCEEDED);
            value.put("completedAt", Instant.now());
            Update update = new Update()
                    .set("currentStep.perTargetStatus." + serverId, value);

            AutomationRun run = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), AutomationRun.class);
            if (run == null) break;

            AutomationRun.StepState stepState = run.getCurrentStep();
            if (Boolean.TRUE.equals(run.getCancelRequested())) {
                cancelCurrentStepAndRun(run, stepState);
                continue;
            }
            Automation automation = automationRepository.findById(run.getAutomationId()).orElse(null);
            if (automation != null && (isRunTimedOut(automation, run) || isStepTimedOut(automation, run))) {
                timeoutCurrentStepAndRun(automation, run, stepState);
                continue;
            }
        boolean allDone = run.getTargetIds().keySet().stream()
                .allMatch(tid -> isTerminal(stepState.getStateOf(tid)));

            if (allDone) {
                if (automation == null) continue;
                run.setStatus(AutomationRunStatus.RUNNING);
                updateRun(run);
                advanceIfReady(automation, run);
            }
        }
    }

    // --- Scheduled timeout scanner: run and step timeouts (esp. waits) ---
    @Scheduled(fixedRateString = "${automation.timeout.scanRateMs:600000}")
    public void scanTimeouts() {
        try {
            Query query = new Query(new Criteria().andOperator(
                    Criteria.where("status").in(AutomationRunStatus.PENDING, AutomationRunStatus.RUNNING),
                    Criteria.where("currentStep.startedAt").exists(true)
            ));
            List<AutomationRun> runs = mongoTemplate.find(query, AutomationRun.class);
            for (AutomationRun run : runs) {
                try {
                    Automation automation = automationRepository.findById(run.getAutomationId()).orElse(null);
                    if (automation == null) continue;
                    boolean isWait = run.getCurrentStep() != null && isWaitType(run.getCurrentStep().getType());
                    if (Boolean.TRUE.equals(run.getCancelRequested()) && isWait) {
                        cancelCurrentStepAndRun(run, run.getCurrentStep());
                        continue;
                    }
                    boolean runTO = isWait && isRunTimedOut(automation, run);
                    boolean stepTO = isWait && isStepTimedOut(automation, run);
                    if (runTO || stepTO) {
                        timeoutCurrentStepAndRun(automation, run, run.getCurrentStep());
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Timeout scan failed: {}", e.getMessage(), e);
        }
    }

    private boolean isWaitType(AutomationStepType type) {
        // Treat implicit state waits for server actions as waits, too
        return type == AutomationStepType.TIMED_WAIT
                || type == AutomationStepType.STATE_WAIT
                || type == AutomationStepType.START
                || type == AutomationStepType.STOP
                || type == AutomationStepType.RESTART
                || type == AutomationStepType.UPDATE;
    }

    private void cancelCurrentStepAndRun(AutomationRun run, AutomationRun.StepState state) {
        if (state != null) {
            for (String targetId : run.getTargetIds().keySet()) {
                state.putStatus(targetId, AutomationStepStatus.SKIPPED, null, Instant.now());
            }
            state.setWakeAt(null);
            state.setLeaseUntil(null);
            state.setWaitingFor(null);
            if (state.getFinishedAt() == null) state.setFinishedAt(Instant.now());
            if (run.getStepHistory() != null) run.getStepHistory().add(state);
        }
        run.setStatus(AutomationRunStatus.CANCELED);
        run.setFinishedAt(Instant.now());
        run.setCurrentStep(null);
        updateRun(run);
        try {
            String title = "Automation run canceled";
            String msg = String.format("Automation (id=%s) run %s was canceled.",
                    run.getAutomationId(),
                    run.getId());
            notificationService.create(run.getTeamId(),
                    "AUTOMATION_RUN_CANCELED",
                    NotificationSeverity.INFO,
                    title,
                    msg,
                    NotificationSource.BACKEND);
        } catch (Exception ignored) { }
    }

    // --- Scheduled scanner: start queued manual runs at scheduledFireAt ---
    @Scheduled(fixedRate = 5_000)
    public void scanDueQueuedRuns() {
        while (true) {
            AutomationRun claimed = claimOneDueQueuedRun();
            if (claimed == null) break;
            executor.execute(() -> startRun(claimed));
        }
    }

    private AutomationRun claimOneDueQueuedRun() {
        Instant now = Instant.now();
        Query q = new Query(new Criteria().andOperator(
                Criteria.where("status").is(AutomationRunStatus.QUEUED),
                Criteria.where("scheduledFireAt").lte(now)
        )).limit(1);
        Update update = new Update().set("status", AutomationRunStatus.RUNNING).set("startedAt", now);
        return mongoTemplate.findAndModify(q, update, FindAndModifyOptions.options().returnNew(true), AutomationRun.class);
    }

}

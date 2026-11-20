package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.DiscordWebhookStep;
import de.swiftbyte.gmc.backend.infrastructure.DiscordWebhookSender;
import de.swiftbyte.gmc.backend.domain.value.DiscordEmbed;
import de.swiftbyte.gmc.backend.expressions.AutomationExpressionEngine;
import de.swiftbyte.gmc.backend.exception.AutomationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordWebhookStepExecutor implements AutomationStepExecutor<DiscordWebhookStep> {

    private final MongoTemplate mongoTemplate;
    private final DiscordWebhookSender discordWebhookSender;
    private final AutomationExpressionEngine expr;

    @Override
    public AutomationStepType getType() {
        return AutomationStepType.DISCORD_WEBHOOK;
    }

    @Override
    public void execute(Automation automation, AutomationRun run, DiscordWebhookStep step) {
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null) return;

        String url = step.getWebhookUrl();
        String contentRaw = step.getContent() == null ? "" : step.getContent();
        String content;
        try {
            content = expr.renderField("discord.content", contentRaw, automation, run);
        } catch (AutomationException e) {
            String reason = "automation.expression_error";
            log.warn("Expression error during discord.content rendering: {}", e.getMessage());
            for (Map.Entry<String, Boolean> entry : run.getTargetIds().entrySet()) {
                String targetId = entry.getKey();
                if (Boolean.TRUE.equals(entry.getValue())) continue;
                state.putStatus(targetId, AutomationStepStatus.FAILED, reason, Instant.now());
            }
            return;
        }

        // Render embed fields (if present) within the automation system
        DiscordEmbed embed = null;
        if (step.getEmbed() != null) {
            var e = step.getEmbed();
            DiscordEmbed out = new DiscordEmbed();
            try {
                out.setTitle(expr.renderField("discord.embed.title", e.getTitle(), automation, run));
                out.setDescription(expr.renderField("discord.embed.description", e.getDescription(), automation, run));
                out.setColor(e.getColor());
                out.setAuthorName(expr.renderField("discord.embed.authorName", e.getAuthorName(), automation, run));
                out.setAuthorUrl(expr.renderField("discord.embed.authorUrl", e.getAuthorUrl(), automation, run));
                out.setAuthorIconUrl(expr.renderField("discord.embed.authorIconUrl", e.getAuthorIconUrl(), automation, run));
            } catch (AutomationException ex) {
                String reason = "automation.expression_error";
                log.warn("Expression error during discord.embed rendering: {}", ex.getMessage());
                for (Map.Entry<String, Boolean> entry : run.getTargetIds().entrySet()) {
                    String targetId = entry.getKey();
                    if (Boolean.TRUE.equals(entry.getValue())) continue;
                    state.putStatus(targetId, AutomationStepStatus.FAILED, reason, Instant.now());
                }
                return;
            }
            embed = out;
        }

        DiscordWebhookSender.Result result = discordWebhookSender.send(url, content, embed);

        for (Map.Entry<String, Boolean> entry : run.getTargetIds().entrySet()) {
            String targetId = entry.getKey();
            if (Boolean.TRUE.equals(entry.getValue())) continue;
            if (result.ok()) {
                state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
            } else {
                state.putStatus(targetId, AutomationStepStatus.FAILED, result.error(), Instant.now());
            }
        }
    }
}

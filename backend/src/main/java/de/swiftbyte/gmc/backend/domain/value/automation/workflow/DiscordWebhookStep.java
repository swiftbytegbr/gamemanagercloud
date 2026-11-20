package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.value.DiscordEmbed;
import de.swiftbyte.gmc.backend.validation.DiscordWebhookUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.discordWebhook")
public class DiscordWebhookStep extends AutomationStep {

    // Full Discord webhook URL
    @NotBlank(message = "automation.discord_webhook_url_required")
    @DiscordWebhookUrl
    private String webhookUrl;

    // Message content (text only, no embeds); Discord limit ~2000 chars
    @Size(max = 2000, message = "automation.discord_content_too_long")
    private String content;

    // Optional simple embed – if provided, will be sent alongside content
    private DiscordEmbed embed;

    public DiscordWebhookStep() {
        setType(AutomationStepType.DISCORD_WEBHOOK);
    }

}

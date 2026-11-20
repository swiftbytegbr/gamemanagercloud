package de.swiftbyte.gmc.backend.domain.value;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Simple Discord embed value object used by automation steps.
 */
@Data
public class DiscordEmbed {
    @Size(max = 256, message = "automation.discord_embed_title_too_long")
    private String title;

    @Size(max = 4096, message = "automation.discord_embed_description_too_long")
    private String description;

    // Decimal RGB color (e.g., 16711680 for red)
    private Integer color;

    // Author fields (optional)
    @Size(max = 256, message = "automation.discord_embed_author_name_too_long")
    private String authorName;
    private String authorUrl;
    private String authorIconUrl; // maps to Discord's author.icon_url
}


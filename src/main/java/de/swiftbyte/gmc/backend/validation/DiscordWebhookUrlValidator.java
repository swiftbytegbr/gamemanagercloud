package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

public class DiscordWebhookUrlValidator implements ConstraintValidator<DiscordWebhookUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank handles requiredness
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();
            if (scheme == null || host == null || path == null) return false;

            if (!"https".equalsIgnoreCase(scheme)) return false;

            boolean hostOk = host.equalsIgnoreCase("discordapp.com")
                    || host.equalsIgnoreCase("discord.com")
                    || host.equalsIgnoreCase("canary.discord.com")
                    || host.equalsIgnoreCase("ptb.discord.com");
            if (!hostOk) return false;

            return path.startsWith("/api/webhooks/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}


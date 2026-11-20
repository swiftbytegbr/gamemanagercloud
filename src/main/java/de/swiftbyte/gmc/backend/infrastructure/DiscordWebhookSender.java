package de.swiftbyte.gmc.backend.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.swiftbyte.gmc.backend.domain.value.DiscordEmbed;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DiscordWebhookSender {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends a Discord webhook with optional content and a single simple embed.
     * Returns Result.ok=true on 2xx, otherwise ok=false with error description.
     */
    public Result send(String webhookUrl, String content, DiscordEmbed embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return new Result(false, "webhook URL missing");
        }
        try {
            ObjectNode root = mapper.createObjectNode();
            boolean hasContent = content != null && !content.isBlank();
            if (hasContent) root.put("content", content);

            boolean includedEmbed = false;
            if (embed != null) {
                boolean hasAny = (embed.getTitle() != null && !embed.getTitle().isBlank())
                        || (embed.getDescription() != null && !embed.getDescription().isBlank())
                        || embed.getColor() != null
                        || (embed.getAuthorName() != null && !embed.getAuthorName().isBlank())
                        || (embed.getAuthorUrl() != null && !embed.getAuthorUrl().isBlank())
                        || (embed.getAuthorIconUrl() != null && !embed.getAuthorIconUrl().isBlank());
                if (hasAny) {
                    ObjectNode e = mapper.createObjectNode();
                    if (embed.getTitle() != null && !embed.getTitle().isBlank()) e.put("title", embed.getTitle());
                    if (embed.getDescription() != null && !embed.getDescription().isBlank()) e.put("description", embed.getDescription());
                    if (embed.getColor() != null) e.put("color", embed.getColor());
                    if ((embed.getAuthorName() != null && !embed.getAuthorName().isBlank())
                            || (embed.getAuthorUrl() != null && !embed.getAuthorUrl().isBlank())
                            || (embed.getAuthorIconUrl() != null && !embed.getAuthorIconUrl().isBlank())) {
                        ObjectNode a = mapper.createObjectNode();
                        if (embed.getAuthorName() != null && !embed.getAuthorName().isBlank()) a.put("name", embed.getAuthorName());
                        if (embed.getAuthorUrl() != null && !embed.getAuthorUrl().isBlank()) a.put("url", embed.getAuthorUrl());
                        if (embed.getAuthorIconUrl() != null && !embed.getAuthorIconUrl().isBlank()) a.put("icon_url", embed.getAuthorIconUrl());
                        e.set("author", a);
                    }
                    List<ObjectNode> arr = new ArrayList<>();
                    arr.add(e);
                    root.set("embeds", mapper.valueToTree(arr));
                    includedEmbed = true;
                }
            }

            if (!hasContent && !includedEmbed) {
                return new Result(false, "empty payload");
            }

            String body = mapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return new Result(true, null);
            }
            return new Result(false, "HTTP " + code);
        } catch (Exception e) {
            log.warn("Discord webhook send failed: {}", e.getMessage());
            return new Result(false, e.getMessage());
        }
    }

    public record Result(boolean ok, String error) {}
}

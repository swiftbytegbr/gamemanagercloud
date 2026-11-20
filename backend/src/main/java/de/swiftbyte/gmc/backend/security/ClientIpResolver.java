package de.swiftbyte.gmc.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    /**
     * Gets the client's real IP address, taking into account proxy headers.
     * Checks Cloudflare and standard proxy headers before falling back.
     */
    public String resolve(HttpServletRequest request) {
        String[] headers = {"CF-Connecting-IP", "X-Forwarded-For"};

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty()) {
                return header.equals("X-Forwarded-For") ? ip.split(",")[0].trim() : ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Resolve IP for WebSocket handshake or other ServerHttpRequest contexts.
     */
    public String resolve(ServerHttpRequest request) {
        String cf = request.getHeaders().getFirst("CF-Connecting-IP");
        if (cf != null && !cf.isEmpty()) return cf;
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        try {
            if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
                return request.getRemoteAddress().getAddress().getHostAddress();
            }
        } catch (Exception ignored) { }
        return null;
    }
}

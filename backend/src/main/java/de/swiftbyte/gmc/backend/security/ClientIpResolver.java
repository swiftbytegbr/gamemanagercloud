package de.swiftbyte.gmc.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the client IP address.
 * <p>
 * The forwarding header ({@code GMC_CLIENT_IP_HEADER}, default {@code X-Forwarded-For}; {@code CF-Connecting-IP}
 * behind Cloudflare) is only trusted if the request comes from a trusted proxy ({@code GMC_TRUSTED_PROXIES},
 * comma-separated IPs or CIDR ranges; private and loopback ranges by default). Otherwise the address of the TCP
 * connection is used, so clients cannot spoof their IP.
 */
@Slf4j
@Component
public class ClientIpResolver {

    private final String header;
    private final List<Cidr> trustedProxies = new ArrayList<>();

    public ClientIpResolver(@Value("${gmc.security.client-ip-header:X-Forwarded-For}") String header,
                            @Value("${gmc.security.trusted-proxies:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8,::1/128,fc00::/7}") String trustedProxies) {
        this.header = header == null || header.isBlank() ? "X-Forwarded-For" : header.trim();
        if (trustedProxies != null) {
            for (String entry : trustedProxies.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                Cidr cidr = Cidr.parse(trimmed);
                if (cidr == null) log.warn("Ignoring invalid trusted proxy entry '{}'", trimmed);
                else this.trustedProxies.add(cidr);
            }
        }
    }

    public String resolve(HttpServletRequest request) {
        return resolve(request.getRemoteAddr(), request.getHeader(header));
    }

    /**
     * Resolve IP for WebSocket handshake or other ServerHttpRequest contexts.
     */
    public String resolve(ServerHttpRequest request) {
        String remote = null;
        try {
            if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
                remote = request.getRemoteAddress().getAddress().getHostAddress();
            }
        } catch (Exception ignored) { }
        return resolve(remote, request.getHeaders().getFirst(header));
    }

    private String resolve(String remoteAddress, String headerValue) {
        if (remoteAddress == null || headerValue == null || headerValue.isBlank() || !isTrusted(remoteAddress)) {
            return remoteAddress;
        }
        // Walk the chain from the right and return the first address that is not a trusted proxy
        String[] hops = headerValue.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (hop.isEmpty()) continue;
            if (Cidr.literal(hop) == null) return remoteAddress; // not an IP literal, ignore the header
            if (!isTrusted(hop) || i == 0) return hop;
        }
        return remoteAddress;
    }

    private boolean isTrusted(String address) {
        for (Cidr cidr : trustedProxies) {
            if (cidr.contains(address)) return true;
        }
        return false;
    }

    private record Cidr(byte[] network, int prefixLength) {

        static Cidr parse(String value) {
            try {
                String[] parts = value.split("/", 2);
                byte[] address = literal(parts[0]);
                if (address == null) return null;
                int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : address.length * 8;
                if (prefix < 0 || prefix > address.length * 8) return null;
                return new Cidr(address, prefix);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        boolean contains(String value) {
            byte[] address = literal(value);
            if (address == null || address.length != network.length) return false;
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }

        // Only parses IP literals, never performs DNS lookups
        private static byte[] literal(String value) {
            if (value == null) return null;
            String v = value.trim();
            if (v.startsWith("[") && v.endsWith("]")) v = v.substring(1, v.length() - 1);
            int zone = v.indexOf('%');
            if (zone >= 0) v = v.substring(0, zone);
            boolean ipv4 = v.matches("\\d{1,3}(\\.\\d{1,3}){3}");
            boolean ipv6 = v.contains(":") && v.matches("[0-9A-Fa-f:.]+");
            if (!ipv4 && !ipv6) return null;
            try {
                byte[] bytes = InetAddress.getByName(v).getAddress();
                // Treat IPv4-mapped IPv6 addresses as IPv4
                if (bytes.length == 16 && isIpv4Mapped(bytes)) {
                    byte[] v4 = new byte[4];
                    System.arraycopy(bytes, 12, v4, 0, 4);
                    return v4;
                }
                return bytes;
            } catch (Exception e) {
                return null;
            }
        }

        private static boolean isIpv4Mapped(byte[] b) {
            for (int i = 0; i < 10; i++) if (b[i] != 0) return false;
            return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
        }
    }
}

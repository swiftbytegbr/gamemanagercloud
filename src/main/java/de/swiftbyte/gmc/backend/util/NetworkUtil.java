package de.swiftbyte.gmc.backend.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NetworkUtil {
    private NetworkUtil() {}

    public static boolean isNonLocalAddress(String ip) {
        if (ip == null || ip.isBlank()) return false;
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return !(addr.isLoopbackAddress() || addr.isAnyLocalAddress());
        } catch (Exception ignored) {
            String s = ip.trim().toLowerCase();
            return !(s.equals("127.0.0.1") || s.equals("::1") || s.equals("localhost"));
        }
    }

    public static List<String> filterNonLocal(List<String> ips) {
        if (ips == null || ips.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String ip : ips) {
            if (isNonLocalAddress(ip)) out.add(ip);
        }
        return out;
    }
}


export function normalizeIp(value: string) {
    const trimmed = value.trim();
    if (!trimmed) return "";

    const withoutCidr = trimmed.split("/")[0] ?? "";
    const bracketMatch = withoutCidr.match(/^\[(.+)](?::\d+)?$/);
    if (bracketMatch?.[1]) return bracketMatch[1];

    const ipv4WithPortMatch = withoutCidr.match(
        /^(\d{1,3}(?:\.\d{1,3}){3}):\d+$/,
    );
    if (ipv4WithPortMatch?.[1]) return ipv4WithPortMatch[1];

    return withoutCidr;
}

export function isPublicIpv4(value: string) {
    const match = value.match(
        /^(\d{1,3})(?:\.(\d{1,3}))(?:\.(\d{1,3}))(?:\.(\d{1,3}))$/,
    );
    if (!match) return false;

    const octets = match.slice(1).map((part) => Number(part));
    if (
        octets.some(
            (octet) => !Number.isFinite(octet) || octet < 0 || octet > 255,
        )
    ) {
        return false;
    }

    const [a, b] = octets;

    if (a === 0) return false; // 0.0.0.0/8
    if (a === 10) return false; // 10.0.0.0/8
    if (a === 127) return false; // 127.0.0.0/8 loopback
    if (a === 169 && b === 254) return false; // link-local
    if (a === 172 && b >= 16 && b <= 31) return false; // 172.16.0.0/12
    if (a === 192 && b === 168) return false; // 192.168.0.0/16
    if (a === 100 && b >= 64 && b <= 127) return false; // CGNAT 100.64.0.0/10
    if (a >= 224) return false; // multicast / reserved

    return true;
}

export function isLocalOrPrivateIpv4(ip: string) {
    const normalized = normalizeIp(ip).toLowerCase();
    if (!normalized) return true;

    if (normalized === "localhost") return true;
    return !isPublicIpv4(normalized);
}

import { format } from "date-fns";

export function formatDate(value: string | undefined) {
    if (!value) return null;
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return null;
    return format(parsed, "PPpp");
}

export function formatGbFromMb(megabytes: number | undefined | null) {
    if (megabytes === undefined || megabytes === null) return null;
    if (!Number.isFinite(megabytes)) return null;
    return `${(megabytes / 1024).toFixed(1)} GB`;
}

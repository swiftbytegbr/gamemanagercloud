import {
    type RefObject,
    useCallback,
    useEffect,
    useLayoutEffect,
    useRef,
} from "react";
import type { RowData, Table } from "@/lib/table.ts";

export function useAutoPageSize<T extends HTMLElement, Row extends RowData>(
    table: Table<Row>,
    containerRef: RefObject<T | null>,
    enabled = true,
    maxPageSize?: number,
) {
    const lastSizeRef = useRef<{ width: number; height: number } | null>(null);
    const baselineRowHeightRef = useRef<number | null>(null);

    const calculate = useCallback(() => {
        const container = containerRef.current;
        if (!container) return;
        const header = container.querySelector("thead") as HTMLElement | null;
        const firstRow = container.querySelector(
            "tbody tr",
        ) as HTMLElement | null;
        if (!firstRow) return;
        const headerHeight = header?.offsetHeight ?? 0;
        // Capture baseline row height once to avoid pageSize oscillation
        if (baselineRowHeightRef.current == null) {
            baselineRowHeightRef.current = firstRow.offsetHeight;
        }
        const rowHeight = baselineRowHeightRef.current;
        if (!rowHeight || rowHeight === 0) return;
        const availableHeight = container.clientHeight - headerHeight;
        const pageSize = Math.floor(availableHeight / rowHeight);
        if (
            pageSize > 0 &&
            table.atoms.pagination.get().pageSize !== pageSize
        ) {
            table.setPageSize(Math.min(pageSize, maxPageSize ?? Infinity));
        }
    }, [containerRef, table, maxPageSize]);

    useLayoutEffect(() => {
        if (!enabled) return;
        calculate();
    });

    useEffect(() => {
        if (!enabled) return;
        window.addEventListener("resize", calculate);
        return () => window.removeEventListener("resize", calculate);
    }, [enabled, calculate]);

    useEffect(() => {
        if (!enabled) return;
        const container = containerRef.current;
        if (!container || typeof ResizeObserver === "undefined") return;

        const ro = new ResizeObserver((entries) => {
            const entry = entries[0];
            if (!entry) return;
            const { width, height } = entry.contentRect;
            const last = lastSizeRef.current;
            if (!last || last.width !== width || last.height !== height) {
                lastSizeRef.current = { width, height };
                calculate();
            }
        });

        ro.observe(container);
        return () => ro.disconnect();
    }, [enabled, containerRef, calculate]);
}

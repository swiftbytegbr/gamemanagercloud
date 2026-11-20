import type { RefObject } from "react";
import { useCallback, useEffect, useRef } from "react";
import type { ScrollMemory } from "@/lib/serverSettings";

export function useScrollMemory(
    containerRef: RefObject<HTMLDivElement | null>,
    storageKey: string,
    canRestore: boolean,
): ScrollMemory {
    const savedScrollRef = useRef<number | null>(null);

    const remember = useCallback(() => {
        const current = containerRef.current?.scrollTop ?? null;
        savedScrollRef.current = current;
        if (current !== null) {
            try {
                sessionStorage.setItem(storageKey, String(current));
            } catch {
                /* ignore */
            }
        }
    }, [containerRef, storageKey]);

    useEffect(() => {
        if (!canRestore) return;
        const element = containerRef.current;
        if (!element) return;

        let restored = savedScrollRef.current;
        if (restored === null) {
            try {
                const stored = sessionStorage.getItem(storageKey);
                if (stored != null) restored = Number(stored);
            } catch {
                /* ignore */
            }
        }

        if (restored === null || Number.isNaN(restored)) return;

        const previousBehavior = element.style.scrollBehavior;
        element.style.scrollBehavior = "auto";
        element.scrollTop = restored;
        savedScrollRef.current = null;
        try {
            sessionStorage.removeItem(storageKey);
        } catch {
            /* ignore */
        }
        requestAnimationFrame(() => {
            element.style.scrollBehavior = previousBehavior;
        });
    }, [canRestore, containerRef, storageKey]);

    return { remember };
}

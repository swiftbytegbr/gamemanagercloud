import { useEffect, useRef, useState } from "react";
import { dialog, hasDialogByTag } from "@/store/dialogStore.tsx";
import { EasterEgg } from "@/components/easter-egg/EasterEgg.tsx";

// Ultra-hidden trigger: 1) Click corners TL -> TR -> BR -> BL within 6s
// Then 2) Type the sequence "future" within 10s

const SEQUENCE = "FUTURE";
const TAG = "clarke";

export function EasterEggGate() {
    const [stage, setStage] = useState<0 | 1>(0);
    const keyBufferRef = useRef<string>("");
    const timerRef = useRef<number | null>(null);
    const cornerTimerRef = useRef<number | null>(null);
    const cornerIndexRef = useRef<number>(0);
    const mountedRef = useRef(false);

    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
            if (timerRef.current) window.clearTimeout(timerRef.current);
            if (cornerTimerRef.current)
                window.clearTimeout(cornerTimerRef.current);
        };
    }, []);

    // Secret manual trigger: window.__cf_futura?.()
    useEffect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (window as any).__cf_futura = () => trigger();
        return () => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            delete (window as any).__cf_futura;
        };
    }, []);

    useEffect(() => {
        if (stage !== 1) return;
        const onKeyDown = (e: KeyboardEvent) => {
            if (hasDialogByTag(TAG)) return;

            if (e.metaKey || e.ctrlKey) return;
            const key = e.key.length === 1 ? e.key.toUpperCase() : e.key;
            if (key.length !== 1 && key !== "Enter") return;

            keyBufferRef.current += key;
            if (keyBufferRef.current.length > SEQUENCE.length) {
                keyBufferRef.current = keyBufferRef.current.slice(
                    -SEQUENCE.length,
                );
            }
            if (keyBufferRef.current.endsWith(SEQUENCE)) {
                trigger();
            }
        };

        window.addEventListener("keydown", onKeyDown);
        timerRef.current = window.setTimeout(() => {
            setStage(0);
            keyBufferRef.current = "";
        }, 10_000);
        return () => {
            window.removeEventListener("keydown", onKeyDown);
            if (timerRef.current) window.clearTimeout(timerRef.current);
            timerRef.current = null;
        };
    }, [stage]);

    function onCornerClick(expectedIdx: number) {
        if (hasDialogByTag(TAG)) return;
        if (stage !== 0) return;
        if (cornerIndexRef.current !== expectedIdx) {
            cornerIndexRef.current = 0;
            if (cornerTimerRef.current)
                window.clearTimeout(cornerTimerRef.current);
            return;
        }
        cornerIndexRef.current += 1;
        if (cornerIndexRef.current === 1) {
            cornerTimerRef.current = window.setTimeout(() => {
                cornerIndexRef.current = 0;
            }, 6_000);
        }
        if (cornerIndexRef.current >= 4) {
            if (cornerTimerRef.current)
                window.clearTimeout(cornerTimerRef.current);
            cornerTimerRef.current = null;
            cornerIndexRef.current = 0;
            setStage(1);
        }
    }

    function trigger() {
        setStage(0);
        keyBufferRef.current = "";
        if (timerRef.current) {
            window.clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        if (hasDialogByTag(TAG)) return;
        dialog(<EasterEgg />, { height: "lg", width: "lg", tag: TAG });
    }

    const common =
        "fixed z-[2147483647] w-8 h-8 opacity-0 hover:opacity-0 focus:opacity-0 outline-none cursor-default pointer-events-auto";

    return (
        <>
            {/* Top-left */}
            <button
                aria-hidden
                tabIndex={-1}
                className={`${common} left-0 top-0`}
                onClick={() => onCornerClick(0)}
            />
            {/* Top-right */}
            <button
                aria-hidden
                tabIndex={-1}
                className={`${common} right-0 top-0`}
                onClick={() => onCornerClick(1)}
            />
            {/* Bottom-right */}
            <button
                aria-hidden
                tabIndex={-1}
                className={`${common} right-0 bottom-0`}
                onClick={() => onCornerClick(2)}
            />
            {/* Bottom-left */}
            <button
                aria-hidden
                tabIndex={-1}
                className={`${common} left-0 bottom-0`}
                onClick={() => onCornerClick(3)}
            />
        </>
    );
}

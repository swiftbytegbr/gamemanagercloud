import { useEffect, useRef, useState, type ReactNode } from "react";

import {
    Popover,
    PopoverAnchor,
    PopoverContent,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils.ts";

type HoverPopoverProps = {
    children: ReactNode;
    content: ReactNode;
    delay?: number;
    disabled?: boolean;
    enableClickToOpen?: boolean;
    wide?: boolean;
};

export function HoverPopover({
    children,
    content,
    delay = 700,
    disabled = false,
    enableClickToOpen = false,
    wide = false,
}: HoverPopoverProps) {
    const [open, setOpen] = useState(false);
    const anchorRef = useRef<HTMLSpanElement | null>(null);
    const hoverTimer = useRef<number | null>(null);

    const clearHoverTimer = () => {
        if (hoverTimer.current !== null) {
            window.clearTimeout(hoverTimer.current);
            hoverTimer.current = null;
        }
    };

    useEffect(() => {
        return () => clearHoverTimer();
    }, []);

    const scheduleOpen = () => {
        if (disabled) return;
        clearHoverTimer();
        hoverTimer.current = window.setTimeout(
            () => setOpen(true),
            Math.max(0, delay),
        );
    };

    const closePopover = () => {
        clearHoverTimer();
        setOpen(false);
    };

    const handleOpenChange = (next: boolean) => {
        setOpen(next);
        if (!next) clearHoverTimer();
    };

    const handleMouseEnter = () => {
        if (disabled) return;
        if (delay < 0) return;
        scheduleOpen();
    };

    const handleMouseLeave = () => {
        if (delay < 0) return;
        closePopover();
    };

    return (
        <Popover open={open} onOpenChange={handleOpenChange}>
            <PopoverAnchor
                onClick={() => enableClickToOpen && handleOpenChange(!open)}
                asChild>
                <span
                    ref={anchorRef}
                    onMouseEnter={handleMouseEnter}
                    onMouseLeave={handleMouseLeave}>
                    {children}
                </span>
            </PopoverAnchor>
            {!disabled && (
                <PopoverContent
                    side="top"
                    align="start"
                    onPointerDownOutside={(event) => {
                        if (
                            enableClickToOpen &&
                            anchorRef.current?.contains(event.target as Node)
                        ) {
                            event.preventDefault();
                        }
                    }}
                    className={cn(
                        "max-w-xs",
                        wide && "min-w-[18rem] max-w-none",
                    )}>
                    <div className="text-sm leading-relaxed whitespace-pre-wrap">
                        {content}
                    </div>
                </PopoverContent>
            )}
        </Popover>
    );
}

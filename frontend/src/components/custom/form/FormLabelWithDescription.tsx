import { FormLabel } from "@/components/ui/form";
import {
    Popover,
    PopoverAnchor,
    PopoverContent,
} from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { Info } from "lucide-react";
import { useRef, useState } from "react";

type Props = {
    label: string;
    description?: string;
    required?: boolean;
    className?: string;
};

export function FormLabelWithDescription({
    label,
    description,
    required,
    className,
}: Props) {
    const [open, setOpen] = useState(false);
    const [pinned, setPinned] = useState(false);
    const hoverTimer = useRef<number | null>(null);

    const hoverTime = 700;

    const hasDescription = Boolean(
        description && description.trim().length > 0,
    );

    const clearHoverTimer = () => {
        if (hoverTimer.current) {
            window.clearTimeout(hoverTimer.current);
            hoverTimer.current = null;
        }
    };

    const onMouseEnter = () => {
        if (!hasDescription) return;
        clearHoverTimer();
        hoverTimer.current = window.setTimeout(() => {
            setOpen(true);
        }, hoverTime);
    };

    const onMouseLeave = () => {
        clearHoverTimer();
        if (!pinned) setOpen(false);
    };

    const onButtonClick = () => {
        if (!hasDescription) return;
        setPinned((prev) => {
            const next = !prev;
            setOpen(next || !open);
            return next;
        });
    };

    const handleOpenChange = (value: boolean) => {
        setOpen(value);
        if (!value) setPinned(false);
    };

    return (
        <Popover open={open} onOpenChange={handleOpenChange}>
            <PopoverAnchor asChild>
                <div
                    className={`flex items-center gap-2 w-full min-w-0 ${className ?? ""}`}
                    onMouseEnter={onMouseEnter}
                    onMouseLeave={onMouseLeave}>
                    <FormLabel className="leading-tight whitespace-normal break-words min-w-0">
                        {label}
                        {required ? " *" : ""}
                    </FormLabel>
                    {hasDescription && (
                        <Button
                            type="button"
                            variant="ghost"
                            size="smallIcon"
                            aria-label="Show field description"
                            onClick={onButtonClick}>
                            <Info className="size-4 text-muted-foreground" />
                        </Button>
                    )}
                </div>
            </PopoverAnchor>
            {hasDescription && (
                <PopoverContent side="top" align="start" className="max-w-xs">
                    <div className="text-sm leading-relaxed whitespace-pre-wrap">
                        {description}
                    </div>
                </PopoverContent>
            )}
        </Popover>
    );
}

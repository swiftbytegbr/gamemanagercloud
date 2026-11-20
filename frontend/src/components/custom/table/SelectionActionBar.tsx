import { Button } from "@/components/ui/button.tsx";
import type { ReactNode } from "react";

type SelectionActionBarProps = {
    selectedCount: number;
    entityLabel: string;
    onClearSelection: () => void;
    children?: ReactNode;
};

export function SelectionActionBar(props: SelectionActionBarProps) {
    if (props.selectedCount === 0) {
        return null;
    }

    const label =
        props.selectedCount === 1
            ? `${props.selectedCount} ${props.entityLabel} selected`
            : `${props.selectedCount} ${props.entityLabel}s selected`;

    return (
        <div className="sticky bottom-0 left-0 right-0 z-10 border-t border-border bg-background px-5 py-3 backdrop-blur supports-[backdrop-filter]:bg-background/90 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <p className="text-sm font-medium">{label}</p>
            <div className="flex flex-wrap gap-2 items-center">
                {props.children}
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={props.onClearSelection}>
                    Clear
                </Button>
            </div>
        </div>
    );
}

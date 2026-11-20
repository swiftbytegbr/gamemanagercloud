import type { ReactNode } from "react";
import { cn } from "@/lib/utils.ts";

interface CenteredProps {
    className?: string;
    children: ReactNode;
}

export function Centered(props: CenteredProps) {
    return (
        <div
            className={cn(
                "h-full w-full flex items-center justify-center",
                props.className,
            )}>
            {props.children}
        </div>
    );
}

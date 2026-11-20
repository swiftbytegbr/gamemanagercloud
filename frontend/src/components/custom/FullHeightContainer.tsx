import type { ReactNode } from "react";
import { cn } from "@/lib/utils.ts";

export function FullHeightContainer(props: {
    children?: ReactNode;
    className?: string;
    childClassName?: string;
}) {
    return (
        <div className={cn("w-full h-full", props.className)}>
            <div
                className={cn(
                    "w-full h-full bg-secondary rounded-md p-5",
                    props.childClassName,
                )}>
                {props.children}
            </div>
        </div>
    );
}

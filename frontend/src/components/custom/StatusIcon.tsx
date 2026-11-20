import type { NodeStatus } from "@/store/nodeStore.ts";
import type { ServerState } from "@/store/serverStore.ts";
import { cn, statusColorBgMap } from "@/lib/utils.ts";
import type { ReactNode } from "react";
import { cva, type VariantProps } from "class-variance-authority";

type StatusIconProps = {
    status: NodeStatus | ServerState;
    className?: string;
    children?: ReactNode;
} & VariantProps<typeof statusIconVariants>;

const statusIconVariants = cva(
    "flex justify-center items-center aspect-square",
    {
        variants: {
            size: {
                sm: "h-2.5 w-2.5",
                md: "h-4 w-4",
                lg: "h-6 w-6",
            },
            box: {
                true: "rounded-[0.25rem]",
                false: "rounded-full",
            },
        },
        defaultVariants: {
            size: "sm",
            box: false,
        },
    },
);

export function StatusIcon({
    status,
    className,
    children,
    size,
    box,
}: StatusIconProps) {
    const colorClass = statusColorBgMap[status];

    return (
        <div
            className={cn(
                statusIconVariants({ size, box }),
                colorClass,
                className,
            )}>
            {children}
        </div>
    );
}

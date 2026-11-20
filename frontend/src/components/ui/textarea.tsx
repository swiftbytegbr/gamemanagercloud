import * as React from "react";

import { cn } from "@/lib/utils";
import { cva, type VariantProps } from "class-variance-authority";

const textareaVariants = cva(
    "focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive flex field-sizing-content min-h-16 w-full min-w-0 rounded-md border px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
    {
        variants: {
            variant: {
                default:
                    "border-input placeholder:text-muted-foreground bg-transparent dark:bg-input/30",
                cutOut: "bg-background",
            },
            resize: {
                true: "resize-y",
                false: "resize-none overflow-y-auto",
            },
            height: {
                small: "h-16",
                medium: "h-24",
                large: "h-36",
                auto: "h-auto",
            },
        },
        defaultVariants: {
            variant: "default",
            resize: true,
            height: "auto",
        },
    },
);

function Textarea({
    className,
    variant,
    resize,
    height,
    ...props
}: React.ComponentProps<"textarea"> & VariantProps<typeof textareaVariants>) {
    return (
        <textarea
            data-slot="textarea"
            className={cn(
                textareaVariants({ variant, resize, height, className }),
            )}
            {...props}
        />
    );
}

export { Textarea };

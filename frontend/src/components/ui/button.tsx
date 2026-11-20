import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
    "inline-flex items-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",
    {
        variants: {
            variant: {
                default:
                    "bg-primary text-primary-foreground shadow-xs hover:bg-primary/80",
                destructive:
                    "bg-destructive-foreground text-white shadow-xs hover:bg-destructive-foreground/90 focus-visible:ring-destructive-foreground/20 dark:focus-visible:ring-destructive-foreground/40 dark:bg-destructive-foreground/60 dark:hover:bg-destructive-foreground/50",
                /*outline:
                    "border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:bg-input/30 dark:border-input dark:hover:bg-input/50",*/
                outline:
                    "border border-primary/50 hover:border-primary hover:bg-secondary/30 bg-transparent text-primary shadow-xs",
                input: "border border-input hover:border-input/90 hover:bg-input/10 bg-transparent text-muted-foreground shadow-xs",
                secondary:
                    "text-secondary-foreground shadow-xs hover:bg-secondary",
                tertiary:
                    "text-tertiary-foreground shadow-xs bg-tertiary hover:bg-tertiary/90",
                serverStart:
                    "text-white bg-green-600 shadow-xs hover:bg-green-600/90",
                serverStop:
                    "text-white bg-red-600 shadow-xs hover:bg-red-600/90",
                serverRestart:
                    "text-white bg-orange-500 shadow-xs hover:bg-orange-500/90",
                navbar: "text-tertiary-foreground shadow-xs hover:bg-tertiary",
                //ghost: "hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",
                ghost: "",
                link: "text-primary underline-offset-4 hover:underline",
                cutout: "bg-background shadow-md hover:shadow-lg hover:bg-tertiary/50",
            },
            size: {
                default: "h-9 px-4 py-2 has-[>svg]:px-3",
                tiny: "h-6 rounded-sm px-3",
                sm: "h-8 rounded-md gap-1.5 px-3 has-[>svg]:px-2.5",
                lg: "h-10 rounded-md px-6 has-[>svg]:px-4",
                icon: "size-9",
                smallIcon: "size-7",
            },
            justify: {
                start: "justify-start",
                center: "justify-center",
                end: "justify-end",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
            justify: "center",
        },
    },
);

function Button({
    className,
    variant,
    size,
    justify,
    asChild = false,
    ...props
}: React.ComponentProps<"button"> &
    VariantProps<typeof buttonVariants> & {
        asChild?: boolean;
    }) {
    const Comp = asChild ? Slot : "button";

    return (
        <Comp
            data-slot="button"
            className={cn(
                buttonVariants({ variant, size, justify, className }),
            )}
            {...props}
        />
    );
}

export { Button };

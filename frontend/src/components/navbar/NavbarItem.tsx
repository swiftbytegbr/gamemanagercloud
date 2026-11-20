import { Button } from "@/components/ui/button.tsx";
import type { ComponentType } from "react";
import { cn } from "@/lib/utils.ts";
import { router } from "@/Router.tsx";

interface NavbarItemProps {
    link: string;
    active?: boolean;
    disabled?: boolean;
    icon: ComponentType<{ className?: string }>;
    label: string;
    onClick?: () => void;
    className?: string;
}

export function NavbarItem(props: NavbarItemProps) {
    return (
        <Button
            variant="navbar"
            size="lg"
            justify="start"
            disabled={props.disabled}
            className={cn(
                "w-full uppercase text-xl",
                props.active && "bg-tertiary",
                props.className,
            )}
            onClick={() => {
                if (props.onClick) {
                    props.onClick();
                }
                router.navigate({ to: `${props.link}` });
            }}>
            <props.icon className="size-5" />
            {props.label}
        </Button>
    );
}

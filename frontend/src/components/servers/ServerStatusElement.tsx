import { cn, statusColorTextMap } from "@/lib/utils.ts";
import type { Server } from "@/store/serverStore.ts";

type ServerStatusElementProps = {
    server: Server;
    hiddenWhenSmall?: boolean;
};

export function ServerStatusElement(props: ServerStatusElementProps) {
    return (
        <div
            className={cn(
                "items-center gap-3",
                props.hiddenWhenSmall ? "hidden 2xl:flex" : "flex",
            )}>
            <p className="text-2xl font-semibold uppercase">Status</p>
            <p className={statusColorTextMap[props.server.state]}>
                {props.server.state}
            </p>
        </div>
    );
}

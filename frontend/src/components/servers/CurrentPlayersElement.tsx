import { Spinner } from "@/components/ui/spinner.tsx";
import type { Server } from "@/store/serverStore.ts";
import { cn } from "@/lib/utils.ts";

type CurrentPlayersElementProps = {
    isProfileLoading: boolean;
    server: Server;
    maxPlayers: number;
    hiddenWhenSmall?: boolean;
};

export function CurrentPlayersElement(props: CurrentPlayersElementProps) {
    return (
        <div
            className={cn(
                "items-center gap-3",
                props.hiddenWhenSmall ? "hidden 2xl:flex" : "flex",
            )}>
            <p className="text-2xl font-semibold uppercase">Players</p>
            <p>
                {props.isProfileLoading ? (
                    <Spinner size="small" />
                ) : (
                    <>{`${props.server.onlinePlayers ?? 0}/${props.maxPlayers}`}</>
                )}
            </p>
        </div>
    );
}

import { Separator } from "@/components/ui/separator.tsx";
import type { Server } from "@/store/serverStore.ts";
import { Button } from "@/components/ui/button.tsx";
import { formatDate } from "@/lib/formatters.ts";

type ServerInfoPopoverContentProps = {
    server: Server;
};

export function ServerInfoPopoverContent(props: ServerInfoPopoverContentProps) {
    const primaryIp = props.server.serverIp;

    const joinAddress = primaryIp
        ? `${primaryIp}:${props.server.gameType === "ARK_EVOLVED" ? (props.server.queryPort ?? "NaN") : props.server.serverPort}`
        : "Unavailable";

    const createdLabel = formatDate(props.server.created);

    return (
        <div className="flex flex-col gap-4 text-sm leading-relaxed">
            <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Server Details
                </p>
                <dl className="mt-2 grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
                    <dt className="text-muted-foreground">Node</dt>
                    <dd className="font-mono text-xs">
                        {props.server.nodeName}
                    </dd>
                    <dt className="text-muted-foreground">Node ID</dt>
                    <dd className="font-mono text-xs">{props.server.nodeId}</dd>
                    <dt className="text-muted-foreground">Server ID</dt>
                    <dd className="font-mono text-xs">{props.server.id}</dd>
                    <dt className="text-muted-foreground">Install Directory</dt>
                    <dd className="font-mono text-xs overflow-x-auto">{`${props.server.serverDirectory.replace("/", "\\")}\\${props.server.id}`}</dd>
                    {createdLabel && (
                        <>
                            <dt className="text-muted-foreground">Created</dt>
                            <dd className="font-mono text-xs">
                                {createdLabel}
                            </dd>
                        </>
                    )}
                </dl>
            </div>

            <Separator />

            <div className="flex flex-col gap-3">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        Connection
                    </p>
                    <dl className="mt-2 grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
                        <dt className="text-muted-foreground">IP Address</dt>
                        <dd className="font-mono text-xs">
                            {props.server.serverIp ?? "Unavailable"}
                        </dd>
                        <dt className="text-muted-foreground">Join Address</dt>
                        <dd className="font-mono text-xs">
                            {joinAddress}
                            {props.server.gameType === "ARK_EVOLVED" &&
                                props.server.queryPort && (
                                    <Button
                                        className="ml-4"
                                        variant="tertiary"
                                        onClick={() =>
                                            (window.location.href = `steam://connect/${props.server.serverIp}:${props.server.queryPort}`)
                                        }
                                        size="tiny">
                                        Join
                                    </Button>
                                )}
                        </dd>
                    </dl>
                </div>
            </div>
        </div>
    );
}

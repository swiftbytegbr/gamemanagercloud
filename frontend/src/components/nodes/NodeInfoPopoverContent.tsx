import { Separator } from "@/components/ui/separator.tsx";
import { uniqueStrings } from "@/lib/collections.ts";
import { formatDate, formatGbFromMb } from "@/lib/formatters.ts";
import { isLocalOrPrivateIpv4 } from "@/lib/network.ts";
import type { Node } from "@/store/nodeStore.ts";

type NodeInfoPopoverContentProps = {
    node: Node;
};

export function NodeInfoPopoverContent(props: NodeInfoPopoverContentProps) {
    const hostname = props.node.systemData.hostname ?? "Unavailable";
    const externalIps = uniqueStrings(
        (props.node.systemData.ipAddresses ?? []).filter(
            (ip) => !isLocalOrPrivateIpv4(ip),
        ),
    );
    const cpuName = props.node.systemData.cpu.name ?? "Unavailable";
    const ramLabel = formatGbFromMb(props.node.systemData.ram) ?? "Unavailable";
    const lastLoginLabel =
        props.node.status === "OFFLINE"
            ? (formatDate(props.node.lastLogin) ?? "Unavailable")
            : null;

    return (
        <div className="flex flex-col gap-4 text-sm leading-relaxed">
            <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Node Details
                </p>
                <dl className="mt-2 grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
                    <dt className="text-muted-foreground">Node ID</dt>
                    <dd className="font-mono text-xs">{props.node.id}</dd>
                    <dt className="text-muted-foreground">Hostname</dt>
                    <dd className="font-mono text-xs">{hostname}</dd>
                    <dt className="text-muted-foreground">IP Addresses</dt>
                    <dd className="font-mono text-xs">
                        {externalIps.length ? (
                            <ul className="list-disc pl-4">
                                {externalIps.map((ip) => (
                                    <li key={ip}>{ip}</li>
                                ))}
                            </ul>
                        ) : (
                            "Unavailable"
                        )}
                    </dd>
                    <dt className="text-muted-foreground">Last Login</dt>
                    <dd className="font-mono text-xs">{lastLoginLabel}</dd>
                </dl>
            </div>

            <Separator />

            <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Hardware
                </p>
                <dl className="mt-2 grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
                    <dt className="text-muted-foreground">Available RAM</dt>
                    <dd className="font-mono text-xs">{ramLabel}</dd>
                    <dt className="text-muted-foreground">CPU</dt>
                    <dd className="font-mono text-xs">{cpuName}</dd>
                </dl>
            </div>
        </div>
    );
}

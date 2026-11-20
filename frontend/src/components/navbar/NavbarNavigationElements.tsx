import { NavbarItem } from "@/components/navbar/NavbarItem.tsx";
import { CalendarSync, Gauge, Network, Server, Users } from "lucide-react";
import { useCallback } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { hasPermission, useActiveTeam } from "@/store/teamStore.ts";
import { useNodeQuery } from "@/store/nodeStore.ts";
import { cn, nodeStateOrder, serverStateOrder } from "@/lib/utils.ts";
import { StatusIcon } from "@/components/custom/StatusIcon.tsx";
import { useServerQuery } from "@/store/serverStore.ts";
import { useUserQuery } from "@/store/userStore.ts";

export function NavbarNavigationElements(props: {
    onElementClick?: () => void;
}) {
    const { location } = useRouterState();

    const activeTeam = useActiveTeam();

    const { data: user } = useUserQuery();
    const { data: nodes } = useNodeQuery(activeTeam);
    const { data: servers } = useServerQuery(activeTeam);

    const isActive = useCallback(
        (
            navbarElement:
                "dashboard" | "server" | "node" | "team" | "automation",
        ) => {
            return location.pathname.includes(`/${navbarElement}`);
        },
        [location.pathname],
    );

    const isNodeActive = useCallback(
        (nodeId: string) => {
            return location.pathname.includes(`/node/${nodeId}`);
        },
        [location.pathname],
    );

    const isServerActive = useCallback(
        (serverId: string) => {
            return location.pathname.includes(`/server/${serverId}`);
        },
        [location.pathname],
    );

    const getSubPath = useCallback(() => {
        const possibleSubPaths = ["settings", "rcon", "backups"];
        const subPath = location.pathname.split("/").pop();
        return possibleSubPaths.includes(subPath ?? "settings")
            ? subPath
            : "settings";
    }, [location.pathname]);

    const sortedServers = servers
        ? [...servers].sort(
              (a, b) =>
                  serverStateOrder.indexOf(a.state) -
                  serverStateOrder.indexOf(b.state),
          )
        : [];

    const gameTypeLabels: Record<string, string> = {
        ARK_ASCENDED: "ARK: Survival Ascended",
        ARK_EVOLVED: "ARK: Survival Evolved",
    };

    const gameTypeOrder = ["ARK_EVOLVED", "ARK_ASCENDED"] as const;

    const sortedNodes = nodes
        ? [...nodes].sort(
              (a, b) =>
                  nodeStateOrder.indexOf(a.status) -
                  nodeStateOrder.indexOf(b.status),
          )
        : [];

    return (
        <>
            <NavbarItem
                label="Dashboard"
                link="/dashboard"
                icon={Gauge}
                active={isActive("dashboard")}
                onClick={props.onElementClick}
            />
            {hasPermission(activeTeam, user?.id, "ACCESS_SERVERS") && (
                <div
                    className={
                        isActive("server") && servers?.length != 0
                            ? "bg-background rounded-t-md rounded-b-md"
                            : ""
                    }>
                    <NavbarItem
                        label="Servers"
                        link="/server"
                        icon={Server}
                        active={isActive("server")}
                        onClick={props.onElementClick}
                    />
                    {sortedServers && sortedServers.length > 0 && (
                        <div
                            className={cn(
                                "overflow-y-auto transition-all",
                                isActive("server")
                                    ? "py-1 max-h-50"
                                    : "max-h-0",
                            )}>
                            {gameTypeOrder.map((gt, index) => {
                                const grouped = sortedServers.filter(
                                    (s) => s.gameType === gt,
                                );
                                if (grouped.length === 0) return null;
                                return (
                                    <div key={gt}>
                                        <div
                                            className={cn(
                                                "ml-6 mt-2 mb-1 text-xs font-medium text-muted",
                                                index !== 0 ? "pt-2" : "",
                                            )}>
                                            {gameTypeLabels[gt] ?? gt}
                                        </div>
                                        {grouped.map((server) => (
                                            <Link
                                                className={cn(
                                                    "flex items-center gap-3 m-2 ml-6 text-base leading-6",
                                                    isServerActive(server.id)
                                                        ? "text-secondary-foreground"
                                                        : "text-muted-secondary",
                                                )}
                                                to={`/server/${server.id}/${getSubPath()}`}
                                                key={server.id}>
                                                <StatusIcon
                                                    status={server.state}
                                                />
                                                {server.displayName}
                                            </Link>
                                        ))}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
            {hasPermission(activeTeam, user?.id, "ACCESS_NODES") && (
                <div
                    className={
                        isActive("node") && nodes?.length != 0
                            ? "bg-background rounded-t-md rounded-b-md"
                            : ""
                    }>
                    <NavbarItem
                        label="Nodes"
                        link="/node"
                        icon={Network}
                        active={isActive("node")}
                        onClick={props.onElementClick}
                    />
                    {sortedNodes && sortedNodes?.length > 0 && (
                        <div
                            className={cn(
                                "overflow-y-auto transition-all",
                                isActive("node") ? "py-1 max-h-50" : "max-h-0",
                            )}>
                            {sortedNodes.map((node) => (
                                <Link
                                    className={cn(
                                        "flex items-center gap-3 m-2 ml-6 text-base leading-6",
                                        isNodeActive(node.id)
                                            ? "text-secondary-foreground"
                                            : "text-muted-secondary",
                                    )}
                                    to={`/node/${node.id}/settings`}
                                    key={node.id}>
                                    <StatusIcon status={node.status} />
                                    {node.settings.name}
                                </Link>
                            ))}
                        </div>
                    )}
                </div>
            )}
            <NavbarItem
                label="Automations"
                link="/automation"
                icon={CalendarSync}
                active={isActive("automation")}
                onClick={props.onElementClick}
            />
            <NavbarItem
                label="Team"
                link="/team"
                icon={Users}
                active={isActive("team")}
                onClick={props.onElementClick}
            />
        </>
    );
}

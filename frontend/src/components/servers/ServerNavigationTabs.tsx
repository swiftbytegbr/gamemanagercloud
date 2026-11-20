import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { Monitor, Save, Server, Settings, SquarePlus } from "lucide-react";
import { useCallback } from "react";
import { router } from "@/Router.tsx";
import { toast } from "sonner";
import { useServerQuery } from "@/store/serverStore.ts";
import { useParams, useRouterState } from "@tanstack/react-router";
import { useActiveTeam } from "@/store/teamStore.ts";
import { useNodeQuery } from "@/store/nodeStore.ts";
import { CreateServerDialog } from "@/components/servers/dialogs/CreateServerDialog.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { serverStateOrder } from "@/lib/utils.ts";

export function ServerNavigationTabs() {
    const activeTeam = useActiveTeam();
    const { data: servers } = useServerQuery(activeTeam);
    const { data: nodes } = useNodeQuery(activeTeam);

    const { location } = useRouterState();
    const params = useParams({ strict: false });

    const getActiveTab = useCallback(() => {
        if (location.pathname.includes("/settings")) return "settings";
        else if (location.pathname.includes("/rcon")) return "rcon";
        else if (location.pathname.includes("/backups")) return "backups";
        else return "servers";
    }, [location.pathname]);

    const setActiveTab = (tab: string) => {
        const sortedServers = servers
            ? [...servers].sort(
                  (a, b) =>
                      serverStateOrder.indexOf(a.state) -
                      serverStateOrder.indexOf(b.state),
              )
            : [];

        const serverId: string = params.serverId ?? sortedServers?.[0]?.id;

        if (!serverId && tab !== "servers" && tab !== "create") {
            router.navigate({ to: "/server" });
            toast.warning(
                "You need to create a server before you can access other tabs.",
            );
            return;
        }

        const routes: Record<string, () => void> = {
            create: () => {
                if (nodes?.length === 0) {
                    toast.warning(
                        "You need to create a node before you can create a server.",
                    );
                    return;
                }
                dialog(<CreateServerDialog />);
            },
            settings: () =>
                router.navigate({
                    to: "/server/$serverId/settings",
                    params: { serverId },
                }),
            rcon: () =>
                router.navigate({
                    to: "/server/$serverId/rcon",
                    params: { serverId },
                }),
            backups: () =>
                router.navigate({
                    to: "/server/$serverId/backups",
                    params: { serverId },
                }),
        };

        if (routes[tab]) {
            routes[tab]!();
        } else {
            router.navigate({ to: "/server" });
        }
    };

    return (
        <>
            <Tabs
                value={getActiveTab()}
                onValueChange={setActiveTab}
                className="h-auto mt-auto ml-auto lg:ml-0"
                activationMode="manual">
                <TabsList>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="servers">
                        <Server className="size-5" />
                        <p className="hidden lg:block">Servers</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="settings">
                        <Settings className="size-5" />
                        <p className="hidden lg:block">Settings</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="rcon">
                        <Monitor className="size-5" />
                        <p className="hidden lg:block">RCON</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="backups">
                        <Save className="size-5" />
                        <p className="hidden lg:block">Backups</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="create">
                        <SquarePlus className="size-5" />
                        <p className="hidden lg:block">Add Server</p>
                    </TabsTrigger>
                </TabsList>
            </Tabs>
        </>
    );
}

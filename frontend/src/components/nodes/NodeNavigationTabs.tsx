import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { Network, Settings, SquarePlus } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { router } from "@/Router.tsx";
import { toast } from "sonner";
import { useParams, useRouterState } from "@tanstack/react-router";
import { useActiveTeam } from "@/store/teamStore.ts";
import { type Node, useNodeQuery } from "@/store/nodeStore.ts";
import { CreateNodeDialog } from "@/components/nodes/CreateNodeDialog.tsx";
import { closeDialogsByTag, dialog } from "@/store/dialogStore.tsx";
import { nodeStateOrder } from "@/lib/utils.ts";
import { InstallCertInfoDialog } from "@/components/nodes/InstallCertInfoDialog.tsx";

export function NodeNavigationTabs() {
    const activeTeam = useActiveTeam();
    const { data: nodes } = useNodeQuery(activeTeam);

    const { location } = useRouterState();
    const params = useParams({ strict: false });

    const [nodeCreateNode, setNodeCreateNode] = useState<Node | null>(null);

    useEffect(() => {
        if (nodeCreateNode) {
            const createNode: Node | undefined = nodes?.find(
                (node) => node.id === nodeCreateNode.id,
            );
            if (createNode && createNode?.status != "CREATION") {
                closeDialogsByTag("createNodeDialog");
                setNodeCreateNode(null);
                dialog(<InstallCertInfoDialog />, {
                    tag: "installCertInfoDialog",
                    modal: true,
                });
            }
        }
    }, [nodeCreateNode, nodes]);

    const getActiveTab = useCallback(() => {
        if (location.pathname.includes("/settings")) return "settings";
        else return "nodes";
    }, [location.pathname]);

    const setActiveTab = (tab: string) => {
        const sortedNodes = nodes
            ? [...nodes].sort(
                  (a, b) =>
                      nodeStateOrder.indexOf(a.status) -
                      nodeStateOrder.indexOf(b.status),
              )
            : [];

        const nodeId: string = params.nodeId ?? sortedNodes?.[0]?.id;

        if (!nodeId && tab !== "nodes" && tab !== "create") {
            router.navigate({ to: "/node" });
            toast.warning(
                "You need to create a node before you can access other tabs.",
            );
            return;
        }

        const routes: Record<string, () => void> = {
            create: () => {
                dialog(
                    <CreateNodeDialog
                        node={nodeCreateNode}
                        setNode={setNodeCreateNode}
                    />,
                    {
                        tag: "createNodeDialog",
                        onClose: () => setNodeCreateNode(null),
                    },
                );
            },
            settings: () =>
                router.navigate({
                    to: "/node/$nodeId/settings",
                    params: { nodeId },
                }),
        };

        if (routes[tab]) {
            routes[tab]!();
        } else {
            router.navigate({ to: "/node" });
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
                        value="nodes">
                        <Network className="size-5" />
                        <p className="hidden lg:block">Nodes</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="settings">
                        <Settings className="size-5" />
                        <p className="hidden lg:block">Settings</p>
                    </TabsTrigger>
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="create">
                        <SquarePlus className="size-5" />
                        <p className="hidden lg:block">Add Node</p>
                    </TabsTrigger>
                </TabsList>
            </Tabs>
        </>
    );
}

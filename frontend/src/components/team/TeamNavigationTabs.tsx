import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { ListChecks, Settings, Users } from "lucide-react";
import { useCallback } from "react";
import { router } from "@/Router.tsx";
import { useRouterState } from "@tanstack/react-router";
import { hasPermission, useActiveTeam } from "@/store/teamStore.ts";
import { useUserQuery } from "@/store/userStore.ts";

export function TeamNavigationTabs() {
    const { location } = useRouterState();
    const { data: user } = useUserQuery();
    const activeTeam = useActiveTeam();

    const getActiveTab = useCallback(() => {
        if (
            location.pathname.includes("/settings") &&
            hasPermission(activeTeam, user?.id, "MANAGE_TEAM")
        )
            return "settings";
        if (location.pathname.includes("/action-log")) return "action-log";
        return "members";
    }, [activeTeam, location.pathname, user?.id]);

    const setActiveTab = (tab: string) => {
        const routes: Record<string, () => void> = {
            members: () => router.navigate({ to: "/team/members" }),
            "action-log": () => router.navigate({ to: "/team/action-log" }),
            settings: () => router.navigate({ to: "/team/settings" }),
        };

        if (routes[tab]) {
            routes[tab]!();
        } else {
            router.navigate({ to: "/team/members" });
        }
    };

    return (
        <Tabs
            value={getActiveTab()}
            onValueChange={setActiveTab}
            className="h-auto mt-auto ml-auto lg:ml-0"
            activationMode="manual">
            <TabsList>
                <TabsTrigger
                    className="text-xl gap-2 uppercase"
                    value="members">
                    <Users className="size-5" />
                    <p className="hidden lg:block">Members</p>
                </TabsTrigger>
                <TabsTrigger
                    className="text-xl gap-2 uppercase"
                    value="action-log">
                    <ListChecks className="size-5" />
                    <p className="hidden lg:block">Action Log</p>
                </TabsTrigger>
                {hasPermission(activeTeam, user?.id, "MANAGE_TEAM") && (
                    <TabsTrigger
                        className="text-xl gap-2 uppercase"
                        value="settings">
                        <Settings className="size-5" />
                        <p className="hidden lg:block">Settings</p>
                    </TabsTrigger>
                )}
            </TabsList>
        </Tabs>
    );
}

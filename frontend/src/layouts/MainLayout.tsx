import { Outlet, useRouterState } from "@tanstack/react-router";
import { UserDropDownMenu } from "@/components/layout/UserDropDownMenu.tsx";
import { AuthGuard } from "@/controller/AuthGuard.tsx";
import { useUserQuery } from "@/store/userStore.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useActiveTeam, useTeamQuery } from "@/store/teamStore.ts";
import { useEffect } from "react";
import { queryClient } from "@/store/queryConfig.ts";
import { Navbar } from "@/components/navbar/Navbar.tsx";
import { ServerNavigationTabs } from "@/components/servers/ServerNavigationTabs.tsx";
import { NodeNavigationTabs } from "@/components/nodes/NodeNavigationTabs.tsx";
import { TeamNavigationTabs } from "@/components/team/TeamNavigationTabs.tsx";
import { StompSubscriber } from "@/controller/StompSubscriber.tsx";
import { useNodeQuery } from "@/store/nodeStore.ts";
import { useServerQuery } from "@/store/serverStore.ts";
import { cn } from "@/lib/utils.ts";
import { useAuthStore } from "@/store/authStore.ts";
import { CreateTeamForm } from "@/components/start-popups/CreateTeamForm.tsx";
import { dialog, hasDialogByTag } from "@/store/dialogStore.tsx";
import { VerifyEmailForm } from "@/components/start-popups/VerifyEmailForm.tsx";

export const MainLayout = () => {
    const { data: user, isLoading: isLoadingUser } = useUserQuery();
    const { data: teams, isLoading: isLoadingTeams } = useTeamQuery();
    const activeTeam = useActiveTeam();
    const { isLoading: isLoadingNodes } = useNodeQuery(activeTeam);
    const { isLoading: isLoadingServers } = useServerQuery(activeTeam);

    const { location } = useRouterState();
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

    const isLoading =
        isLoadingUser || isLoadingTeams || isLoadingNodes || isLoadingServers;

    useEffect(() => {
        queryClient.invalidateQueries().then();
    }, []);

    useEffect(() => {
        if (isLoading) return;
        if (!isAuthenticated) return;
        if (!user) return;

        if (!user.emailVerified && !hasDialogByTag("verify-email")) {
            dialog(<VerifyEmailForm user={user} />, {
                modal: true,
                disableCloseButton: true,
                tag: "verify-email",
            });
        }
    }, [isLoading, isAuthenticated, user]);

    useEffect(() => {
        if (isLoading) return;
        if (!isAuthenticated) return;
        if (!user) return;

        const noTeams = (teams?.length ?? 0) === 0;
        if (user.emailVerified && noTeams && !hasDialogByTag("create-team")) {
            dialog(<CreateTeamForm user={user} />, {
                modal: true,
                disableCloseButton: true,
                tag: "create-team",
            });
        }
    }, [isLoading, isAuthenticated, user, teams]);

    return (
        <AuthGuard guard="authenticated">
            {isLoading ? (
                <div className="h-full flex justify-center items-center">
                    <Spinner size="large" />
                </div>
            ) : (
                <>
                    <StompSubscriber>
                        <div className="grid grid-cols-[4rem_1fr_4rem] lg:grid-cols-[16rem_1fr] grid-rows-[4.5rem_1fr] h-screen w-screen">
                            <div className="hidden lg:block bg-secondary" />
                            <Navbar />
                            <div className="col-span-2 lg:col-span-1 bg-secondary p-4 pl-0 lg:pl-4 pb-0 flex pr-7">
                                {location.pathname.includes("/server") && (
                                    <ServerNavigationTabs />
                                )}
                                {location.pathname.includes("/node") && (
                                    <NodeNavigationTabs />
                                )}
                                {location.pathname.includes("/team") && (
                                    <TeamNavigationTabs />
                                )}
                                <UserDropDownMenu />
                            </div>
                            <div
                                className={cn(
                                    "flex flex-col p-7 col-span-3 lg:col-span-1 lg:order-3 overflow-hidden min-h-auto lg:h-auto",
                                    location.pathname.includes("/dashboard")
                                        ? "h-fit"
                                        : "h-auto",
                                )}>
                                <Outlet />
                            </div>
                        </div>
                    </StompSubscriber>
                </>
            )}
        </AuthGuard>
    );
};

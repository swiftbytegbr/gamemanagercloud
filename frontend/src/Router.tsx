import {
    createRootRoute,
    createRoute,
    Navigate,
    Outlet,
    Router,
    useParams,
} from "@tanstack/react-router";
import { LoginPage } from "@/pages/login/LoginPage.tsx";
import { MainLayout } from "@/layouts/MainLayout.tsx";
import { DashboardPage } from "@/pages/app/dashboard/DashboardPage.tsx";
import { LogoutPage } from "@/pages/logout/LogoutPage.tsx";
import { SignupPage } from "@/pages/signup/SignupPage.tsx";
import { ImprintPage } from "@/pages/legal/ImprintPage.tsx";
import { PrivacyPolicyPage } from "@/pages/legal/PrivacyPolicyPage.tsx";
import { TOSPage } from "@/pages/legal/TOSPage.tsx";
import { NotFoundPage } from "@/pages/NotFoundPage.tsx";
import { ResetPasswordPage } from "@/pages/reset-password/ResetPasswordPage.tsx";
import { NodePage } from "@/pages/app/node/NodePage.tsx";
import { NodeSettingsPage } from "@/pages/app/node/[nodeId]/NodeSettingsPage.tsx";
import { ServerPage } from "@/pages/app/server/ServerPage.tsx";
import { ServerSettingsPage } from "@/pages/app/server/[serverId]/ServerSettingsPage.tsx";
import { ServerRCONPage } from "@/pages/app/server/[serverId]/ServerRCONPage.tsx";
import { ServerBackupsPage } from "@/pages/app/server/[serverId]/ServerBackupsPage.tsx";
import { DialogProvider } from "@/components/DialogProvider.tsx";
import { RequirePermission } from "@/controller/RequirePermission.tsx";
import { TeamMembersPage } from "@/pages/app/team/TeamMembersPage.tsx";
import { TeamActionLogPage } from "@/pages/app/team/TeamActionLogPage.tsx";
import { RootErrorPage } from "@/pages/RootErrorPage.tsx";
import { TeamSettingsPage } from "@/pages/app/team/TeamSettingsPage.tsx";
import { AuthGuard } from "@/controller/AuthGuard.tsx";

const rootRoute = createRootRoute({
    component: () => (
        <>
            <DialogProvider />
            <Outlet />
        </>
    ),
    notFoundComponent: NotFoundPage,
    errorComponent: RootErrorPage,
});

const indexRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/",
    component: () => <AuthGuard />,
});

const loginRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/login",
    component: LoginPage,
});

const logoutRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/logout",
    component: LogoutPage,
});

const signupRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/signup",
    component: SignupPage,
});

const resetPasswordRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/reset-password",
    component: ResetPasswordPage,
});

const imprintRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/imprint",
    component: ImprintPage,
});

const privacyPolicyRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/privacy-policy",
    component: PrivacyPolicyPage,
});

const tosRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: "/terms-of-service",
    component: TOSPage,
});

const authenticatedRoute = createRoute({
    getParentRoute: () => rootRoute,
    id: "authenticated",
    component: MainLayout,
    notFoundComponent: NotFoundPage,
});

const dashboardRoute = createRoute({
    getParentRoute: () => authenticatedRoute,
    path: "/dashboard",
    component: DashboardPage,
});

/* --------- /server Routes --------- */
const serverRoute = createRoute({
    getParentRoute: () => authenticatedRoute,
    path: "/server",
    component: () => <RequirePermission permission="ACCESS_SERVERS" />,
    notFoundComponent: NotFoundPage,
});

const serverIndexRoute = createRoute({
    getParentRoute: () => serverRoute,
    path: "/",
    component: ServerPage,
});

const serverRedirectRoute = createRoute({
    getParentRoute: () => serverRoute,
    path: "/$serverId",
    component: () => <Navigate to="./settings" />,
});

const serverSettingsRoute = createRoute({
    getParentRoute: () => serverRoute,
    path: "/$serverId/settings",
    component: function ServerSettingsRoute() {
        const params = useParams({ strict: false });
        return <ServerSettingsPage key={params.serverId} />;
    },
});

const serverRconRoute = createRoute({
    getParentRoute: () => serverRoute,
    path: "/$serverId/rcon",
    component: ServerRCONPage,
});

const serverBackupsRoute = createRoute({
    getParentRoute: () => serverRoute,
    path: "/$serverId/backups",
    component: ServerBackupsPage,
});

/* --------- /node Routes --------- */
const nodeRoute = createRoute({
    getParentRoute: () => authenticatedRoute,
    path: "/node",
    component: () => <RequirePermission permission="ACCESS_NODES" />,
    notFoundComponent: NotFoundPage,
});

const nodeIndexRoute = createRoute({
    getParentRoute: () => nodeRoute,
    path: "/",
    component: NodePage,
});

const nodeRedirectRoute = createRoute({
    getParentRoute: () => nodeRoute,
    path: "/$nodeId",
    component: () => <Navigate to="./settings" />,
});

const nodeSettingsRoute = createRoute({
    getParentRoute: () => nodeRoute,
    path: "/$nodeId/settings",
    component: NodeSettingsPage,
});

/* --------- /team Routes --------- */
const teamRoute = createRoute({
    getParentRoute: () => authenticatedRoute,
    path: "/team",
    component: Outlet,
    notFoundComponent: NotFoundPage,
});

const teamIndexRoute = createRoute({
    getParentRoute: () => teamRoute,
    path: "/",
    component: () => <Navigate to="./members" />,
});

const teamMembersRoute = createRoute({
    getParentRoute: () => teamRoute,
    path: "/members",
    component: TeamMembersPage,
});

const teamActionLogRoute = createRoute({
    getParentRoute: () => teamRoute,
    path: "/action-log",
    component: TeamActionLogPage,
});

const teamSettingsRoute = createRoute({
    getParentRoute: () => teamRoute,
    path: "/settings",
    component: () => (
        <RequirePermission permission="MANAGE_TEAM">
            <TeamSettingsPage />
        </RequirePermission>
    ),
});

rootRoute.addChildren([
    indexRoute,
    loginRoute,
    logoutRoute,
    signupRoute,
    resetPasswordRoute,
    imprintRoute,
    privacyPolicyRoute,
    tosRoute,
    authenticatedRoute.addChildren([
        dashboardRoute,
        serverRoute.addChildren([
            serverIndexRoute,
            serverRedirectRoute,
            serverSettingsRoute,
            serverRconRoute,
            serverBackupsRoute,
        ]),
        nodeRoute.addChildren([
            nodeIndexRoute,
            nodeRedirectRoute,
            nodeSettingsRoute,
        ]),
        teamRoute.addChildren([
            teamIndexRoute,
            teamMembersRoute,
            teamActionLogRoute,
            teamSettingsRoute,
        ]),
    ]),
]);

export const router = new Router({ routeTree: rootRoute });

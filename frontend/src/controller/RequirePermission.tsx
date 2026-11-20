import { Outlet, useRouter } from "@tanstack/react-router";
import {
    hasPermission,
    type Permission,
    useActiveTeam,
} from "@/store/teamStore.ts";
import { type ReactNode, useEffect } from "react";
import { useUserQuery } from "@/store/userStore.ts";
import { toast } from "sonner";

type RequirePermissionProps = {
    permission: Permission;
    children?: ReactNode;
};

export function RequirePermission(props: RequirePermissionProps) {
    const { data: user } = useUserQuery();
    const activeTeam = useActiveTeam();
    const router = useRouter();

    useEffect(() => {
        if (!user) return;
        if (hasPermission(activeTeam, user?.id, props.permission)) return;

        toast.error(
            `You do not have permission to access this page. Please ask your team admin to grant you the permissions '${props.permission}'.`,
        );
        router.navigate({ to: "/dashboard" });
    }, [activeTeam, props.permission, router, user]);

    if (props.children) return <>{props.children}</>;
    return <Outlet />;
}

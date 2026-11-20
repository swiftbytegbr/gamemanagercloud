import { type ReactNode, useEffect } from "react";
import { useAuthStore } from "@/store/authStore.ts";
import { router } from "@/Router.tsx";

interface AuthGuardProps {
    children?: ReactNode;
    guard?: "authenticated" | "unauthenticated" | undefined;
}

export function AuthGuard(props: AuthGuardProps) {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const isAuthResolved = useAuthStore((state) => state.isAuthResolved);
    const initializeAuth = useAuthStore((state) => state.initialize);

    useEffect(() => {
        if (!isAuthResolved) {
            void initializeAuth();
        }
    }, [initializeAuth, isAuthResolved]);

    useEffect(() => {
        if (!isAuthResolved) return;
        switch (props.guard) {
            case "authenticated": {
                if (!isAuthenticated) router.navigate({ to: "/login" });
                break;
            }
            case "unauthenticated": {
                if (isAuthenticated) router.navigate({ to: "/dashboard" });
                break;
            }
            default: {
                router.navigate({
                    to: isAuthenticated ? "/dashboard" : "/login",
                });
                break;
            }
        }
    }, [isAuthResolved, isAuthenticated, props.guard]);

    if (!isAuthResolved) return null;

    if (props.guard === "authenticated" && !isAuthenticated) return null;
    if (props.guard === "unauthenticated" && isAuthenticated) return null;
    return <>{props.children}</>;
}

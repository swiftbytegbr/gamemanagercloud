import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore.ts";
import { toast } from "sonner";
import { logout, logoutAll } from "@/api/authApi.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { closeDialog } from "@/store/dialogStore.tsx";
import { router } from "@/Router.tsx";

export function LogoutPage() {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const isAuthResolved = useAuthStore((state) => state.isAuthResolved);
    const clearSession = useAuthStore((state) => state.logout);
    const initializeAuth = useAuthStore((state) => state.initialize);

    useEffect(() => {
        if (!isAuthResolved) {
            void initializeAuth();
            return;
        }

        if (!isAuthenticated) {
            router.navigate({ to: "/login" });
            return;
        }

        let isCancelled = false;

        const logoutAsync = async () => {
            closeDialog();
            const params = new URLSearchParams(window.location.search);

            try {
                if (params.has("all")) {
                    await logoutAll();
                } else {
                    await logout();
                }
                toast.success("You have been logged out successfully.");
            } catch (error) {
                console.error("Failed to log out", error);
                toast.error("Failed to log out. Please try again.");
            } finally {
                await clearSession();
                if (!isCancelled) {
                    await router.navigate({ to: "/login" });
                }
            }
        };

        logoutAsync().then();

        return () => {
            isCancelled = true;
        };
    }, [isAuthenticated, isAuthResolved, clearSession, initializeAuth]);

    return (
        <>
            {/*<Navigate to="/login" />*/}
            <div className="h-full flex justify-center items-center">
                <Spinner size="large" />
            </div>
        </>
    );
}

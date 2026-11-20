import { RouterProvider } from "@tanstack/react-router";
import { router } from "@/Router.tsx";
import { Toaster } from "@/components/ui/sonner.tsx";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/store/queryConfig.ts";
import { OfflineOverlay } from "@/components/OfflineOverlay.tsx";
import { EasterEggGate } from "@/components/easter-egg/EasterEggGate.tsx";

export default function App() {
    return (
        <>
            <QueryClientProvider client={queryClient}>
                {/*<Snowflake />*/}
                <EasterEggGate />
                <RouterProvider router={router} />
                <OfflineOverlay />
                <Toaster
                    position="bottom-right"
                    expand
                    richColors
                    closeButton
                />
            </QueryClientProvider>
        </>
    );
}

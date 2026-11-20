import { useEffect, useState } from "react";
import { WifiOff } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";

export function OfflineOverlay() {
    const [online, setOnline] = useState<boolean>(navigator.onLine);

    useEffect(() => {
        const on = () => setOnline(true);
        const off = () => setOnline(false);
        window.addEventListener("online", on);
        window.addEventListener("offline", off);
        return () => {
            window.removeEventListener("online", on);
            window.removeEventListener("offline", off);
        };
    }, []);

    if (online) return null;

    const tryReconnect = async () => {
        const res = await fetch(`/robots.txt?ts=${Date.now()}`, {
            cache: "no-store",
        });
        if (res.ok) {
            setOnline(navigator.onLine);
        }
    };

    return (
        <div
            className="fixed inset-0 z-[9999] flex items-center justify-center p-6"
            style={{ backgroundColor: "#161c24F2" }}
            aria-live="assertive"
            role="dialog"
            aria-modal>
            <div className="max-w-md w-full text-center">
                <div className="mx-auto mb-6 flex size-16 items-center justify-center rounded-full bg-white/5">
                    <WifiOff className="size-8 text-muted-foreground" />
                </div>
                <h1 className="text-3xl font-semibold text-white">
                    You’re offline
                </h1>
                <p className="mt-2 text-muted-foreground">
                    Please connect to the internet to continue using Game
                    Manager Cloud.
                </p>
                <div className="mt-6 flex items-center justify-center gap-3">
                    <Button variant="outline" onClick={tryReconnect}>
                        Try again
                    </Button>
                </div>
            </div>
        </div>
    );
}

import type { SettingProfile } from "@/store/serverStore.ts";
import { useMemo } from "react";

export function useMaxPlayers(
    server?: { gameType: string },
    profile?: SettingProfile,
): number {
    return useMemo(() => {
        if (!server) return 0;
        if (server.gameType === "ARK_EVOLVED") {
            return Number(profile?.questionMarkParams["MaxPlayers"] ?? "70");
        }
        if (server.gameType === "ARK_ASCENDED") {
            return Number(profile?.hyphenParams["WinLiveMaxPlayers"] ?? "70");
        }
        return 0;
    }, [server, profile]);
}

import axiosClient from "@/api/axiosConfig.ts";
import type { GameType, SettingProfile } from "@/store/serverStore.ts";

export const fetchServers = async (teamId: string) => {
    return (await axiosClient.get(`/server/by-team/${teamId}`)).data;
};

export const createServer = async (
    nodeId: string,
    name: string,
    game: string,
    enablePreaquaticaBeta: boolean,
    serverDirectory?: string,
) => {
    return (
        await axiosClient.post(`/server/create`, {
            nodeId: nodeId,
            name: name,
            gameType: game,
            map: `TheIsland${game === "ARK_ASCENDED" ? "_WP" : ""}`,
            enablePreaquaticaBeta: enablePreaquaticaBeta,
            serverDirectory: serverDirectory,
        })
    ).data;
};

export const fetchSettingProfile = async (profileId: string) => {
    return (await axiosClient.get(`/setting/profile/${profileId}`)).data;
};

export const fetchSettingTemplates = async (gameType: GameType) => {
    return (await axiosClient.get(`/setting/template?gameType=${gameType}`))
        .data;
};

export const deleteServer = async (serverId: string, forceDelete: boolean) => {
    return (
        await axiosClient.post(`/server/${serverId}/delete`, {
            forceDelete: forceDelete,
        })
    ).data;
};

export const startServer = async (serverId: string) => {
    return (await axiosClient.post(`/server/${serverId}/start`)).data;
};

export const stopServer = async (
    serverId: string,
    forceStop = false,
    delayMinutes?: number,
    delayedStopMessage?: string,
) => {
    return (
        await axiosClient.post(`/server/${serverId}/stop`, {
            forceStop: forceStop,
            delayMinutes: delayMinutes,
            delayedStopMessage: delayedStopMessage,
        })
    ).data;
};

export const restartServer = async (
    serverId: string,
    delayMinutes?: number,
    delayedRestartMessage?: string,
) => {
    return (
        await axiosClient.post(`/server/${serverId}/restart`, {
            delayMinutes: delayMinutes,
            delayedRestartMessage: delayedRestartMessage,
        })
    ).data;
};

export const updateServer = async (
    serverId: string,
    startAfterUpdate: boolean = false,
) => {
    return (
        await axiosClient.post(`/server/${serverId}/update`, {
            startAfterUpdate: startAfterUpdate,
        })
    ).data;
};

export const resetSettings = async (serverId: string) => {
    return (await axiosClient.post(`/server/${serverId}/reset-settings`)).data;
};

export const renameServer = async (serverId: string, displayName: string) => {
    return (
        await axiosClient.post(`/server/${serverId}/change-display-name`, {
            displayName: displayName,
        })
    ).data;
};

export const changeServerDirectory = async (
    serverId: string,
    newDirectory: string,
) => {
    return (
        await axiosClient.post(`/server/${serverId}/change-directory`, {
            serverDirectory: newDirectory,
        })
    ).data;
};

export const backupServer = async (serverId: string, backupName: string) => {
    return (
        await axiosClient.post(`/server/${serverId}/backup`, {
            name: backupName,
        })
    ).data;
};

export const deleteBackup = async (serverId: string, backupId: string) => {
    return await axiosClient.post(`/server/${serverId}/delete-backup`, {
        backupId: backupId,
    });
};

export const rollbackServer = async (
    serverId: string,
    backupId: string,
    rollbackPlayers: boolean,
) => {
    return (
        await axiosClient.post(`/server/${serverId}/rollback`, {
            backupId: backupId,
            rollbackPlayers: rollbackPlayers,
        })
    ).data;
};

export const saveSettingProfile = async (profile: SettingProfile) => {
    return (
        await axiosClient.post(`/setting/profile`, {
            ...profile,
        })
    ).data;
};

export const syncSettingProfile = async (
    sourceProfile: string,
    targetProfiles: string[],
    gameSettingKeys: Record<string, string[]>,
    gameUserSettingKeys: Record<string, string[]>,
    hyphenParamKeys: string[],
    questionMarkParamKeys: string[],
    gmcSettingKeys: string[],
) => {
    return (
        await axiosClient.post(`/setting/profile/copy`, {
            sourceId: sourceProfile,
            destinationIds: targetProfiles,
            gameSettingKeys: gameSettingKeys,
            gameUserSettingKeys: gameUserSettingKeys,
            hyphenParamKeys: hyphenParamKeys,
            questionMarkParamKeys: questionMarkParamKeys,
            gmcSettingKeys: gmcSettingKeys,
        })
    ).data;
};

export const sendCommand = async (serverId: string, command: string) => {
    return (
        await axiosClient.post(`/server/${serverId}/rcon`, {
            command: command,
        })
    ).data;
};

export const wipeSavegames = async (serverId: string) => {
    return (await axiosClient.post(`/server/${serverId}/wipe-savegames`)).data;
};

export const parseIni = async (ini: string) => {
    return (await axiosClient.post("/setting/profile/parse-ini", { file: ini }))
        .data;
};

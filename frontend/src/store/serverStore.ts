import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore.ts";
import {
    fetchServers,
    fetchSettingProfile,
    fetchSettingTemplates,
} from "@/api/serverApi.ts";
import { hasPermission, type Team } from "@/store/teamStore.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";

export type ServerState =
    | "ONLINE"
    | "OFFLINE"
    | "INITIALIZING"
    | "STOPPING"
    | "RESTARTING"
    | "CREATING"
    | "DELETING"
    | "UNKNOWN";

export type ServerBackup = {
    backupId: string;
    serverId: string;
    createdAt: string;
    expiresAt: string;
    autoBackup: boolean;
    name: string;
    size: number;
};

export type ServerCommand = {
    command: string;
    response: string;
    timestamp: string;
};

export type GameType = "ARK_ASCENDED" | "ARK_EVOLVED";

export type Server = {
    id: string;
    displayName: string;
    nodeId: string;
    created: string;
    version: string;
    gameType: GameType;
    state: ServerState;
    serverDirectory: string;
    settingProfileId: string;
    onlinePlayers: number;
    backups: ServerBackup[];
    commands: ServerCommand[];

    nodeName: string;
    serverIp: string;
    map: string;
    maxPlayers: number;
    modCount: number;
    serverPort: number;
    queryPort?: number;
    rconPort: number;
};

export type SettingProfileValue = string | boolean | number | string[];

export type SettingProfile = {
    id: string;
    teamId: string;
    profileName: string;
    gameType: string;
    lastModified: string;
    map: string;
    gameSettings: Record<string, Record<string, SettingProfileValue>>;
    gameUserSettings: Record<string, Record<string, SettingProfileValue>>;
    questionMarkParams: Record<string, SettingProfileValue>;
    hyphenParams: Record<string, SettingProfileValue>;
    gmcSettings: Record<string, SettingProfileValue>;
};

export type SettingGameType = GameType | "ARK_BOTH";

export type SettingTemplate = {
    id: string;
    gameType: SettingGameType;
    location: string;
    section: string;
    key: string;
    category: string;
    displayName: string;
    description: string;
    placeholder: string;
    type: "STRING" | "BOOLEAN" | "INTEGER" | "FLOAT" | "LIST";
    appearance:
        "TEXT" | "PASSWORD" | "CHECKBOX" | "LIST" | "TEXT_AREA" | "SUBTITLE";
    disableSync: boolean;
    defaultValue?: boolean | number | string;
    required: boolean;
    verifyingRegex: string;
    regexMessage: string;
    rangeFrom: number;
    rangeTo: number;
    position: number;
    inline: boolean;
};

export const useServerQuery = <TData = Server[]>(
    team: Team | null,
    options?: Partial<UseQueryOptions<Server[], Error, TData>>,
) => {
    const teamId = team?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const { data: user } = useUserQuery();

    return useQuery<Server[], Error, TData>({
        queryKey: queryKeys.servers.list(teamId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            return await fetchServers(teamId);
        },
        staleTime: 1000 * 60 * 5,
        enabled: !!team && hasPermission(team, user?.id, "ACCESS_SERVERS"),
        ...options,
    });
};

export const useServerByIdQuery = (
    team: Team | null,
    serverId: string | undefined,
) => {
    return useServerQuery<Server | undefined>(team, {
        select: (servers) => servers.find((server) => server.id === serverId),
        enabled: !!team && !!serverId,
    });
};

export const useSettingProfileQuery = <TData = SettingProfile>(
    team: Team | string | null,
    profileId: string | undefined,
    options?: Partial<UseQueryOptions<SettingProfile, Error, TData>>,
) => {
    const teamId = typeof team === "string" ? team : team?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    return useQuery<SettingProfile, Error, TData>({
        queryKey: queryKeys.settingProfiles.detail(teamId, profileId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            return await fetchSettingProfile(profileId ?? "");
        },
        //Do NOT cache setting profiles
        staleTime: 0,
        enabled: !!team && !!profileId,
        ...options,
    });
};

export const refetchServerQuery = async (teamId?: string | null) => {
    const queryKey = teamId
        ? queryKeys.servers.list(teamId)
        : queryKeys.servers.all;
    await queryClient.invalidateQueries({ queryKey });
};

export const findBackupNameFromCache = (backupId: string | null) => {
    if (!backupId) return null;

    const matchingQueries = queryClient
        .getQueryCache()
        .findAll({ queryKey: queryKeys.servers.all });

    for (const query of matchingQueries) {
        const servers = queryClient.getQueryData<Server[]>(query.queryKey);
        if (!servers || servers.length === 0) continue;

        for (const server of servers) {
            const backup = server.backups.find(
                (existingBackup) => existingBackup.backupId === backupId,
            );
            if (backup) return backup.name;
        }
    }

    return null;
};

const categories = [
    "Server",
    "GMC",
    "Automations",
    "General",
    "Visual",
    "Player",
    "Dino",
    "Engrams",
    "Level",
    "Environment",
    "Structures",
    "Crafting",
    "Stack Size",
    "Supply Crate",
    "Spawn",
    "Custom",
    "Rules",
    "Uncategorized",
];

export const useSettingTemplateQuery = <TData = SettingTemplate[]>(
    gameType?: GameType,
    options?: Partial<UseQueryOptions<SettingTemplate[], Error, TData>>,
) => {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    return useQuery<SettingTemplate[], Error, TData>({
        queryKey: queryKeys.settingTemplates.list(gameType),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!gameType) return [];
            const data: SettingTemplate[] =
                await fetchSettingTemplates(gameType);

            return data
                .filter(
                    (template) =>
                        template.gameType === gameType ||
                        template.gameType === "ARK_BOTH",
                )
                .sort((a, b) => {
                    const catA = categories.indexOf(a.category);
                    const catB = categories.indexOf(b.category);
                    if (catA !== catB) {
                        return catA - catB;
                    }
                    return a.position - b.position;
                });
        },
        staleTime: 1000 * 60 * 30,
        enabled: isAuthenticated && !!gameType,
        ...options,
    });
};

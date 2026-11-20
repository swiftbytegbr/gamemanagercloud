import { create } from "zustand";
import {
    useInfiniteQuery,
    useQuery,
    type UseQueryOptions,
} from "@tanstack/react-query";
import { fetchApiKeys, fetchTeams, getActionLog } from "@/api/teamApi.ts";
import { useAuthStore } from "@/store/authStore.ts";
import { useMemo } from "react";
import { queryClient } from "@/store/queryConfig.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";

export type LogEntryType =
    | "SERVER_CREATION"
    | "SERVER_DELETION"
    | "SERVER_START"
    | "SERVER_STOP"
    | "SERVER_RESTART"
    | "SERVER_RCON_COMMAND"
    | "SERVER_BACKUP"
    | "SERVER_BACKUP_DELETION"
    | "SERVER_ROLLBACK"
    | "SERVER_UPDATE"
    | "SERVER_SETTINGS_CHANGE"
    | "NODE_CREATION"
    | "NODE_REGISTRATION"
    | "NODE_DELETION"
    | "NODE_SETTINGS_CHANGE"
    | "NODE_UPDATE"
    | "TEAM_CREATION"
    | "TEAM_INVITATION"
    | "TEAM_JOIN"
    | "TEAM_LEAVE"
    | "TEAM_KICK"
    | "TEAM_PERMISSION_CHANGE"
    | "TEAM_NAME_CHANGE"
    | "TEAM_ICON_CHANGE"
    | "TEAM_APPLICATION_CREATION"
    | "TEAM_APPLICATION_DELETION"
    | "SETTING_PROFILE_UPDATE";

export type LogEntry = {
    id: string;
    type: LogEntryType;
    timestamp: string;
    teamId?: string;
    issuerId: string;
    affectedNodeId?: string;
    affectedServerId?: string;
    affectedUserId?: string;
    actionInformation?: string;
};

export type TeamActionLogPage = {
    content: LogEntry[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
};

export type TeamMember = {
    id: string;
    name: string;
    email: string;
    imageId: string;
    permissions: Permission[];
    twoFAEnabled: boolean;
};

export type MemberInvite = {
    id: string;
    name: string;
    email: string;
    imageId: string;
    invitedAt: string;
    expired: boolean;
};

export type Permission =
    | "ACCESS_TEAM"
    | "MANAGE_TEAM"
    | "ACCESS_NODES"
    | "MANAGE_NODES"
    | "ACCESS_SERVERS"
    | "MANAGE_SERVERS"
    | "ACCESS_LOGS"
    | "MANAGE_LOGS"
    | "ACCESS_PROFILES"
    | "MANAGE_PROFILES"
    | "ACCESS_AUTOMATIONS"
    | "MANAGE_AUTOMATIONS"
    | "ALL";

export type DiscordNotificationSeverity =
    "INFO" | "WARNING" | "ERROR" | "CRITICAL";

export type TeamNotificationSettings = {
    discordWebhookUrl: string;
    discordNotificationsMinSeverity: DiscordNotificationSeverity;
};

export type Team = {
    id: string;
    name: string;
    iconImageId: string;
    owner: TeamMember;
    members: TeamMember[];
    invitedMembers: MemberInvite[];
    inviteCode: string;
    notificationSettings?: TeamNotificationSettings;
};

export type ApiKey = {
    applicationId: string;
    name: string;
    permissions: Permission[];
};

type TeamState = {
    activeTeamId: string | null;
    selectActiveTeam: (teamId: string | null) => void;
    ensureActiveTeam: (teams: Team[]) => void;
};

export const useTeamStore = create<TeamState>((set, get) => ({
    activeTeamId: null,
    selectActiveTeam: (teamId: string | null) => {
        const previousTeamId = get().activeTeamId;
        if (teamId !== previousTeamId) {
            if (teamId) {
                queryClient
                    .invalidateQueries({ queryKey: queryKeys.servers.all })
                    .then();
                queryClient
                    .invalidateQueries({ queryKey: queryKeys.nodes.all })
                    .then();
            }
            localStorage.setItem("lastActiveTeamId", teamId ?? "");
        }
        set({ activeTeamId: teamId });
    },
    ensureActiveTeam: (teams) => {
        if (!get().activeTeamId && teams.length > 0) {
            const lastActiveTeamId = localStorage.getItem("lastActiveTeamId");
            if (
                lastActiveTeamId &&
                teams.find((team) => team.id === lastActiveTeamId)
            ) {
                set({ activeTeamId: lastActiveTeamId });
            } else {
                const teamId = teams[0].id;
                localStorage.setItem("lastActiveTeamId", teamId ?? "");
                set({ activeTeamId: teamId });
            }
        } else if (teams.length > 0) {
            if (!teams.find((team) => team.id === get().activeTeamId)) {
                const teamId = teams[0].id;
                localStorage.setItem("lastActiveTeamId", teamId ?? "");
                set({ activeTeamId: teamId });
            }
        }
    },
}));

export const useTeamQuery = () => {
    // Only fetch teams when authenticated to avoid caching an empty list
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    return useQuery<Team[]>({
        queryKey: queryKeys.teams.all,
        enabled: isAuthenticated,
        queryFn: async () => {
            const teams = await fetchTeams();
            useTeamStore.getState().ensureActiveTeam(teams);
            return teams;
        },
        staleTime: 1000 * 60 * 5,
    });
};

export const useActiveTeam = () => {
    const { data: teams } = useTeamQuery();
    const { activeTeamId, ensureActiveTeam } = useTeamStore();

    return useMemo(() => {
        ensureActiveTeam(teams ?? []);
        return teams?.find((team: Team) => team.id === activeTeamId) || null;
    }, [ensureActiveTeam, teams, activeTeamId]);
};

export const useApiKeyQuery = <TData = ApiKey[]>(
    team: Team | null,
    options?: Partial<UseQueryOptions<ApiKey[], Error, TData>>,
) => {
    const teamId = team?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const { data: user } = useUserQuery();

    return useQuery<ApiKey[], Error, TData>({
        queryKey: queryKeys.apiKeys.list(teamId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            return await fetchApiKeys(teamId);
        },
        staleTime: 1000 * 60 * 5,
        enabled: !!team && hasPermission(team, user?.id, "MANAGE_TEAM"),
        ...options,
    });
};

const buildEmptyActionLogPage = (
    page: number,
    size: number,
): TeamActionLogPage => ({
    content: [],
    page,
    size,
    totalElements: 0,
    totalPages: 0,
    first: page === 0,
    last: true,
    numberOfElements: 0,
    empty: true,
});

export const DEFAULT_ACTION_LOG_PAGE_SIZE = 20;

export const useTeamActionLogInfiniteQuery = (
    teamId: string | null | undefined,
    pageSize = DEFAULT_ACTION_LOG_PAGE_SIZE,
) => {
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

    return useInfiniteQuery<TeamActionLogPage, Error>({
        queryKey: queryKeys.teamActionLog.list(teamId ?? null, pageSize),
        enabled: isAuthenticated && !!teamId,
        initialPageParam: 0,
        queryFn: async ({ pageParam }) => {
            if (!teamId) {
                return buildEmptyActionLogPage(0, pageSize);
            }
            const currentPage =
                typeof pageParam === "number" ? pageParam : Number(pageParam);
            return (await getActionLog(
                teamId,
                currentPage,
                Math.max(pageSize, 1),
            )) as TeamActionLogPage;
        },
        getNextPageParam: (lastPage) => {
            if (!lastPage) return undefined;
            if (lastPage.last) return undefined;
            const nextPage = lastPage.page + 1;
            if (lastPage.totalPages && nextPage >= lastPage.totalPages) {
                return undefined;
            }
            return nextPage;
        },
        placeholderData: (previousData) => previousData,
    });
};

export const getTeamMemberPermissions = (
    team: Team | null,
    userId?: string,
): Permission[] => {
    if (!team) return [];
    if (team.owner.id === userId) {
        return team.owner.permissions ?? [];
    }
    return (
        team.members
            .filter((member) => member.id === userId)
            .map((member) => member.permissions)
            .flat() ?? []
    );
};

export const getTeamMembers = (team: Team | null): TeamMember[] => {
    if (!team) return [];
    return [team.owner, ...team.members];
};

export const getTeamMemberById = (
    team: Team | null,
    memberId?: string,
): TeamMember | null => {
    if (!team || !memberId) return null;
    if (team.owner.id === memberId) {
        return team.owner;
    }
    return team.members.find((member) => member.id === memberId) ?? null;
};

export const getTeamMemberMap = (
    team: Team | null,
): Map<string, TeamMember> => {
    return new Map(getTeamMembers(team).map((member) => [member.id, member]));
};

export const hasPermission = (
    team: Team | null,
    userId: string | undefined,
    permission: Permission,
) => {
    return getTeamMemberPermissions(team, userId).some(
        (it) => it === permission || it === "ALL",
    );
};

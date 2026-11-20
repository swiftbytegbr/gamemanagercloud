import type { ServerState } from "@/store/serverStore.ts";
import { hasPermission, type Team } from "@/store/teamStore.ts";
import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { fetchAutomations } from "@/api/automationsApi.ts";

// ----- define target selector types -----

export type ExplicitTargetSelector = {
    type: "EXPLICIT";
    ids: string[];
};

export type DynamicTargetSelector = {
    type: "DYNAMIC";
    // Ignore other values for now, as dynamic selectors are not yet supported by the frontend
};

export type TriggerSourceTargetSelector = {
    type: "TRIGGER_SOURCE";
};

export type TargetSelector =
    | ExplicitTargetSelector
    | DynamicTargetSelector
    | TriggerSourceTargetSelector;

// ----- define automation step types -----

export type StartStep = {
    type: "START";
};

export type StopStep = {
    type: "STOP";
    message: string;
    delayMinutes: number;
    forceStop: boolean;
};

export type RestartStep = {
    type: "RESTART";
    message: string;
    delayMinutes: number;
};

export type UpdateStep = {
    type: "UPDATE";
};

export type BackupStep = {
    type: "BACKUP";
    backupName: string;
};

export type SettingSyncStep = {
    type: "SETTING_SYNC";
    profileId: string;
    gameSettingKeys: Record<string, string[]>;
    gameUserSettingKeys: Record<string, string[]>;
    hyphenParamKeys: string[];
    questionMarkParamKeys: string[];
    gmcSettingKeys: string[];
};

export type RconCommandStep = {
    type: "RCON_COMMAND";
    command: string;
};

export type ConditionStep = {
    type: "CONDITION";
    state: ServerState;
};

export type TimedWaitStep = {
    type: "TIMED_WAIT";
    waitMinutes: number;
};

export type StateWaitStep = {
    type: "STATE_WAIT";
    desiredState: ServerState;
};

export type NotifyStep = {
    type: "NOTIFY";
    title: string;
    message: string;
    severity: "INFO" | "WARNING" | "ERROR" | "CRITICAL";
};

//TODO move to a more fitting location
export type DiscordEmbed = {
    title: string;
    description: string;

    //Color in decimal format
    color: number;

    //Optional fields
    authorName?: string;
    authorUrl?: string;
    authorIconUrl?: string;
};

export type DiscordWebhookStep = {
    type: "DISCORD_WEBHOOK";
    webhookUrl: string;
    content: string;
    embed?: DiscordEmbed;
};

export type AutomationStep =
    | StartStep
    | StopStep
    | RestartStep
    | UpdateStep
    | BackupStep
    | SettingSyncStep
    | RconCommandStep
    | ConditionStep
    | TimedWaitStep
    | StateWaitStep
    | NotifyStep
    | DiscordWebhookStep;

// ----- define automation trigger types -----

export type ManualTrigger = {
    type: "MANUAL";
};

export type ScheduledTrigger = {
    type: "SCHEDULED";
    cron: string;
    nextFireAt: string;
    leaseUntil?: string;
};

export type StateChangeTrigger = {
    type: "STATE_CHANGE";
    newState: ServerState;
    serverIds: string[];
};

export type AutomationTrigger =
    ManualTrigger | ScheduledTrigger | StateChangeTrigger;

export type Automation = {
    id: string;
    teamId: string;
    name: string;
    description: string;
    enabled: boolean;
    createdAt: string;
    updatedAt: string;
    targets: TargetSelector;
    workflow: AutomationStep[];
    trigger: AutomationTrigger;
    runTimeoutSec?: number;
};

export const useAutomationQuery = <TData = Automation[]>(
    team: Team | null,
    options?: Partial<UseQueryOptions<Automation[], Error, TData>>,
) => {
    const teamId = team?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const { data: user } = useUserQuery();

    return useQuery<Automation[], Error, TData>({
        queryKey: queryKeys.automations.list(teamId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            return await fetchAutomations(teamId);
        },
        staleTime: 1000 * 60 * 5,
        placeholderData: () => [],
        enabled: !!team && hasPermission(team, user?.id, "ACCESS_AUTOMATIONS"),
        ...options,
    });
};

export const useAutomationByIdQuery = (
    team: Team | null,
    automationId: string | undefined,
) => {
    const { data: user } = useUserQuery();

    return useAutomationQuery<Automation | undefined>(team, {
        select: (automations) =>
            automations.find((automation) => automation.id === automationId),
        enabled:
            !!team &&
            !!automationId &&
            hasPermission(team, user?.id, "ACCESS_AUTOMATIONS"),
    });
};

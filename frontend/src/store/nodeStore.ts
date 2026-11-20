import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore.ts";
import { hasPermission, type Team } from "@/store/teamStore.ts";
import {
    fetchNodes,
    fetchNodeTasks,
    getLatestNodeVersion,
} from "@/api/nodeApi.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateQueryData } from "@/store/queryCache.ts";

export type NodeStatus = "ONLINE" | "OFFLINE" | "CREATION";

export type NodeSettings = {
    name: string;
    defaultServerDirectory: string;
    serverBackupsDirectory: string;
    enableAutoUpdate: boolean;
    manageFirewallAutomatically: boolean;
};

export type NodeSystemInfo = {
    os: string;
    hostname: string;
    ipAddresses: string[];
    ram: number;
    storage: {
        name: string;
        usedBytes: number;
        totalBytes: number;
    }[];
    cpu: {
        name: string;
        cores: number;
        threads: number;
        frequency: number;
    };
};

export type Node = {
    id: string;
    teamId: string;
    daemonVersion: string;
    inviteToken: number;
    lastAlive: string;
    lastLogin: string;
    logoutReason: string;
    status: NodeStatus;
    settings: NodeSettings;
    systemData: NodeSystemInfo;
};

export type NodeTask = {
    id: string;
    nodeId: string;
    cancellable: boolean;
    type: NodeTaskType;
    targetIds?: string[];
    affectedIds?: string[];
    context: Record<string, unknown>;
    progressPercentage: number;
    state: NodeTaskState;
    errorMessage: string;
    createdAt: string;
    finishedAt: string;
};

export type NodeTaskType =
    | "BACKUP"
    | "ROLLBACK"
    | "TIMED_SHUTDOWN"
    | "TIMED_RESTART"
    | "SERVER_DIRECTORY_CHANGE"
    | "BACKUP_DIRECTORY_CHANGE";

export type NodeTaskState =
    "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";

export const useNodeQuery = <TData = Node[]>(
    team: Team | null,
    options?: Partial<UseQueryOptions<Node[], Error, TData>>,
) => {
    const teamId = team?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const { data: user } = useUserQuery();

    return useQuery<Node[], Error, TData>({
        queryKey: queryKeys.nodes.list(teamId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            return await fetchNodes(teamId);
        },
        staleTime: 1000 * 60 * 5,
        placeholderData: () => [],
        enabled: !!team && hasPermission(team, user?.id, "ACCESS_NODES"),
        ...options,
    });
};

export const useNodeByIdQuery = (
    team: Team | null,
    nodeId: string | undefined,
) => {
    const { data: user } = useUserQuery();

    return useNodeQuery<Node | undefined>(team, {
        select: (nodes) => nodes.find((node) => node.id === nodeId),
        enabled:
            !!team && !!nodeId && hasPermission(team, user?.id, "ACCESS_NODES"),
    });
};

export const useLatestNodeVersionQuery = () =>
    useQuery<string>({
        queryKey: queryKeys.latestNodeVersion.all,
        queryFn: async () => await getLatestNodeVersion(),
        staleTime: 1000 * 60 * 5,
    });

export const refetchNodeQuery = async (teamId?: string | null) => {
    const queryKey = teamId
        ? queryKeys.nodes.list(teamId)
        : queryKeys.nodes.all;
    await queryClient.invalidateQueries({ queryKey });
};

export const useNodeTasksQuery = <TData = NodeTask[]>(
    team: Team | null,
    node: Node | null,
    options?: Partial<UseQueryOptions<NodeTask[], Error, TData>>,
) => {
    const teamId = team?.id;
    const nodeId = node?.id;
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const { data: user } = useUserQuery();

    return useQuery<NodeTask[], Error, TData>({
        queryKey: queryKeys.nodeTasks.list(teamId, nodeId),
        queryFn: async () => {
            if (!isAuthenticated) return [];
            if (!teamId) return [];
            if (!nodeId) return [];
            return await fetchNodeTasks(nodeId);
        },
        staleTime: 1000 * 60 * 5,
        placeholderData: () => [],
        enabled: !!team && hasPermission(team, user?.id, "ACCESS_NODES"),
        ...options,
    });
};

export const useNodeTasksByType = (
    team: Team | null,
    node: Node | null,
    taskType: NodeTaskType | undefined,
) => {
    const { data: user } = useUserQuery();

    return useNodeTasksQuery<NodeTask[] | undefined>(team, node, {
        select: (nodeTasks) =>
            nodeTasks.filter((nodeTask) => nodeTask.type === taskType),
        enabled:
            !!team &&
            !!taskType &&
            hasPermission(team, user?.id, "ACCESS_NODES"),
    });
};

export const refetchNodeTasksQuery = async (
    teamId?: string | null,
    nodeId?: string | null,
) => {
    const queryKey =
        teamId && nodeId
            ? queryKeys.nodeTasks.list(teamId, nodeId)
            : queryKeys.nodeTasks.all;
    await queryClient.invalidateQueries({ queryKey });
};

type NodeTasksQueryMeta = {
    teamId?: string | null;
    nodeId?: string | null;
};

const isNodeTasksQueryMeta = (value: unknown): value is NodeTasksQueryMeta =>
    typeof value === "object" && value !== null;

export const applyNodeTaskUpdate = (updatedTask: NodeTask) => {
    const matchingQueries = queryClient
        .getQueryCache()
        .findAll({ queryKey: queryKeys.nodeTasks.all });

    matchingQueries.forEach((query) => {
        const [, meta] = query.queryKey;
        if (!isNodeTasksQueryMeta(meta)) return;
        if (meta.nodeId !== updatedTask.nodeId) return;

        updateQueryData<NodeTask[]>(query.queryKey, (oldData) => {
            if (!oldData || oldData.length === 0) return oldData;
            const index = oldData.findIndex(
                (task) => task.id === updatedTask.id,
            );
            if (index === -1) return oldData;
            const next = [...oldData];
            next[index] = updatedTask;
            return next;
        });
    });
};

import { useEffect, useMemo, useState } from "react";

import { useActiveTeam } from "@/store/teamStore.ts";
import {
    type Node,
    type NodeTask,
    useNodeByIdQuery,
    useNodeTasksByType,
} from "@/store/nodeStore.ts";
import type { Server } from "@/store/serverStore.ts";

const activeTaskStates = new Set<NodeTask["state"]>(["PENDING", "RUNNING"]);

export const getNodeTaskServerTargets = (task?: NodeTask) => {
    if (!task) return [];
    return task.affectedIds ?? task.targetIds ?? [];
};

export const nodeTaskAffectsServer = (
    task: NodeTask | undefined,
    serverId: string,
) => {
    if (!task) return false;
    return getNodeTaskServerTargets(task).includes(serverId);
};

export const isNodeTaskActive = (task?: NodeTask) => {
    if (!task) return false;
    return activeTaskStates.has(task.state);
};

const getRawNodeTaskDelay = (task?: NodeTask) => {
    if (!task) return null;
    const context = task.context as { delay?: unknown } | undefined;
    if (!context) return null;
    return context.delay ?? null;
};

export const getNodeTaskDelayMinutes = (task?: NodeTask) => {
    const rawDelay = getRawNodeTaskDelay(task);
    if (typeof rawDelay === "number" && Number.isFinite(rawDelay)) {
        return rawDelay;
    }
    if (typeof rawDelay === "string") {
        const parsed = Number(rawDelay);
        if (Number.isFinite(parsed)) return parsed;
    }
    return null;
};

const getNodeTaskDeadline = (task?: NodeTask) => {
    if (!task) return null;
    const delayMinutes = getNodeTaskDelayMinutes(task);
    if (delayMinutes === null) return null;
    const createdAt = Date.parse(task.createdAt);
    if (Number.isNaN(createdAt)) return null;
    return createdAt + delayMinutes * 60_000;
};

const formatRemainingDuration = (remainingMs: number | null) => {
    if (remainingMs === null) return null;
    const clamped = Math.max(0, remainingMs);
    const totalSeconds = Math.ceil(clamped / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes > 0) {
        return `${minutes}m ${seconds.toString().padStart(2, "0")}s`;
    }
    return `${seconds}s`;
};

export const useNodeTaskDelayCountdown = (task?: NodeTask) => {
    const [remainingMs, setRemainingMs] = useState<number | null>(null);

    useEffect(() => {
        const deadline = getNodeTaskDeadline(task);
        if (!deadline) return;
        if (typeof window === "undefined") return;
        let timeoutId: number | null = null;

        const scheduleUpdate = (delay: number) => {
            timeoutId = window.setTimeout(() => {
                const remaining = Math.max(0, deadline - Date.now());
                setRemainingMs(remaining);
                if (remaining > 0) {
                    scheduleUpdate(1000);
                }
            }, delay);
        };

        scheduleUpdate(0);
        return () => {
            if (timeoutId !== null) {
                window.clearTimeout(timeoutId);
            }
        };
    }, [task]);

    return useMemo(() => {
        if (!task || remainingMs === null) return null;
        return formatRemainingDuration(remainingMs);
    }, [remainingMs, task]);
};

export const useServerPowerTasks = (
    server: Server,
    nodeOverride?: Node | null,
) => {
    const activeTeam = useActiveTeam();
    const shouldFetchNode = !nodeOverride;
    const { data: fetchedNode } = useNodeByIdQuery(
        shouldFetchNode ? activeTeam : null,
        shouldFetchNode ? server.nodeId : undefined,
    );

    const node = nodeOverride ?? fetchedNode ?? null;

    const shutdownQuery = useNodeTasksByType(
        activeTeam,
        node,
        "TIMED_SHUTDOWN",
    );
    const restartQuery = useNodeTasksByType(activeTeam, node, "TIMED_RESTART");

    const activeShutdownTask = useMemo(
        () =>
            (shutdownQuery.data ?? []).find(
                (task) =>
                    nodeTaskAffectsServer(task, server.id) &&
                    isNodeTaskActive(task),
            ),
        [server.id, shutdownQuery.data],
    );

    const activeRestartTask = useMemo(
        () =>
            (restartQuery.data ?? []).find(
                (task) =>
                    nodeTaskAffectsServer(task, server.id) &&
                    isNodeTaskActive(task),
            ),
        [server.id, restartQuery.data],
    );

    return {
        node,
        activeShutdownTask,
        activeRestartTask,
        shutdownQuery,
        restartQuery,
    };
};

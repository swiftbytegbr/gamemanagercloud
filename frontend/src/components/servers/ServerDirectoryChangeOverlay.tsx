import { type ReactNode, useMemo } from "react";

import { type Server } from "@/store/serverStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import {
    isNodeTaskActive,
    nodeTaskAffectsServer,
} from "@/hooks/useServerPowerTasks.ts";
import {
    useNodeByIdQuery,
    useNodeTasksByType,
    type NodeTask,
} from "@/store/nodeStore.ts";
import { cn } from "@/lib/utils.ts";

type ServerDirectoryChangeOverlayProps = {
    server: Server;
    children: ReactNode;
    className?: string;
};

const findLatestActiveTask = (tasks: NodeTask[]) =>
    tasks
        .slice()
        .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))[0] ??
    null;

export const ServerDirectoryChangeOverlay = ({
    server,
    children,
    className,
}: ServerDirectoryChangeOverlayProps) => {
    const activeTeam = useActiveTeam();

    const { data: node } = useNodeByIdQuery(activeTeam, server.nodeId);
    const { data: directoryChangeTasks } = useNodeTasksByType(
        activeTeam,
        node ?? null,
        "SERVER_DIRECTORY_CHANGE",
    );

    const activeTask = useMemo(() => {
        if (!server?.id) return null;
        const relevantTasks = (directoryChangeTasks ?? []).filter(
            (task) =>
                nodeTaskAffectsServer(task, server.id) &&
                isNodeTaskActive(task),
        );
        if (relevantTasks.length === 0) return null;
        return findLatestActiveTask(relevantTasks);
    }, [directoryChangeTasks, server?.id]);

    const progress = Math.min(
        100,
        Math.max(0, activeTask?.progressPercentage ?? 0),
    );

    return (
        <div className={cn("relative w-full h-full flex flex-col", className)}>
            {children}
            {activeTask && (
                <div className="absolute inset-0 z-40 flex flex-col items-center justify-center gap-4 bg-background/80 backdrop-blur-sm text-center p-6">
                    <div>
                        <h2 className="text-2xl font-semibold">
                            Server directory change in progress
                        </h2>
                        <p className="text-sm text-muted-foreground mt-1">
                            You will regain access to the server once all files
                            have been moved!
                        </p>
                    </div>
                    <div className="w-full max-w-md space-y-2">
                        <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
                            <div
                                className="h-full bg-primary transition-[width] duration-300 ease-out"
                                style={{ width: `${progress}%` }}
                                aria-hidden="true"
                            />
                        </div>
                        <div className="text-sm font-medium">
                            {progress.toFixed(0)}% complete
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

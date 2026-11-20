import { type ReactNode, useMemo } from "react";

import { type Server } from "@/store/serverStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { cn } from "@/lib/utils.ts";
import {
    useNodeByIdQuery,
    useNodeTasksByType,
    type NodeTask,
} from "@/store/nodeStore.ts";
import { isNodeTaskActive } from "@/hooks/useServerPowerTasks.ts";

type ServerBackupDirectoryChangeOverlayProps = {
    server: Server;
    children: ReactNode;
    className?: string;
};

const findLatestActiveTask = (tasks: NodeTask[]) =>
    tasks
        .slice()
        .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))[0] ??
    null;

export const ServerBackupDirectoryChangeOverlay = ({
    server,
    children,
    className,
}: ServerBackupDirectoryChangeOverlayProps) => {
    const activeTeam = useActiveTeam();

    const { data: node } = useNodeByIdQuery(activeTeam, server.nodeId);
    const { data: backupDirectoryChangeTasks } = useNodeTasksByType(
        activeTeam,
        node ?? null,
        "BACKUP_DIRECTORY_CHANGE",
    );

    const activeTask = useMemo(() => {
        const relevantTasks = (backupDirectoryChangeTasks ?? []).filter(
            (task) => isNodeTaskActive(task),
        );
        if (relevantTasks.length === 0) return null;
        return findLatestActiveTask(relevantTasks);
    }, [backupDirectoryChangeTasks]);

    return (
        <div className={cn("relative w-full h-full flex flex-col", className)}>
            {children}
            {activeTask && (
                <div className="absolute inset-0 z-40 flex flex-col items-center justify-center gap-4 bg-background/80 backdrop-blur-sm text-center p-6">
                    <div>
                        <h2 className="text-2xl font-semibold">
                            Backup directory change in progress
                        </h2>
                        <p className="text-sm text-muted-foreground mt-1">
                            Backups are temporarily unavailable while the node
                            completes this operation.
                        </p>
                    </div>
                    <div className="w-full max-w-md space-y-2">
                        <p className="text-sm text-muted-foreground">
                            You will regain access once the node finishes
                            updating the backup directory.
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

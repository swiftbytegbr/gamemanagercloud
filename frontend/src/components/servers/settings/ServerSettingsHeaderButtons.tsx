import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { cn } from "@/lib/utils.ts";
import {
    CloudDownload,
    CloudUpload,
    Play,
    Power,
    RotateCw,
    Save,
    XIcon,
} from "lucide-react";
import type { Server } from "@/store/serverStore.ts";
import { startServer, stopServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { ServerManagementDropdown } from "@/components/servers/ServerManagementDropdown.tsx";
import { type Dispatch, type SetStateAction } from "react";
import { dialog } from "@/store/dialogStore.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { DeleteServerDialog } from "@/components/servers/dialogs/DeleteServerDialog.tsx";
import { BackupServerDialog } from "@/components/servers/dialogs/BackupServerDialog.tsx";
import { ServerPowerDialog } from "@/components/servers/dialogs/ServerPowerDialog.tsx";
import { UpdateServerDialog } from "@/components/servers/dialogs/UpdateServerDialog.tsx";
import { cancelNodeTask } from "@/api/nodeApi.ts";
import {
    useNodeTaskDelayCountdown,
    useServerPowerTasks,
} from "@/hooks/useServerPowerTasks.ts";

type ServerSettingsHeaderButtonsProps = {
    server: Server;
    isAnyLoading: boolean;
    onSave?: () => void;
    setLoading: Dispatch<SetStateAction<boolean>>;
    saveDisabled: boolean;
    disableSettings?: boolean;
    disableBackups?: boolean;
};

export function ServerSettingsHeaderButtons({
    server,
    isAnyLoading,
    onSave,
    setLoading,
    saveDisabled,
    disableSettings,
    disableBackups,
}: ServerSettingsHeaderButtonsProps) {
    const {
        activeShutdownTask,
        activeRestartTask,
        shutdownQuery,
        restartQuery,
    } = useServerPowerTasks(server);

    const hasStopTask = !!activeShutdownTask;
    const stopCountdownLabel =
        useNodeTaskDelayCountdown(activeShutdownTask) ?? "Cancel";
    const stopCancelDisabled =
        hasStopTask && activeShutdownTask?.cancellable === false;
    const hideStopButton =
        !hasStopTask &&
        [
            "OFFLINE",
            "UNKNOWN",
            "STOPPING",
            "CREATING",
            "DELETING",
            "INITIALIZING",
        ].includes(server.state);
    const hasRestartTask = !!activeRestartTask;
    const restartCountdownLabel =
        useNodeTaskDelayCountdown(activeRestartTask) ?? "Cancel";
    const restartCancelDisabled =
        hasRestartTask && activeRestartTask?.cancellable === false;
    const hideRestartButton =
        !hasRestartTask &&
        [
            "OFFLINE",
            "UNKNOWN",
            "STOPPING",
            "CREATING",
            "DELETING",
            "INITIALIZING",
        ].includes(server.state);

    const handleStopClick = () => {
        if (activeShutdownTask) {
            if (!activeShutdownTask.cancellable) return;
            setLoading(true);
            cancelNodeTask(activeShutdownTask.nodeId, activeShutdownTask.id)
                .then(() => {
                    toast.success(
                        `Canceled delayed stop for ${server.displayName}`,
                    );
                })
                .finally(() => {
                    void shutdownQuery.refetch();
                    setLoading(false);
                });
            return;
        }

        dialog(
            <ServerPowerDialog
                action="stop"
                servers={[server]}
                setLoading={setLoading}
                isLoading={isAnyLoading}
            />,
        );
    };

    const handleRestartClick = () => {
        if (activeRestartTask) {
            if (!activeRestartTask.cancellable) return;
            setLoading(true);
            cancelNodeTask(activeRestartTask.nodeId, activeRestartTask.id)
                .then(() => {
                    toast.success(
                        `Canceled delayed restart for ${server.displayName}`,
                    );
                })
                .finally(() => {
                    void restartQuery.refetch();
                    setLoading(false);
                });
            return;
        }

        dialog(
            <ServerPowerDialog
                action="restart"
                servers={[server]}
                setLoading={setLoading}
                isLoading={isAnyLoading}
            />,
        );
    };

    return (
        <>
            <Button
                className={cn(
                    [
                        "ONLINE",
                        "INITIALIZING",
                        "RESTARTING",
                        "STOPPING",
                    ].includes(server.state)
                        ? "hidden"
                        : "",
                )}
                disabled={isAnyLoading || server.state != "OFFLINE"}
                size="tiny"
                variant="serverStart"
                onClick={() => {
                    setLoading(true);
                    startServer(server.id)
                        .then(() =>
                            toast.success(
                                `${server.displayName} is now starting!`,
                            ),
                        )
                        .finally(() => setLoading(false));
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <Play />
                    </>
                )}
                {["INITIALIZING"].includes(server.state)
                    ? "Starting..."
                    : "Start"}
            </Button>
            <div
                className={cn(
                    "flex flex-col gap-1 items-start",
                    hideStopButton && "hidden",
                )}>
                <Button
                    disabled={
                        isAnyLoading ||
                        (hasStopTask
                            ? stopCancelDisabled
                            : server.state != "ONLINE" || hasRestartTask)
                    }
                    size="tiny"
                    variant="serverStop"
                    onClick={handleStopClick}>
                    {isAnyLoading ? (
                        <Spinner size="tiny" />
                    ) : (
                        <>{hasStopTask ? <XIcon /> : <Power />}</>
                    )}
                    {hasStopTask ? stopCountdownLabel : "Stop"}
                </Button>
            </div>
            <Button
                className={cn(
                    [
                        "OFFLINE",
                        "UNKNOWN",
                        "CREATING",
                        "DELETING",
                        "ONLINE",
                    ].includes(server.state)
                        ? "hidden"
                        : "",
                )}
                disabled={isAnyLoading}
                size="tiny"
                variant="serverStop"
                onClick={() => {
                    dialog(
                        <ConfirmDialog
                            title={`Kill "${server.displayName}"`}
                            message={`Are you sure you want to kill the server process?`}
                            confirmText="Kill"
                            onConfirm={() => {
                                setLoading(true);
                                stopServer(server.id, true)
                                    .then(() =>
                                        toast.success(
                                            `${server.displayName} is now stopping!`,
                                        ),
                                    )
                                    .finally(() => setLoading(false));
                            }}
                        />,
                    );
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <Power />
                    </>
                )}
                Kill
            </Button>
            <div
                className={cn(
                    "flex flex-col gap-1 items-start",
                    hideRestartButton && "hidden",
                )}>
                <Button
                    disabled={
                        isAnyLoading ||
                        (hasRestartTask
                            ? restartCancelDisabled
                            : server.state != "ONLINE" || hasStopTask)
                    }
                    size="tiny"
                    variant="serverRestart"
                    onClick={handleRestartClick}>
                    {isAnyLoading ? (
                        <Spinner size="tiny" />
                    ) : (
                        <>{hasRestartTask ? <XIcon /> : <RotateCw />}</>
                    )}
                    {hasRestartTask ? restartCountdownLabel : "Restart"}
                </Button>
            </div>
            <Button
                disabled={isAnyLoading || server.state != "OFFLINE"}
                size="tiny"
                variant="tertiary"
                onClick={() => {
                    dialog(
                        <UpdateServerDialog
                            server={server}
                            isLoading={isAnyLoading}
                            setLoading={setLoading}
                        />,
                    );
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <CloudDownload />
                    </>
                )}
                Update / Verify
            </Button>
            {!disableBackups && (
                <Button
                    onClick={() =>
                        dialog(
                            <BackupServerDialog
                                server={server}
                                isLoading={isAnyLoading}
                                setLoading={setLoading}
                            />,
                        )
                    }
                    disabled={isAnyLoading || server.state === "UNKNOWN"}
                    size="tiny"
                    variant="tertiary">
                    {isAnyLoading ? (
                        <Spinner size="tiny" />
                    ) : (
                        <>
                            <Save />
                        </>
                    )}
                    Backup
                </Button>
            )}
            {!disableSettings && (
                <Button
                    disabled={isAnyLoading || saveDisabled}
                    onClick={onSave}
                    size="tiny">
                    {isAnyLoading ? (
                        <Spinner size="tiny" />
                    ) : (
                        <>
                            <CloudUpload />
                        </>
                    )}
                    Save
                </Button>
            )}
            <ServerManagementDropdown
                disabled={isAnyLoading}
                server={server}
                onDelete={() => dialog(<DeleteServerDialog server={server} />)}
            />
        </>
    );
}

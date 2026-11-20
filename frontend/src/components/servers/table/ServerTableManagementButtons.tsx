import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { cn } from "@/lib/utils.ts";
import { CloudDownload, Monitor, Play, Power, Save, XIcon } from "lucide-react";
import type { Server } from "@/store/serverStore.ts";
import { startServer, updateServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { router } from "@/Router.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ServerPowerDialog } from "@/components/servers/dialogs/ServerPowerDialog.tsx";
import { cancelNodeTask } from "@/api/nodeApi.ts";
import { useServerPowerTasks } from "@/hooks/useServerPowerTasks.ts";
import type { Node } from "@/store/nodeStore.ts";

type ServerSettingsHeaderButtonsProps = {
    server: Server;
    isAnyLoading?: boolean;
    setLoading: (value: boolean) => void;
    node?: Node | null;
};

export function ServerTableManagementButtons({
    server,
    isAnyLoading,
    setLoading,
    node,
}: ServerSettingsHeaderButtonsProps) {
    const { activeShutdownTask, shutdownQuery } = useServerPowerTasks(
        server,
        node ?? null,
    );

    const hasCancelableStopTask =
        !!activeShutdownTask && activeShutdownTask.cancellable;
    const hideStopButton =
        !hasCancelableStopTask &&
        ["OFFLINE", "UNKNOWN", "STOPPING", "CREATING", "DELETING"].includes(
            server.state,
        );
    const stopButtonAriaLabel = hasCancelableStopTask
        ? `Cancel delayed stop for ${server.displayName}`
        : `Stop server ${server.displayName}`;

    const handleStopClick = () => {
        if (hasCancelableStopTask && activeShutdownTask) {
            setLoading(true);
            cancelNodeTask(activeShutdownTask.nodeId, activeShutdownTask.id)
                .then(() =>
                    toast.success(
                        `Canceled delayed stop for ${server.displayName}`,
                    ),
                )
                .catch((error) => {
                    const message =
                        error instanceof Error
                            ? error.message
                            : "Unexpected error";
                    toast.error(
                        `Failed to cancel delayed stop for ${server.displayName}: ${message}`,
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
                isLoading={Boolean(isAnyLoading)}
                setLoading={setLoading}
            />,
        );
    };

    return (
        <div className="flex gap-3" onClick={(e) => e.stopPropagation()}>
            <Button
                aria-label={`Start server ${server.displayName}`}
                className={cn(
                    ["ONLINE", "INITIALIZING", "RESTARTING"].includes(
                        server.state,
                    )
                        ? "hidden"
                        : "",
                )}
                disabled={isAnyLoading || server.state != "OFFLINE"}
                size="smallIcon"
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
            </Button>
            <div
                className={cn(
                    "flex flex-col items-center gap-1",
                    hideStopButton && "hidden",
                )}>
                <Button
                    aria-label={stopButtonAriaLabel}
                    disabled={
                        isAnyLoading ||
                        (!hasCancelableStopTask && server.state != "ONLINE")
                    }
                    size="smallIcon"
                    variant="serverStop"
                    onClick={handleStopClick}>
                    {isAnyLoading ? (
                        <Spinner size="tiny" />
                    ) : (
                        <>{hasCancelableStopTask ? <XIcon /> : <Power />}</>
                    )}
                </Button>
            </div>
            <Button
                aria-label={`Update server ${server.displayName}`}
                disabled={isAnyLoading || server.state != "OFFLINE"}
                size="smallIcon"
                variant="tertiary"
                onClick={() => {
                    setLoading(true);
                    updateServer(server.id)
                        .then(() =>
                            toast.success(
                                `${server.displayName} is now updating / verifying!`,
                            ),
                        )
                        .finally(() => setLoading(false));
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <CloudDownload />
                    </>
                )}
            </Button>
            <Button
                aria-label={`Open RCON for ${server.displayName}`}
                disabled={isAnyLoading}
                size="smallIcon"
                variant="tertiary"
                onClick={() => {
                    router.navigate({ to: `/server/${server.id}/rcon` });
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <Monitor />
                    </>
                )}
            </Button>
            <Button
                aria-label={`Open backups for ${server.displayName}`}
                disabled={isAnyLoading}
                size="smallIcon"
                variant="tertiary"
                onClick={() => {
                    router.navigate({ to: `/server/${server.id}/backups` });
                }}>
                {isAnyLoading ? (
                    <Spinner size="tiny" />
                ) : (
                    <>
                        <Save />
                    </>
                )}
            </Button>
        </div>
    );
}

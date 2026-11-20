import { type ReactNode, useEffect } from "react";
import type { InfiniteData } from "@tanstack/react-query";
import { stompClient } from "@/api/stompConfig.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import {
    findBackupNameFromCache,
    refetchServerQuery,
    type Server,
    type ServerCommand,
    type ServerState,
} from "@/store/serverStore.ts";
import { useAuthStore } from "@/store/authStore.ts";
import {
    type NodeTask,
    applyNodeTaskUpdate,
    refetchNodeQuery,
    refetchNodeTasksQuery,
} from "@/store/nodeStore.ts";
import {
    DEFAULT_ACTION_LOG_PAGE_SIZE,
    type LogEntry,
    type TeamActionLogPage,
} from "@/store/teamStore.ts";
import { publishProfileUpdateEvent } from "@/store/eventStore.ts";
import { closeDialogsByTag, dialog } from "@/store/dialogStore.tsx";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { toast } from "sonner";

const asString = (value: unknown): string | null =>
    typeof value === "string" ? value : null;

export function StompSubscriber(props: { children?: ReactNode }) {
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

    useEffect(() => {
        if (!isAuthenticated) {
            if (stompClient.active) stompClient.deactivate().then();
            return;
        }

        stompClient.onConnect = () => {
            stompClient.subscribe("/user/queue/node/login", async () => {
                await refetchNodeQuery();
            });

            stompClient.subscribe("/user/queue/node/logout", async () => {
                await refetchNodeQuery();
            });

            stompClient.subscribe("/user/queue/node/settings", async () => {
                await refetchNodeQuery();
            });

            stompClient.subscribe(
                "/user/queue/node/task-update",
                async (data) => {
                    const body: { nodeTask: NodeTask } = JSON.parse(data.body);
                    applyNodeTaskUpdate(body.nodeTask);
                },
            );

            stompClient.subscribe("/user/queue/node/task-create", async () => {
                await refetchNodeTasksQuery();
            });

            stompClient.subscribe(
                "/user/queue/node/task-complete",
                async (data) => {
                    const body: { nodeTask: NodeTask } = JSON.parse(data.body);
                    const nodeTask: NodeTask = body.nodeTask;

                    if (nodeTask.state === "FAILED") {
                        dialog(
                            <>
                                <DialogHeader>
                                    <DialogTitle>Task Failed</DialogTitle>
                                    <DialogDescription>
                                        The task {nodeTask.id} failed with the
                                        error message:
                                    </DialogDescription>
                                </DialogHeader>
                                <div>{nodeTask.errorMessage ?? "UNKNOWN"}</div>
                                <DialogFooter className="flex flex-col gap-4 w-full mt-4">
                                    <DialogClose>
                                        <Button
                                            onClick={() =>
                                                closeDialogsByTag(
                                                    "nodeTaskFailed",
                                                )
                                            }
                                            variant="tertiary">
                                            Close
                                        </Button>
                                    </DialogClose>
                                </DialogFooter>
                            </>,
                            {
                                tag: "nodeTaskFailed",
                                modal: true,
                            },
                        );
                    }

                    if (
                        nodeTask.state === "SUCCEEDED" &&
                        nodeTask.type === "ROLLBACK"
                    ) {
                        const context = nodeTask.context as
                            | { backupId?: unknown; backupName?: unknown }
                            | undefined;

                        const backupId = asString(context?.backupId);
                        const backupNameFromContext = asString(
                            context?.backupName,
                        );
                        const backupName =
                            backupNameFromContext ??
                            findBackupNameFromCache(backupId) ??
                            backupId ??
                            "backup";

                        toast.success(
                            `Rollback to ${backupName} completed successfully.`,
                        );
                    }

                    await refetchNodeTasksQuery();
                },
            );

            stompClient.subscribe(
                "/user/queue/server/trigger-refetch",
                async () => {
                    await refetchServerQuery();
                },
            );

            stompClient.subscribe(
                "/user/queue/server/directory-change-failed",
                async () => {
                    await refetchServerQuery();
                },
            );

            stompClient.subscribe("/user/queue/server/state", async (data) => {
                const body: { serverId: string; state: ServerState } =
                    JSON.parse(data.body);

                queryClient.setQueriesData(
                    { queryKey: queryKeys.servers.all },
                    (oldData: Server[] | undefined) => {
                        if (!oldData) {
                            refetchServerQuery().then();
                            return oldData;
                        }
                        const index = oldData.findIndex(
                            (server) => server.id === body.serverId,
                        );
                        if (index === -1) {
                            refetchServerQuery().then();
                            return oldData;
                        }
                        const newData = [...oldData];
                        newData[index] = {
                            ...newData[index],
                            state: body.state,
                        };
                        return newData;
                    },
                );
            });

            stompClient.subscribe("/user/queue/server/rcon", async (data) => {
                const body: { serverId: string; command: ServerCommand } =
                    JSON.parse(data.body);
                queryClient.setQueriesData(
                    { queryKey: queryKeys.servers.all },
                    (oldData: Server[] | undefined) => {
                        if (!oldData) {
                            refetchServerQuery().then();
                            return oldData;
                        }
                        const index = oldData.findIndex(
                            (server) => server.id === body.serverId,
                        );
                        if (index === -1) {
                            refetchServerQuery().then();
                            return oldData;
                        }
                        const newData = [...oldData];
                        newData[index] = {
                            ...newData[index],
                            commands: [
                                ...newData[index].commands,
                                body.command,
                            ],
                        };
                        return newData;
                    },
                );
            });

            stompClient.subscribe("/user/queue/server/delete", async () => {
                await refetchServerQuery();
            });

            stompClient.subscribe("/user/queue/server/backup", async () => {
                await refetchServerQuery();
            });

            stompClient.subscribe(
                "/user/queue/server/delete-backup",
                async () => {
                    await refetchServerQuery();
                },
            );

            stompClient.subscribe(
                "/user/queue/setting/profile-update",
                async (data) => {
                    const body: { profileId: string; userId: string } =
                        JSON.parse(data.body);
                    publishProfileUpdateEvent(body.profileId, body.userId);
                },
            );

            stompClient.subscribe("/user/queue/team/change", async () => {
                await queryClient.invalidateQueries({
                    queryKey: queryKeys.teams.all,
                });
            });

            stompClient.subscribe(
                "/user/queue/team/action-log",
                async (data) => {
                    const body: LogEntry = JSON.parse(data.body);
                    const teamId = body.teamId;

                    if (!teamId) {
                        await queryClient.invalidateQueries({
                            queryKey: queryKeys.teamActionLog.all,
                        });
                        return;
                    }

                    const matchingQueries = queryClient
                        .getQueryCache()
                        .findAll({ queryKey: queryKeys.teamActionLog.all });

                    matchingQueries.forEach((query) => {
                        const [, meta] = query.queryKey as [
                            string,
                            { teamId?: string | null; pageSize?: number },
                        ];
                        if (meta?.teamId !== teamId) {
                            return;
                        }
                        queryClient.setQueryData<
                            InfiniteData<TeamActionLogPage>
                        >(query.queryKey, (oldData) => {
                            if (!oldData) return oldData;

                            const pageSize =
                                meta?.pageSize ?? DEFAULT_ACTION_LOG_PAGE_SIZE;
                            const safePageSize = Math.max(pageSize, 1);
                            const existingEntries =
                                oldData.pages.flatMap(
                                    (page) => page.content ?? [],
                                ) ?? [];

                            if (
                                existingEntries.some(
                                    (entry) => entry.id === body.id,
                                )
                            ) {
                                return oldData;
                            }

                            const filteredExisting = existingEntries.filter(
                                (entry) => entry.id !== body.id,
                            );

                            const pagesLoaded =
                                oldData.pages.length > 0
                                    ? oldData.pages.length
                                    : 1;
                            const maxEntries =
                                pagesLoaded * safePageSize || safePageSize;

                            const updatedEntries = [body, ...filteredExisting];
                            const limitedEntries =
                                maxEntries > 0
                                    ? updatedEntries.slice(0, maxEntries)
                                    : updatedEntries;

                            const baseTotalElements =
                                oldData.pages[0]?.totalElements ??
                                filteredExisting.length;
                            const newTotalElements =
                                baseTotalElements > 0
                                    ? baseTotalElements + 1
                                    : filteredExisting.length + 1;

                            const baseTotalPages =
                                oldData.pages[0]?.totalPages ??
                                Math.max(
                                    1,
                                    Math.ceil(
                                        Math.max(filteredExisting.length, 1) /
                                            safePageSize,
                                    ),
                                );
                            const newTotalPages = Math.max(
                                baseTotalPages,
                                Math.ceil(newTotalElements / safePageSize),
                            );

                            if (oldData.pages.length === 0) {
                                const newPage: TeamActionLogPage = {
                                    content: limitedEntries,
                                    page: 0,
                                    size: safePageSize,
                                    totalElements: newTotalElements,
                                    totalPages: newTotalPages,
                                    first: true,
                                    last: true,
                                    numberOfElements: limitedEntries.length,
                                    empty: limitedEntries.length === 0,
                                };
                                return {
                                    pages: [newPage],
                                    pageParams:
                                        oldData.pageParams.length > 0
                                            ? oldData.pageParams
                                            : [0],
                                };
                            }

                            const nextPages = oldData.pages.map(
                                (page, index) => {
                                    const start = index * safePageSize;
                                    const end = start + safePageSize;
                                    const slice = limitedEntries.slice(
                                        start,
                                        end,
                                    );
                                    return {
                                        ...page,
                                        content: slice,
                                        numberOfElements: slice.length,
                                        empty: slice.length === 0,
                                        totalElements: newTotalElements,
                                        totalPages: newTotalPages,
                                    };
                                },
                            );

                            return {
                                ...oldData,
                                pages: nextPages,
                            };
                        });
                    });
                },
            );
        };

        if (!stompClient.active) stompClient.activate();

        return () => {
            if (stompClient.active) stompClient.deactivate().then();
        };
    }, [isAuthenticated]);

    return <>{props.children}</>;
}

import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import { useParams } from "@tanstack/react-router";
import { useActiveTeam } from "@/store/teamStore.ts";
import {
    type ServerBackup,
    useServerByIdQuery,
    useSettingProfileQuery,
} from "@/store/serverStore.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { NotFoundPage } from "@/pages/NotFoundPage.tsx";
import { Button } from "@/components/ui/button.tsx";
import { RotateCcw, Save, Search, Trash } from "lucide-react";
import { Input, InputIcon, InputRoot } from "@/components/ui/input.tsx";
import {
    flexRender,
    type RowSelectionState,
    useTable,
} from "@tanstack/react-table";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import { cn } from "@/lib/utils.ts";
import { useMemo, useRef, useState } from "react";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import { closeDialog, dialog } from "@/store/dialogStore.tsx";
import { BackupServerDialog } from "@/components/servers/dialogs/BackupServerDialog.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { RollbackServerDialog } from "@/components/servers/dialogs/RollbackServerDialog.tsx";
import { deleteBackup } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { ServerSettingsHeader } from "@/components/servers/settings/ServerSettingsHeader.tsx";
import { useMaxPlayers } from "@/hooks/useMaxPlayers.ts";
import { HoverPopover } from "@/components/custom/HoverPopover.tsx";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import { SelectionActionBar } from "@/components/custom/table/SelectionActionBar.tsx";
import { useNodeByIdQuery, useNodeTasksByType } from "@/store/nodeStore.ts";
import {
    isNodeTaskActive,
    nodeTaskAffectsServer,
} from "@/hooks/useServerPowerTasks.ts";
import { ServerDirectoryChangeOverlay } from "@/components/servers/ServerDirectoryChangeOverlay.tsx";
import { ServerBackupDirectoryChangeOverlay } from "@/components/servers/ServerBackupDirectoryChangeOverlay.tsx";

type BackupRow = ServerBackup & {
    isTask?: boolean;
};

export function ServerBackupsPage() {
    const [isAnyLoading, setLoading] = useState(false);
    const [rowSelection, setRowSelection] = useState<RowSelectionState>({});

    const params = useParams({ strict: false });
    const activeTeam = useActiveTeam();

    const containerRef = useRef<HTMLDivElement>(null);

    const { data: server, isLoading: isServerLoading } = useServerByIdQuery(
        activeTeam,
        params.serverId ?? "",
    );

    const { data: profile, isLoading: isProfileLoading } =
        useSettingProfileQuery(activeTeam, server?.settingProfileId);

    const maxPlayers = useMaxPlayers(server, profile);

    const columnsHelper = createColumnHelper<BackupRow>();

    const { data: node } = useNodeByIdQuery(activeTeam, server?.nodeId);

    const { data: backupTasks } = useNodeTasksByType(
        activeTeam,
        node ?? null,
        "BACKUP",
    );

    const serverId = server?.id;

    const activeBackupTasks = useMemo(
        () =>
            serverId
                ? (backupTasks ?? []).filter(
                      (task) =>
                          nodeTaskAffectsServer(task, serverId) &&
                          isNodeTaskActive(task),
                  )
                : [],
        [backupTasks, serverId],
    );

    const backupTaskRows = useMemo<BackupRow[]>(() => {
        if (!serverId) return [];
        const sortedTasks = [...activeBackupTasks].sort(
            (a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt),
        );

        return sortedTasks.map((task) => {
            const context = task.context as {
                backupName?: unknown;
            };
            const backupName =
                typeof context?.backupName === "string"
                    ? context.backupName
                    : "Pending Backup";
            return {
                backupId: `task-${task.id}`,
                serverId: serverId,
                createdAt: task.createdAt,
                expiresAt: task.createdAt,
                autoBackup: false,
                name: backupName,
                size: 0,
                isTask: true,
            };
        });
    }, [activeBackupTasks, serverId]);

    const columns = columnsHelper.columns([
        columnsHelper.display({
            id: "select",
            header: ({ table }) => (
                <Checkbox
                    aria-label="Select all backups"
                    checked={
                        table.getIsAllPageRowsSelected() ||
                        (table.getIsSomePageRowsSelected() && "indeterminate")
                    }
                    onCheckedChange={(value) =>
                        table.toggleAllPageRowsSelected(!!value)
                    }
                />
            ),
            cell: ({ row }) => (
                <Checkbox
                    aria-label={`Select backup ${row.original.name}`}
                    checked={row.getIsSelected()}
                    disabled={row.original.isTask}
                    onCheckedChange={(value) => row.toggleSelected(!!value)}
                />
            ),
        }),
        columnsHelper.accessor("name", {
            id: "name",
            header: "Backup Name",
        }),
        columnsHelper.accessor("createdAt", {
            id: "createdAt",
            header: "Created At",
            cell: (info) => new Date(info.getValue()).toLocaleString(),
        }),
        columnsHelper.accessor("expiresAt", {
            id: "expiresAt",
            header: "Expires At",
            cell: (info) =>
                info.row.original.isTask ? (
                    <Spinner size="tiny" />
                ) : (
                    new Date(info.getValue()).toLocaleString()
                ),
        }),
        columnsHelper.accessor("size", {
            id: "size",
            header: "Size (KB)",
            cell: (info) => {
                if (info.row.original.isTask) {
                    return <Spinner size="tiny" />;
                }
                const size = info.getValue() as number;
                return `${(size / 1024).toFixed(2)} KB`;
            },
        }),
        columnsHelper.display({
            id: "management",
            header: "Management",
            cell: (info) => {
                if (info.row.original.isTask) {
                    return (
                        <div className="flex items-center justify-center">
                            <Spinner size="tiny" />
                        </div>
                    );
                }
                return (
                    <div className="flex gap-4">
                        <HoverPopover
                            content="Server must be offline for a rollback"
                            disabled={
                                isAnyLoading || server?.state === "OFFLINE"
                            }>
                            <Button
                                disabled={
                                    isAnyLoading || server?.state !== "OFFLINE"
                                }
                                onClick={() => {
                                    dialog(
                                        <RollbackServerDialog
                                            backup={info.row.original}
                                            isLoading={isAnyLoading}
                                            server={server}
                                            setLoading={setLoading}
                                        />,
                                        {
                                            tag: "rollbackServerDialog",
                                        },
                                    );
                                }}>
                                {isAnyLoading ? <Spinner /> : <RotateCcw />}
                                Rollback
                            </Button>
                        </HoverPopover>
                        <Button
                            aria-label="Delete backup"
                            size="icon"
                            variant="destructive"
                            disabled={
                                isAnyLoading || server?.state === "UNKNOWN"
                            }
                            onClick={() => {
                                dialog(
                                    <ConfirmDialog
                                        title="Delete Backup"
                                        message={`Are you sure you want to delete the backup "${info.row.original.name}"? This action cannot be undone.`}
                                        confirmText="Delete Backup"
                                        onConfirm={() => {
                                            if (!server) return;
                                            setLoading(true);
                                            deleteBackup(
                                                server.id,
                                                info.row.original.backupId,
                                            )
                                                .then(() => {
                                                    toast.success(
                                                        "Started deletion of backup successfully. This may take a while.",
                                                    );
                                                    closeDialog();
                                                })
                                                .finally(() =>
                                                    setLoading(false),
                                                );
                                        }}
                                    />,
                                );
                            }}>
                            {isAnyLoading ? <Spinner /> : <Trash />}
                        </Button>
                    </div>
                );
            },
        }),
    ]);

    const tableData = useMemo<BackupRow[]>(
        () => [
            ...backupTaskRows,
            ...((server?.backups as BackupRow[] | undefined) ?? []),
        ],
        [backupTaskRows, server?.backups],
    );

    const hasActiveBackupTask = activeBackupTasks.length > 0;

    const table = useTable({
        features: appTableFeatures,
        data: tableData,
        columns: columns,
        initialState: {
            pagination: {
                pageIndex: 0,
                pageSize: 10,
            },
            sorting: [
                {
                    id: "createdAt",
                    desc: true,
                },
            ],
        },
        enableRowSelection: (row) => !row.original.isTask,
        onRowSelectionChange: setRowSelection,
        state: {
            rowSelection,
        },
    });

    useAutoPageSize(table, containerRef);

    const selectedBackups = table
        .getSelectedRowModel()
        .flatRows.map((row) => row.original);
    const canDeleteSelected =
        selectedBackups.length > 0 &&
        !isAnyLoading &&
        server?.state !== "UNKNOWN";

    const handleDeleteSelected = () => {
        if (!server || !selectedBackups.length) return;
        dialog(
            <ConfirmDialog
                title="Delete Backups"
                message={`Delete ${selectedBackups.length} selected backup${
                    selectedBackups.length > 1 ? "s" : ""
                }? This action cannot be undone.`}
                confirmText="Delete Backups"
                onConfirm={() => {
                    setLoading(true);
                    const deleteSelected = async () => {
                        for (const backup of selectedBackups) {
                            await deleteBackup(server.id, backup.backupId);
                        }
                    };
                    deleteSelected()
                        .then(() => {
                            toast.success(
                                `Started deletion of ${selectedBackups.length} backup${
                                    selectedBackups.length > 1 ? "s" : ""
                                }.`,
                            );
                            closeDialog();
                        })
                        .finally(() => {
                            table.resetRowSelection();
                            setLoading(false);
                        });
                }}
            />,
        );
    };

    if (isServerLoading) {
        return (
            <div className="w-full h-full flex justify-center items-center">
                <Spinner size="large" />
            </div>
        );
    }

    if (!server) {
        return <NotFoundPage />;
    }

    return (
        <ServerDirectoryChangeOverlay server={server}>
            <ServerBackupDirectoryChangeOverlay
                server={server}
                className="w-full h-full py-0 flex flex-col">
                <ServerSettingsHeader
                    server={server}
                    maxPlayers={maxPlayers}
                    isLoading={isAnyLoading}
                    isProfileLoading={isProfileLoading}
                    disableSettings
                />
                <div className="w-full h-full bg-secondary rounded-md pt-2 flex flex-col mt-5">
                    <div className="p-4 flex flex-col md:flex-row items-center gap-4">
                        <Button
                            disabled={
                                isAnyLoading ||
                                !["ONLINE", "OFFLINE"].includes(server.state) ||
                                hasActiveBackupTask
                            }
                            onClick={() => {
                                dialog(
                                    <BackupServerDialog
                                        server={server}
                                        isLoading={isAnyLoading}
                                        setLoading={setLoading}
                                    />,
                                );
                            }}>
                            {isAnyLoading ? <Spinner /> : <Save />}
                            Create Backup
                        </Button>
                        <InputRoot className="max-w-56 mx-5">
                            <InputIcon>
                                <Search className="text-muted-foreground" />
                            </InputIcon>
                            <Input
                                value={
                                    (table
                                        .getColumn("name")
                                        ?.getFilterValue() as string) ?? ""
                                }
                                onChange={(event) =>
                                    table
                                        .getColumn("name")
                                        ?.setFilterValue(event.target.value)
                                }
                                placeholder="Search Backup"
                            />
                        </InputRoot>
                        <Button
                            variant="destructive"
                            className="md:ml-auto"
                            disabled={
                                isAnyLoading || server?.state === "UNKNOWN"
                            }
                            onClick={() => {
                                dialog(
                                    <ConfirmDialog
                                        title="Delete All Backups"
                                        message="Are you sure you want to delete all backups? This action cannot be undone."
                                        confirmText="Delete All Backups"
                                        onConfirm={() => {
                                            const deleteAll = async () => {
                                                setLoading(true);
                                                for (const backup of server.backups) {
                                                    await deleteBackup(
                                                        server.id,
                                                        backup.backupId,
                                                    );
                                                }
                                            };
                                            deleteAll()
                                                .then(async () => {
                                                    toast.success(
                                                        "Started deletion of all backups successfully. This may take a while.",
                                                    );
                                                    closeDialog();
                                                })
                                                .finally(() =>
                                                    setLoading(false),
                                                );
                                        }}
                                    />,
                                );
                            }}>
                            {isAnyLoading ? <Spinner /> : <Trash />}
                            Delete all Backups
                        </Button>
                    </div>
                    <div
                        ref={containerRef}
                        className="grow overflow-y-auto h-[530px] lg:h-auto">
                        <Table className="border-b">
                            <TableHeader className="bg-primary-foreground/30">
                                {table.getHeaderGroups().map((headerGroup) => (
                                    <TableRow key={headerGroup.id}>
                                        {headerGroup.headers.map((header) => {
                                            return (
                                                <TableHead
                                                    className={cn(
                                                        header.column.id ===
                                                            "select" &&
                                                            "pl-5 w-14",
                                                        header.column.id ===
                                                            "name" &&
                                                            "pl-2 lg:w-[320px]",
                                                        header.column.id ===
                                                            "management" &&
                                                            "pr-5",
                                                    )}
                                                    key={header.id}>
                                                    {header.isPlaceholder
                                                        ? null
                                                        : flexRender(
                                                              header.column
                                                                  .columnDef
                                                                  .header,
                                                              header.getContext(),
                                                          )}
                                                </TableHead>
                                            );
                                        })}
                                    </TableRow>
                                ))}
                            </TableHeader>
                            <TableBody>
                                {table.getRowModel().rows?.length ? (
                                    table.getRowModel().rows.map((row) => (
                                        <TableRow
                                            key={row.id}
                                            data-state={
                                                row.getIsSelected() &&
                                                "selected"
                                            }>
                                            {row
                                                .getVisibleCells()
                                                .map((cell) => (
                                                    <TableCell
                                                        className={cn(
                                                            cell.column.id ===
                                                                "select" &&
                                                                "pl-5 w-14",
                                                            cell.column.id ===
                                                                "name" &&
                                                                "pl-2 lg:w-[320px]",
                                                            cell.column.id ===
                                                                "management" &&
                                                                "pr-5",
                                                        )}
                                                        key={cell.id}>
                                                        {flexRender(
                                                            cell.column
                                                                .columnDef.cell,
                                                            cell.getContext(),
                                                        )}
                                                    </TableCell>
                                                ))}
                                        </TableRow>
                                    ))
                                ) : (
                                    <TableRow>
                                        <TableCell
                                            colSpan={columns.length}
                                            className="h-24 text-center">
                                            No results.
                                        </TableCell>
                                    </TableRow>
                                )}
                            </TableBody>
                        </Table>
                        <SelectionActionBar
                            selectedCount={selectedBackups.length}
                            entityLabel="backup"
                            onClearSelection={() => table.resetRowSelection()}>
                            <Button
                                variant="destructive"
                                size="sm"
                                disabled={!canDeleteSelected}
                                onClick={handleDeleteSelected}>
                                {isAnyLoading ? (
                                    <Spinner size="tiny" />
                                ) : (
                                    <Trash />
                                )}
                                Delete Selected
                            </Button>
                        </SelectionActionBar>
                    </div>
                    <TableNavigation table={table} />
                </div>
            </ServerBackupDirectoryChangeOverlay>
        </ServerDirectoryChangeOverlay>
    );
}

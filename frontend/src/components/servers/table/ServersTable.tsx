import { appTableFeatures, createColumnHelper, type Row } from "@/lib/table.ts";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import {
    flexRender,
    type RowSelectionState,
    useTable,
} from "@tanstack/react-table";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { CloudDownload, Play, Power, Save, Search } from "lucide-react";
import { type FC, useMemo, useRef, useState } from "react";
import { useActiveTeam } from "@/store/teamStore.ts";
import {
    type Server,
    type SettingProfile,
    useServerQuery,
    useSettingProfileQuery,
} from "@/store/serverStore.ts";
import { StatusIcon } from "@/components/custom/StatusIcon.tsx";
import { cn, serverStateOrder, statusColorTextMap } from "@/lib/utils.ts";
import { Input, InputIcon, InputRoot } from "@/components/ui/input.tsx";
import { router } from "@/Router.tsx";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { MultiSelect } from "@/components/ui/multi-select.tsx";
import { type Node, useNodeQuery } from "@/store/nodeStore.ts";
import { ServerManagementDropdown } from "@/components/servers/ServerManagementDropdown.tsx";
import { ServerTableManagementButtons } from "@/components/servers/table/ServerTableManagementButtons.tsx";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import { DeleteServerDialog } from "@/components/servers/dialogs/DeleteServerDialog.tsx";
import { ServerPowerDialog } from "@/components/servers/dialogs/ServerPowerDialog.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import { SelectionActionBar } from "@/components/custom/table/SelectionActionBar.tsx";
import { Button } from "@/components/ui/button.tsx";
import { backupServer, startServer, updateServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { format } from "date-fns";

type ServersTableProps = {
    pageSize?: number;
};

export function ServersTable(props: ServersTableProps) {
    const [activeTab, setActiveTab] = useState("all");
    const [loading, setLoading] = useState(false);
    const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
    const containerRef = useRef<HTMLDivElement>(null);

    const activeTeam = useActiveTeam();

    const { data: servers } = useServerQuery(activeTeam);
    const { data: nodes } = useNodeQuery(activeTeam);
    const nodeMap = useMemo(() => {
        const map = new Map<string, Node>();
        nodes?.forEach((node) => {
            map.set(node.id, node);
        });
        return map;
    }, [nodes]);

    const totalServers = servers?.length || 0;
    const onlineServers =
        servers?.filter((server) => server.state === "ONLINE").length || 0;
    const initializingServers =
        servers?.filter((server) => server.state === "INITIALIZING").length ||
        0;
    const offlineServers =
        servers?.filter((server) => server.state === "OFFLINE").length || 0;

    const updateActiveTab = (status: string) => {
        setActiveTab(status);

        table
            .getColumn("status")
            ?.setFilterValue(
                status === "all" ? undefined : status.toUpperCase(),
            );
    };

    const columnsHelper = createColumnHelper<Server>();

    const MapCell: FC<{
        server: Server;
        getValue: (
            profile: SettingProfile | undefined,
            server: Server,
        ) => string;
    }> = ({ server, getValue }) => {
        const { data: profile, isLoading } = useSettingProfileQuery(
            activeTeam,
            server.settingProfileId,
        );
        if (isLoading) return <Spinner />;
        return <span>{getValue(profile, server)}</span>;
    };

    const columns = columnsHelper.columns([
        columnsHelper.display({
            id: "select",
            header: ({ table }) => (
                <Checkbox
                    aria-label="Select all servers"
                    checked={
                        table.getIsAllPageRowsSelected() ||
                        (table.getIsSomePageRowsSelected() && "indeterminate")
                    }
                    onCheckedChange={(value) =>
                        table.toggleAllPageRowsSelected(!!value)
                    }
                    onClick={(event) => event.stopPropagation()}
                />
            ),
            cell: ({ row }) => (
                <Checkbox
                    aria-label={`Select ${row.original.displayName}`}
                    checked={row.getIsSelected()}
                    onCheckedChange={(value) => row.toggleSelected(!!value)}
                    onClick={(event) => event.stopPropagation()}
                />
            ),
        }),
        columnsHelper.accessor("displayName", {
            id: "serverName",
            header: "Server Name",
            enableSorting: true,
        }),
        columnsHelper.accessor("nodeId", {
            id: "node",
            header: "Node",
            cell: (info) => {
                const node = nodes?.find((n) => n.id === info.getValue());
                return (
                    <span>{node ? node.settings.name : info.getValue()}</span>
                );
            },
        }),
        columnsHelper.accessor("gameType", {
            id: "gameType",
            header: "Game Type",
            enableSorting: true,
            cell: (info) => {
                const gameTypeLabels: Record<string, string> = {
                    ARK_ASCENDED: "ASA",
                    ARK_EVOLVED: "ASE",
                };
                const val = info.getValue();
                return <span>{gameTypeLabels[val] ?? val}</span>;
            },
        }),
        columnsHelper.accessor("settingProfileId", {
            id: "map",
            header: "Map",
            cell: (info) => (
                <MapCell
                    getValue={(profile) => profile?.map ?? "-"}
                    server={info.row.original}
                />
            ),
        }),
        columnsHelper.accessor("settingProfileId", {
            id: "mods",
            header: "Mods",
            cell: (info) => (
                <MapCell
                    getValue={(profile) => {
                        const mods =
                            profile?.gameUserSettings["ServerSettings"][
                                "ActiveMods"
                            ] ?? "";
                        if (!mods) return "0";
                        return (mods as string).split(",").length.toString();
                    }}
                    server={info.row.original}
                />
            ),
        }),
        columnsHelper.accessor("settingProfileId", {
            id: "playerCount",
            header: "Players",
            cell: (info) => (
                <MapCell
                    getValue={(profile, server) => {
                        const maxPlayers =
                            server.gameType === "ARK_EVOLVED"
                                ? (profile?.questionMarkParams["MaxPlayers"] ??
                                  "70")
                                : server.gameType === "ARK_ASCENDED"
                                  ? (profile?.hyphenParams?.[
                                        "WinLiveMaxPlayers"
                                    ] ?? "70")
                                  : "70";
                        return `${server.onlinePlayers ?? 0}/${maxPlayers}`;
                    }}
                    server={info.row.original}
                />
            ),
        }),
        columnsHelper.accessor("state", {
            id: "status",
            header: "Status",
            sortFn: (rowA, rowB) => {
                const order = serverStateOrder;
                return (
                    order.indexOf(rowA.original.state) -
                    order.indexOf(rowB.original.state)
                );
            },
            enableSorting: true,
            cell: (info) => (
                <p className={statusColorTextMap[info.getValue()]}>
                    {info.getValue()}
                </p>
            ),
        }),
        columnsHelper.display({
            id: "management",
            header: "Management",
            cell: (info) => {
                return (
                    <ServerTableManagementButtons
                        server={info.row.original}
                        setLoading={setLoading}
                        isAnyLoading={loading}
                        node={nodeMap.get(info.row.original.nodeId)}
                    />
                );
            },
        }),
        columnsHelper.display({
            id: "additionalActions",
            cell: (info) => {
                return (
                    <div className="flex justify-end gap-3">
                        <ServerManagementDropdown
                            server={info.row.original}
                            onDelete={() => {
                                dialog(
                                    <DeleteServerDialog
                                        server={info.row.original}
                                    />,
                                );
                            }}
                        />
                    </div>
                );
            },
        }),
    ]);

    const nodeIdArrayFilter = (
        row: Row<Server>,
        _: string,
        filterValue: string,
    ) => {
        if (!filterValue) return true;
        return filterValue.split(",").includes(row.original.nodeId);
    };

    const tableData = useMemo(() => servers ?? [], [servers]);

    const table = useTable({
        features: appTableFeatures,
        data: tableData,
        columns: columns,
        globalFilterFn: nodeIdArrayFilter,
        initialState: {
            pagination: {
                pageIndex: 0,
                pageSize: props.pageSize ?? 10,
            },
            sorting: [
                {
                    id: "status",
                    desc: false,
                },
            ],
        },
        enableRowSelection: true,
        onRowSelectionChange: setRowSelection,
        state: {
            rowSelection,
        },
    });

    useAutoPageSize(table, containerRef, props.pageSize === undefined);

    const selectedServers = table
        .getSelectedRowModel()
        .flatRows.map((row) => row.original);
    const offlineStates = ["OFFLINE"];
    const onlineStates = ["ONLINE"];
    const backupAllowedStates = ["ONLINE", "OFFLINE"];
    const startableServers = selectedServers.filter((server) =>
        offlineStates.includes(server.state),
    );
    const stoppableServers = selectedServers.filter((server) =>
        onlineStates.includes(server.state),
    );
    const updateableServers = startableServers;
    const backupableServers = selectedServers.filter((server) =>
        backupAllowedStates.includes(server.state),
    );

    const allSelectedEligible = (eligibleServers: Server[]) =>
        eligibleServers.length > 0 &&
        eligibleServers.length === selectedServers.length;

    const canBulkStart = allSelectedEligible(startableServers) && !loading;
    const canBulkStop = allSelectedEligible(stoppableServers) && !loading;
    const canBulkUpdate = allSelectedEligible(updateableServers) && !loading;
    const canBulkBackup = allSelectedEligible(backupableServers) && !loading;

    const runServerAction = (
        targets: Server[],
        perform: (server: Server) => Promise<unknown>,
        successMessage: (server: Server) => string,
        failureMessage: (server: Server) => string,
    ) => {
        if (!targets.length) return;
        setLoading(true);
        Promise.all(
            targets.map((server) =>
                perform(server)
                    .then(() => toast.success(successMessage(server)))
                    .catch((error) => {
                        const message =
                            error instanceof Error
                                ? error.message
                                : "Unexpected error";
                        toast.error(`${failureMessage(server)}: ${message}`);
                    }),
            ),
        ).finally(() => {
            setLoading(false);
            table.resetRowSelection();
        });
    };

    const handleBulkStart = () => {
        runServerAction(
            startableServers,
            (server) => startServer(server.id),
            (server) => `${server.displayName} is now starting!`,
            (server) => `Failed to start ${server.displayName}`,
        );
    };

    const handleBulkStop = () => {
        if (!stoppableServers.length) return;
        dialog(
            <ServerPowerDialog
                action="stop"
                servers={stoppableServers}
                isLoading={loading}
                setLoading={setLoading}
                onComplete={() => table.resetRowSelection()}
            />,
        );
    };

    const handleBulkUpdate = () => {
        runServerAction(
            updateableServers,
            (server) => updateServer(server.id),
            (server) => `${server.displayName} is now updating / verifying!`,
            (server) => `Failed to update ${server.displayName}`,
        );
    };

    const handleBulkBackup = () => {
        const timestamp = format(new Date(), "yyyy.MM.dd_HH-mm-ss");
        runServerAction(
            backupableServers,
            (server) =>
                backupServer(server.id, `${timestamp}_${server.displayName}`),
            (server) => `${server.displayName} backup started!`,
            (server) => `Failed to backup ${server.displayName}`,
        );
    };

    return (
        <>
            <Tabs
                value={activeTab}
                onValueChange={updateActiveTab}
                className="h-auto"
                activationMode="manual">
                <TabsList className="text-primary-foreground border-b-muted px-4 justify-center lg:justify-start">
                    <TabsTrigger className="gap-2 uppercase" value="all">
                        <p className="hidden lg:block">All Servers</p>
                        <StatusIcon size="lg" box status="UNKNOWN">
                            <p className="leading-4 text-sm">{totalServers}</p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger className="gap-2 uppercase" value="online">
                        <p className="hidden lg:block">Online</p>
                        <StatusIcon size="lg" box status="ONLINE">
                            <p className="leading-4 text-sm">{onlineServers}</p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger
                        className="gap-2 uppercase"
                        value="initializing">
                        <p className="hidden lg:block">Initializing</p>
                        <StatusIcon size="lg" box status="INITIALIZING">
                            <p className="leading-4 text-sm">
                                {initializingServers}
                            </p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger className="gap-2 uppercase" value="offline">
                        <p className="hidden lg:block">Offline</p>
                        <StatusIcon size="lg" box status="OFFLINE">
                            <p className="leading-4 text-sm">
                                {offlineServers}
                            </p>
                        </StatusIcon>
                    </TabsTrigger>
                </TabsList>
            </Tabs>
            <div className="flex flex-col lg:flex-row my-5 mx-auto lg:mx-5 gap-3 lg:gap-5 items-center">
                <MultiSelect
                    className="max-w-80"
                    variant="secondary"
                    placeholder="Filter by Node"
                    maxCount={1}
                    onValueChange={(value) => {
                        table.setGlobalFilter(value.join(","));
                    }}
                    options={
                        nodes?.map((node) => ({
                            value: node.id,
                            label: node.settings.name,
                        })) ?? []
                    }
                />
                <InputRoot className="max-w-56 min-h-10">
                    <InputIcon className="top-[0.6rem]">
                        <Search className="text-muted-foreground" />
                    </InputIcon>
                    <Input
                        className="min-h-10"
                        value={
                            (table
                                .getColumn("serverName")
                                ?.getFilterValue() as string) ?? ""
                        }
                        onChange={(event) =>
                            table
                                .getColumn("serverName")
                                ?.setFilterValue(event.target.value)
                        }
                        placeholder="Search Server"
                    />
                </InputRoot>
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
                                                header.column.id === "select" &&
                                                    "pl-5 w-14",
                                                header.column.id ===
                                                    "serverName" &&
                                                    "pl-2 lg:w-[320px]",
                                            )}
                                            key={header.id}>
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                      header.column.columnDef
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
                                    onClick={() =>
                                        router.navigate({
                                            to: `/server/${row.original.id}/settings`,
                                        })
                                    }
                                    className="cursor-pointer"
                                    key={row.id}
                                    data-state={
                                        row.getIsSelected() && "selected"
                                    }>
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell
                                            className={cn(
                                                cell.column.id === "select" &&
                                                    "pl-5 w-14",
                                                cell.column.id ===
                                                    "serverName" &&
                                                    "pl-2 lg:w-[320px]",
                                            )}
                                            onClick={
                                                cell.column.id === "select"
                                                    ? (event) =>
                                                          event.stopPropagation()
                                                    : undefined
                                            }
                                            key={cell.id}>
                                            {flexRender(
                                                cell.column.columnDef.cell,
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
                    selectedCount={selectedServers.length}
                    entityLabel="server"
                    onClearSelection={() => table.resetRowSelection()}>
                    <Button
                        variant="serverStart"
                        size="sm"
                        disabled={!canBulkStart}
                        onClick={handleBulkStart}>
                        <Play className="size-4" />
                        Start Selected
                    </Button>
                    <Button
                        variant="serverStop"
                        size="sm"
                        disabled={!canBulkStop}
                        onClick={handleBulkStop}>
                        <Power className="size-4" />
                        Stop Selected
                    </Button>
                    <Button
                        variant="tertiary"
                        size="sm"
                        disabled={!canBulkUpdate}
                        onClick={handleBulkUpdate}>
                        <CloudDownload className="size-4" />
                        Update Selected
                    </Button>
                    <Button
                        variant="tertiary"
                        size="sm"
                        disabled={!canBulkBackup}
                        onClick={handleBulkBackup}>
                        <Save className="size-4" />
                        Backup Selected
                    </Button>
                </SelectionActionBar>
            </div>
            <TableNavigation table={table} />
        </>
    );
}

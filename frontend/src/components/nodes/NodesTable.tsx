import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import { cn, nodeStateOrder, statusColorTextMap } from "@/lib/utils.ts";
import { flexRender, useTable } from "@tanstack/react-table";
import { router } from "@/Router.tsx";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { StatusIcon } from "@/components/custom/StatusIcon.tsx";
import { Input, InputIcon, InputRoot } from "@/components/ui/input.tsx";
import { Search } from "lucide-react";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { useMemo, useRef, useState } from "react";
import { type Node, useNodeQuery } from "@/store/nodeStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { useServerQuery } from "@/store/serverStore.ts";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import { NodeManagementDropdown } from "@/components/nodes/NodeManagementDropdown.tsx";

export function NodesTable() {
    const [activeTab, setActiveTab] = useState("all");
    const containerRef = useRef<HTMLDivElement>(null);

    const { data: nodes } = useNodeQuery(useActiveTeam());
    const { data: servers } = useServerQuery(useActiveTeam());

    const totalNodes = nodes?.length || 0;
    const onlineNodes =
        nodes?.filter((node) => node.status === "ONLINE").length || 0;
    const creationNodes =
        nodes?.filter((node) => node.status === "CREATION").length || 0;
    const offlineNodes =
        nodes?.filter((node) => node.status === "OFFLINE").length || 0;

    const updateActiveTab = (status: string) => {
        setActiveTab(status);

        table
            .getColumn("status")
            ?.setFilterValue(
                status === "all" ? undefined : status.toUpperCase(),
            );
    };

    const columnsHelper = createColumnHelper<Node>();

    const columns = columnsHelper.columns([
        columnsHelper.accessor("settings.name", {
            id: "nodeName",
            header: "Node Name",
            enableSorting: true,
        }),
        columnsHelper.accessor(
            (row) => (row.daemonVersion ? row.daemonVersion : "UNKNOWN"),
            {
                id: "daemonVersion",
                header: "Version",
                enableSorting: true,
            },
        ),
        columnsHelper.accessor("status", {
            id: "status",
            header: "Status",
            sortFn: (rowA, rowB) => {
                const order = nodeStateOrder;
                return (
                    order.indexOf(rowA.original.status) -
                    order.indexOf(rowB.original.status)
                );
            },
            enableSorting: true,
            cell: (info) => (
                <p className={statusColorTextMap[info.getValue()]}>
                    {info.getValue()}
                </p>
            ),
        }),
        columnsHelper.accessor(
            (row) => servers?.filter((it) => it.nodeId === row.id)?.length ?? 0,
            {
                id: "serverCount",
                header: "Servers",
                enableSorting: true,
            },
        ),
        columnsHelper.display({
            id: "actions",
            cell: (info) => {
                return (
                    <div className="flex justify-end gap-3">
                        <NodeManagementDropdown node={info.row.original} />
                    </div>
                );
            },
        }),
    ]);

    const tableData = useMemo(() => nodes ?? [], [nodes]);

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
                    id: "status",
                    desc: false,
                },
            ],
        },
    });

    useAutoPageSize(table, containerRef);

    return (
        <>
            <Tabs
                value={activeTab}
                onValueChange={updateActiveTab}
                className="h-auto"
                activationMode="manual">
                <TabsList className="text-primary-foreground border-b-muted px-4 justify-center lg:justify-start">
                    <TabsTrigger className="gap-2 uppercase" value="all">
                        <p className="hidden lg:block">All Nodes</p>
                        <StatusIcon size="lg" box status="UNKNOWN">
                            <p className="leading-4 text-sm">{totalNodes}</p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger className="gap-2 uppercase" value="online">
                        <p className="hidden lg:block">Online</p>
                        <StatusIcon size="lg" box status="ONLINE">
                            <p className="leading-4 text-sm">{onlineNodes}</p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger className="gap-2 uppercase" value="creation">
                        <p className="hidden lg:block">Creation</p>
                        <StatusIcon size="lg" box status="CREATION">
                            <p className="leading-4 text-sm">{creationNodes}</p>
                        </StatusIcon>
                    </TabsTrigger>
                    <TabsTrigger className="gap-2 uppercase" value="offline">
                        <p className="hidden lg:block">Offline</p>
                        <StatusIcon size="lg" box status="OFFLINE">
                            <p className="leading-4 text-sm">{offlineNodes}</p>
                        </StatusIcon>
                    </TabsTrigger>
                </TabsList>
            </Tabs>
            <InputRoot className="max-w-56 my-5 mx-auto lg:mx-5">
                <InputIcon>
                    <Search className="text-muted-foreground" />
                </InputIcon>
                <Input
                    value={
                        (table
                            .getColumn("nodeName")
                            ?.getFilterValue() as string) ?? ""
                    }
                    onChange={(event) =>
                        table
                            .getColumn("nodeName")
                            ?.setFilterValue(event.target.value)
                    }
                    placeholder="Search Node"
                />
            </InputRoot>
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
                                                header.index === 0
                                                    ? "pl-5 lg:w-[320px]"
                                                    : "",
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
                                            to: `/node/${row.original.id}/settings`,
                                        })
                                    }
                                    className="cursor-pointer"
                                    key={row.id}
                                    data-state={
                                        row.getIsSelected() && "selected"
                                    }>
                                    {row.getVisibleCells().map((cell, i) => (
                                        <TableCell
                                            className={cn(
                                                i === 0
                                                    ? "pl-5 lg:w-[320px]"
                                                    : "",
                                            )}
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
            </div>
            <TableNavigation table={table} />
        </>
    );
}

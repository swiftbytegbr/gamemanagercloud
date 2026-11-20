import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import {
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { flexRender, useTable } from "@tanstack/react-table";
import { type Session, useSessionsQuery } from "@/store/authStore.ts";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import { cn } from "@/lib/utils.ts";
import { Button } from "@/components/ui/button.tsx";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { LogOut } from "lucide-react";
import { useMemo, useRef } from "react";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import { useRouter } from "@tanstack/react-router";
import { Spinner } from "@/components/ui/spinner.tsx";

export function SessionsDialog() {
    const { data: sessions, isLoading: isLoadingSessions } = useSessionsQuery();
    const router = useRouter();
    const containerRef = useRef<HTMLDivElement>(null);

    const columnsHelper = createColumnHelper<Session>();

    const columns = columnsHelper.columns([
        columnsHelper.accessor("lastUserAgent", {
            id: "userAgent",
            header: "User Agent",
            cell: (info) => (
                <p className="whitespace-normal leading-normal w-full lg:w-[320px]">
                    {info.getValue()}
                </p>
            ),
        }),
        columnsHelper.accessor("lastIp", {
            id: "ipAddress",
            header: "IP Address",
        }),
        columnsHelper.accessor("created", {
            id: "createdAt",
            header: "Created At",
            cell: (info) => new Date(info.getValue()).toLocaleString(),
        }),
        columnsHelper.accessor("lastAccessed", {
            id: "lastAccessed",
            header: "Last Accessed",
            cell: (info) => new Date(info.getValue()).toLocaleString(),
        }),
        columnsHelper.accessor("expiresAt", {
            id: "expiresAt",
            header: "Expires At",
            cell: (info) => new Date(info.getValue()).toLocaleString(),
        }),
    ]);

    const tableData = useMemo(() => sessions ?? [], [sessions]);

    const table = useTable({
        features: appTableFeatures,
        data: tableData,
        columns: columns,
        initialState: {
            pagination: {
                pageIndex: 0,
                pageSize: 10,
            },
        },
    });

    useAutoPageSize(table, containerRef);

    return (
        <>
            <DialogHeader>
                <DialogTitle>Active Sessions</DialogTitle>
                <div className="flex items-center">
                    <DialogDescription>
                        These are all devices that are currently logged in.
                    </DialogDescription>
                    <Button
                        className="ml-auto"
                        onClick={() => router.navigate({ to: "/logout?all" })}>
                        <LogOut />
                        Logout All Sessions
                    </Button>
                </div>
            </DialogHeader>
            {isLoadingSessions ? (
                <Spinner />
            ) : (
                <div className="flex-1 min-h-0 overflow-x-auto flex flex-col">
                    <div
                        ref={containerRef}
                        className="flex-1 min-h-0 overflow-y-auto overflow-x-auto">
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
                                            className={cn(
                                                row.original.sessionId ===
                                                    "CURRENT"
                                                    ? "bg-chart-2 hover:bg-chart-2/70"
                                                    : "",
                                            )}
                                            data-state={
                                                row.getIsSelected() &&
                                                "selected"
                                            }>
                                            {row
                                                .getVisibleCells()
                                                .map((cell, i) => (
                                                    <TableCell
                                                        className={cn(
                                                            i === 0
                                                                ? "pl-5"
                                                                : "",
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
                    </div>
                    <TableNavigation table={table} />
                </div>
            )}
        </>
    );
}

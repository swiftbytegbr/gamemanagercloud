import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import { flexRender, useTable } from "@tanstack/react-table";
import { useMemo, useRef, useState } from "react";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import { getPermissionGroupLabels } from "@/components/custom/form/FormPermissionSelection.tsx";
import { useApiKeyQuery, type ApiKey, type Team } from "@/store/teamStore.ts";
import { dialog } from "@/store/dialogStore.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { revokeApiKey } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { queryKeys } from "@/store/queryKeys.ts";
import { removeListItemById } from "@/store/queryCache.ts";

type ApiKeysTableProps = {
    team: Team;
};

export function ApiKeysTable(props: ApiKeysTableProps) {
    const [isLoading, setLoading] = useState(false);

    const containerRef = useRef<HTMLDivElement>(null);
    const { data: apiKeys, isLoading: isLoadingKeys } = useApiKeyQuery(
        props.team,
    );

    const handleRevoke = (applicationId: string, name: string) => {
        dialog(
            <ConfirmDialog
                title="Revoke API Key"
                message={`Are you sure you want to revoke "${name}"? This will permanently remove access for that API key.`}
                confirmText="Revoke"
                onConfirm={() => {
                    setLoading(true);
                    revokeApiKey(props.team.id, applicationId)
                        .then(async () => {
                            toast.success("API key revoked successfully.");
                            removeListItemById(
                                queryKeys.apiKeys.list(props.team.id),
                                applicationId,
                            );
                        })
                        .finally(() => setLoading(false));
                }}
            />,
        );
    };

    const columnsHelper = createColumnHelper<ApiKey>();
    const columns = useMemo(
        () =>
            columnsHelper.columns([
                columnsHelper.accessor("name", {
                    id: "name",
                    header: "Name",
                    enableSorting: true,
                }),
                columnsHelper.display({
                    id: "permissions",
                    header: "Permissions",
                    cell: (info) => {
                        const groupLabels = getPermissionGroupLabels(
                            info.row.original.permissions,
                        );
                        return (
                            <div className="flex flex-wrap gap-2">
                                {groupLabels.length ? (
                                    groupLabels.map((label) => (
                                        <Badge key={label} variant="tertiary">
                                            {label}
                                        </Badge>
                                    ))
                                ) : (
                                    <span className="text-muted-foreground">
                                        None
                                    </span>
                                )}
                            </div>
                        );
                    },
                }),
                columnsHelper.display({
                    id: "actions",
                    cell: (info) => (
                        <div className="flex justify-end">
                            <Button
                                disabled={isLoading}
                                onClick={() =>
                                    handleRevoke(
                                        info.row.original.applicationId,
                                        info.row.original.name,
                                    )
                                }
                                variant="destructive"
                                size="sm">
                                {isLoading ? <Spinner size="tiny" /> : "Revoke"}
                            </Button>
                        </div>
                    ),
                }),
            ]),
        [columnsHelper, isLoading],
    );

    const table = useTable({
        features: appTableFeatures,
        data: apiKeys ?? [],
        columns,
        initialState: { pagination: { pageIndex: 0, pageSize: 10 } },
    });

    useAutoPageSize(table, containerRef);

    return (
        <div className="h-full flex flex-col overflow-hidden">
            <div
                ref={containerRef}
                className="grow overflow-y-auto overflow-x-auto">
                <Table className="border-b">
                    <TableHeader className="bg-primary-foreground/30">
                        {table.getHeaderGroups().map((headerGroup) => (
                            <TableRow key={headerGroup.id}>
                                {headerGroup.headers.map((header) => (
                                    <TableHead key={header.id}>
                                        {header.isPlaceholder
                                            ? null
                                            : flexRender(
                                                  header.column.columnDef
                                                      .header,
                                                  header.getContext(),
                                              )}
                                    </TableHead>
                                ))}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows?.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow key={row.id}>
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
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
                                    {isLoadingKeys ? (
                                        <Spinner />
                                    ) : (
                                        "No results."
                                    )}
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
            <TableNavigation table={table} />
        </div>
    );
}

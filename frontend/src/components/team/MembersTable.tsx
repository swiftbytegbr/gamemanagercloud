import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import { useMemo, useRef, useState } from "react";
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
import { Button } from "@/components/ui/button.tsx";
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { Crown, Shield, UserLock, UserMinus } from "lucide-react";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import {
    hasPermission,
    type Team,
    type TeamMember,
} from "@/store/teamStore.ts";
import { ProfilePicture } from "@/components/custom/ProfilePicture.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { EditMemberPermissionsDialog } from "@/components/team/dialogs/EditMemberPermissionsDialog.tsx";
import { kickMember } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import { SelectionActionBar } from "@/components/custom/table/SelectionActionBar.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";

type MembersTableProps = {
    team: Team;
    userId?: string;
    onRefetch: () => Promise<unknown> | void;
};

export function MembersTable(props: MembersTableProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
    const [isBulkActionLoading, setBulkActionLoading] = useState(false);

    const rows = useMemo(() => {
        return [props.team.owner, ...props.team.members];
    }, [props.team.owner, props.team.members]);

    const helper = createColumnHelper<TeamMember>();
    const canManageMembers = hasPermission(
        props.team,
        props.userId,
        "MANAGE_TEAM",
    );

    const columns = helper.columns([
        ...(canManageMembers
            ? [
                  helper.display({
                      id: "select",
                      header: ({ table }) => (
                          <Checkbox
                              aria-label="Select all members"
                              checked={
                                  table.getIsAllPageRowsSelected() ||
                                  (table.getIsSomePageRowsSelected() &&
                                      "indeterminate")
                              }
                              onCheckedChange={(value) =>
                                  table.toggleAllPageRowsSelected(!!value)
                              }
                          />
                      ),
                      cell: ({ row }) => (
                          <Checkbox
                              aria-label={`Select ${row.original.name}`}
                              checked={row.getIsSelected()}
                              onCheckedChange={(value) =>
                                  row.toggleSelected(!!value)
                              }
                          />
                      ),
                  }),
              ]
            : []),
        helper.display({
            id: "owner",
            header: "",
            cell: (info) => {
                if (info.row.original.id === props.team.owner.id) {
                    return <Crown className="text-yellow-400" />;
                } else if (
                    info.row.original.permissions.includes("MANAGE_TEAM")
                ) {
                    return <Shield />;
                } else {
                    return "";
                }
            },
        }),
        helper.accessor("imageId", {
            id: "icon",
            header: "",
            cell: (info) => (
                <ProfilePicture className="h-12" imageId={info.getValue()} />
            ),
        }),
        helper.accessor("name", { id: "name", header: "Username" }),
        helper.accessor("email", { id: "email", header: "E-Mail" }),
        helper.accessor("twoFAEnabled", {
            id: "twoFAEnabled",
            header: "2FA",
            cell: (info) => {
                return info.getValue() ? (
                    <p className="text-green-600">Enabled</p>
                ) : (
                    <p className="text-red-500">Disabled</p>
                );
            },
        }),
        helper.display({
            id: "management",
            cell: (info) => (
                <div
                    className={cn(
                        "flex justify-end gap-2",
                        !canManageMembers && "hidden",
                    )}>
                    <Button
                        disabled={
                            info.row.original.id === props.team.owner.id ||
                            info.row.original.id === props.userId
                        }
                        onClick={() => {
                            dialog(
                                <EditMemberPermissionsDialog
                                    team={props.team}
                                    member={info.row.original}
                                    onSaved={async () => {
                                        await props.onRefetch?.();
                                    }}
                                />,
                                { width: "auto" },
                            );
                        }}
                        variant="tertiary">
                        <UserLock />
                        Permissions
                    </Button>
                    <Button
                        disabled={
                            info.row.original.id === props.team.owner.id ||
                            info.row.original.id === props.userId
                        }
                        onClick={() => {
                            dialog(
                                <ConfirmDialog
                                    title="Kick Member"
                                    message={`Are you sure you want to kick ${info.row.original.name} from the team? They will lose access to all resources associated with this team.`}
                                    onConfirm={() => {
                                        kickMember(
                                            props.team.id,
                                            info.row.original.id,
                                        ).then(async () => {
                                            toast.success(
                                                "The member has been kicked from the team successfully.",
                                            );
                                            await props.onRefetch?.();
                                        });
                                    }}
                                />,
                            );
                        }}
                        variant="destructive">
                        <UserMinus />
                        Kick
                    </Button>
                </div>
            ),
        }),
    ]);

    const table = useTable({
        features: appTableFeatures,
        data: rows,
        columns,
        initialState: { pagination: { pageIndex: 0, pageSize: 10 } },
        enableRowSelection: canManageMembers,
        onRowSelectionChange: setRowSelection,
        state: {
            rowSelection,
        },
    });

    useAutoPageSize(table, containerRef);

    const selectedMembers = table
        .getSelectedRowModel()
        .flatRows.map((row) => row.original);
    const kickTargets = selectedMembers.filter(
        (member) =>
            member.id !== props.team.owner.id && member.id !== props.userId,
    );
    const skippedMembers = selectedMembers.length - kickTargets.length;
    const canKickSelected = kickTargets.length > 0 && !isBulkActionLoading;

    const handleKickSelected = () => {
        if (!kickTargets.length) return;
        dialog(
            <ConfirmDialog
                title="Kick Members"
                message={`Are you sure you want to kick ${
                    kickTargets.length
                } selected member${
                    kickTargets.length > 1 ? "s" : ""
                }? They will lose access to all resources associated with this team.`}
                confirmText="Kick Members"
                onConfirm={() => {
                    setBulkActionLoading(true);
                    Promise.all(
                        kickTargets.map((member) =>
                            kickMember(props.team.id, member.id),
                        ),
                    )
                        .then(async () => {
                            toast.success(
                                `Kicked ${kickTargets.length} member${
                                    kickTargets.length > 1 ? "s" : ""
                                }${
                                    skippedMembers > 0
                                        ? ` (${skippedMembers} skipped)`
                                        : ""
                                }.`,
                            );
                            await props.onRefetch?.();
                        })
                        .finally(() => {
                            setBulkActionLoading(false);
                            table.resetRowSelection();
                        });
                }}
            />,
        );
    };

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
                                    <TableHead
                                        className={cn(
                                            header.column.id === "select" &&
                                                "pl-5 w-14",
                                            header.column.id === "owner"
                                                ? canManageMembers
                                                    ? "pl-2 lg:max-w-24"
                                                    : "pl-5 lg:max-w-24"
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
                                ))}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows?.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow
                                    key={row.id}
                                    data-state={
                                        row.getIsSelected() && "selected"
                                    }>
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell
                                            className={cn(
                                                cell.column.id === "select" &&
                                                    "pl-5 w-14",
                                                cell.column.id === "owner"
                                                    ? canManageMembers
                                                        ? "pl-2 lg:max-w-24"
                                                        : "pl-5 lg:max-w-24"
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
                                    No Members.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                {canManageMembers && (
                    <SelectionActionBar
                        selectedCount={selectedMembers.length}
                        entityLabel="member"
                        onClearSelection={() => table.resetRowSelection()}>
                        <Button
                            variant="destructive"
                            size="sm"
                            disabled={!canKickSelected}
                            onClick={handleKickSelected}>
                            {isBulkActionLoading ? (
                                <Spinner size="tiny" />
                            ) : (
                                <UserMinus />
                            )}
                            Kick Selected
                        </Button>
                    </SelectionActionBar>
                )}
            </div>
            <TableNavigation table={table} />
        </div>
    );
}

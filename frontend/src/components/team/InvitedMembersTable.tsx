import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import { useRef, useState } from "react";
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
import { TableNavigation } from "@/components/custom/table/TableNavigation.tsx";
import { useAutoPageSize } from "@/hooks/useAutoPageSize.ts";
import type { MemberInvite } from "@/store/teamStore.ts";
import { ProfilePicture } from "@/components/custom/ProfilePicture.tsx";
import { Button } from "@/components/ui/button.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { revokeTeamInvite } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { UserMinus } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import { SelectionActionBar } from "@/components/custom/table/SelectionActionBar.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";

type InvitedMembersTableProps = {
    teamId: string;
    invitedMembers: MemberInvite[];
    onRefetch?: () => Promise<unknown> | void;
};

export function InvitedMembersTable(props: InvitedMembersTableProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
    const [isBulkActionLoading, setBulkActionLoading] = useState(false);

    const helper = createColumnHelper<MemberInvite>();
    const columns = helper.columns([
        helper.display({
            id: "select",
            header: ({ table }) => (
                <Checkbox
                    aria-label="Select all invited members"
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
                    aria-label={`Select invite for ${row.original.name}`}
                    checked={row.getIsSelected()}
                    onCheckedChange={(value) => row.toggleSelected(!!value)}
                />
            ),
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
        helper.accessor("invitedAt", {
            id: "invitedAt",
            header: "Invited",
            cell: (info) => new Date(info.getValue()).toLocaleString(),
        }),
        helper.accessor("expired", {
            id: "status",
            header: "Status",
            cell: (info) => {
                return info.getValue() ? (
                    <span className="text-red-500 font-medium">Expired</span>
                ) : (
                    <span className="text-yellow-400 font-medium">Pending</span>
                );
            },
        }),
        helper.display({
            id: "management",
            header: "",
            cell: (info) => (
                <div className="flex gap-2 justify-end">
                    <Button
                        variant="destructive"
                        onClick={() =>
                            dialog(
                                <ConfirmDialog
                                    title="Revoke Invite"
                                    message={`Revoke the invitation for ${info.row.original.name}? They will no longer be able to join with this invite.`}
                                    onConfirm={() => {
                                        revokeTeamInvite(
                                            props.teamId,
                                            info.row.original.id,
                                        ).then(async () => {
                                            toast.success(
                                                "The invite has been revoked.",
                                            );
                                            await props.onRefetch?.();
                                        });
                                    }}
                                />,
                            )
                        }>
                        <UserMinus />
                        Revoke
                    </Button>
                </div>
            ),
        }),
    ]);

    const table = useTable({
        features: appTableFeatures,
        data: props.invitedMembers,
        columns,
        initialState: { pagination: { pageIndex: 0, pageSize: 10 } },
        enableRowSelection: true,
        onRowSelectionChange: setRowSelection,
        state: {
            rowSelection,
        },
    });

    useAutoPageSize(table, containerRef);

    const selectedInvites = table
        .getSelectedRowModel()
        .flatRows.map((row) => row.original);
    const canRevokeSelected =
        selectedInvites.length > 0 && !isBulkActionLoading;

    const handleRevokeSelected = () => {
        if (!selectedInvites.length) return;
        dialog(
            <ConfirmDialog
                title="Revoke Invites"
                message={`Revoke ${
                    selectedInvites.length
                } selected invite${selectedInvites.length > 1 ? "s" : ""}?`}
                confirmText="Revoke Invites"
                onConfirm={() => {
                    setBulkActionLoading(true);
                    Promise.all(
                        selectedInvites.map((invite) =>
                            revokeTeamInvite(props.teamId, invite.id),
                        ),
                    )
                        .then(async () => {
                            toast.success(
                                `Revoked ${selectedInvites.length} invite${
                                    selectedInvites.length > 1 ? "s" : ""
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
                                            header.column.id === "icon" &&
                                                "pl-2 lg:max-w-24",
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
                                                cell.column.id === "icon" &&
                                                    "pl-2 lg:max-w-24",
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
                                    No invited members.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                <SelectionActionBar
                    selectedCount={selectedInvites.length}
                    entityLabel="invite"
                    onClearSelection={() => table.resetRowSelection()}>
                    <Button
                        variant="destructive"
                        size="sm"
                        disabled={!canRevokeSelected}
                        onClick={handleRevokeSelected}>
                        {isBulkActionLoading ? (
                            <Spinner size="tiny" />
                        ) : (
                            <UserMinus />
                        )}
                        Revoke Selected
                    </Button>
                </SelectionActionBar>
            </div>
            <TableNavigation table={table} />
        </div>
    );
}

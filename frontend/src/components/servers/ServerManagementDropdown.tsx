import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
    ClipboardCopy,
    ExternalLink,
    Folder,
    MoreVertical,
    SquarePen,
    Trash,
} from "lucide-react";
import { toast } from "sonner";
import { type Server } from "@/store/serverStore.ts";
import { useRouter } from "@tanstack/react-router";
import { hasPermission, useActiveTeam } from "@/store/teamStore.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { RenameServerDialog } from "@/components/servers/dialogs/RenameServerDialog.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ChangeServerDirectoryDialog } from "@/components/servers/dialogs/ChangeServerDirectoryDialog.tsx";

type ServerManagementDropdownProps = {
    server: Server;
    onDelete: () => void;
    disabled?: boolean;
};

export function ServerManagementDropdown({
    server,
    onDelete,
    disabled,
}: ServerManagementDropdownProps) {
    const { data: user } = useUserQuery();

    const activeTeam = useActiveTeam();
    const router = useRouter();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Open server actions"
                    disabled={disabled}
                    variant="ghost"
                    size="icon">
                    <MoreVertical />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
                onClick={(e) => e.stopPropagation()}
                side="left"
                align="start">
                <DropdownMenuItem
                    onClick={() => {
                        navigator.clipboard
                            .writeText(server.id)
                            .then(() =>
                                toast.success(
                                    "The server ID has been copied to your clipboard.",
                                ),
                            )
                            .catch(() =>
                                toast.error("Failed to copy server ID"),
                            );
                    }}>
                    <ClipboardCopy className="text-secondary-foreground" />
                    Copy Server ID
                </DropdownMenuItem>
                <DropdownMenuItem
                    onClick={() =>
                        dialog(<RenameServerDialog server={server} />)
                    }>
                    <SquarePen className="text-secondary-foreground" />
                    Rename Server
                </DropdownMenuItem>
                <DropdownMenuItem
                    onClick={() =>
                        dialog(<ChangeServerDirectoryDialog server={server} />)
                    }>
                    <Folder className="text-secondary-foreground" />
                    Change Server Directory
                </DropdownMenuItem>
                {hasPermission(activeTeam, user?.id, "ACCESS_NODES") && (
                    <DropdownMenuItem
                        onClick={() =>
                            router.navigate({ to: `/node/${server.nodeId}` })
                        }>
                        <ExternalLink className="text-secondary-foreground" />
                        Go to Node
                    </DropdownMenuItem>
                )}
                <DropdownMenuItem
                    disabled={!["OFFLINE", "UNKNOWN"].includes(server.state)}
                    onClick={onDelete}
                    variant="destructive">
                    <Trash />
                    Delete Server
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

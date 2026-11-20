import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import { Button } from "@/components/ui/button.tsx";
import { ClipboardCopy, Eye, MoreVertical, Trash } from "lucide-react";
import { toast } from "sonner";
import { dialog } from "@/store/dialogStore.tsx";
import { CreateNodeDialog } from "@/components/nodes/CreateNodeDialog.tsx";
import { DeleteNodeDialog } from "@/components/nodes/DeleteNodeDialog.tsx";
import { type Node } from "@/store/nodeStore.ts";

type NodeManagementDropdownProps = {
    node: Node;
};

export function NodeManagementDropdown({ node }: NodeManagementDropdownProps) {
    const showInviteToken = node.status === "CREATION";

    const handleCopyNodeId = () => {
        navigator.clipboard
            .writeText(node.id)
            .then(() =>
                toast.success("Node ID copied to clipboard!", {
                    duration: 2000,
                }),
            )
            .catch(() =>
                toast.error("Failed to copy node ID!", { duration: 2000 }),
            );
    };

    const handleShowInviteToken = () => {
        dialog(<CreateNodeDialog node={node} setNode={() => {}} />, {
            tag: "createNodeDialog",
        });
    };

    const handleDeleteNode = () => {
        dialog(<DeleteNodeDialog node={node} />);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Open node actions"
                    variant="ghost"
                    size="icon">
                    <MoreVertical />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
                onClick={(e) => e.stopPropagation()}
                side="left"
                align="start">
                {showInviteToken && (
                    <DropdownMenuItem onClick={handleShowInviteToken}>
                        <Eye className="text-secondary-foreground" />
                        Show invite token
                    </DropdownMenuItem>
                )}
                <DropdownMenuItem onClick={handleCopyNodeId}>
                    <ClipboardCopy className="text-secondary-foreground" />
                    Copy node ID
                </DropdownMenuItem>
                <DropdownMenuItem
                    onClick={handleDeleteNode}
                    variant="destructive">
                    <Trash />
                    Delete Node
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

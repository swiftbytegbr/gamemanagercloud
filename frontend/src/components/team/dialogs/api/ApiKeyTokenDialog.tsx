import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { ClipboardCopy } from "lucide-react";
import { toast } from "sonner";
import { popDialog } from "@/store/dialogStore.tsx";

type ApiKeyTokenDialogProps = {
    token: string;
};

export function ApiKeyTokenDialog(props: ApiKeyTokenDialogProps) {
    const copyToken = async () => {
        if (typeof navigator === "undefined" || !navigator.clipboard) return;
        try {
            await navigator.clipboard.writeText(props.token);
            toast.success("API key token copied to clipboard.");
        } catch {
            toast.error("Failed to copy the API key token.");
        }
    };

    return (
        <div className="flex flex-col gap-5">
            <DialogHeader className="text-left">
                <DialogTitle>API Key Created</DialogTitle>
                <DialogDescription>
                    You can only view this token once. Store it somewhere safe.
                </DialogDescription>
            </DialogHeader>
            <div className="flex bg-input/30 border border-input rounded-md p-4 font-mono text-sm break-all items-center">
                {props.token}
                <Button
                    aria-label="Copy API key token"
                    onClick={copyToken}
                    className="ml-5"
                    size="icon"
                    variant="outline">
                    <ClipboardCopy />
                </Button>
            </div>
            <DialogFooter>
                <Button variant="tertiary" type="button" onClick={popDialog}>
                    Close
                </Button>
            </DialogFooter>
        </div>
    );
}

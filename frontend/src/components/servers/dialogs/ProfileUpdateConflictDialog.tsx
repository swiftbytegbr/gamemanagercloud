import { useState } from "react";
import { Button } from "@/components/ui/button.tsx";
import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { closeDialog } from "@/store/dialogStore.tsx";

type ProfileUpdateConflictDialogProps = {
    userName: string;
    onReload: () => Promise<void> | void;
};

export function ProfileUpdateConflictDialog({
    userName,
    onReload,
}: ProfileUpdateConflictDialogProps) {
    const [isReloading, setIsReloading] = useState(false);

    const handleReload = async () => {
        setIsReloading(true);
        try {
            await onReload();
            closeDialog();
        } finally {
            setIsReloading(false);
        }
    };

    return (
        <>
            <DialogHeader>
                <DialogTitle>Server Settings Updated</DialogTitle>
                <DialogDescription>
                    This setting profile was updated by {userName}. If you save,
                    you will overwrite their changes.
                </DialogDescription>
            </DialogHeader>
            <DialogFooter>
                <Button
                    onClick={() => closeDialog()}
                    variant="tertiary"
                    disabled={isReloading}>
                    Continue editing
                </Button>
                <Button onClick={handleReload} disabled={isReloading}>
                    {isReloading ? (
                        <Spinner size="tiny" />
                    ) : (
                        "Reload from server"
                    )}
                </Button>
            </DialogFooter>
        </>
    );
}

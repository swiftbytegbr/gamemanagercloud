import { useEffect } from "react";
import { useBlocker } from "@tanstack/react-router";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import {
    closeDialogsByTag,
    dialog,
    hasDialogByTag,
} from "@/store/dialogStore.tsx";

export function useUnsavedChangesPrompt(isDirty: boolean) {
    useBlocker({
        shouldBlockFn: () => {
            if (!isDirty) return false;

            return new Promise<boolean>((resolve) => {
                const tag = "unsaved-settings-navigation";
                if (hasDialogByTag(tag)) return;
                dialog(
                    <ConfirmDialog
                        title="Unsaved changes"
                        message="You have unsaved changes. Leave without saving?"
                        confirmText="Leave"
                        cancelText="Stay"
                        onConfirm={() => {
                            closeDialogsByTag(tag);
                            resolve(false);
                        }}
                        onCancel={() => {
                            closeDialogsByTag(tag);
                            resolve(true);
                        }}
                    />,
                    { disableCloseButton: true, modal: true, tag },
                );
            });
        },
        enableBeforeUnload: () => isDirty,
        withResolver: true,
    });

    useEffect(() => {
        const handler = (event: BeforeUnloadEvent) => {
            if (!isDirty) return;
            event.preventDefault();
        };

        window.addEventListener("beforeunload", handler);
        return () => window.removeEventListener("beforeunload", handler);
    }, [isDirty]);
}

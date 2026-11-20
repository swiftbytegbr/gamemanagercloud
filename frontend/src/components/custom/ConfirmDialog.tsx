import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";

type ConfirmDialogProps = {
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    onConfirm: () => void;
    onCancel?: () => void;
};

export function ConfirmDialog(props: ConfirmDialogProps) {
    return (
        <>
            <DialogHeader>
                <DialogTitle>{props.title}</DialogTitle>
                <DialogDescription>{props.message}</DialogDescription>
            </DialogHeader>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary" onClick={props.onCancel}>
                        {props.cancelText || "Cancel"}
                    </Button>
                </DialogClose>
                <DialogClose asChild>
                    <Button variant="destructive" onClick={props.onConfirm}>
                        {props.confirmText || "Confirm"}
                    </Button>
                </DialogClose>
            </DialogFooter>
        </>
    );
}

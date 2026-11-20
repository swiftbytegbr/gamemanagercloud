import {
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";

export function CreateNodeDialogHeader() {
    return (
        <DialogHeader>
            <DialogTitle className="uppercase text-3xl">
                Create a new node
            </DialogTitle>
            <DialogDescription className="leading-normal">
                A node is your dedicated KVM/root server. Install the GMC
                daemon, create the node, enter the invite token in the daemon
                console – all further actions are done via the GMC panel.
            </DialogDescription>
        </DialogHeader>
    );
}

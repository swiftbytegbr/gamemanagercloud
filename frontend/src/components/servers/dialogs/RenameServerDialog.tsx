import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { type Server } from "@/store/serverStore.ts";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { renameServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";
import { useState } from "react";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateListItemById } from "@/store/queryCache.ts";

type RenameServerDialogProps = {
    server: Server;
};

export function RenameServerDialog({ server }: RenameServerDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const renameServerFormSchema = z.object({
        displayName: z.string({
            message: "Server name is required",
        }),
    });

    const form = useForm<z.infer<typeof renameServerFormSchema>>({
        resolver: zodResolver(renameServerFormSchema),
        defaultValues: {
            displayName: server.displayName,
        },
    });

    const onSubmit = (data: z.infer<typeof renameServerFormSchema>) => {
        setLoading(true);
        renameServer(server.id, data.displayName)
            .then(() => {
                updateListItemById(
                    queryKeys.servers.all,
                    server.id,
                    (current) => ({
                        ...current,
                        displayName: data.displayName,
                    }),
                );
                toast.success("Server renamed successfully.");
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Rename {server.displayName}</DialogTitle>
                <DialogDescription>
                    Enter a new display name for this server.
                </DialogDescription>
            </DialogHeader>
            <div>
                <FormInput
                    form={form}
                    label="Display Name"
                    name="displayName"
                    required
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Rename</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

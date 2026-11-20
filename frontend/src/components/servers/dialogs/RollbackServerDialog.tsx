import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { type Server, type ServerBackup } from "@/store/serverStore.ts";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { rollbackServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { closeDialogsByTag } from "@/store/dialogStore.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";

type BackupServerDialogProps = {
    isLoading: boolean;
    setLoading: (loading: boolean) => void;
    server?: Server;
    backup: ServerBackup;
};

export function RollbackServerDialog({
    isLoading,
    setLoading,
    server,
    backup,
}: BackupServerDialogProps) {
    const deleteServerFormSchema = z.object({
        rollbackPlayers: z.boolean().optional(),
    });

    const form = useForm<z.infer<typeof deleteServerFormSchema>>({
        resolver: zodResolver(deleteServerFormSchema),
        defaultValues: {
            rollbackPlayers: false,
        },
    });

    const onSubmit = (data: z.infer<typeof deleteServerFormSchema>) => {
        setLoading(true);
        rollbackServer(
            server?.id ?? "",
            backup.backupId,
            !!data.rollbackPlayers,
        )
            .then(async () => {
                toast.success(`Rollback requested for ${server?.displayName}.`);
                closeDialogsByTag("rollbackServerDialog");
            })
            .finally(() => setLoading(false));
    };

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Rollback</DialogTitle>
                <DialogDescription>
                    Are you sure you want to rollback the server? This action
                    cannot be undone.
                </DialogDescription>
            </DialogHeader>
            <div>
                <FormCheckbox
                    form={form}
                    label="Rollback Players"
                    name="rollbackPlayers"
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Rollback</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

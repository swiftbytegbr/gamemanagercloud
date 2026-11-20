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
import { format } from "date-fns";
import { backupServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { useEffect } from "react";
import { closeDialog } from "@/store/dialogStore.tsx";

type BackupServerDialogProps = {
    isLoading: boolean;
    setLoading: (loading: boolean) => void;
    server: Server;
};

export function BackupServerDialog({
    isLoading,
    setLoading,
    server,
}: BackupServerDialogProps) {
    const backupServerFormSchema = z.object({
        backupName: z.string({
            message: "Backup name is required",
        }),
    });

    const form = useForm<z.infer<typeof backupServerFormSchema>>({
        resolver: zodResolver(backupServerFormSchema),
        defaultValues: {
            backupName: `${format(new Date(), "yyyy.MM.dd_HH-mm-ss")}_${server.displayName}`,
        },
    });

    const onSubmit = (data: z.infer<typeof backupServerFormSchema>) => {
        setLoading(true);
        backupServer(server.id, data.backupName)
            .then(async () => {
                toast.success(
                    `${server.displayName} backup created successfully!`,
                );
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        form.reset({
            backupName: `${format(new Date(), "yyyy.MM.dd_HH-mm-ss")}_${server.displayName}`,
        });
    }, [form, server.displayName]);

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Create a backup</DialogTitle>
                <DialogDescription className="sr-only">
                    Create a snapshot of your server files and settings.
                </DialogDescription>
            </DialogHeader>
            <div>
                <FormInput
                    form={form}
                    label="Backup Name"
                    name="backupName"
                    required
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Create</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

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
import { changeServerDirectory } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";
import { useState } from "react";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateListItemById } from "@/store/queryCache.ts";

type ChangeServerDirectoryDialogProps = {
    server: Server;
};

export function ChangeServerDirectoryDialog({
    server,
}: ChangeServerDirectoryDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const changeServerDirFormSchema = z.object({
        directory: z.string({
            message: "Server directory name is required",
        }),
    });

    const form = useForm<z.infer<typeof changeServerDirFormSchema>>({
        resolver: zodResolver(changeServerDirFormSchema),
        defaultValues: {
            directory: server.serverDirectory,
        },
    });

    const onSubmit = (data: z.infer<typeof changeServerDirFormSchema>) => {
        setLoading(true);
        changeServerDirectory(server.id, data.directory)
            .then(() => {
                updateListItemById(
                    queryKeys.servers.all,
                    server.id,
                    (current) => ({
                        ...current,
                        serverDirectory: data.directory,
                    }),
                );
                toast.success(
                    "Server directory change initialised successfully.",
                );
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Change server directory</DialogTitle>
                <DialogDescription>
                    Enter a new server directory for {server.displayName}
                </DialogDescription>
            </DialogHeader>
            <div>
                <FormInput
                    form={form}
                    label="Server Directory"
                    name="directory"
                    required
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Change</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

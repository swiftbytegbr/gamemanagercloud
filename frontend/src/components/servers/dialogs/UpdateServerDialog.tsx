import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { updateServer } from "@/api/serverApi.ts";
import type { Server } from "@/store/serverStore.ts";
import { closeDialog } from "@/store/dialogStore.tsx";
import { toast } from "sonner";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";

type UpdateServerDialogProps = {
    server: Server;
    isLoading: boolean;
    setLoading: (loading: boolean) => void;
};

export function UpdateServerDialog({
    server,
    isLoading,
    setLoading,
}: UpdateServerDialogProps) {
    const updateServerFormSchema = z.object({
        startAfterUpdate: z.boolean(),
    });

    const form = useForm<z.infer<typeof updateServerFormSchema>>({
        resolver: zodResolver(updateServerFormSchema),
        defaultValues: {
            startAfterUpdate: false,
        },
    });

    const onSubmit = (data: z.infer<typeof updateServerFormSchema>) => {
        setLoading(true);
        updateServer(server.id, data.startAfterUpdate)
            .then(() => {
                toast.success(
                    `${server.displayName} is now updating / verifying!`,
                );
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    return (
        <Form {...form}>
            <DialogHeader>
                <DialogTitle>Update / Verify {server.displayName}?</DialogTitle>
                <DialogDescription>
                    Are you sure you want to update or verify the server files?
                </DialogDescription>
            </DialogHeader>
            <FormCheckbox
                form={form}
                name="startAfterUpdate"
                label="Automatically start the server after finishing the update"
                disabled={isLoading}
            />
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary" disabled={isLoading}>
                        Cancel
                    </Button>
                </DialogClose>
                <Button
                    disabled={isLoading}
                    onClick={form.handleSubmit(onSubmit)}
                    type="submit">
                    {isLoading ? <Spinner /> : <>Update</>}
                </Button>
            </DialogFooter>
        </Form>
    );
}

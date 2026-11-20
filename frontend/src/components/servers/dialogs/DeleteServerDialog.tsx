import { Button } from "../../ui/button.tsx";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNodeByIdQuery } from "@/store/nodeStore.ts";
import { Form } from "@/components/ui/form.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { type Server } from "@/store/serverStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { TriangleAlert } from "lucide-react";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { deleteServer } from "@/api/serverApi.ts";
import { closeDialog } from "@/store/dialogStore.tsx";
import { queryKeys } from "@/store/queryKeys.ts";
import { removeListItemById } from "@/store/queryCache.ts";

type DeleteServerDialogProps = {
    server: Server | null;
};

export function DeleteServerDialog(props: DeleteServerDialogProps) {
    const { data: node } = useNodeByIdQuery(
        useActiveTeam(),
        props.server?.nodeId,
    );
    const [isLoading, setLoading] = useState(false);

    const deleteServerFormSchema = z.object({
        friendlyName: z
            .string({
                message: "Server name is required",
            })
            .refine((value) => value === props.server?.displayName, {
                message:
                    "Server name does not match. Please enter the correct server name to confirm deletion.",
            }),
        forceDelete: z.boolean(),
    });

    const form = useForm<z.infer<typeof deleteServerFormSchema>>({
        resolver: zodResolver(deleteServerFormSchema),
        defaultValues: {
            friendlyName: "",
            forceDelete: false,
        },
    });

    const isForceDelete = form.watch("forceDelete");

    useEffect(() => {
        if (props.server) {
            form.reset({
                friendlyName: "",
                forceDelete: node?.status != "ONLINE",
            });
        }
    }, [props.server, node?.status, form]);

    function onSubmit(data: z.infer<typeof deleteServerFormSchema>) {
        setLoading(true);
        deleteServer(props.server?.id ?? "", data.forceDelete)
            .then(() => {
                if (props.server?.id) {
                    removeListItemById(queryKeys.servers.all, props.server.id);
                }
                toast.success("Server deleted successfully.");
                closeDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <Form {...form}>
            <DialogHeader>
                <DialogTitle>
                    Are you sure you want to delete {props.server?.displayName}?
                </DialogTitle>
                <DialogDescription>
                    This action cannot be undone. Please enter the name of the
                    server to confirm.
                </DialogDescription>
            </DialogHeader>
            <FormCheckbox form={form} name="forceDelete" label="Force Delete" />
            {isForceDelete && (
                <div className="bg-destructive/70 p-4 rounded-md flex gap-3">
                    <TriangleAlert className="size-24" />
                    <p>
                        Force deletion will delete the server without properly
                        disconnecting it. The server files won't be deleted by
                        the daemon. You must enable this when your node is
                        offline.
                    </p>
                </div>
            )}
            <FormInput
                form={form}
                label="Server Name"
                name="friendlyName"
                required
            />
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={isLoading} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button
                    disabled={isLoading}
                    onClick={form.handleSubmit(onSubmit)}
                    type="submit"
                    variant="destructive">
                    {isLoading ? <Spinner /> : <>Delete Server</>}
                </Button>
            </DialogFooter>
        </Form>
    );
}

import { Button } from "../ui/button";
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
import { type Node } from "@/store/nodeStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { removeListItemById } from "@/store/queryCache.ts";
import { Form } from "@/components/ui/form.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useState } from "react";
import { deleteNode } from "@/api/nodeApi.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";

type DeleteNodeDialogProps = {
    node: Node | null;
};

export function DeleteNodeDialog(props: DeleteNodeDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const deleteNodeFormSchema = z.object({
        friendlyName: z
            .string({
                message: "Node name is required",
            })
            .refine((value) => value === props.node?.settings?.name, {
                message:
                    "Node name does not match. Please enter the correct node name to confirm deletion.",
            }),
    });

    const form = useForm<z.infer<typeof deleteNodeFormSchema>>({
        resolver: zodResolver(deleteNodeFormSchema),
        defaultValues: {
            friendlyName: "",
        },
    });

    function onSubmit() {
        setLoading(true);
        deleteNode(props.node?.id ?? "")
            .then(() => {
                if (props.node?.id) {
                    removeListItemById(queryKeys.nodes.all, props.node.id);
                }
                toast.success("Node deleted successfully.");
                closeDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <Form {...form}>
            <DialogHeader>
                <DialogTitle>
                    Are you sure you want to delete {props.node?.settings?.name}
                    ?
                </DialogTitle>
                <DialogDescription>
                    This action cannot be undone. Please enter the name of the
                    node to confirm.
                </DialogDescription>
            </DialogHeader>
            <FormInput
                form={form}
                label="Node Name"
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
                    {isLoading ? <Spinner /> : <>Delete Node</>}
                </Button>
            </DialogFooter>
        </Form>
    );
}

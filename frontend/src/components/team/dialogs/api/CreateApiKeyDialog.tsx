import { useState } from "react";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { createApiKey } from "@/api/teamApi.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { dialog, popDialog } from "@/store/dialogStore.tsx";
import { toast } from "sonner";
import type { Permission, Team } from "@/store/teamStore.ts";
import { ApiKeyTokenDialog } from "@/components/team/dialogs/api/ApiKeyTokenDialog.tsx";
import { FormPermissionSelection } from "@/components/custom/form/FormPermissionSelection.tsx";

type CreateApiKeyDialogProps = {
    team: Team;
};

const createApiKeySchema = z.object({
    name: z
        .string({
            error: "Name is required",
        })
        .min(1, {
            error: "Name is required",
        })
        .max(64, {
            error: "Name must be at most 64 characters",
        }),
    permissions: z.array(z.custom<Permission>()),
});

export function CreateApiKeyDialog(props: CreateApiKeyDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const form = useForm<z.infer<typeof createApiKeySchema>>({
        resolver: zodResolver(createApiKeySchema),
        defaultValues: {
            name: "",
            permissions: [],
        },
    });

    const handleCreate = async (data: z.infer<typeof createApiKeySchema>) => {
        setLoading(true);
        try {
            const response = await createApiKey(
                props.team.id,
                data.name,
                data.permissions,
            );
            await queryClient.invalidateQueries({
                queryKey: queryKeys.apiKeys.list(props.team.id),
            });
            toast.success("API key created successfully.");
            popDialog();
            dialog(<ApiKeyTokenDialog token={response.applicationToken} />, {
                modal: true,
                disableCloseButton: true,
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <ManagedForm form={form} onSubmit={handleCreate}>
            <DialogHeader>
                <DialogTitle>Create API Key</DialogTitle>
                <DialogDescription>
                    Generate a new API key for {props.team.name}.
                </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4">
                <FormInput
                    form={form}
                    label="Name"
                    name="name"
                    required
                    disabled={isLoading}
                    placeholder="e.g. Discord Bot"
                />
                <FormPermissionSelection
                    form={form}
                    name="permissions"
                    label="Permissions"
                    disabled={isLoading}
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={isLoading} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : "Create API Key"}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

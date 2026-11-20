import { useState } from "react";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { useForm } from "react-hook-form";
import { Spinner } from "@/components/ui/spinner.tsx";
import type { Permission, Team, TeamMember } from "@/store/teamStore.ts";
import { setMemberPermissions } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { popDialog } from "@/store/dialogStore.tsx";
import { FormPermissionSelection } from "@/components/custom/form/FormPermissionSelection.tsx";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

type EditMemberPermissionsDialogProps = {
    team: Team;
    member: TeamMember;
    onSaved?: () => void;
};

export function EditMemberPermissionsDialog(
    props: EditMemberPermissionsDialogProps,
) {
    const [saving, setSaving] = useState(false);

    const changePermissionSchema = z.object({
        permissions: z.array(z.custom<Permission>()),
    });

    const form = useForm<z.infer<typeof changePermissionSchema>>({
        resolver: zodResolver(changePermissionSchema),
        defaultValues: {
            permissions: props.member.permissions,
        },
    });

    async function onSave(values: z.infer<typeof changePermissionSchema>) {
        setSaving(true);
        try {
            await setMemberPermissions(
                props.team.id,
                props.member.id,
                values.permissions,
            );
            toast.success("Permissions updated successfully.");
            props.onSaved?.();
            popDialog();
        } finally {
            setSaving(false);
        }
    }

    return (
        <ManagedForm form={form} onSubmit={onSave}>
            <DialogHeader>
                <DialogTitle>Edit Permissions</DialogTitle>
                <DialogDescription>
                    Update permissions for {props.member.name} (
                    {props.member.email})
                </DialogDescription>
            </DialogHeader>
            <FormPermissionSelection
                form={form}
                name="permissions"
                label="Permissions"
            />
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={saving} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button disabled={saving} type="submit">
                    {saving ? <Spinner /> : "Save"}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

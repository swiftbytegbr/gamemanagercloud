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
import { ManagedForm } from "@/components/ui/form.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useState } from "react";
import { type Team } from "@/store/teamStore.ts";
import { inviteMember } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { popDialog } from "@/store/dialogStore.tsx";

type InviteMemberDialogProps = {
    team: Team;
};

export function InviteMemberDialog(props: InviteMemberDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const inviteMemberFormSchema = z.object({
        email: z.email({
            message: "A valid email address is required",
        }),
    });

    const form = useForm<z.infer<typeof inviteMemberFormSchema>>({
        resolver: zodResolver(inviteMemberFormSchema),
        defaultValues: {
            email: "",
        },
    });

    function onSubmit(data: z.infer<typeof inviteMemberFormSchema>) {
        setLoading(true);
        inviteMember(props.team.id, data.email)
            .then(() => {
                toast.success(
                    "The user has been successfully added to your team. They will get access as soon as they have confirmed their team membership with the confirmation code in the e-mail that has now been sent to them.",
                );
                popDialog();
            })
            .catch((err) => {
                if (err.status === 404)
                    toast.error(
                        "A user with the specified e-mail address does not exist.",
                    );
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Invite Member</DialogTitle>
                <DialogDescription>
                    Invite a Member to {props.team.name}
                </DialogDescription>
            </DialogHeader>
            <FormInput form={form} label="E-Mail" name="email" required />
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={isLoading} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Invite</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

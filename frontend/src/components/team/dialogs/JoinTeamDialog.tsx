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
import { joinTeam } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { closeDialogsByTag, popDialog } from "@/store/dialogStore.tsx";
import { useTeamQuery } from "@/store/teamStore.ts";

export function JoinTeamDialog() {
    const [isLoading, setLoading] = useState(false);

    const { refetch: refetchTeams } = useTeamQuery();

    const inviteMemberFormSchema = z.object({
        inviteToken: z
            .string({
                message: "Invite Token is required",
            })
            .min(6, {
                message: "Invite Token must be at least 6 characters long",
            })
            .max(6, {
                message: "Invite Token must be at most 6 characters long",
            }),
    });

    const form = useForm<z.infer<typeof inviteMemberFormSchema>>({
        resolver: zodResolver(inviteMemberFormSchema),
        defaultValues: {
            inviteToken: "",
        },
    });

    function onSubmit(data: z.infer<typeof inviteMemberFormSchema>) {
        setLoading(true);
        joinTeam(data.inviteToken)
            .then(async () => {
                toast.success("You have successfully joined the team.");
                await refetchTeams();
                popDialog();
                closeDialogsByTag("create-team");
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Join a Team</DialogTitle>
                <DialogDescription>
                    Ask the team owner to invite you with your email address.
                    After that you can enter the invite token you got by email
                    here to join the team.
                </DialogDescription>
            </DialogHeader>
            <FormInput
                form={form}
                label="Invite Token"
                name="inviteToken"
                type="number"
                required
            />
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={isLoading} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Join</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

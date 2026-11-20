import { ManagedForm } from "@/components/ui/form.tsx";
import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTeamQuery } from "@/store/teamStore.ts";
import { createTeam } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { type User } from "@/store/userStore.ts";
import { dialog, popDialog } from "@/store/dialogStore.tsx";
import { JoinTeamDialog } from "@/components/team/dialogs/JoinTeamDialog.tsx";
import { createTeamSchema } from "@/components/team/createTeamSchema.ts";

type CreateTeamFormProps = {
    user: User;
};

export function CreateTeamForm(props: CreateTeamFormProps) {
    const [isLoading, setLoading] = useState(false);
    const { refetch: refetchTeams } = useTeamQuery();

    const form = useForm<z.infer<typeof createTeamSchema>>({
        resolver: zodResolver(createTeamSchema),
        defaultValues: {
            teamName: `${props.user.personalInformation.username}'s Team`,
        },
    });

    function onSubmit(data: z.infer<typeof createTeamSchema>) {
        setLoading(true);
        createTeam(data.teamName)
            .then(async () => {
                await refetchTeams();
                toast.success("Team created successfully.");
                popDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Create your first team!</DialogTitle>
                <DialogDescription>
                    This is where you manage your members and nodes...
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col space-y-5">
                <FormInput
                    form={form}
                    name="teamName"
                    label="Team name"
                    required
                />
            </div>
            <DialogFooter>
                <div className="w-full flex flex-col items-center justify-center gap-6">
                    <Button
                        disabled={isLoading}
                        type="submit"
                        size="lg"
                        className="uppercase w-full">
                        {isLoading ? <Spinner size="small" /> : "Create Team"}
                    </Button>
                    <Button
                        onClick={() => dialog(<JoinTeamDialog />)}
                        size="lg"
                        variant="tertiary"
                        className="uppercase w-full">
                        Or Join A Team
                    </Button>
                </div>
            </DialogFooter>
        </ManagedForm>
    );
}

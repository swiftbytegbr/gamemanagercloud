import { Button } from "../../ui/button.tsx";
import {
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
import { useState } from "react";
import { type Team } from "@/store/teamStore.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ChangeTeamPicture } from "@/components/team/dialogs/ChangeTeamPicture.tsx";
import { changeTeamName } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateListItemById } from "@/store/queryCache.ts";

type EditTeamDialogProps = {
    team: Team;
};

export function EditTeamDialog(props: EditTeamDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const editTeamFormSchema = z.object({
        teamName: z
            .string({
                message: "Team name is required",
            })
            .min(1, {
                message: "Team name is required",
            })
            .min(3, {
                message: "Team name must be at least 3 characters long",
            })
            .max(40, {
                message: "Team name must be at most 40 characters long",
            }),
    });

    const form = useForm<z.infer<typeof editTeamFormSchema>>({
        resolver: zodResolver(editTeamFormSchema),
        defaultValues: {
            teamName: props.team.name,
        },
    });

    function onSubmit(data: z.infer<typeof editTeamFormSchema>) {
        setLoading(true);

        changeTeamName(props.team.id, data.teamName)
            .then(() => {
                updateListItemById(
                    queryKeys.teams.all,
                    props.team.id,
                    (team) => ({
                        ...team,
                        name: data.teamName,
                    }),
                );
                toast.success("Team name updated successfully.");
            })
            .finally(() => setLoading(false));
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Edit Team</DialogTitle>
                <DialogDescription className="sr-only">
                    Edit your team details here.
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-5">
                <ChangeTeamPicture team={props.team} />
                <hr />
                <div className="flex flex-col gap-4">
                    <ManagedForm form={form} onSubmit={onSubmit}>
                        <h3>Team Name</h3>
                        <FormInput
                            form={form}
                            label="Team Name"
                            name="teamName"
                            required
                        />
                        <Button disabled={isLoading} type="submit">
                            {isLoading ? <Spinner /> : "Change Team Name"}
                        </Button>
                    </ManagedForm>
                </div>
            </div>
            <DialogFooter />
        </>
    );
}

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
import { Form } from "@/components/ui/form.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useState } from "react";
import { toast } from "sonner";
import { popDialog } from "@/store/dialogStore.tsx";
import { type Team } from "@/store/teamStore.ts";
import { deleteTeam } from "@/api/teamApi.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { removeListItemById } from "@/store/queryCache.ts";

type DeleteTeamDialogProps = {
    team: Team;
};

export function DeleteTeamDialog(props: DeleteTeamDialogProps) {
    const [isLoading, setLoading] = useState(false);

    const deleteTeamFormSchema = z.object({
        displayName: z
            .string({
                message: "Team name is required",
            })
            .refine((value) => value === props.team.name, {
                message:
                    "Team name does not match. Please enter the correct team name to confirm deletion.",
            }),
    });

    const form = useForm<z.infer<typeof deleteTeamFormSchema>>({
        resolver: zodResolver(deleteTeamFormSchema),
        defaultValues: {
            displayName: "",
        },
    });

    function onSubmit() {
        setLoading(true);
        deleteTeam(props.team.id)
            .then(() => {
                removeListItemById(queryKeys.teams.all, props.team.id);
                toast.success("Team deleted successfully.");
                popDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <Form {...form}>
            <DialogHeader>
                <DialogTitle>
                    Are you sure you want to delete {props.team.name}?
                </DialogTitle>
                <DialogDescription>
                    This action cannot be undone. This will permanently delete
                    the team and remove all its members, nodes and servers.
                    Please enter the name of the team to confirm.
                </DialogDescription>
            </DialogHeader>
            <FormInput
                form={form}
                label="Team Name"
                name="displayName"
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
                    {isLoading ? <Spinner /> : <>Delete Team</>}
                </Button>
            </DialogFooter>
        </Form>
    );
}

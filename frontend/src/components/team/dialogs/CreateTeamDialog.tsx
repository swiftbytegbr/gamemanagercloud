import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { createTeam } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { useState } from "react";
import type { Team } from "@/store/teamStore.ts";
import { popDialog } from "@/store/dialogStore.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { createTeamSchema } from "@/components/team/createTeamSchema.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { upsertListItem } from "@/store/queryCache.ts";

export function CreateTeamDialog() {
    const [isLoading, setLoading] = useState(false);

    const form = useForm<z.infer<typeof createTeamSchema>>({
        resolver: zodResolver(createTeamSchema),
        defaultValues: {
            teamName: "",
        },
    });

    function onSubmit(data: z.infer<typeof createTeamSchema>) {
        setLoading(true);
        createTeam(data.teamName)
            .then((team: Team) => {
                upsertListItem(queryKeys.teams.all, team);
                //TODO open invite dialog
                toast.success("Team created successfully.");
                popDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Create a Team</DialogTitle>
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
                <div className="w-full flex justify-center">
                    <Button
                        disabled={isLoading}
                        type="submit"
                        size="lg"
                        className="uppercase w-full">
                        {isLoading ? <Spinner size="small" /> : "Create team"}
                    </Button>
                </div>
            </DialogFooter>
        </ManagedForm>
    );
}

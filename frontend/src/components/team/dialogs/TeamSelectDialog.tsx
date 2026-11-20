import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import {
    useActiveTeam,
    useTeamQuery,
    useTeamStore,
} from "@/store/teamStore.ts";
import { TeamPicture } from "@/components/custom/TeamPicture.tsx";
import { SquareArrowOutUpRight } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { closeDialog, dialog } from "@/store/dialogStore.tsx";
import { useRouter } from "@tanstack/react-router";
import { CreateTeamDialog } from "@/components/team/dialogs/CreateTeamDialog.tsx";
import { JoinTeamDialog } from "@/components/team/dialogs/JoinTeamDialog.tsx";

export function TeamSelectDialog() {
    const { data: teams } = useTeamQuery();

    const activeTeam = useActiveTeam();

    const selectActiveTeam = useTeamStore((state) => state.selectActiveTeam);

    const router = useRouter();

    return (
        <div className="flex flex-col min-h-0">
            <DialogHeader className="mb-4">
                <DialogTitle>Team Selection</DialogTitle>
                <DialogDescription className="sr-only">
                    Select a team to switch to or manage your teams.
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-3 flex-1 min-h-0 overflow-y-auto px-2">
                {teams?.map((team) => (
                    <div
                        className="flex h-12 items-center gap-2 w-full bg-background shadow-md rounded-md p-2"
                        key={team.id}>
                        <Button
                            onClick={() => {
                                selectActiveTeam(team.id);
                                router.navigate({ to: "/dashboard" });
                                closeDialog();
                            }}
                            disabled={team.id === activeTeam?.id}
                            className="mr-2 size-9 sm:w-fit">
                            <span className="hidden md:block">Switch</span>
                            <SquareArrowOutUpRight className="md:hidden" />
                        </Button>
                        <div className="flex h-full gap-2 items-center flex-1 w-0">
                            <TeamPicture className="h-full" team={team} />
                            <span className="truncate">{team.name}</span>
                        </div>
                    </div>
                ))}
            </div>
            <DialogFooter className="pt-3">
                <div className="w-full flex flex-col gap-3">
                    <Button
                        onClick={() => dialog(<JoinTeamDialog />)}
                        variant="tertiary"
                        className="w-full">
                        Join a Team
                    </Button>
                    <Button
                        onClick={() => dialog(<CreateTeamDialog />)}
                        className="w-full">
                        Create a Team
                    </Button>
                </div>
            </DialogFooter>
        </div>
    );
}

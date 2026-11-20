import { Button } from "@/components/ui/button.tsx";
import { useMemo, useRef, useState } from "react";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import ImagePicker, {
    type ProfileImagePickerHandle,
} from "@/components/custom/ImagePicker.tsx";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/spinner.tsx";
import { type Team, useTeamQuery } from "@/store/teamStore.ts";
import { TeamPicture } from "@/components/custom/TeamPicture.tsx";
import { changeTeamIcon } from "@/api/teamApi.ts";
import { dialog, popDialog } from "@/store/dialogStore.tsx";

type ChangeTeamPictureProps = {
    team: Team;
};

export function ChangeTeamPicture(props: ChangeTeamPictureProps) {
    const { data: teams, refetch: refetchTeams } = useTeamQuery();
    const currentTeam = useMemo(
        () => teams?.find((t) => t.id === props.team.id) ?? props.team,
        [teams, props.team],
    );

    return (
        <div>
            <h3 className="mb-4">Team Profile Picture</h3>
            <div className="flex flex-col w-full items-center gap-4">
                <TeamPicture className="w-20 h-20" team={currentTeam} />
                <Button
                    className="w-full"
                    onClick={() =>
                        dialog(
                            <ChangeTeamPictureDialog
                                team={props.team}
                                onUpdated={refetchTeams}
                            />,
                        )
                    }>
                    Change Team Picture
                </Button>
            </div>
        </div>
    );
}

type ChangeTeamPictureDialogProps = {
    team: Team;
    onUpdated: () => void;
};

function ChangeTeamPictureDialog({
    team,
    onUpdated,
}: ChangeTeamPictureDialogProps) {
    const [ready, setReady] = useState(false);
    const [loading, setLoading] = useState(false);
    const pickerRef = useRef<ProfileImagePickerHandle | null>(null);

    return (
        <>
            <DialogHeader>
                <DialogTitle>Change Team Picture</DialogTitle>
                <DialogDescription className="sr-only">
                    Select and crop an image to use as the team picture.
                </DialogDescription>
            </DialogHeader>
            <div>
                <ImagePicker ref={pickerRef} onReadyChange={setReady} />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button
                    disabled={!ready || loading}
                    onClick={async () => {
                        setLoading(true);
                        const file =
                            await pickerRef.current?.exportCroppedFile();
                        if (!file) {
                            setLoading(false);
                            return;
                        }

                        changeTeamIcon(team.id, file)
                            .then(() => {
                                toast.success(
                                    "Team picture changed successfully.",
                                );
                                onUpdated();
                                popDialog();
                            })
                            .finally(() => setLoading(false));
                    }}>
                    {loading ? <Spinner /> : <>Change</>}
                </Button>
            </DialogFooter>
        </>
    );
}

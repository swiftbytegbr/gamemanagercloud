import { Button } from "@/components/ui/button.tsx";
import { useRef, useState } from "react";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import ImagePicker, {
    type ProfileImagePickerHandle,
} from "@/components/custom/ImagePicker.tsx";
import {
    refetchUserQuery,
    type User,
    useUserQuery,
} from "@/store/userStore.ts";
import { changeAvatar } from "@/api/userApi.ts";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ProfilePicture } from "@/components/custom/ProfilePicture.tsx";

type ChangeProfilePictureProps = {
    user?: User;
};

export function ChangeProfilePicture(props: ChangeProfilePictureProps) {
    const [dialogOpen, setDialogOpen] = useState(false);
    const [ready, setReady] = useState(false);
    const [loading, setLoading] = useState(false);
    const pickerRef = useRef<ProfileImagePickerHandle | null>(null);
    const { data: queriedUser } = useUserQuery();
    const currentUser = queriedUser ?? props.user;

    return (
        <div>
            <h3 className="mb-4">Profile Picture</h3>
            <div className="flex flex-col w-full items-center gap-4">
                <ProfilePicture className="w-20 h-20" user={currentUser} />
                <Button className="w-full" onClick={() => setDialogOpen(true)}>
                    Change Profile Picture
                </Button>
            </div>
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Change Profile Picture</DialogTitle>
                        <DialogDescription className="sr-only">
                            Select and crop an image to use as your profile
                            picture.
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
                                if (!file) return;

                                changeAvatar(file)
                                    .then(() => {
                                        toast.success(
                                            "Profile picture changed successfully",
                                        );
                                        refetchUserQuery();
                                        setDialogOpen(false);
                                    })
                                    .finally(() => setLoading(false));
                            }}>
                            {loading ? <Spinner /> : <>Change</>}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}

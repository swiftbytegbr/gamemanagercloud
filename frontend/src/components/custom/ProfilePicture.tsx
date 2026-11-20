import type { User } from "@/store/userStore.ts";
import { UserRound } from "lucide-react";
import { API_BASE_URL } from "@/api/axiosConfig.ts";

type ProfilePictureProps = {
    user?: User | null;
    imageId?: string;
    className?: string;
};

export function ProfilePicture(props: ProfilePictureProps) {
    return (
        <div className={props.className}>
            {props.user?.avatarImageId || props.imageId ? (
                <img
                    alt="Profile Picture"
                    className="rounded-full aspect-square h-full object-cover max-w-none"
                    src={`${API_BASE_URL}/images/${props.imageId ? props.imageId : props.user?.avatarImageId}`}
                />
            ) : (
                <div className="flex h-full aspect-square justify-center items-center bg-tertiary rounded-full">
                    <UserRound className="w-[60%] h-[60%]" />
                </div>
            )}
        </div>
    );
}

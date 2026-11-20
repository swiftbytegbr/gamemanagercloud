import { UsersRound } from "lucide-react";
import type { Team } from "@/store/teamStore.ts";
import { API_BASE_URL } from "@/api/axiosConfig.ts";

type TeamPictureProps = {
    team: Team | undefined | null;
    className?: string;
};
export function TeamPicture(props: TeamPictureProps) {
    return (
        <div className={props.className}>
            {props.team?.iconImageId ? (
                <img
                    alt="Team Picture"
                    className="rounded-full aspect-square h-full object-cover"
                    src={`${API_BASE_URL}/images/${props.team.iconImageId}`}
                />
            ) : (
                <div className="flex h-full aspect-square justify-center items-center bg-tertiary rounded-full">
                    <UsersRound className="w-[60%] h-[60%]" />
                </div>
            )}
        </div>
    );
}

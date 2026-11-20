import { Button } from "@/components/ui/button.tsx";
import {
    hasPermission,
    useActiveTeam,
    useTeamQuery,
} from "@/store/teamStore.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { MembersTable } from "@/components/team/MembersTable.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { InviteMemberDialog } from "@/components/team/dialogs/InviteMemberDialog.tsx";
import { EditTeamDialog } from "@/components/team/dialogs/EditTeamDialog.tsx";
import { LogOut, Pencil, Trash, UserPlus } from "lucide-react";
import { InvitedMembersTable } from "@/components/team/InvitedMembersTable.tsx";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs.tsx";
import { useMemo } from "react";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { leaveTeam } from "@/api/teamApi.ts";
import { toast } from "sonner";
import { DeleteTeamDialog } from "@/components/team/dialogs/DeleteTeamDialog.tsx";
import { StatusIcon } from "@/components/custom/StatusIcon.tsx";

export function TeamMembersPage() {
    const { data: user } = useUserQuery();
    const activeTeam = useActiveTeam();
    const { data: teams, refetch: refetchTeams } = useTeamQuery();

    const currentTeam = useMemo(() => {
        if (!activeTeam) return null;
        return teams?.find((t) => t.id === activeTeam.id) ?? activeTeam;
    }, [activeTeam, teams]);

    if (!currentTeam) return null;
    const isOwner = currentTeam.owner.id === user?.id;

    return (
        <div className="w-full h-full py-0">
            <div className="w-full h-full bg-secondary rounded-md pt-2 flex flex-col">
                <div className="grow overflow-hidden flex flex-col">
                    <Tabs
                        defaultValue="members"
                        className="flex flex-col grow overflow-hidden">
                        <div className="flex flex-col-reverse sm:flex-row items-center sm:items-end">
                            <TabsList className="grow text-primary-foreground border-b-muted px-4 justify-center sm:justify-start">
                                <TabsTrigger className="gap-2" value="members">
                                    Members
                                    <StatusIcon size="lg" box status="UNKNOWN">
                                        <p className="leading-4 text-sm">
                                            {currentTeam.members.length + 1}
                                        </p>
                                    </StatusIcon>
                                </TabsTrigger>
                                {hasPermission(
                                    currentTeam,
                                    user?.id,
                                    "MANAGE_TEAM",
                                ) && (
                                    <TabsTrigger
                                        className="gap-2"
                                        value="invites">
                                        Invited
                                        <StatusIcon
                                            size="lg"
                                            box
                                            status="UNKNOWN">
                                            <p className="leading-4 text-sm">
                                                {
                                                    currentTeam.invitedMembers
                                                        .length
                                                }
                                            </p>
                                        </StatusIcon>
                                    </TabsTrigger>
                                )}
                            </TabsList>
                            <div className="flex border-b-muted sm:border-b-2 px-4 gap-4 pb-2">
                                {hasPermission(
                                    currentTeam,
                                    user?.id,
                                    "MANAGE_TEAM",
                                ) && (
                                    <>
                                        <Button
                                            size="sm"
                                            onClick={() =>
                                                dialog(
                                                    <InviteMemberDialog
                                                        team={currentTeam}
                                                    />,
                                                )
                                            }>
                                            <UserPlus />
                                            <p className="hidden lg:block">
                                                Invite
                                            </p>
                                        </Button>
                                        <Button
                                            size="sm"
                                            variant="tertiary"
                                            onClick={() =>
                                                dialog(
                                                    <EditTeamDialog
                                                        team={currentTeam}
                                                    />,
                                                )
                                            }>
                                            <Pencil />
                                            <p className="hidden lg:block">
                                                Edit
                                            </p>
                                        </Button>
                                    </>
                                )}
                                {!isOwner ? (
                                    <Button
                                        size="sm"
                                        variant="destructive"
                                        onClick={() =>
                                            dialog(
                                                <ConfirmDialog
                                                    title={`Leave ${currentTeam.name}?`}
                                                    message={
                                                        "You will lose access to this team's nodes and servers."
                                                    }
                                                    confirmText="Leave Team"
                                                    onConfirm={async () => {
                                                        await leaveTeam(
                                                            currentTeam.id,
                                                        );
                                                        toast.success(
                                                            "You left the team.",
                                                        );
                                                        await refetchTeams();
                                                    }}
                                                />,
                                            )
                                        }>
                                        <LogOut />
                                        <p className="hidden lg:block">
                                            Leave Team
                                        </p>
                                    </Button>
                                ) : (
                                    <Button
                                        size="sm"
                                        variant="destructive"
                                        onClick={() =>
                                            dialog(
                                                <DeleteTeamDialog
                                                    team={currentTeam}
                                                />,
                                            )
                                        }>
                                        <Trash />
                                        <p className="hidden lg:block">
                                            Delete Team
                                        </p>
                                    </Button>
                                )}
                            </div>
                        </div>
                        <TabsContent
                            value="members"
                            className="flex flex-col grow overflow-hidden">
                            <div className="grow overflow-hidden">
                                <MembersTable
                                    team={currentTeam}
                                    userId={user?.id}
                                    onRefetch={refetchTeams}
                                />
                            </div>
                        </TabsContent>
                        {hasPermission(
                            currentTeam,
                            user?.id,
                            "MANAGE_TEAM",
                        ) && (
                            <TabsContent
                                value="invites"
                                className="flex flex-col grow overflow-hidden">
                                <InvitedMembersTable
                                    teamId={currentTeam.id}
                                    invitedMembers={
                                        currentTeam.invitedMembers ?? []
                                    }
                                    onRefetch={refetchTeams}
                                />
                            </TabsContent>
                        )}
                    </Tabs>
                </div>
            </div>
        </div>
    );
}

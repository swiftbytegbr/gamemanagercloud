import axiosClient from "@/api/axiosConfig.ts";
import type {
    ApiKey,
    Permission,
    TeamNotificationSettings,
} from "@/store/teamStore.ts";

export const fetchTeams = async () => {
    return (await axiosClient.get("/team/my")).data;
};

export const createTeam = async (teamName: string) => {
    return (await axiosClient.post("/team/create", { name: teamName })).data;
};

export const deleteTeam = async (teamId: string) => {
    return (await axiosClient.delete(`/team/${teamId}`)).data;
};

export const inviteMember = async (teamId: string, email: string) => {
    return (
        await axiosClient.post(`/team/${teamId}/invite`, { userEmail: email })
    ).data;
};

export const joinTeam = async (inviteCode: string) => {
    return (await axiosClient.post(`/team/join`, { inviteCode: inviteCode }))
        .data;
};

export const leaveTeam = async (teamId: string) => {
    return (await axiosClient.post(`/team/${teamId}/leave`)).data;
};

export const kickMember = async (teamId: string, userId: string) => {
    return (
        await axiosClient.post(`/team/${teamId}/kick`, { memberId: userId })
    ).data;
};

export const changeTeamName = async (teamId: string, name: string) => {
    return (await axiosClient.post(`/team/${teamId}/name`, { name: name }))
        .data;
};

export const changeTeamIcon = async (teamId: string, image: File) => {
    return (
        await axiosClient.post(`/team/${teamId}/icon`, image, {
            headers: {
                "Content-Type": image.type,
            },
        })
    ).data;
};

export const setMemberPermissions = async (
    teamId: string,
    memberId: string,
    permissions: Permission[],
) => {
    return (
        await axiosClient.post(`/team/${teamId}/permission`, {
            memberId: memberId,
            newPermissions: permissions,
        })
    ).data;
};

export const revokeTeamInvite = async (teamId: string, userId: string) => {
    return (await axiosClient.delete(`/team/${teamId}/invite/${userId}`)).data;
};

export const createApiKey = async (
    teamId: string,
    name: string,
    permissions: Permission[],
): Promise<{ applicationToken: string }> => {
    return (
        await axiosClient.post(`/team/${teamId}/application-user`, {
            applicationName: name,
            permissions: permissions,
        })
    ).data;
};

export const fetchApiKeys = async (teamId: string): Promise<ApiKey[]> => {
    return (await axiosClient.get(`/team/${teamId}/application-users`)).data;
};

export const revokeApiKey = async (teamId: string, applicationId: string) => {
    return (
        await axiosClient.delete(
            `/team/${teamId}/application-user/${applicationId}`,
        )
    ).data;
};

export const getActionLog = async (teamId: string, page = 1, size = 30) => {
    return (
        await axiosClient.get(
            `/team/${teamId}/action-log?page=${page}&size=${size}`,
        )
    ).data;
};

export const updateTeamNotificationSettings = async (
    teamId: string,
    settings: TeamNotificationSettings,
) => {
    return (
        await axiosClient.post(
            `/team/${teamId}/notification-settings`,
            settings,
        )
    ).data;
};

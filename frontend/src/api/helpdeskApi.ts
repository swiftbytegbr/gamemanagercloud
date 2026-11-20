import axiosClient from "./axiosConfig";

export const reportBug = async (
    subject: string,
    message: string,
    teamId: string,
    discordUsername?: string,
) => {
    return (
        await axiosClient.post("/helpdesk/bug-report", {
            subject: subject,
            message: message,
            discordUsername: discordUsername,
            teamId: teamId,
        })
    ).data;
};

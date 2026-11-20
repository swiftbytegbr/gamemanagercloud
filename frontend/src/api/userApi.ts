import axiosClient from "@/api/axiosConfig.ts";
import type {
    Address,
    CompanyInformation,
    NotificationSettings,
    PersonalInformation,
} from "@/store/userStore.ts";

export const fetchUser = async () => {
    return (await axiosClient.get("/user/@me")).data;
};

export const changeEmail = async (email: string) => {
    return (await axiosClient.post("/user/@me/change-email", { email: email }))
        .data;
};

export const changePersonalInfo = async (info: PersonalInformation) => {
    return (
        await axiosClient.post("/user/@me/personal-info", {
            personalInformation: info,
        })
    ).data;
};

export const changeAvatar = async (image: File) => {
    return (
        await axiosClient.post("/user/@me/avatar", image, {
            headers: {
                "Content-Type": image.type,
            },
        })
    ).data;
};

export const changeCompanyInfo = async (info: {
    companyInformation: CompanyInformation;
    billingAddress: Address;
}) => {
    return (await axiosClient.post("/user/@me/company-info", info)).data;
};

export const updateNotifications = async (
    notifications: NotificationSettings,
) => {
    return (
        await axiosClient.post("/user/@me/notification-settings", notifications)
    ).data;
};

export const changePassword = async (
    oldPassword: string,
    newPassword: string,
) => {
    return (
        await axiosClient.post("/user/@me/change-password", {
            oldPassword: oldPassword,
            newPassword,
        })
    ).data;
};

export type BackupCodesResponse = {
    backupCodesGenerated: boolean;
    backupCodes: string[];
};

export type TotpSetupResponse = {
    secret: string;
    provisioningUri: string;
    issuer: string;
    accountName: string;
};

export const enableEmailTwoFA = async () => {
    return (
        await axiosClient.post<BackupCodesResponse>(
            "/user/@me/2fa/email/enable",
        )
    ).data;
};

type DisableTwoFaPayload = {
    twoFACode?: string;
    backupCode?: string;
};

export const disableEmailTwoFA = async (
    payload?: DisableTwoFaPayload,
): Promise<void> => {
    await axiosClient.post("/user/@me/2fa/email/disable", payload ?? {});
};

export const setupTotpTwoFA = async () => {
    return (
        await axiosClient.post<TotpSetupResponse>("/user/@me/2fa/totp/setup")
    ).data;
};

export const verifyTotpTwoFA = async (code: string) => {
    return (
        await axiosClient.post<BackupCodesResponse>(
            "/user/@me/2fa/totp/verify",
            {
                code,
            },
        )
    ).data;
};

export const disableTotpTwoFA = async (payload: DisableTwoFaPayload) => {
    await axiosClient.post("/user/@me/2fa/totp/disable", payload);
};

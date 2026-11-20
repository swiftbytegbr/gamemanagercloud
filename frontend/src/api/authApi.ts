import axiosClient from "@/api/axiosConfig.ts";

interface LoginRequestData {
    email: string;
    password: string;
    rememberMe: boolean;
    twoFACode?: string;
    backupCode?: string;
}

export const login = async (loginData: LoginRequestData) => {
    const response = await axiosClient.post("/auth/login", loginData);

    return response.data;
};

export const logout = async () => {
    return (await axiosClient.post("/auth/logout")).data;
};

export const logoutAll = async () => {
    return (await axiosClient.post("/auth/logout/all")).data;
};

interface RegisterBillingAddress {
    street: string;
    city: string;
    region: string;
    postalCode: string;
    countryCode: string;
}

interface RegisterCompanyInformation {
    companyName: string;
    countryCode: string;
    vatNumber: string;
}

interface RegisterRequestData {
    email: string;
    password: string;
    username: string;
    name: string;
    companyInformation?: RegisterCompanyInformation;
    billingAddress?: RegisterBillingAddress;
}

export const register = async (registerData: RegisterRequestData) => {
    await axiosClient.post("/auth/register", registerData);
    return await login({
        email: registerData.email,
        password: registerData.password,
        rememberMe: false,
    });
};

export const requestPasswordReset = async (requestData: { email: string }) => {
    const response = await axiosClient.post(
        "/auth/reset-password/request",
        requestData,
    );
    return response.data;
};

export const resetPassword = async (resetData: {
    email: string;
    verificationCode: string;
    newPassword: string;
}) => {
    const response = await axiosClient.post(
        "/auth/reset-password/verify",
        resetData,
    );
    return response.data;
};

export const verifyEmail = async (verificationToken: number) => {
    return (
        await axiosClient.post("/auth/register/verify", {
            token: verificationToken,
        })
    ).data;
};

export const verifyEmailResend = async () => {
    return (await axiosClient.post("/auth/register/resend")).data;
};

export const getActiveSessions = async () => {
    return (await axiosClient.get("/auth/session")).data;
};

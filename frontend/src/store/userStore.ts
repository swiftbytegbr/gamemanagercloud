import { fetchUser } from "@/api/userApi.ts";
import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateQueryData } from "@/store/queryCache.ts";

export type Address = {
    countryCode: string;
    postalCode: string;
    city: string;
    region: string;
    street: string;
};

export type PersonalInformation = {
    name: string;
    username: string;
    billingAddress?: Address;
};

export type CompanyInformation = {
    companyName: string;
    countryCode: string;
    vatNumber: string;
};

export type NotificationSettings = {
    emailOnLogin: boolean;
};

export type User = {
    id: string;
    email: string;
    emailVerified: boolean;
    twoFAType: "NONE" | "EMAIL" | "TOTP";
    accountType: "USER" | "ADMIN" | "APPLICATION";
    personalInformation: PersonalInformation;
    companyInformation?: CompanyInformation;
    notificationSettings: NotificationSettings;
    avatarImageId: string;
};

export const useUserQuery = () => {
    return useQuery<User>({
        queryKey: queryKeys.user.all,
        queryFn: async () => {
            const user: User = await fetchUser();
            return user;
        },
        enabled: useAuthStore.getState().isAuthenticated,
        staleTime: 1000 * 60 * 5,
    });
};

export const refetchUserQuery = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.user.all });
};

export const updateUserCache = (
    updater: (current: User | undefined) => User | undefined,
) => {
    updateQueryData<User>(queryKeys.user.all, updater);
};

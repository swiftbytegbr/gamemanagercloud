import { create } from "zustand";
import { useQuery } from "@tanstack/react-query";
import { getActiveSessions } from "@/api/authApi.ts";
import { queryClient } from "@/store/queryConfig.ts";
import { useTeamStore } from "@/store/teamStore.ts";
import { isAxiosError } from "axios";
import { queryKeys } from "@/store/queryKeys.ts";

type AuthState = {
    isAuthenticated: boolean;
    isAuthResolved: boolean;
    initialize: () => Promise<void> | void;
    login: () => void;
    logout: () => Promise<void>;
};

export const useAuthStore = create<AuthState>((set, get) => {
    let initializationPromise: Promise<void> | null = null;

    return {
        isAuthenticated: false,
        isAuthResolved: false,

        initialize: async () => {
            if (get().isAuthResolved) return;
            if (initializationPromise) return initializationPromise;

            initializationPromise = getActiveSessions()
                .then(() => {
                    set({ isAuthenticated: true, isAuthResolved: true });
                })
                .catch((error) => {
                    if (isAxiosError(error) && error.response?.status === 401) {
                        set({ isAuthenticated: false, isAuthResolved: true });
                        return;
                    }
                    console.error("Failed to determine auth state", error);
                    set({ isAuthenticated: false, isAuthResolved: true });
                })
                .finally(() => {
                    initializationPromise = null;
                });

            return initializationPromise;
        },

        login: () => {
            set({ isAuthenticated: true, isAuthResolved: true });
            queryClient.invalidateQueries().then();
        },

        logout: async () => {
            const { selectActiveTeam } = useTeamStore.getState();

            set({ isAuthenticated: false, isAuthResolved: true });
            selectActiveTeam(null);
            await queryClient.cancelQueries();
            queryClient.clear();
        },
    };
});

export type Session = {
    sessionId: string;
    userId: string;
    created: string;
    lastAccessed: string;
    lastIp: string;
    lastUserAgent: string;
    expiresAt: string;
};

export const useSessionsQuery = () => {
    return useQuery<Session[]>({
        queryKey: queryKeys.sessions.all,
        queryFn: async () => {
            if (!useAuthStore.getState().isAuthenticated) return [];
            return getActiveSessions();
        },
        staleTime: 0,
    });
};

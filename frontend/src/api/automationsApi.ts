import type { Automation } from "@/store/automationsStore.ts";
import axiosClient from "@/api/axiosConfig.ts";

export const fetchAutomations = async (teamId: string) => {
    return (await axiosClient.get<Automation[]>(`/team/${teamId}/automations`))
        .data;
};

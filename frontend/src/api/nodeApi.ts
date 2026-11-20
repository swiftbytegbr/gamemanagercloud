import axiosClient from "@/api/axiosConfig.ts";
import type { NodeSettings } from "@/store/nodeStore.ts";

export const fetchNodes = async (teamId: string) => {
    return (await axiosClient.get(`/node/by-team/${teamId}`)).data;
};

export const createNode = async (teamId: string, name: string) => {
    return (
        await axiosClient.post("/node/create", {
            teamId: teamId,
            name: name,
        })
    ).data;
};

export const saveNodeSettings = async (
    nodeId: string,
    settings: NodeSettings,
) => {
    return (await axiosClient.post(`/node/${nodeId}/settings`, settings)).data;
};

export const updateNode = async (nodeId: string) => {
    return (await axiosClient.post(`/node/${nodeId}/update`)).data;
};

export const getLatestNodeVersion = async () => {
    return (await axiosClient.get("/node/latest-version")).data;
};

export const deleteNode = async (nodeId: string) => {
    return (await axiosClient.delete(`/node/${nodeId}`)).data;
};

export const fetchNodeTasks = async (nodeId: string) => {
    return (await axiosClient.get(`/node/${nodeId}/task`)).data;
};

export const cancelNodeTask = async (nodeId: string, taskId: string) => {
    return (await axiosClient.post(`/node/${nodeId}/task/${taskId}/cancel`))
        .data;
};

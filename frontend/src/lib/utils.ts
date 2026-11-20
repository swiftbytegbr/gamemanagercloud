import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import type { NodeStatus } from "@/store/nodeStore.ts";
import type { ServerState } from "@/store/serverStore.ts";

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

export const statusColorTextMap: Record<NodeStatus | ServerState, string> = {
    ONLINE: "text-green-600",
    OFFLINE: "text-red-600",
    INITIALIZING: "text-orange-400",
    CREATION: "text-orange-400",
    STOPPING: "text-orange-400",
    RESTARTING: "text-orange-400",
    CREATING: "text-orange-400",
    DELETING: "text-orange-400",
    UNKNOWN: "text-gray-400",
};

export const statusColorBgMap: Record<NodeStatus | ServerState, string> = {
    ONLINE: "bg-green-600",
    OFFLINE: "bg-red-600",
    INITIALIZING: "bg-orange-400",
    CREATION: "bg-orange-400",
    STOPPING: "bg-orange-400",
    RESTARTING: "bg-orange-400",
    CREATING: "bg-orange-400",
    DELETING: "bg-orange-400",
    UNKNOWN: "bg-gray-400",
};

export const serverStateOrder = [
    "ONLINE",
    "RESTARTING",
    "STOPPING",
    "CREATING",
    "OFFLINE",
    "DELETING",
    "UNKNOWN",
];

export const nodeStateOrder = ["ONLINE", "CREATION", "OFFLINE"];

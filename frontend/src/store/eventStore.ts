import { useEffect } from "react";

export type ProfileUpdatePayload = {
    profileId: string;
    userId: string;
};

type EventMap = {
    profileUpdate: ProfileUpdatePayload;
};

type EventName = keyof EventMap;
type EventHandler<K extends EventName> = (payload: EventMap[K]) => void;

const listeners: { [K in EventName]: Set<EventHandler<K>> } = {
    profileUpdate: new Set(),
};

function publishEvent<K extends EventName>(eventName: K, payload: EventMap[K]) {
    listeners[eventName].forEach((handler) => handler(payload));
}

function subscribeEvent<K extends EventName>(
    eventName: K,
    handler: EventHandler<K>,
) {
    listeners[eventName].add(handler);
    return () => {
        listeners[eventName].delete(handler);
    };
}

export type ProfileUpdateHandler = EventHandler<"profileUpdate">;

export function publishProfileUpdateEvent(profileId: string, userId: string) {
    publishEvent("profileUpdate", { profileId, userId });
}

export function subscribeToProfileUpdate(handler: ProfileUpdateHandler) {
    return subscribeEvent("profileUpdate", handler);
}

export function useProfileUpdateEvent(handler: ProfileUpdateHandler) {
    useEffect(() => {
        const unsubscribe = subscribeToProfileUpdate(handler);
        return () => unsubscribe();
    }, [handler]);
}

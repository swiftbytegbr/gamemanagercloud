import type { QueryKey } from "@tanstack/react-query";
import { queryClient } from "@/store/queryConfig.ts";

export const updateQueryData = <T>(
    queryKey: QueryKey,
    updater: (current: T | undefined) => T | undefined,
) => {
    queryClient.setQueryData<T>(queryKey, (current: T | undefined) =>
        updater(current),
    );
};

type WithId = {
    id: string;
};

export const upsertListItem = <T extends WithId>(
    queryKey: QueryKey,
    item: T,
    options?: { position?: "start" | "end" },
) => {
    const position = options?.position ?? "start";
    queryClient.setQueriesData<T[]>({ queryKey }, (oldData) => {
        if (!oldData) return oldData;
        const index = oldData.findIndex((existing) => existing.id === item.id);
        if (index === -1) {
            return position === "end" ? [...oldData, item] : [item, ...oldData];
        }
        const next = [...oldData];
        next[index] = item;
        return next;
    });
};

export const updateListItemById = <T extends WithId>(
    queryKey: QueryKey,
    itemId: string,
    updater: (item: T) => T,
    options?: { onMiss?: () => void },
): boolean => {
    let updated = false;
    queryClient.setQueriesData<T[]>({ queryKey }, (oldData) => {
        if (!oldData) return oldData;
        const next = oldData.map((item) => {
            if (item.id !== itemId) return item;
            updated = true;
            return updater(item);
        });
        return updated ? next : oldData;
    });
    if (!updated) {
        options?.onMiss?.();
    }
    return updated;
};

export const removeListItemById = <T extends WithId>(
    queryKey: QueryKey,
    itemId: string,
) => {
    queryClient.setQueriesData<T[]>({ queryKey }, (oldData) => {
        if (!oldData) return oldData;
        const next = oldData.filter((item) => item.id !== itemId);
        return next.length === oldData.length ? oldData : next;
    });
};

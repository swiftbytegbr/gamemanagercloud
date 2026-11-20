import { QueryClient } from "@tanstack/react-query";

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: (failureCount) => {
                if (typeof navigator !== "undefined" && !navigator.onLine)
                    return false;
                return failureCount < 2;
            },
        },
        mutations: {
            retry: 0,
        },
    },
});

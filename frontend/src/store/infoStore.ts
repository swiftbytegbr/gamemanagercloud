import { useQuery } from "@tanstack/react-query";
import { fetchVersionInfo } from "@/api/infoApi.ts";
import { queryKeys } from "@/store/queryKeys.ts";

export interface VersionInfo {
    version: string;
    description: string;
    changelog: string;
    releaseDate: string;
}

export const useVersionInfoQuery = () => {
    return useQuery<VersionInfo>({
        queryKey: queryKeys.versionInfo.all,
        queryFn: fetchVersionInfo,
        staleTime: 1000 * 60 * 30,
    });
};

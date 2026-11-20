import { useCallback } from "react";
import type { SubmitHandler, UseFormReturn } from "react-hook-form";
import type {
    SettingProfile,
    SettingProfileValue,
} from "@/store/serverStore.ts";
import { saveSettingProfile } from "@/api/serverApi.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateQueryData } from "@/store/queryCache.ts";
import { toast } from "sonner";
import {
    buildSettingProfileFromForm,
    type CustomIniSnapshot,
    type ScrollMemory,
} from "@/lib/serverSettings";

type Params = {
    form: UseFormReturn<Record<string, unknown>>;
    profile?: SettingProfile;
    customIni: CustomIniSnapshot;
    scrollMemory: ScrollMemory;
    setLoading: (loading: boolean) => void;
};

export function useServerSettingsSubmit({
    form,
    profile,
    customIni,
    scrollMemory,
    setLoading,
}: Params) {
    return useCallback<SubmitHandler<Record<string, unknown>>>(
        async (data) => {
            if (!profile) return;
            scrollMemory.remember();
            setLoading(true);
            try {
                const typedData = data as Record<string, SettingProfileValue>;
                const nextProfile = buildSettingProfileFromForm(
                    profile,
                    typedData,
                    customIni,
                );

                await saveSettingProfile(nextProfile);
                updateQueryData<SettingProfile>(
                    queryKeys.settingProfiles.detail(
                        profile.teamId,
                        profile.id,
                    ),
                    () => nextProfile,
                );
                form.reset(form.getValues());
                toast.success(
                    "Settings saved successfully! Please restart your server to apply the new settings.",
                );
            } finally {
                setLoading(false);
            }
        },
        [profile, customIni, scrollMemory, setLoading, form],
    );
}

import {
    createElement,
    useCallback,
    useDeferredValue,
    useEffect,
    useLayoutEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { useActiveTeam } from "@/store/teamStore.ts";
import {
    useServerByIdQuery,
    useSettingProfileQuery,
    useSettingTemplateQuery,
} from "@/store/serverStore.ts";
import { useServerSettingsForm } from "@/hooks/serverSettings/useServerSettingsForm.ts";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useCustomIniState } from "./useCustomIniState";
import { useSettingsSearch } from "./useSettingsSearch";
import { useScrollMemory } from "./useScrollMemory";
import { useServerSettingsSubmit } from "./useServerSettingsSubmit";
import { useUnsavedChangesPrompt } from "./useUnsavedChangesPrompt";
import {
    buildSyncableSettings,
    collectUserCustomSettings,
    createCategoryList,
    SERVER_SETTINGS_SCROLL_PREFIX,
} from "@/lib/serverSettings";
import { closeDialogsByTag, dialog } from "@/store/dialogStore.tsx";
import { ProfileUpdateConflictDialog } from "@/components/servers/dialogs/ProfileUpdateConflictDialog.tsx";
import {
    type ProfileUpdatePayload,
    useProfileUpdateEvent,
} from "@/store/eventStore.ts";
import { toast } from "sonner";

export function useServerSettingsController(serverId?: string) {
    const activeTeam = useActiveTeam();
    const { data: server, isLoading: isServerLoading } = useServerByIdQuery(
        activeTeam,
        serverId ?? "",
    );
    const {
        data: profile,
        isLoading: isProfileLoading,
        refetch: refetchProfile,
    } = useSettingProfileQuery(activeTeam, server?.settingProfileId);
    const { data: templates, isLoading: isTemplateLoading } =
        useSettingTemplateQuery(server?.gameType);

    const { schema, defaults, processedTemplates, isProcessing } =
        useServerSettingsForm(templates, profile);

    const resolver = useMemo(() => zodResolver(schema), [schema]);
    const form = useForm({
        resolver,
        defaultValues: defaults,
    });

    useEffect(() => {
        form.reset(defaults);
    }, [defaults, form]);

    const deferredTemplates = useDeferredValue(processedTemplates);
    const isDeferredPending = processedTemplates !== deferredTemplates;

    const categoriesList = useMemo(
        () => createCategoryList(deferredTemplates),
        [deferredTemplates],
    );

    const userCustomSettings = useMemo(
        () => collectUserCustomSettings(profile, templates),
        [profile, templates],
    );
    const customIni = useCustomIniState(userCustomSettings);
    const syncableSettings = useMemo(
        () => buildSyncableSettings(deferredTemplates, userCustomSettings),
        [deferredTemplates, userCustomSettings],
    );

    const categoryRefs = useRef<Record<string, HTMLDivElement>>({});
    const registerCategoryRef = useCallback(
        (category: string, node: HTMLDivElement | null) => {
            if (!node) {
                delete categoryRefs.current[category];
                return;
            }
            categoryRefs.current[category] = node;
        },
        [],
    );

    const containerRef = useRef<HTMLDivElement | null>(null);
    const activeGameType = profile?.gameType ?? server?.gameType ?? null;
    const scrollStorageKey = useMemo(
        () => `${SERVER_SETTINGS_SCROLL_PREFIX}${activeGameType ?? "unknown"}`,
        [activeGameType],
    );
    const scrollMemory = useScrollMemory(
        containerRef,
        scrollStorageKey,
        !isProcessing && !isDeferredPending,
    );
    const rememberScrollPosition = scrollMemory.remember;
    const profileVersion = profile
        ? `${profile.id}:${profile.lastModified}`
        : null;

    useLayoutEffect(() => {
        if (!profileVersion) return;
        return () => {
            rememberScrollPosition();
        };
    }, [profileVersion, rememberScrollPosition]);

    const search = useSettingsSearch(deferredTemplates);
    const [isLoading, setLoading] = useState(false);

    useUnsavedChangesPrompt(form.formState.isDirty);

    const handleSubmit = useServerSettingsSubmit({
        form,
        profile,
        customIni,
        scrollMemory,
        setLoading,
    });

    const reloadProfile = useCallback(async () => {
        await refetchProfile({ cancelRefetch: false });
    }, [refetchProfile]);

    const handleReloadFromServer = useCallback(async () => {
        await reloadProfile();
    }, [reloadProfile]);

    const resolveUserName = useCallback(
        (userId: string) => {
            if (!activeTeam) return userId;
            if (activeTeam.owner?.id === userId) {
                return activeTeam.owner.name;
            }
            const member = activeTeam.members.find(
                (teamMember) => teamMember.id === userId,
            );
            return member?.name ?? userId;
        },
        [activeTeam],
    );

    const handleProfileUpdateEvent = useCallback(
        ({ profileId, userId }: ProfileUpdatePayload) => {
            if (!profile || profileId !== profile.id) return;
            const updatedBy = resolveUserName(userId);

            if (!form.formState.isDirty) {
                toast.success(
                    `${updatedBy} updated the settings profile. Reloaded changes from server successfully.`,
                );
                handleReloadFromServer().then();
                return;
            }

            closeDialogsByTag("profile-update-conflict");
            dialog(
                createElement(ProfileUpdateConflictDialog, {
                    userName: updatedBy,
                    onReload: handleReloadFromServer,
                }),
                {
                    tag: "profile-update-conflict",
                    modal: true,
                    disableCloseButton: true,
                },
            );
        },
        [
            profile,
            form.formState.isDirty,
            resolveUserName,
            handleReloadFromServer,
        ],
    );

    useProfileUpdateEvent(handleProfileUpdateEvent);

    return {
        server,
        profile,
        templates,
        form,
        processedTemplates: deferredTemplates,
        categoriesList,
        customIni,
        syncableSettings,
        categoryRefs,
        registerCategoryRef,
        containerRef,
        search,
        status: {
            isServerLoading,
            isProfileLoading,
            isTemplateLoading,
        },
        processingState: {
            isProcessing,
            isDeferredPending,
        },
        loadingState: {
            isLoading,
            setLoading,
        },
        actions: {
            handleSubmit,
            reloadProfile,
        },
    };
}

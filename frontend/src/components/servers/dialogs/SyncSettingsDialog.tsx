import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import {
    refetchServerQuery,
    type Server,
    type SettingTemplate,
    useServerQuery,
} from "@/store/serverStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import { syncSettingProfile } from "@/api/serverApi.ts";
import { ArrowBigDown, ArrowBigRight } from "lucide-react";
import { closeDialog } from "@/store/dialogStore.tsx";
import { useMemo } from "react";
import { cn } from "@/lib/utils.ts";

export type SyncSettingsDialogProps = {
    server: Server;
    settings: Record<string, SyncableSetting[]>;
    setLoading: (loading: boolean) => void;
};

export type SyncableSetting = Pick<
    SettingTemplate,
    "location" | "section" | "key" | "disableSync"
>;

const syncSchema = z.object({
    selectedCategories: z
        .array(z.string())
        .min(1, "Please select at least one category."),
    selectedServerIds: z
        .array(z.string())
        .min(1, "Please select at least one server."),
});

export function SyncSettingsDialog({
    server,
    settings,
    setLoading,
}: SyncSettingsDialogProps) {
    const team = useActiveTeam();
    const { data: servers = [] } = useServerQuery(team);

    const targetServers = useMemo(() => {
        return servers.filter(
            (s) => s.id !== server.id && s.gameType === server.gameType,
        );
    }, [servers, server.id, server.gameType]);

    const form = useForm<z.infer<typeof syncSchema>>({
        resolver: zodResolver(syncSchema),
        defaultValues: {
            selectedCategories: [],
            selectedServerIds: [],
        },
    });

    const categoryKeys = useMemo(() => {
        const keys = Object.keys(settings || {});
        if (!keys.includes("Custom")) return keys;
        return [...keys.filter((key) => key !== "Custom"), "Custom"];
    }, [settings]);

    function toggleAllCategories(select: boolean) {
        form.setValue("selectedCategories", select ? categoryKeys : [], {
            shouldDirty: true,
            shouldValidate: true,
        });
    }

    function toggleAllServers(select: boolean) {
        const ids = targetServers.map((s) => s.settingProfileId);
        form.setValue("selectedServerIds", select ? ids : [], {
            shouldDirty: true,
            shouldValidate: true,
        });
    }

    function onToggleCategory(key: string, checked: boolean) {
        const current = new Set(form.getValues("selectedCategories"));
        if (checked) current.add(key);
        else current.delete(key);
        form.setValue("selectedCategories", Array.from(current), {
            shouldDirty: true,
            shouldValidate: true,
        });
    }

    function onToggleServer(id: string, checked: boolean) {
        const current = new Set(form.getValues("selectedServerIds"));
        if (checked) current.add(id);
        else current.delete(id);
        form.setValue("selectedServerIds", Array.from(current), {
            shouldDirty: true,
            shouldValidate: true,
        });
    }

    async function onSubmit(values: z.infer<typeof syncSchema>) {
        setLoading(true);
        const gameSettingKeys: Record<string, string[]> = {};
        const gameUserSettingKeys: Record<string, string[]> = {};
        const hyphenSettingKeys: string[] = [];
        const questionMarkSettingKeys: string[] = [];
        const gmcSettingKeys: string[] = [];

        Object.entries(settings).forEach(([key, elements]) => {
            if (values.selectedCategories.includes(key)) {
                elements.forEach((element) => {
                    if (element.disableSync) return;
                    switch (element.location) {
                        case "Game.ini":
                            if (!gameSettingKeys[element.section]) {
                                gameSettingKeys[element.section] = [];
                            }
                            gameSettingKeys[element.section].push(element.key);
                            break;
                        case "GameUserSettings.ini":
                            if (!gameUserSettingKeys[element.section]) {
                                gameUserSettingKeys[element.section] = [];
                            }
                            gameUserSettingKeys[element.section].push(
                                element.key,
                            );
                            break;
                        case "CommandLineOption":
                            if (element.section === "-") {
                                hyphenSettingKeys.push(element.key);
                            } else if (element.section === "?") {
                                questionMarkSettingKeys.push(element.key);
                            }
                            break;
                        case "GMCSettings":
                            if (element.key != "Map") {
                                gmcSettingKeys.push(element.key);
                            }
                            break;
                    }
                });
            }
        });
        syncSettingProfile(
            server.settingProfileId,
            values.selectedServerIds.filter(
                (it) => server.settingProfileId != it,
            ),
            gameSettingKeys,
            gameUserSettingKeys,
            hyphenSettingKeys,
            questionMarkSettingKeys,
            gmcSettingKeys,
        )
            .then(() => {
                toast.success("Settings synced successfully.");
                refetchServerQuery();
                closeDialog();
            })
            .finally(() => setLoading(false));
    }

    const selectedCategories = form.watch("selectedCategories");
    const selectedServerIds = form.watch("selectedServerIds");

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Sync Settings: {server.displayName}</DialogTitle>
                <DialogDescription>
                    Select the categories on the left and the servers on the
                    right to which they should be applied.
                </DialogDescription>
            </DialogHeader>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-[1fr_3rem_1fr]">
                <div className="flex flex-col gap-3">
                    <h3 className="text-center">Categories</h3>
                    <div
                        className={cn(
                            "text-red-500 text-sm",
                            !form.formState.errors.selectedCategories
                                ?.message && "hidden",
                        )}>
                        {form.formState.errors.selectedCategories?.message}
                    </div>
                    <div className="grow rounded-md p-3 bg-background">
                        <div className="mb-3 flex items-center">
                            <Checkbox
                                checked={
                                    selectedCategories.length ===
                                        categoryKeys.length &&
                                    categoryKeys.length > 0
                                }
                                onCheckedChange={(val) =>
                                    toggleAllCategories(val === true)
                                }
                            />
                            <p className="font-medium ml-2">Select All</p>
                        </div>
                        <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                            {categoryKeys.length === 0 && (
                                <p className="text-sm text-muted-foreground">
                                    No Categories Found
                                </p>
                            )}
                            {categoryKeys.map((key) => {
                                const id = `cat-${key}`;
                                const checked =
                                    selectedCategories.includes(key);
                                return (
                                    <div
                                        key={key}
                                        className="flex items-center gap-2">
                                        <Checkbox
                                            id={id}
                                            checked={checked}
                                            onCheckedChange={(v) =>
                                                onToggleCategory(
                                                    key,
                                                    Boolean(v),
                                                )
                                            }
                                        />
                                        <label
                                            htmlFor={id}
                                            className="cursor-pointer select-none">
                                            {key}
                                        </label>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
                <div className="flex items-center justify-center">
                    <ArrowBigRight
                        strokeWidth={1.25}
                        className="hidden md:block size-12"
                    />
                    <ArrowBigDown
                        strokeWidth={1.25}
                        className="md:hidden size-12"
                    />
                </div>
                <div className="flex flex-col gap-3">
                    <h3 className="text-center">Servers</h3>
                    <div
                        className={cn(
                            "text-red-500 text-sm mt-1",
                            !form.formState.errors.selectedServerIds?.message &&
                                "hidden",
                        )}>
                        {form.formState.errors.selectedServerIds?.message}
                    </div>
                    <div className="grow rounded-md bg-background p-3">
                        <div className="mb-3 flex items-center">
                            <Checkbox
                                checked={
                                    selectedServerIds.length ===
                                        targetServers.length &&
                                    targetServers.length > 0
                                }
                                onCheckedChange={(val) =>
                                    toggleAllServers(val === true)
                                }
                            />
                            <p className="font-medium ml-2">Select All</p>
                        </div>
                        <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                            {targetServers.length === 0 && (
                                <p className="text-sm text-muted-foreground">
                                    No Server Found
                                </p>
                            )}
                            {targetServers.map((s) => {
                                const id = `srv-${s.id}`;
                                const checked = selectedServerIds.includes(
                                    s.settingProfileId,
                                );
                                return (
                                    <div
                                        key={s.settingProfileId}
                                        className="flex items-center gap-2">
                                        <Checkbox
                                            id={id}
                                            checked={checked}
                                            onCheckedChange={(v) =>
                                                onToggleServer(
                                                    s.settingProfileId,
                                                    Boolean(v),
                                                )
                                            }
                                        />
                                        <label
                                            htmlFor={id}
                                            className="cursor-pointer select-none">
                                            {s.displayName}
                                        </label>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </div>

            <DialogFooter className="mt-2 md:mt-0">
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button type="submit">Sync Settings</Button>
            </DialogFooter>
        </ManagedForm>
    );
}

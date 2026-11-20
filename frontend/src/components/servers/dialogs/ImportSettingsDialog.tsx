import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Form } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useForm, type UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { parseIni, saveSettingProfile } from "@/api/serverApi.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateQueryData } from "@/store/queryCache.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";
import type { SettingProfile } from "@/store/serverStore.ts";

const importSettingsSchema = z.object({
    gameUserSettings: z
        .instanceof(FileList)
        .refine((files) => files.length <= 1, {
            message: "Please select only one file.",
        })
        .refine(
            (files) => files.length === 0 || files[0].size <= 10 * 1024 * 1024,
            {
                message: "File size must be less than 10MB.",
            },
        )
        .refine(
            (files) =>
                files.length === 0 ||
                files[0].name.endsWith(".ini") ||
                files[0].name.endsWith(".txt"),
            {
                message: "File type must be .txt or .ini.",
            },
        )
        .transform((files) => files[0])
        .optional(),
    gameSettings: z
        .instanceof(FileList)
        .refine((files) => files.length <= 1, {
            message: "Please select only one file.",
        })
        .refine(
            (files) => files.length === 0 || files[0].size <= 10 * 1024 * 1024,
            {
                message: "File size must be less than 10MB.",
            },
        )
        .refine(
            (files) =>
                files.length === 0 ||
                files[0].name.endsWith(".ini") ||
                files[0].name.endsWith(".txt"),
            {
                message: "File type must be .txt or .ini.",
            },
        )
        .transform((files) => files[0])
        .optional(),
});

export type ImportSettingsDialogProps = {
    profile: SettingProfile;
    setLoading: (loading: boolean) => void;
};

export function ImportSettingsDialog({
    profile,
    setLoading,
}: ImportSettingsDialogProps) {
    const importForm = useForm<
        z.input<typeof importSettingsSchema>,
        undefined,
        z.infer<typeof importSettingsSchema>
    >({
        resolver: zodResolver(importSettingsSchema),
    });

    const castImportForm = importForm as unknown as UseFormReturn<
        z.input<typeof importSettingsSchema>
    >;

    async function onSubmitImport(data: z.infer<typeof importSettingsSchema>) {
        if (!data.gameSettings && !data.gameUserSettings) {
            toast.error("Please select at least one file to import.");
            return;
        }

        setLoading(true);

        try {
            const gameUserSettings = data.gameUserSettings
                ? await parseIni(await data.gameUserSettings.text())
                : undefined;
            const gameSettings = data.gameSettings
                ? await parseIni(await data.gameSettings.text())
                : undefined;

            const mergedGameUserSettings = {
                ...profile.gameUserSettings,
                ...gameUserSettings,
                ...Object.keys(gameUserSettings || {}).reduce(
                    (acc, section) => {
                        acc[section] = {
                            ...profile.gameUserSettings?.[section],
                            ...gameUserSettings?.[section],
                        };
                        return acc;
                    },
                    {} as Record<string, Record<string, unknown>>,
                ),
            };

            const mergedGameSettings = {
                ...profile.gameSettings,
                ...gameSettings,
                ...Object.keys(gameSettings || {}).reduce(
                    (acc, section) => {
                        acc[section] = {
                            ...profile.gameSettings?.[section],
                            ...gameSettings?.[section],
                        };
                        return acc;
                    },
                    {} as Record<string, Record<string, unknown>>,
                ),
            };

            const newProfile: SettingProfile = {
                ...profile,
                gameSettings: mergedGameSettings,
                gameUserSettings: mergedGameUserSettings,
            };

            if (gameUserSettings?.SessionSettings?.Port) {
                if (newProfile.gameType === "ARK_ASCENDED") {
                    newProfile.hyphenParams["port"] = Number(
                        gameUserSettings["SessionSettings"]["Port"],
                    );
                } else if (newProfile.gameType === "ARK_EVOLVED") {
                    newProfile.questionMarkParams["Port"] = Number(
                        gameUserSettings["SessionSettings"]["Port"],
                    );
                }
            }

            if (gameUserSettings?.SessionSettings?.QueryPort) {
                if (newProfile.gameType === "ARK_EVOLVED") {
                    newProfile.questionMarkParams["QueryPort"] = Number(
                        gameUserSettings["SessionSettings"]["QueryPort"],
                    );
                }
            }

            saveSettingProfile(newProfile)
                .then(() => {
                    updateQueryData<SettingProfile>(
                        queryKeys.settingProfiles.detail(
                            profile.teamId,
                            profile.id,
                        ),
                        () => newProfile,
                    );
                    toast.success("Settings imported successfully!");
                    closeDialog();
                })
                .finally(() => setLoading(false));
        } catch (error) {
            toast.error(
                "Failed to parse the provided INI files. Please check the format.",
            );
            setLoading(false);
            console.error(error);
            return;
        }
    }

    return (
        <Form {...importForm}>
            <DialogHeader>
                <DialogTitle>Import your Settings</DialogTitle>
                <DialogDescription asChild>
                    <div>
                        <p className="text-yellow-400">
                            <span className="font-bold">Attention!</span>{" "}
                            [Experimental function]
                        </p>
                        <p>
                            All settings will be overwritten (cannot be undone)
                            and errors may occur.
                        </p>
                    </div>
                </DialogDescription>
                <div className="mt-5 flex flex-col gap-3">
                    <FormInput
                        type="file"
                        form={castImportForm}
                        label="GameUserSettings.ini"
                        name="gameUserSettings"
                        accept=".ini,.txt"
                    />
                    <FormInput
                        type="file"
                        form={castImportForm}
                        label="Game.ini"
                        name="gameSettings"
                        accept=".ini,.txt"
                    />
                </div>
                <DialogFooter className="mt-5">
                    <DialogClose asChild>
                        <Button variant="tertiary">Cancel</Button>
                    </DialogClose>
                    <DialogClose asChild>
                        <Button
                            onClick={importForm.handleSubmit(onSubmitImport)}>
                            Import Settings
                        </Button>
                    </DialogClose>
                </DialogFooter>
            </DialogHeader>
        </Form>
    );
}

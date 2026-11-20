import { useMemo, useEffect, useState } from "react";
import { appConfig } from "@/config.ts";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ManagedForm } from "@/components/ui/form.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { FormSelect } from "@/components/custom/form/FormSelect.tsx";
import {
    type DiscordNotificationSeverity,
    useActiveTeam,
    useTeamQuery,
} from "@/store/teamStore.ts";
import { useUserQuery } from "@/store/userStore.ts";
import { updateTeamNotificationSettings } from "@/api/teamApi.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { updateListItemById } from "@/store/queryCache.ts";
import { dialog } from "@/store/dialogStore.tsx";
import { Link } from "@tanstack/react-router";
import { ManageApiKeysDialog } from "@/components/team/dialogs/api/ManageApiKeysDialog.tsx";

const discordWebhookSchema = z
    .string()
    .trim()
    .refine(
        (value) => {
            if (!value || value.length === 0) return true;
            return z.string().url().safeParse(value).success;
        },
        { message: "Please enter a valid webhook URL (or leave it empty)." },
    );

const teamNotificationSettingsSchema = z.object({
    discordWebhookUrl: discordWebhookSchema,
    discordNotificationsMinSeverity: z.enum([
        "INFO",
        "WARNING",
        "ERROR",
        "CRITICAL",
    ]),
});

const severityOptions: { value: DiscordNotificationSeverity; label: string }[] =
    [
        { value: "INFO", label: "Info" },
        { value: "WARNING", label: "Warning" },
        { value: "ERROR", label: "Error" },
        { value: "CRITICAL", label: "Critical" },
    ];

export function TeamSettingsPage() {
    const { data: user } = useUserQuery();
    const activeTeam = useActiveTeam();
    const { data: teams, dataUpdatedAt: teamsDataUpdatedAt } = useTeamQuery();

    const [isLoading, setLoading] = useState(false);

    const currentTeam = useMemo(() => {
        if (!activeTeam) return null;
        return teams?.find((t) => t.id === activeTeam.id) ?? activeTeam;
    }, [activeTeam, teams]);

    const defaultValues = useMemo(() => {
        return {
            discordWebhookUrl:
                currentTeam?.notificationSettings?.discordWebhookUrl ?? "",
            discordNotificationsMinSeverity:
                currentTeam?.notificationSettings
                    ?.discordNotificationsMinSeverity ?? "WARNING",
        };
    }, [
        currentTeam?.notificationSettings?.discordWebhookUrl,
        currentTeam?.notificationSettings?.discordNotificationsMinSeverity,
    ]);

    const form = useForm<z.infer<typeof teamNotificationSettingsSchema>>({
        resolver: zodResolver(teamNotificationSettingsSchema),
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form, teamsDataUpdatedAt]);

    if (!currentTeam) return null;

    const onSubmit = async (
        data: z.infer<typeof teamNotificationSettingsSchema>,
    ) => {
        if (!user) return;
        setLoading(true);
        updateTeamNotificationSettings(currentTeam.id, {
            discordWebhookUrl: data.discordWebhookUrl?.trim() ?? "",
            discordNotificationsMinSeverity:
                data.discordNotificationsMinSeverity,
        })
            .then(async () => {
                toast.success(
                    "Team notification settings updated successfully.",
                );
                updateListItemById(
                    queryKeys.teams.all,
                    currentTeam.id,
                    (team) => ({
                        ...team,
                        notificationSettings: {
                            discordWebhookUrl:
                                data.discordWebhookUrl?.trim() ?? "",
                            discordNotificationsMinSeverity:
                                data.discordNotificationsMinSeverity,
                        },
                    }),
                );
            })
            .finally(() => setLoading(false));
    };

    return (
        <div className="h-full flex flex-col">
            <div className="flex flex-col lg:flex-row gap-3 items-center justify-between">
                <p className="text-2xl font-semibold">Team Settings</p>
            </div>

            <div className="h-0 w-full pt-5 grow">
                <div className="overflow-y-auto w-full h-full bg-secondary rounded-md p-5 flex flex-col gap-5">
                    <ManagedForm form={form} onSubmit={onSubmit}>
                        <div>
                            <h3>Notifications</h3>
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 h-fit mt-2">
                                <FormInput
                                    form={form}
                                    label="Discord Webhook URL"
                                    name="discordWebhookUrl"
                                    inputMode="url"
                                    disabled={isLoading}
                                    placeholder="Not configured"
                                />
                                <FormSelect
                                    form={form}
                                    label="Minimum Severity"
                                    name="discordNotificationsMinSeverity"
                                    items={severityOptions}
                                    disabled={isLoading}
                                />
                                <div className="lg:col-span-2 flex justify-end">
                                    <Button disabled={isLoading} type="submit">
                                        {isLoading ? (
                                            <Spinner />
                                        ) : (
                                            <>Update Notifications</>
                                        )}
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </ManagedForm>
                    <hr />
                    <div>
                        <h3>API Keys</h3>
                        <p className="text-sm text-muted-foreground">
                            You can use our API to give other software, such as
                            Discord bots, access to GMC. These require an API
                            key for authentication. You can learn more here:{" "}
                            <Link
                                to={appConfig.docsUrl}
                                className="text-primary"
                                target="_blank">
                                {appConfig.docsUrl}
                            </Link>
                        </p>
                        <Button
                            className="mt-4"
                            onClick={() =>
                                dialog(
                                    <ManageApiKeysDialog team={currentTeam} />,
                                    {
                                        height: "xl",
                                        width: "xl",
                                    },
                                )
                            }>
                            Manage API Keys
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}

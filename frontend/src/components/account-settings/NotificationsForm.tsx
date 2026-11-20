import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ManagedForm } from "@/components/ui/form.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { updateNotifications } from "@/api/userApi.ts";
import { updateUserCache } from "@/store/userStore.ts";
import { toast } from "sonner";
import { z } from "zod";
import type { NotificationSettings, User } from "@/store/userStore.ts";
import { changeNotificationsSchema } from "@/components/account-settings/schemas.ts";

export function NotificationsForm({ user }: { user?: User }) {
    const [loading, setLoading] = useState(false);
    const form = useForm<z.infer<typeof changeNotificationsSchema>>({
        resolver: zodResolver(changeNotificationsSchema),
        defaultValues: {
            emailOnLogin: user?.notificationSettings?.emailOnLogin || false,
        },
    });

    function onSubmit(data: z.infer<typeof changeNotificationsSchema>) {
        setLoading(true);
        const notificationSettings: NotificationSettings = {
            emailOnLogin: data.emailOnLogin ?? false,
        };
        updateNotifications(notificationSettings)
            .then(() => {
                toast.success("Notification settings updated successfully.");
                updateUserCache((current) =>
                    current
                        ? {
                              ...current,
                              notificationSettings,
                          }
                        : current,
                );
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <div>
                <div className="flex flex-col gap-4 mt-2">
                    <FormCheckbox
                        form={form}
                        label="Notification of new login"
                        name="emailOnLogin"
                    />
                    <Button disabled={loading} type="submit">
                        {loading ? <Spinner /> : <>Update Notifications</>}
                    </Button>
                </div>
            </div>
        </ManagedForm>
    );
}

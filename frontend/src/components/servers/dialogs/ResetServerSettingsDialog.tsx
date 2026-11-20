import { Button } from "@/components/ui/button.tsx";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { type Server, type SettingProfile } from "@/store/serverStore.ts";
import { resetSettings } from "@/api/serverApi.ts";
import { closeDialog } from "@/store/dialogStore.tsx";
import { queryClient } from "@/store/queryConfig.ts";
import { queryKeys } from "@/store/queryKeys.ts";

type ResetServerSettingsDialogProps = {
    server: Server;
    profile: SettingProfile;
};

export function ResetServerSettingsDialog(
    props: ResetServerSettingsDialogProps,
) {
    const [isLoading, setLoading] = useState(false);

    const resetFormSchema = z.object({
        friendlyName: z
            .string({ message: "Server name is required" })
            .refine((value) => value === props.server.displayName, {
                message:
                    "Server name does not match. Please enter the correct server name to confirm.",
            }),
    });

    const form = useForm<z.infer<typeof resetFormSchema>>({
        resolver: zodResolver(resetFormSchema),
        defaultValues: {
            friendlyName: "",
        },
    });

    useEffect(() => {
        form.reset({ friendlyName: "" });
    }, [props.server, form]);

    function onSubmit() {
        setLoading(true);
        resetSettings(props.server.id)
            .then(async () => {
                await queryClient.invalidateQueries({
                    queryKey: queryKeys.settingProfiles.detail(
                        props.profile.teamId,
                        props.profile.id,
                    ),
                });
                toast.success("Settings have been reset successfully!");
                closeDialog();
            })
            .finally(() => setLoading(false));
    }

    return (
        <Form {...form}>
            <DialogHeader>
                <DialogTitle>
                    Reset settings for {props.server.displayName}?
                </DialogTitle>
                <DialogDescription>
                    This action cannot be undone. Please enter the name of the
                    server to confirm.
                </DialogDescription>
            </DialogHeader>
            <FormInput
                form={form}
                label="Server Name"
                name="friendlyName"
                required
            />
            <DialogFooter>
                <DialogClose asChild>
                    <Button disabled={isLoading} variant="tertiary">
                        Cancel
                    </Button>
                </DialogClose>
                <Button
                    disabled={isLoading}
                    onClick={form.handleSubmit(onSubmit)}
                    type="submit"
                    variant="destructive">
                    {isLoading ? <Spinner /> : <>Reset Settings</>}
                </Button>
            </DialogFooter>
        </Form>
    );
}

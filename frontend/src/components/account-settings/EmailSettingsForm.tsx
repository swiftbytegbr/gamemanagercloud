import { ManagedForm } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { changeEmail } from "@/api/userApi.ts";
import { closeDialog } from "@/store/dialogStore.tsx";
import { useRouter } from "@tanstack/react-router";
import { toast } from "sonner";
import { useState } from "react";
import { z } from "zod";
import { changeEmailSchema } from "@/components/account-settings/schemas.ts";

export function EmailSettingsForm() {
    const [loading, setLoading] = useState(false);
    const router = useRouter();

    const form = useForm<z.infer<typeof changeEmailSchema>>({
        resolver: zodResolver(changeEmailSchema),
        defaultValues: {
            email: "",
            emailRepeat: "",
        },
    });

    function onSubmit(data: z.infer<typeof changeEmailSchema>) {
        setLoading(true);
        changeEmail(data.email)
            .then(() => {
                closeDialog();
                router.navigate({ to: "/logout" });
                toast.success(
                    "Email changed successfully. Please log in again.",
                );
            })
            .finally(() => setLoading(false));
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <div>
                <h3>E-Mail</h3>
                <div className="flex flex-col gap-4 mt-2">
                    <FormInput
                        form={form}
                        label="Change E-Mail"
                        name="email"
                        required
                    />
                    <FormInput
                        form={form}
                        label="Repeat E-Mail"
                        name="emailRepeat"
                        required
                    />
                    <Button disabled={loading} type="submit">
                        {loading ? <Spinner /> : <>Change Email</>}
                    </Button>
                </div>
            </div>
        </ManagedForm>
    );
}

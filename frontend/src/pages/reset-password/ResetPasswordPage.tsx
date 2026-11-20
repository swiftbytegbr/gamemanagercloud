import { AuthGuard } from "@/controller/AuthGuard.tsx";
import { LoginLayout } from "@/layouts/LoginLayout.tsx";
import { useState } from "react";
import { requestPasswordReset, resetPassword } from "@/api/authApi.ts";
import { toast } from "sonner";
import { ResetPasswordFormOne } from "@/components/reset-password/ResetPasswordFormOne.tsx";
import { ResetPasswordFormTwo } from "@/components/reset-password/ResetPasswordFormTwo.tsx";
import { router } from "@/Router.tsx";

export function ResetPasswordPage() {
    const [isLoading, setLoading] = useState(false);
    const [state, setState] = useState<"1" | "2">("1");
    const [email, setEmail] = useState<string>("");

    function onSubmitOne(email: string) {
        setLoading(true);
        requestPasswordReset({ email: email })
            .then(() => {
                setEmail(email);
                setState("2");
                toast.success("Check your email for a password reset link!");
            })
            .catch((error) => {
                if (error.response && error.response.status === 404) {
                    toast.error("No user found with that email address!");
                }
            })
            .finally(() => setLoading(false));
    }

    function onSubmitTwo(data: {
        verificationToken: string;
        newPassword: string;
    }) {
        setLoading(true);
        resetPassword({
            email: email,
            verificationCode: data.verificationToken,
            newPassword: data.newPassword,
        })
            .then(() => {
                toast.success(
                    "Your password has been reset successfully! You can now log in with your new password.",
                );
                router.navigate({ to: "/login" });
            })
            .finally(() => setLoading(false));
    }

    return (
        <AuthGuard guard="unauthenticated">
            <LoginLayout>
                {state === "1" ? (
                    <ResetPasswordFormOne
                        onSubmit={onSubmitOne}
                        isLoading={isLoading}
                    />
                ) : (
                    <ResetPasswordFormTwo
                        onSubmit={onSubmitTwo}
                        isLoading={isLoading}
                    />
                )}
            </LoginLayout>
        </AuthGuard>
    );
}

import { ManagedForm } from "@/components/ui/form.tsx";
import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { SendHorizonal } from "lucide-react";
import { type User } from "@/store/userStore.ts";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { verifyEmail, verifyEmailResend } from "@/api/authApi.ts";
import { toast } from "sonner";
import { updateUserCache } from "@/store/userStore.ts";
import { popDialog } from "@/store/dialogStore.tsx";
import { useRouter } from "@tanstack/react-router";

const verifyEmailSchema = z.object({
    verificationCode: z
        .string({
            message: "Token is required",
        })
        .min(1, {
            message: "Token is required",
        })
        .min(6, {
            message: "Token must be 6 characters long",
        })
        .max(6, {
            message: "Token must be 6 characters long",
        })
        .regex(/^\d+$/, {
            message: "Must be a number",
        }),
});

type VerifyEmailFormProps = {
    user: User;
};

export function VerifyEmailForm(props: VerifyEmailFormProps) {
    const [isButtonDisabled, setIsButtonDisabled] = useState(false);
    const [isLoading, setLoading] = useState(false);
    const router = useRouter();

    const form = useForm<z.infer<typeof verifyEmailSchema>>({
        resolver: zodResolver(verifyEmailSchema),
        defaultValues: {
            verificationCode: "",
        },
    });

    function onSubmit(data: z.infer<typeof verifyEmailSchema>) {
        setLoading(true);
        verifyEmail(Number(data.verificationCode))
            .then(() => {
                toast.success("Your email has been verified successfully.");
                updateUserCache((current) =>
                    current
                        ? {
                              ...current,
                              emailVerified: true,
                          }
                        : current,
                );
                popDialog();
            })
            .finally(() => setLoading(false));
    }

    const handleResendCode = () => {
        setIsButtonDisabled(true);
        verifyEmailResend().then(() => {
            toast.success(
                "Your verification code has been resent to your email.",
            );
        });
        setTimeout(() => {
            setIsButtonDisabled(false);
        }, 30000);
    };

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Verify your email!</DialogTitle>
                <DialogDescription>
                    Please enter the verification code you received by e-mail.
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col space-y-5">
                <div>
                    <p>Your E-Mail address</p>
                    <small className="mb-5">{props.user?.email}</small>
                </div>
                <div>
                    <FormInput
                        form={form}
                        name="verificationCode"
                        label="Verification code"
                        required
                    />
                    <Button
                        disabled={isButtonDisabled}
                        onClick={handleResendCode}
                        variant="ghost"
                        size="sm"
                        type="button">
                        {isButtonDisabled ? (
                            <Spinner size="small" />
                        ) : (
                            <SendHorizonal />
                        )}{" "}
                        Resend verification code
                    </Button>
                </div>
            </div>
            <DialogFooter>
                <div className="w-full flex gap-3 justify-center">
                    <Button
                        disabled={isLoading}
                        type="submit"
                        size="lg"
                        className="uppercase w-fit">
                        {isLoading ? (
                            <Spinner size="small" />
                        ) : (
                            "Confirm verification code"
                        )}
                    </Button>
                    <Button
                        disabled={isLoading}
                        type="button"
                        size="lg"
                        variant="tertiary"
                        onClick={() => router.navigate({ to: "/logout" })}
                        className="uppercase w-fit">
                        {isLoading ? (
                            <Spinner size="small" />
                        ) : (
                            "Logout instead"
                        )}
                    </Button>
                </div>
            </DialogFooter>
        </ManagedForm>
    );
}

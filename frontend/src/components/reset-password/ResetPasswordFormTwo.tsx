import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormFooterLinks } from "@/components/custom/form/FormFooterLinks.tsx";

const passwordResetSchemaTwo = z.object({
    verificationToken: z
        .string({
            message: "Token is required",
        })
        .min(1, {
            message: "Token is required",
        })
        .min(8, {
            message: "Token must be 8 characters long",
        })
        .max(8, {
            message: "Token must be 8 characters long",
        }),
    password: z
        .string({
            message: "Password is required",
        })
        .min(8, {
            message: "Password must be at least 8 characters long",
        })
        .regex(/[A-Z]/, {
            message: "Password must contain at least one uppercase letter",
        })
        .regex(/[a-z]/, {
            message: "Password must contain at least one lowercase letter",
        })
        .regex(/[0-9!@#$%^&*(),.?":{}|<>]/, {
            message:
                "Password must contain at least one number or special character",
        }),
});

interface ResetPasswordFormTwoProps {
    isLoading?: boolean;
    onSubmit: (data: {
        verificationToken: string;
        newPassword: string;
    }) => void;
}

export function ResetPasswordFormTwo(props: ResetPasswordFormTwoProps) {
    const form = useForm<z.infer<typeof passwordResetSchemaTwo>>({
        resolver: zodResolver(passwordResetSchemaTwo),
        defaultValues: {
            verificationToken: "",
            password: "",
        },
    });

    function onSubmit(data: z.infer<typeof passwordResetSchemaTwo>) {
        props.onSubmit({
            verificationToken: data.verificationToken,
            newPassword: data.password,
        });
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <Card className="lg:w-[400px] bg-secondary">
                <CardHeader>
                    <CardTitle className="text-3xl uppercase">
                        Reset Password
                    </CardTitle>
                    <CardDescription>
                        Enter the verification token you received via email and
                        change your password.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="grid w-full items-center gap-4">
                        <div className="flex flex-col space-y-5">
                            <FormInput
                                form={form}
                                name="verificationToken"
                                label="Token"
                                required
                            />
                            <FormInput
                                form={form}
                                name="password"
                                label="New Password"
                                required
                                autocomplete="new-password"
                                type="password"
                            />
                        </div>
                    </div>
                </CardContent>
                <CardFooter>
                    <div className="w-full">
                        <Button
                            disabled={props.isLoading}
                            className="w-full uppercase"
                            type="submit">
                            {props.isLoading ? <Spinner /> : "Reset Password"}
                        </Button>
                        <FormFooterLinks />
                    </div>
                </CardFooter>
            </Card>
        </ManagedForm>
    );
}

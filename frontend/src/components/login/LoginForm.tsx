import {
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
    ManagedForm,
} from "@/components/ui/form.tsx";
import { Card, CardContent } from "@/components/ui/card.tsx";
import { Link } from "@tanstack/react-router";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { PasswordInput } from "@/components/ui/password-input.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { LoginLayout } from "@/layouts/LoginLayout.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { LoginFormFooter } from "@/components/login/LoginFormFooter.tsx";
import { LoginFormHeader } from "@/components/login/LoginFormHeader.tsx";

const loginFormSchema = z.object({
    email: z
        .email({
            message: "A valid email is required",
        })
        .min(1, {
            message: "Email is required",
        }),
    password: z
        .string({
            message: "Password is required",
        })
        .min(1, {
            message: "Password is required",
        }),
    rememberMe: z.boolean(),
});

type LoginFormProps = {
    isLoading?: boolean;
    onSubmit: (data: {
        email: string;
        password: string;
        rememberMe: boolean;
    }) => void;
};

export function LoginForm(props: LoginFormProps) {
    const form = useForm<z.infer<typeof loginFormSchema>>({
        resolver: zodResolver(loginFormSchema),
        defaultValues: {
            rememberMe: false,
            email: "",
            password: "",
        },
    });

    return (
        <LoginLayout>
            <ManagedForm form={form} onSubmit={props.onSubmit}>
                <Card className="lg:w-[400px] bg-secondary">
                    <LoginFormHeader />
                    <CardContent>
                        <div className="grid w-full items-center gap-4">
                            <div className="flex flex-col space-y-5">
                                <FormInput
                                    form={form}
                                    name="email"
                                    label="E-Mail"
                                    autocomplete="email username"
                                    required
                                />
                                <FormField
                                    control={form.control}
                                    name="password"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Password *</FormLabel>
                                            <FormControl>
                                                <PasswordInput
                                                    placeholder="Password"
                                                    autoComplete="current-password"
                                                    {...field}
                                                />
                                            </FormControl>
                                            <div className="flex">
                                                <FormMessage />
                                                <FormDescription className="ml-auto self-center">
                                                    <Link to="/reset-password">
                                                        <small>
                                                            Forgot password?
                                                        </small>
                                                    </Link>
                                                </FormDescription>
                                            </div>
                                        </FormItem>
                                    )}
                                />
                                <FormCheckbox
                                    form={form}
                                    name="rememberMe"
                                    label="Remember me for 30 days"
                                />
                            </div>
                        </div>
                    </CardContent>
                    <LoginFormFooter isLoading={props.isLoading} />
                </Card>
            </ManagedForm>
        </LoginLayout>
    );
}

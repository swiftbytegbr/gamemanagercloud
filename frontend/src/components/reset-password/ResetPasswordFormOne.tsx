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

const passwordResetSchemaOne = z.object({
    email: z
        .email({
            message: "A valid email address is required",
        })
        .min(1, {
            message: "Email is required",
        }),
});

interface ResetPasswordFormOneProps {
    isLoading?: boolean;
    onSubmit: (email: string) => void;
}

export function ResetPasswordFormOne(props: ResetPasswordFormOneProps) {
    const form = useForm<z.infer<typeof passwordResetSchemaOne>>({
        resolver: zodResolver(passwordResetSchemaOne),
        defaultValues: {
            email: "",
        },
    });

    function onSubmit(data: z.infer<typeof passwordResetSchemaOne>) {
        props.onSubmit(data.email);
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <Card className="lg:w-[400px] bg-secondary">
                <CardHeader>
                    <CardTitle className="text-3xl uppercase">
                        Reset Password
                    </CardTitle>
                    <CardDescription>
                        Please enter the e-mail address of your GMC account!
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="grid w-full items-center gap-4">
                        <div className="flex flex-col space-y-5">
                            <FormInput
                                form={form}
                                name="email"
                                label="E-Mail"
                                required
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

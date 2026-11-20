import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { SignupFormTemplate } from "@/components/signup/SignupFormTemplate.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";

const signupFormSchemaOne = z.object({
    username: z
        .string({
            message: "Username is required",
        })
        .min(3, {
            message: "Username must be at least 3 characters long",
        })
        .max(15, {
            message: "Username must be at most 15 characters long",
        }),
    email: z.email({
        message: "A valid email address is required",
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

interface SignupFirstSectionProps {
    onSubmit: (data: {
        username: string;
        email: string;
        password: string;
    }) => void;
}

export function SignupFormOne(props: SignupFirstSectionProps) {
    const form = useForm<z.infer<typeof signupFormSchemaOne>>({
        resolver: zodResolver(signupFormSchemaOne),
        defaultValues: {
            username: "",
            email: "",
            password: "",
        },
    });

    async function onSubmit(data: z.infer<typeof signupFormSchemaOne>) {
        props.onSubmit(data);
    }

    return (
        <SignupFormTemplate form={form} onSubmit={onSubmit}>
            <FormInput form={form} name="username" label="Username" required />
            <FormInput
                form={form}
                name="email"
                label="E-Mail"
                required
                autocomplete="email username"
            />
            <FormInput
                form={form}
                name="password"
                label="Password"
                type="password"
                autocomplete="new-password"
                required
            />
        </SignupFormTemplate>
    );
}

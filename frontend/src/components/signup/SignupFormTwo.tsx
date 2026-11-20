import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { SignupFormTemplate } from "@/components/signup/SignupFormTemplate.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { FormCountrySelect } from "@/components/custom/form/FormCountrySelect.tsx";

const signupFormSchemaTwo = z
    .object({
        firstName: z
            .string({
                message: "First name is required",
            })
            .min(1, {
                message: "First name is required",
            }),
        lastName: z
            .string({
                message: "Last name is required",
            })
            .min(1, {
                message: "Last name is required",
            }),
        country: z
            .string({
                message: "Country is required",
            })
            .optional(),
        postalCode: z
            .string({
                message: "Postal code is required",
            })
            .regex(/^\d*$/, {
                message: "Must be a number",
            })
            .optional(),
        city: z
            .string({
                message: "City is required",
            })
            .optional(),
        region: z
            .string({
                message: "Region is required",
            })
            .optional(),
        street: z
            .string({
                message: "Street is required",
            })
            .optional(),
        company: z.boolean(),
        companyName: z.string().optional(),
        vatNumber: z.string().optional(),
    })
    .superRefine((data, ctx) => {
        const fields = [
            "country",
            "postalCode",
            "city",
            "region",
            "street",
        ] as const;
        const isEmpty = (value?: string | null) => !value || value.length === 0;

        if (data.company) {
            const missing = fields.filter((field) => isEmpty(data[field]));
            if (missing.length > 0) {
                for (const field of fields) {
                    ctx.addIssue({
                        code: "custom",
                        message: "Billing address is required for companies",
                        path: [field],
                    });
                }
            }
            if (!data.companyName || data.companyName.length === 0) {
                ctx.addIssue({
                    code: "custom",
                    message: "Company name is required",
                    path: ["companyName"],
                });
            }
            if (!data.vatNumber || data.vatNumber.length === 0) {
                ctx.addIssue({
                    code: "custom",
                    message: "VAT number is required",
                    path: ["vatNumber"],
                });
            }
        }

        const anySet = fields.some((field) => !isEmpty(data[field]));
        const allSet = fields.every((field) => !isEmpty(data[field]));
        if (anySet && !allSet) {
            for (const field of fields) {
                ctx.addIssue({
                    code: "custom",
                    message:
                        "Please fill out all billing address fields or leave them all empty.",
                    path: [field],
                });
            }
        }
    });

interface SignupSecondSectionProps {
    onSubmit: (data: {
        firstName: string;
        lastName: string;
        country?: string;
        postalCode?: string;
        city?: string;
        region?: string;
        street?: string;
        company: boolean;
        companyName?: string;
        vatNumber?: string;
    }) => void;
    isLoading?: boolean;
}

export function SignupFormTwo(props: SignupSecondSectionProps) {
    const form = useForm<z.infer<typeof signupFormSchemaTwo>>({
        resolver: zodResolver(signupFormSchemaTwo),
        defaultValues: {
            firstName: "",
            lastName: "",
            country: "",
            postalCode: "",
            city: "",
            region: "",
            street: "",
            company: false,
            companyName: "",
            vatNumber: "",
        },
    });

    async function onSubmit(data: z.infer<typeof signupFormSchemaTwo>) {
        props.onSubmit(data);
    }

    const isCompany = form.watch("company");

    return (
        <SignupFormTemplate
            form={form}
            onSubmit={onSubmit}
            submitButtonText="Create Account"
            isLoading={props.isLoading}>
            <p className="bg-warning-foreground/60 rounded-2xl p-2">
                In the Early Access phase, billing information is{" "}
                <strong>optional</strong>. If you leave them out here, you may
                have to add them later.
            </p>
            <div className="flex gap-4">
                <FormInput
                    form={form}
                    name="firstName"
                    label="First name"
                    required
                    disableError
                />
                <FormInput
                    form={form}
                    name="lastName"
                    label="Last name"
                    required
                    disableError
                />
            </div>
            <FormCountrySelect
                form={form}
                name="country"
                label="Country"
                disableError
            />
            <div className="flex gap-4">
                <FormInput
                    form={form}
                    name="postalCode"
                    label="Postal code"
                    type="text"
                    inputMode="numeric"
                    onlyDigits
                    disableError
                />
                <FormInput form={form} name="city" label="City" disableError />
            </div>
            <FormInput form={form} name="region" label="Region" disableError />
            <FormInput form={form} name="street" label="Street" disableError />
            <FormCheckbox
                form={form}
                name="company"
                label="Register as a company"
            />
            {isCompany && (
                <>
                    <FormInput
                        form={form}
                        name="companyName"
                        label="Company name"
                        required
                        disableError
                    />
                    <FormInput
                        form={form}
                        name="vatNumber"
                        label="VAT number"
                        required
                        disableError
                    />
                </>
            )}
        </SignupFormTemplate>
    );
}

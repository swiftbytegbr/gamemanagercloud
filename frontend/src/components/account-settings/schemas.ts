import { z } from "zod";

export const changeEmailSchema = z
    .object({
        email: z.email({ message: "Please enter a valid email" }),
        emailRepeat: z.string({ message: "Please repeat your email" }),
    })
    .refine((data) => data.email === data.emailRepeat, {
        message: "Emails do not match",
        path: ["emailRepeat"],
    });

export const changeUserInfoSchema = z
    .object({
        username: z
            .string({ message: "Username is required" })
            .min(3, { message: "Username must be at least 3 characters long" })
            .max(15, {
                message: "Username must be at most 15 characters long",
            }),
        legalName: z
            .string({ message: "Legal name is required" })
            .min(1, { message: "Legal name is required" }),
        country: z.string({ message: "Country is required" }).optional(),
        postalCode: z
            .string({ message: "Postal code is required" })
            .regex(/^\d*$/, { message: "Must be a number" })
            .optional(),
        city: z.string({ message: "City is required" }).optional(),
        region: z.string({ message: "Region is required" }).optional(),
        street: z.string({ message: "Street is required" }).optional(),
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

export const changeNotificationsSchema = z.object({
    emailOnLogin: z.boolean().optional(),
});

const passwordSchema = z
    .string({ message: "Password is required" })
    .min(8, { message: "Password must be at least 8 characters long" })
    .regex(/[A-Z]/, {
        message: "Password must contain at least one uppercase letter",
    })
    .regex(/[a-z]/, {
        message: "Password must contain at least one lowercase letter",
    })
    .regex(/[0-9!@#$%^&*(),.?":{}|<>]/, {
        message:
            "Password must contain at least one number or special character",
    });

export const changePasswordSchema = z
    .object({
        password: passwordSchema,
        newPassword: passwordSchema,
        passwordRepeat: z.string({
            message: "Please repeat your password",
        }),
    })
    .refine((data) => data.newPassword === data.passwordRepeat, {
        message: "Passwords do not match",
        path: ["passwordRepeat"],
    });

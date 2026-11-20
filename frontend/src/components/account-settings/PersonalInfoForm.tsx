import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ManagedForm } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { FormCountrySelect } from "@/components/custom/form/FormCountrySelect.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { changeCompanyInfo, changePersonalInfo } from "@/api/userApi.ts";
import { updateUserCache } from "@/store/userStore.ts";
import { toast } from "sonner";
import { z } from "zod";
import type { PersonalInformation, User } from "@/store/userStore.ts";
import { changeUserInfoSchema } from "@/components/account-settings/schemas.ts";

export function PersonalInfoForm({ user }: { user?: User }) {
    const [loading, setLoading] = useState(false);

    const form = useForm<z.infer<typeof changeUserInfoSchema>>({
        resolver: zodResolver(changeUserInfoSchema),
        defaultValues: {
            username: user?.personalInformation.username || "",
            legalName: user?.personalInformation.name,
            country:
                user?.personalInformation?.billingAddress?.countryCode || "",
            postalCode:
                user?.personalInformation.billingAddress?.postalCode || "",
            city: user?.personalInformation.billingAddress?.city || "",
            region: user?.personalInformation.billingAddress?.region || "",
            street: user?.personalInformation.billingAddress?.street || "",
            company: !!user?.companyInformation,
            companyName: user?.companyInformation?.companyName || "",
            vatNumber: user?.companyInformation?.vatNumber || "",
        },
    });

    const isCompany = form.watch("company");

    async function onSubmit(data: z.infer<typeof changeUserInfoSchema>) {
        setLoading(true);

        const billingFields = [
            data.country,
            data.postalCode,
            data.city,
            data.region,
            data.street,
        ];
        const allBillingSet = billingFields.every((v) => v && v.length > 0);

        const personalInformation: PersonalInformation = {
            name: data.legalName,
            username: data.username,
            ...(allBillingSet
                ? {
                      billingAddress: {
                          countryCode: data.country!,
                          postalCode: data.postalCode!,
                          city: data.city!,
                          region: data.region!,
                          street: data.street!,
                      },
                  }
                : {}),
        };
        try {
            if (data.company) {
                await changeCompanyInfo({
                    companyInformation: {
                        companyName: data.companyName!,
                        countryCode: data.country!,
                        vatNumber: data.vatNumber!,
                    },
                    billingAddress: {
                        countryCode: data.country!,
                        postalCode: data.postalCode!,
                        city: data.city!,
                        region: data.region!,
                        street: data.street!,
                    },
                });
            }

            changePersonalInfo(personalInformation)
                .then(() => {
                    toast.success("Personal information updated successfully.");
                    updateUserCache((current) => {
                        if (!current) return current;
                        return {
                            ...current,
                            personalInformation,
                            companyInformation: data.company
                                ? {
                                      companyName: data.companyName!,
                                      countryCode: data.country!,
                                      vatNumber: data.vatNumber!,
                                  }
                                : current.companyInformation,
                        };
                    });
                })
                .finally(() => setLoading(false));
        } catch {
            setLoading(false);
        }
    }

    const firstError = Object.values(form.formState.errors)[0]?.message;
    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <div>
                <h3>Personal Information</h3>
                <div className="flex flex-col gap-4 mt-2">
                    <FormInput
                        form={form}
                        label="Username"
                        name="username"
                        required
                        disableError
                    />
                    <FormInput
                        form={form}
                        name="legalName"
                        label="Full Name"
                        required
                        disableError
                    />
                    <FormCountrySelect
                        form={form}
                        name="country"
                        label="Country"
                        disableError
                    />
                    <div className="flex gap-4">
                        <FormInput
                            className="w-full"
                            form={form}
                            name="postalCode"
                            label="Postal code"
                            type="text"
                            inputMode="numeric"
                            onlyDigits
                            disableError
                        />
                        <FormInput
                            className="w-full"
                            form={form}
                            name="city"
                            label="City"
                            disableError
                        />
                    </div>
                    <FormInput
                        form={form}
                        name="region"
                        label="Region"
                        disableError
                    />
                    <FormInput
                        form={form}
                        name="street"
                        label="Street"
                        disableError
                    />
                    {!user?.companyInformation && (
                        <FormCheckbox
                            form={form}
                            name="company"
                            label="Register as a company"
                        />
                    )}
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
                    <p className="text-center text-red-400">{firstError}</p>
                    <Button disabled={loading} type="submit">
                        {loading ? <Spinner /> : <>Change Information</>}
                    </Button>
                </div>
            </div>
        </ManagedForm>
    );
}

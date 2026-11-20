import { LoginLayout } from "@/layouts/LoginLayout.tsx";
import { SignupFormOne } from "@/components/signup/SignupFormOne.tsx";
import { useState } from "react";
import { SignupFormTwo } from "@/components/signup/SignupFormTwo.tsx";
import { register } from "@/api/authApi.ts";
import { router } from "@/Router.tsx";
import { toast } from "sonner";
import { useAuthStore } from "@/store/authStore.ts";
import { AuthGuard } from "@/controller/AuthGuard.tsx";

export function SignupPage() {
    const markAuthenticated = useAuthStore((state) => state.login);

    const [isLoading, setLoading] = useState(false);
    const [firstSectionData, setFirstSectionData] = useState<
        { username: string; email: string; password: string } | undefined
    >();

    const handleSubmitOne = async (data: {
        username: string;
        email: string;
        password: string;
    }) => {
        setFirstSectionData(data);
    };

    const handleSubmitTwo = async (data: {
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
    }) => {
        setLoading(true);
        if (!firstSectionData) return;

        const billingFields = [
            data.country,
            data.postalCode,
            data.city,
            data.region,
            data.street,
        ];
        const allBillingSet = billingFields.every((v) => v && v.length > 0);

        const registerData = {
            email: firstSectionData.email,
            password: firstSectionData.password,
            username: firstSectionData.username,
            name: `${data.firstName} ${data.lastName}`.trim(),
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
            ...(data.company
                ? {
                      companyInformation: {
                          companyName: data.companyName!,
                          countryCode: data.country!,
                          vatNumber: data.vatNumber!,
                      },
                  }
                : {}),
        } as const;

        register(registerData)
            .then(() => {
                markAuthenticated();

                router.navigate({ to: "/dashboard" });
                toast.success("Registration successful");
            })
            .finally(() => setLoading(false));
    };

    return (
        <AuthGuard guard="unauthenticated">
            <LoginLayout>
                {!firstSectionData && (
                    <SignupFormOne onSubmit={handleSubmitOne} />
                )}
                {firstSectionData && (
                    <SignupFormTwo
                        onSubmit={handleSubmitTwo}
                        isLoading={isLoading}
                    />
                )}
            </LoginLayout>
        </AuthGuard>
    );
}

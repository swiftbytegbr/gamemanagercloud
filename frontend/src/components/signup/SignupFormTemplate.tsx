import { Card, CardContent } from "@/components/ui/card.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import type { ReactNode } from "react";
import type { FieldValues, UseFormReturn } from "react-hook-form";
import { SignupFormFooter } from "@/components/signup/SignupFormFooter.tsx";
import { SignupFormHeader } from "@/components/signup/SignupFormHeader.tsx";

interface SignupFormTemplateProps<TFormValues extends FieldValues> {
    children: ReactNode;
    form: UseFormReturn<TFormValues>;
    isLoading?: boolean;
    onSubmit: (data: TFormValues) => void;
    submitButtonText?: string;
}

export function SignupFormTemplate<TFormValues extends FieldValues>(
    props: SignupFormTemplateProps<TFormValues>,
) {
    return (
        <ManagedForm form={props.form} onSubmit={props.onSubmit}>
            <Card className="lg:w-[400px] bg-secondary">
                <SignupFormHeader />
                <CardContent>
                    <div className="grid w-full items-center gap-4">
                        <div className="flex flex-col space-y-5">
                            {props.children}
                        </div>
                    </div>
                </CardContent>
                <SignupFormFooter
                    submitButtonText={
                        props.submitButtonText
                            ? props.submitButtonText
                            : "Continue"
                    }
                    isLoading={props.isLoading}
                />
            </Card>
        </ManagedForm>
    );
}

import {
    FormControl,
    FormField,
    FormItem,
    FormMessage,
} from "@/components/ui/form.tsx";
import type {
    ControllerRenderProps,
    FieldValues,
    Path,
    UseFormReturn,
} from "react-hook-form";
import { cn } from "@/lib/utils.ts";
import { Textarea } from "@/components/ui/textarea.tsx";
import { FormLabelWithDescription } from "@/components/custom/form/FormLabelWithDescription";
import { parseFieldKey } from "@/lib/settingFieldKey.ts";

interface FormTextAreaProps<TFormValues extends FieldValues> {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    variant?: "default" | "cutOut";
    height?: "small" | "medium" | "large" | "auto";
    className?: string;
    label: string;
    placeholder?: string;
    description?: string;
    required?: boolean;
    disableError?: boolean;
    disabled?: boolean;
    resize?: boolean;
    isListInput?: boolean;
}

export function FormTextArea<TFormValues extends FieldValues>({
    required = false,
    disableError = false,
    ...props
}: FormTextAreaProps<TFormValues>) {
    const setValue = (
        value: string,
        field: ControllerRenderProps<TFormValues>,
    ) => {
        const { key } = parseFieldKey(field.name);
        if (!value) {
            field.onChange("");
            return;
        }
        if (!key) {
            field.onChange(value);
            return;
        }
        field.onChange(value.replaceAll(`${key}=`, ""));
    };

    return (
        <FormField
            disabled={props.disabled}
            control={props.form.control}
            name={props.name}
            render={({ field }) => (
                <FormItem className={cn("h-fit min-w-0", props.className)}>
                    <FormLabelWithDescription
                        label={props.label}
                        description={props.description}
                        required={required}
                    />
                    <FormControl>
                        <Textarea
                            placeholder={
                                props.placeholder
                                    ? props.placeholder
                                    : props.label
                            }
                            wrap="off"
                            height={props.height}
                            resize={props.resize}
                            variant={props.variant}
                            value={field.value ?? ""}
                            onChange={(event) =>
                                props.isListInput
                                    ? setValue(event.target.value, field)
                                    : field.onChange(event.target.value)
                            }
                        />
                    </FormControl>
                    {!disableError && <FormMessage />}
                </FormItem>
            )}
        />
    );
}

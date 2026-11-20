import {
    FormControl,
    FormField,
    FormItem,
    FormMessage,
} from "@/components/ui/form.tsx";
import { Checkbox } from "@/components/ui/checkbox.tsx";
import type { FieldValues, Path, UseFormReturn } from "react-hook-form";
import { cn } from "@/lib/utils.ts";
import { FormLabelWithDescription } from "@/components/custom/form/FormLabelWithDescription";

interface FormCheckboxProps<TFormValues extends FieldValues> {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    className?: string;
    label: string;
    description?: string;
    disableError?: boolean;
    reverseLabel?: boolean;
    disabled?: boolean;
}

export function FormCheckbox<TFormValues extends FieldValues>(
    props: FormCheckboxProps<TFormValues>,
) {
    return (
        <FormField
            control={props.form.control}
            name={props.name}
            render={({ field }) => (
                <FormItem className={cn("h-fit", props.className)}>
                    <div className="flex items-center h-fit gap-3">
                        {props.reverseLabel && (
                            <FormLabelWithDescription
                                label={props.label}
                                description={props.description}
                            />
                        )}
                        <FormControl
                            className={props.reverseLabel ? "ml-auto" : ""}>
                            <Checkbox
                                disabled={props.disabled}
                                {...field}
                                checked={Boolean(field.value)}
                                onCheckedChange={field.onChange}
                            />
                        </FormControl>
                        {!props.reverseLabel && (
                            <FormLabelWithDescription
                                label={props.label}
                                description={props.description}
                            />
                        )}
                    </div>
                    <FormMessage />
                </FormItem>
            )}
        />
    );
}

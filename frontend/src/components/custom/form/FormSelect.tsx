import {
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form.tsx";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.tsx";
import type { FieldValues, Path, UseFormReturn } from "react-hook-form";

type FormInputProps<TFormValues extends FieldValues> = {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    items?: { value: string; label: string }[];
    label: string;
    emptyText?: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    disableError?: boolean;
};

export function FormSelect<TFormValues extends FieldValues>({
    items = [],
    emptyText = "No options available",
    ...props
}: FormInputProps<TFormValues>) {
    return (
        <FormField
            control={props.form.control}
            name={props.name}
            render={({ field }) => (
                <FormItem>
                    <FormLabel className="pointer-events-none">
                        {props.label} {props.required ? " *" : ""}
                    </FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                        <FormControl className="w-full">
                            <SelectTrigger>
                                <SelectValue
                                    placeholder={
                                        props.placeholder
                                            ? props.placeholder
                                            : props.label
                                    }
                                />
                            </SelectTrigger>
                        </FormControl>
                        <SelectContent className="max-h-[250px]">
                            {items.length > 0 ? (
                                items.map((item) => (
                                    <SelectItem
                                        currentValue={field.value}
                                        key={item.value}
                                        value={item.value}>
                                        {item.label}
                                    </SelectItem>
                                ))
                            ) : (
                                <SelectItem value="-" disabled>
                                    {emptyText}
                                </SelectItem>
                            )}
                        </SelectContent>
                        {!props.disableError && <FormMessage />}
                    </Select>
                </FormItem>
            )}
        />
    );
}

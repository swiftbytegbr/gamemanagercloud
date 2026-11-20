import {
    FormControl,
    FormField,
    FormItem,
    FormMessage,
} from "@/components/ui/form.tsx";
import { PasswordInput } from "@/components/ui/password-input.tsx";
import { Button } from "@/components/ui/button.tsx";
import type {
    ControllerRenderProps,
    FieldValues,
    Path,
    UseFormReturn,
} from "react-hook-form";
import { Input } from "@/components/ui/input.tsx";
import { cn } from "@/lib/utils.ts";
import * as React from "react";
import { FormLabelWithDescription } from "@/components/custom/form/FormLabelWithDescription";

interface FormInputProps<TFormValues extends FieldValues> {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    className?: string;
    label: string;
    description?: string;
    autocomplete?:
        "off" | "email username" | "current-password" | "new-password";
    placeholder?: string;
    type?: "text" | "number" | "password" | "integer" | "file";
    inputMode?:
        | "none"
        | "text"
        | "tel"
        | "url"
        | "email"
        | "numeric"
        | "decimal"
        | "search";
    pattern?: string;
    onlyDigits?: boolean;
    accept?: string;
    required?: boolean;
    disableError?: boolean;
    disabled?: boolean;
}

export function FormInput<TFormValues extends FieldValues>({
    type = "text",
    required = false,
    disableError = false,
    autocomplete = "off",
    ...props
}: FormInputProps<TFormValues>) {
    const inputRef = React.useRef<HTMLInputElement | null>(null);
    const setValue = (
        value: string,
        field: ControllerRenderProps<TFormValues>,
    ) => {
        const onlyDigits = props.onlyDigits;
        const raw = value ?? "";
        const processed = onlyDigits ? raw.replace(/\D+/g, "") : raw;
        if (!value || value === "") {
            if (type === "integer" || type === "number") {
                field.onChange(null as unknown as TFormValues);
            } else {
                field.onChange("");
            }
        } else if (type === "integer" && Number.isInteger(Number(processed))) {
            field.onChange(processed);
        } else if (type === "number" && !isNaN(Number(processed))) {
            field.onChange(processed);
        } else if (type === "password" || type === "text") {
            field.onChange(processed);
        }
    };

    return (
        <FormField
            disabled={props.disabled}
            control={props.form.control}
            name={props.name}
            render={({ field }) => (
                <FormItem className={cn("h-full min-w-0", props.className)}>
                    <FormLabelWithDescription
                        label={props.label}
                        description={props.description}
                        required={required}
                        className="grow"
                    />
                    {type === "password" ? (
                        <FormControl>
                            <PasswordInput
                                placeholder={
                                    props.placeholder
                                        ? props.placeholder
                                        : props.label
                                }
                                {...field}
                                value={field.value ?? ""}
                            />
                        </FormControl>
                    ) : type === "file" ? (
                        <>
                            <FormControl>
                                <Input
                                    accept={props.accept}
                                    type="file"
                                    autoComplete={autocomplete}
                                    ref={(el) => {
                                        inputRef.current = el;
                                        field.ref(el);
                                    }}
                                    name={field.name}
                                    onBlur={field.onBlur}
                                    onChange={(e) =>
                                        field.onChange(e.target.files)
                                    }
                                    required={required}
                                    className="sr-only"
                                />
                            </FormControl>
                            <div className="flex flex-col mt-1 items-center gap-3">
                                <Button
                                    type="button"
                                    variant="input"
                                    disabled={props.disabled}
                                    className="w-full"
                                    onClick={() => inputRef.current?.click()}>
                                    Select file
                                </Button>
                                <span className="text-xs text-muted-foreground truncate max-w-[60%]">
                                    {(() => {
                                        const files = field.value as
                                            FileList | undefined;
                                        if (!files || files.length === 0)
                                            return "No file selected";
                                        return files.length === 1
                                            ? files[0].name
                                            : `${files.length} files selected`;
                                    })()}
                                </span>
                            </div>
                        </>
                    ) : (
                        <FormControl>
                            <Input
                                placeholder={
                                    props.placeholder
                                        ? props.placeholder
                                        : props.label
                                }
                                autoComplete={autocomplete}
                                inputMode={props.inputMode}
                                pattern={props.pattern}
                                {...field}
                                value={field.value ?? ""}
                                onChange={(e) =>
                                    setValue(e.target.value, field)
                                }
                            />
                        </FormControl>
                    )}
                    {!disableError && <FormMessage />}
                </FormItem>
            )}
        />
    );
}

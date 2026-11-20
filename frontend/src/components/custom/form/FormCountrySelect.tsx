import { useMemo, useState } from "react";
import {
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover.tsx";
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command.tsx";
import { cn } from "@/lib/utils.ts";
import type { FieldValues, Path, UseFormReturn } from "react-hook-form";
import iso3311a2 from "iso-3166-1-alpha-2";

type FormCountrySelectProps<TFormValues extends FieldValues> = {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    disableError?: boolean;
};

export function FormCountrySelect<TFormValues extends FieldValues>({
    form,
    name,
    label,
    placeholder,
    required,
    disabled,
    disableError,
}: FormCountrySelectProps<TFormValues>) {
    const [open, setOpen] = useState(false);

    const { byCode } = useMemo(() => {
        const data = iso3311a2.getData() as Record<string, string>;
        const entries = Object.entries(data);

        const sorted = entries.sort((a, b) => a[1].localeCompare(b[1]));
        const byCode = new Map(sorted.map(([code, name]) => [code, name]));
        const byName = new Map(sorted.map(([code, name]) => [name, code]));
        const codes = new Set(byCode.keys());
        return { codes, byCode, byName };
    }, []);

    return (
        <FormField
            control={form.control}
            name={name}
            render={({ field }) => {
                const selectedLabel = field.value
                    ? (byCode.get(field.value as string) ?? String(field.value))
                    : undefined;

                return (
                    <FormItem>
                        <FormLabel className="pointer-events-none">
                            {label} {required ? " *" : ""}
                        </FormLabel>
                        <Popover open={open} onOpenChange={setOpen} modal>
                            <PopoverTrigger asChild>
                                <FormControl>
                                    <Button
                                        type="button"
                                        role="combobox"
                                        variant="input"
                                        aria-expanded={open}
                                        disabled={disabled}
                                        className={cn(
                                            "w-full justify-between",
                                            !selectedLabel &&
                                                "text-muted-foreground",
                                        )}
                                        onClick={() =>
                                            setOpen((prev) => !prev)
                                        }>
                                        {selectedLabel || placeholder || label}
                                        <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                                    </Button>
                                </FormControl>
                            </PopoverTrigger>
                            <PopoverContent
                                className="w-[--radix-popover-trigger-width] p-0 max-h-80"
                                align="start">
                                <Command>
                                    <CommandInput placeholder="Search country..." />
                                    <CommandList className="max-h-64 overflow-y-auto">
                                        <CommandEmpty>
                                            No country found.
                                        </CommandEmpty>
                                        <CommandGroup>
                                            {Array.from(byCode.entries()).map(
                                                ([code, name]) => (
                                                    <CommandItem
                                                        key={code}
                                                        value={`${name} ${code}`}
                                                        onSelect={() => {
                                                            field.onChange(
                                                                code,
                                                            );
                                                            setOpen(false);
                                                        }}
                                                        className="cursor-pointer">
                                                        <Check
                                                            className={cn(
                                                                "mr-2 h-4 w-4",
                                                                (field.value as string) ===
                                                                    code
                                                                    ? "opacity-100"
                                                                    : "opacity-0",
                                                            )}
                                                        />
                                                        <span className="grow">
                                                            {name}
                                                        </span>
                                                        <span className="text-muted-foreground ml-2">
                                                            {code}
                                                        </span>
                                                    </CommandItem>
                                                ),
                                            )}
                                        </CommandGroup>
                                    </CommandList>
                                </Command>
                            </PopoverContent>
                        </Popover>
                        {!disableError && <FormMessage />}
                    </FormItem>
                );
            }}
        />
    );
}

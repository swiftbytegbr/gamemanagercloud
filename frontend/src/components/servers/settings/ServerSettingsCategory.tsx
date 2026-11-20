import { cn } from "@/lib/utils.ts";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import type { SettingTemplate } from "@/store/serverStore.ts";
import type { UseFormReturn } from "react-hook-form";
import { FormTextArea } from "@/components/custom/form/FormTextArea.tsx";
import { getFieldKey } from "@/lib/settingFieldKey.ts";

export type ServerSettingsCategoryProps = {
    category: string;
    templates: SettingTemplate[];
    form: UseFormReturn;
    index: number;
    categoryRef?: (el: HTMLDivElement | null) => void;
};

export function ServerSettingsCategory({
    category,
    templates,
    form,
    index,
    categoryRef,
}: ServerSettingsCategoryProps) {
    return (
        <div
            id={category}
            ref={categoryRef}
            className={cn(
                "grid bg-secondary rounded-md p-5 xl:grid-cols-2 2xl:grid-cols-3 gap-5",
                index !== 0 ? "mt-5" : "",
            )}>
            <h2 className="uppercase xl:col-span-2 2xl:col-span-3">
                {category}
            </h2>
            {templates.map((template) => (
                <div
                    key={template.id}
                    id={template.id}
                    tabIndex={-1}
                    className={cn(
                        template.inline
                            ? "col-span-1"
                            : "xl:col-span-2 2xl:col-span-3",
                        "flex items-center w-full min-w-0 data-[highlighted=true]:ring-2 ring-offset-8 ring-offset-secondary ring-primary rounded-md",
                    )}>
                    {template.appearance === "SUBTITLE" ? (
                        <div className="w-full mt-4">
                            <h3 className="text-lg text-input font-semibold">
                                {template.displayName}
                            </h3>
                            <hr />
                        </div>
                    ) : template.appearance === "CHECKBOX" ? (
                        <FormCheckbox
                            form={form}
                            label={template.displayName}
                            className="grow"
                            name={getFieldKey(template)}
                            description={template.description}
                        />
                    ) : template.appearance === "TEXT_AREA" ? (
                        <FormTextArea
                            form={form}
                            name={getFieldKey(template)}
                            label={template.displayName}
                            variant="cutOut"
                            resize={false}
                            height="large"
                            className="grow"
                            required={template.required}
                            description={template.description}
                            placeholder={template.placeholder}
                        />
                    ) : template.appearance === "LIST" ? (
                        <FormTextArea
                            form={form}
                            name={getFieldKey(template)}
                            label={template.displayName}
                            variant="cutOut"
                            resize={false}
                            height="large"
                            className="grow"
                            required={template.required}
                            description={`Insert your list as text in the text area, one entry per line. ${template.description ?? ""}`}
                            placeholder={template.placeholder}
                            isListInput
                        />
                    ) : (
                        <FormInput
                            form={form}
                            name={getFieldKey(template)}
                            label={template.displayName}
                            className="grow"
                            description={template.description}
                            placeholder={template.placeholder}
                            required={template.required}
                            type={
                                template.appearance === "PASSWORD"
                                    ? "password"
                                    : template.type === "INTEGER"
                                      ? "integer"
                                      : template.type === "FLOAT"
                                        ? "number"
                                        : "text"
                            }
                        />
                    )}
                </div>
            ))}
        </div>
    );
}

import { useEffect, useState } from "react";
import { z, ZodString } from "zod";
import type { SettingProfile, SettingTemplate } from "@/store/serverStore";
import { getFieldKey } from "@/lib/settingFieldKey.ts";

export function useServerSettingsForm(
    templates?: SettingTemplate[],
    profile?: SettingProfile,
) {
    const [state, setState] = useState<{
        isProcessing: boolean;
        schema: z.ZodObject<Record<string, z.ZodTypeAny>>;
        defaults: Record<string, unknown>;
        processedTemplates: Record<string, SettingTemplate[]>;
    }>({
        isProcessing: true,
        schema: z.object({}),
        defaults: {},
        processedTemplates: {},
    });

    useEffect(() => {
        let cancelled = false;
        setState((current) => ({ ...current, isProcessing: true }));

        Promise.resolve().then(() => {
            if (!templates || !profile)
                return {
                    schema: z.object({}),
                    defaults: {},
                    processedTemplates: {},
                };

            const zodSchema: Record<string, z.ZodTypeAny> = {};
            const defaults: Record<string, unknown> = {};
            const processedTemplates: Record<string, SettingTemplate[]> = {};

            for (const template of templates) {
                if (!processedTemplates[template.category])
                    processedTemplates[template.category] = [];
                processedTemplates[template.category].push(template);

                const requiredError = `${template.displayName} is required`;
                const isRequired = template.required;

                let zodType: z.ZodTypeAny = (() => {
                    switch (template.type) {
                        case "BOOLEAN":
                            return z.boolean({ message: requiredError });
                        default:
                            return z.string({ message: requiredError });
                    }
                })();

                if (template.verifyingRegex && zodType instanceof ZodString) {
                    zodType = zodType.regex(
                        new RegExp(template.verifyingRegex),
                        {
                            message: template.regexMessage
                                ? template.regexMessage
                                : `${template.displayName} needs to match the pattern: ${template.verifyingRegex}`,
                        },
                    );
                }

                if (!isRequired) {
                    zodType = zodType.optional();
                } else if (zodType instanceof ZodString) {
                    zodType = zodType.min(1, { message: requiredError });
                }

                switch (template.type) {
                    case "STRING":
                        zodType = zodType.transform((val) =>
                            val === "" ? undefined : val,
                        );
                        if (isRequired) {
                            zodType = zodType.refine((val) => val !== null, {
                                message: requiredError,
                            });
                        }
                        break;
                    case "LIST":
                        zodType = zodType.transform((val): string[] => {
                            if (Array.isArray(val)) {
                                return val;
                            }
                            if (typeof val === "string" && val.length > 0) {
                                return val.split(/\n/);
                            }
                            return [];
                        });
                        break;
                    case "INTEGER":
                        zodType = zodType
                            .or(z.null())
                            .refine(
                                (val) => {
                                    if (val === null || val === "") {
                                        return true;
                                    }
                                    if (
                                        template.rangeFrom === 0 &&
                                        template.rangeTo === 0
                                    ) {
                                        return true;
                                    }
                                    if (!(
                                        val !== null &&
                                        Number(val) >= template.rangeFrom
                                    )) {
                                        return false;
                                    }

                                    return Number(val) <= template.rangeTo;
                                },
                                {
                                    message: `${template.displayName} must be between ${template.rangeFrom} and ${template.rangeTo}`,
                                },
                            )
                            .transform((val) => {
                                if (
                                    val === "" ||
                                    val === null ||
                                    typeof val === "undefined"
                                )
                                    return null;
                                if (typeof val === "number") return Number(val);
                                return Number(val);
                            });
                        if (isRequired) {
                            zodType = zodType.refine((val) => val !== null, {
                                message: requiredError,
                            });
                        }
                        break;
                    case "FLOAT":
                        zodType = zodType.or(z.null()).transform((val) => {
                            if (
                                val === "" ||
                                val === null ||
                                typeof val === "undefined"
                            )
                                return null;
                            if (typeof val === "number") return Number(val);
                            return Number(val);
                        });
                        if (isRequired) {
                            zodType = zodType.refine((val) => val !== null, {
                                message: requiredError,
                            });
                        }
                        break;
                }

                const key = getFieldKey(template);
                let value;
                if (template.location === "GameUserSettings.ini") {
                    value =
                        profile?.gameUserSettings[template.section]?.[
                            template.key
                        ];
                } else if (template.location === "Game.ini") {
                    value =
                        profile?.gameSettings[template.section]?.[template.key];
                } else if (
                    template.location === "CommandLineOption" &&
                    template.section === "-"
                ) {
                    value = profile?.hyphenParams?.[template.key];
                } else if (
                    template.location === "CommandLineOption" &&
                    template.section === "?"
                ) {
                    value = profile?.questionMarkParams?.[template.key];
                } else {
                    if (template.key === "Map") {
                        value = profile.map;
                    } else {
                        value = profile?.gmcSettings?.[template.key];
                    }
                }

                if (value === null || typeof value === "undefined") {
                    defaults[key] = undefined;
                } else if (typeof value === "boolean") {
                    defaults[key] = value;
                } else if (Array.isArray(value)) {
                    defaults[key] = value.join("\n");
                } else {
                    defaults[key] = String(value);
                }

                zodSchema[key] = zodType;
            }

            if (!cancelled) {
                setState({
                    isProcessing: false,
                    schema: z.object(zodSchema),
                    defaults,
                    processedTemplates,
                });
            }
        });

        return () => {
            cancelled = true;
        };
    }, [templates, profile]);

    return state;
}

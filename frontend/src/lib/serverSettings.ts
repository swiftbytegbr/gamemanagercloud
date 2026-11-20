import type { Dispatch, SetStateAction } from "react";
import type { CustomIniSections } from "@/components/servers/settings/CustomIniEditor.tsx";
import type {
    SettingProfile,
    SettingProfileValue,
    SettingTemplate,
} from "@/store/serverStore.ts";
import { parseFieldKey } from "@/lib/settingFieldKey.ts";

export const CUSTOM_CATEGORY = "Custom";
export const SERVER_SETTINGS_SCROLL_PREFIX = "serverSettingsScroll:";

export type MissingSetting = {
    location: string;
    section: string;
    key: string;
    value: SettingProfileValue;
};

export type CustomIniSnapshot = {
    game: CustomIniSections;
    gus: CustomIniSections;
};

export type CustomIniState = CustomIniSnapshot & {
    setGame: Dispatch<SetStateAction<CustomIniSections>>;
    setGus: Dispatch<SetStateAction<CustomIniSections>>;
};

export type ScrollMemory = {
    remember: () => void;
};

export function createCategoryList(
    processedTemplates: Record<string, SettingTemplate[]>,
) {
    const categories = Object.keys(processedTemplates).filter(
        (category) => category !== CUSTOM_CATEGORY,
    );
    categories.push(CUSTOM_CATEGORY);
    return categories;
}

export function collectUserCustomSettings(
    profile?: SettingProfile,
    templates?: SettingTemplate[],
): MissingSetting[] {
    if (!profile || !templates) return [];

    const templateKeys = new Set(
        templates.map(
            (template) =>
                `${template.location}:${template.section}:${template.key}`,
        ),
    );

    const missing: MissingSetting[] = [];

    const collect = (
        source: Record<string, Record<string, SettingProfileValue>>,
        location: "Game.ini" | "GameUserSettings.ini",
    ) => {
        Object.entries(source).forEach(([section, settings]) => {
            const normalizedSection = normalizeSectionName(section);
            Object.entries(settings).forEach(([key, value]) => {
                const lookup = `${location}:${normalizedSection}:${key}`;
                if (!templateKeys.has(lookup)) {
                    missing.push({
                        location,
                        section: normalizedSection,
                        key,
                        value,
                    });
                }
            });
        });
    };

    collect(profile.gameUserSettings, "GameUserSettings.ini");
    collect(profile.gameSettings, "Game.ini");

    return missing;
}

export function buildSyncableSettings(
    processedTemplates: Record<string, SettingTemplate[]>,
    userCustomSettings: MissingSetting[],
) {
    const categories: Record<
        string,
        Array<
            Pick<
                SettingTemplate,
                "location" | "section" | "key" | "disableSync"
            >
        >
    > = {};

    Object.entries(processedTemplates).forEach(([category, templates]) => {
        categories[category] = templates.map((template) => ({
            location: template.location,
            section: template.section,
            key: template.key,
            disableSync: template.disableSync,
        }));
    });

    if (userCustomSettings.length > 0) {
        categories[CUSTOM_CATEGORY] = [
            ...(categories[CUSTOM_CATEGORY] ?? []),
            ...userCustomSettings.map(({ location, section, key }) => ({
                location,
                section,
                key,
                disableSync: false,
            })),
        ];
    }

    return categories;
}

export function buildCustomIniSections(
    settings: MissingSetting[],
): CustomIniSnapshot {
    if (settings.length === 0) {
        return { game: {}, gus: {} };
    }

    const sorted = [...settings].sort((a, b) => {
        if (a.location !== b.location) {
            return a.location.localeCompare(b.location);
        }
        if (a.section !== b.section) {
            return a.section.localeCompare(b.section);
        }
        return a.key.localeCompare(b.key);
    });

    const snapshot: CustomIniSnapshot = { game: {}, gus: {} };

    sorted.forEach(({ location, section, key, value }) => {
        const target = location === "Game.ini" ? snapshot.game : snapshot.gus;
        const normalizedSection = normalizeSectionName(section);
        const normalizedValue = Array.isArray(value)
            ? value.map((item) => `${key}=${String(item)}`)
            : [`${key}=${String(value ?? "")}`];

        target[normalizedSection] = [
            target[normalizedSection] ?? "",
            ...normalizedValue,
        ]
            .filter(Boolean)
            .join("\n");
    });

    return snapshot;
}

export function buildSettingProfileFromForm(
    profile: SettingProfile,
    data: Record<string, SettingProfileValue>,
    customIni: CustomIniSnapshot,
): SettingProfile {
    const next: SettingProfile = {
        ...profile,
        gameSettings: {},
        hyphenParams: {},
        gameUserSettings: {},
        questionMarkParams: {},
        gmcSettings: {},
    };

    Object.entries(data).forEach(([compoundKey, value]) => {
        const { location, section, key } = parseFieldKey(compoundKey);
        if (!location || !key) return;
        const normalizedSection = section ?? "";

        switch (location) {
            case "GameUserSettings.ini":
                if (!next.gameUserSettings[normalizedSection]) {
                    next.gameUserSettings[normalizedSection] = {};
                }
                next.gameUserSettings[normalizedSection][key] = value;
                break;
            case "Game.ini":
                if (!next.gameSettings[normalizedSection]) {
                    next.gameSettings[normalizedSection] = {};
                }
                next.gameSettings[normalizedSection][key] = value;
                break;
            case "CommandLineOption":
                if (normalizedSection === "-") {
                    next.hyphenParams[key] = value;
                } else if (normalizedSection === "?") {
                    next.questionMarkParams[key] = value;
                }
                break;
            case "GMCSettings":
                if (key === "Map") {
                    next.map = value as string;
                } else {
                    next.gmcSettings[key] = value;
                }
                break;
        }
    });

    applyCustomSections(next, "Game.ini", customIni.game);
    applyCustomSections(next, "GameUserSettings.ini", customIni.gus);

    return next;
}

function applyCustomSections(
    profile: SettingProfile,
    location: "Game.ini" | "GameUserSettings.ini",
    sections: CustomIniSections,
) {
    const destination =
        location === "Game.ini"
            ? profile.gameSettings
            : profile.gameUserSettings;

    Object.entries(sections).forEach(([sectionName, text]) => {
        if (!text?.trim()) return;
        const section = normalizeSectionName(sectionName);
        if (!destination[section]) {
            destination[section] = {};
        }
        const parsed = parseCustomSectionText(text);
        Object.entries(parsed).forEach(([key, value]) => {
            const existing = destination[section][key];
            if (typeof existing === "undefined") {
                destination[section][key] = value;
            } else {
                destination[section][key] = mergeProfileValues(existing, value);
            }
        });
    });
}

function parseCustomSectionText(text: string) {
    const result: Record<string, SettingProfileValue> = {};
    const lines = text.split(/\r?\n/);
    for (const raw of lines) {
        const line = raw.trim();
        if (!line || line.startsWith(";") || line.startsWith("#")) continue;
        const idx = line.indexOf("=");
        if (idx <= 0) continue;
        const key = line.slice(0, idx).trim();
        if (!key) continue;
        const value = coerceValue(line.slice(idx + 1));
        if (!(key in result)) {
            result[key] = value;
        } else {
            result[key] = mergeProfileValues(result[key], value);
        }
    }
    return result;
}

function coerceValue(raw: string): string | number | boolean {
    const value = raw.trim();
    const lower = value.toLowerCase();
    if (lower === "true") return true;
    if (lower === "false") return false;
    if (/^-?\d+$/.test(value)) {
        const parsed = Number.parseInt(value, 10);
        return Number.isSafeInteger(parsed) ? parsed : value;
    }
    if (/^-?\d*\.\d+$/.test(value)) {
        const parsed = Number.parseFloat(value);
        return Number.isFinite(parsed) ? parsed : value;
    }
    return value;
}

function mergeProfileValues(
    existing: SettingProfileValue,
    incoming: SettingProfileValue,
): SettingProfileValue {
    const toArray = (val: SettingProfileValue): string[] => {
        if (Array.isArray(val)) return val.map(String);
        if (typeof val === "undefined" || val === null) return [];
        return [String(val as string | number | boolean)];
    };

    if (Array.isArray(existing) || Array.isArray(incoming)) {
        return [...toArray(existing), ...toArray(incoming)];
    }

    if (typeof existing === "undefined") return incoming;
    if (typeof incoming === "undefined") return existing;
    return [
        String(existing as string | number | boolean),
        String(incoming as string | number | boolean),
    ];
}

export function normalizeSectionName(section: string) {
    return section.replaceAll("#", ".");
}

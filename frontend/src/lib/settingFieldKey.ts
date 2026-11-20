import type { SettingTemplate } from "@/store/serverStore.ts";

const FIELD_KEY_SEPARATOR = ":";

const encodeFieldKeyPart = (part: string) =>
    encodeURIComponent(part ?? "").replace(/\./g, "%2E");

const decodeFieldKeyPart = (part?: string) => {
    if (!part) return "";
    try {
        return decodeURIComponent(part);
    } catch {
        return part;
    }
};

export function getFieldKey(template: SettingTemplate) {
    return [template.location, template.section, template.key]
        .map(encodeFieldKeyPart)
        .join(FIELD_KEY_SEPARATOR);
}

export function parseFieldKey(fieldKey: string) {
    const [location, section, key] = fieldKey
        .split(FIELD_KEY_SEPARATOR)
        .map((part) => decodeFieldKeyPart(part));
    return {
        location: location ?? "",
        section: section ?? "",
        key: key ?? "",
    };
}

import { startTransition, useEffect, useMemo, useRef, useState } from "react";
import type { UseFormReturn } from "react-hook-form";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { cn } from "@/lib/utils.ts";
import { dialog } from "@/store/dialogStore.tsx";
import { Trash } from "lucide-react";

export type CustomIniSections = Record<string, string>;

type CustomIniEditorProps = {
    title: string;
    location: "Game.ini" | "GameUserSettings.ini";
    sections: CustomIniSections;
    onChange: (next: CustomIniSections) => void;
    form?: UseFormReturn;
    className?: string;
};

export function CustomIniEditor({
    title,
    location,
    sections,
    onChange,
    form,
    className,
}: CustomIniEditorProps) {
    const [selectedSection, setSelectedSection] = useState<string>("");
    const [newSection, setNewSection] = useState("");
    const [localSections, setLocalSections] =
        useState<CustomIniSections>(sections);

    const markedDirtyRef = useRef(false);
    const markDirtyOnce = () => {
        if (!form || markedDirtyRef.current) return;
        try {
            form.register("__customIniTouched");
            form.setValue("__customIniTouched" as never, 1 as never, {
                shouldDirty: true,
                shouldTouch: true,
                shouldValidate: false,
            });
            markedDirtyRef.current = true;
        } catch {
            /* ignore */
        }
    };

    useEffect(() => {
        const same = () => {
            const a = sections;
            const b = localSections;
            const aKeys = Object.keys(a);
            const bKeys = Object.keys(b);
            if (aKeys.length !== bKeys.length) return false;
            for (const k of aKeys) if (a[k] !== b[k]) return false;
            return true;
        };
        if (!same()) setLocalSections(sections);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sections]);

    const sectionNames = useMemo(
        () => Object.keys(localSections).sort((a, b) => a.localeCompare(b)),
        [localSections],
    );

    useEffect(() => {
        if (!selectedSection && sectionNames.length > 0) {
            setSelectedSection(sectionNames[0]);
        }
    }, [selectedSection, sectionNames]);

    const addSection = () => {
        const name = newSection.trim();
        if (!name) return;
        if (localSections[name]) {
            setSelectedSection(name);
            setNewSection("");
            return;
        }
        markDirtyOnce();
        const next = { ...localSections, [name]: "" };
        setLocalSections(next);
        startTransition(() => onChange(next));
        setSelectedSection(name);
        setNewSection("");
    };

    const removeSection = (name: string) => {
        markDirtyOnce();
        const next = { ...localSections };
        delete next[name];
        setLocalSections(next);
        startTransition(() => onChange(next));
        if (selectedSection === name) setSelectedSection("");
    };

    const requestRemoveSection = (name: string) => {
        dialog(
            <ConfirmDialog
                title={`Remove [${name}]?`}
                message={`This will delete the [${name}] section from ${location}. This action cannot be undone.`}
                confirmText="Remove"
                onConfirm={() => removeSection(name)}
            />,
        );
    };

    const updateText = (name: string, text: string) => {
        markDirtyOnce();
        const next = { ...localSections, [name]: text };
        setLocalSections(next);
        startTransition(() => onChange(next));
    };

    return (
        <div
            className={cn(
                "flex flex-col bg-secondary rounded-md p-5 gap-4 w-full border-input border",
                className,
            )}>
            <h3>{title}</h3>
            <div className="flex flex-col md:flex-row gap-4 min-h-40">
                <div className="w-full md:w-52 xl:w-60 2xl:w-72 min-w-48 flex flex-col gap-2 md:border-r md:border-input p-2">
                    <div className="flex gap-2">
                        <Input
                            value={newSection}
                            onChange={(e) => setNewSection(e.target.value)}
                            placeholder="Add section"
                        />
                        <Button onClick={addSection}>Add</Button>
                    </div>
                    <div className="flex flex-col gap-1 overflow-auto pr-1 h-40 overflow-y-auto">
                        {sectionNames.length === 0 ? (
                            <span className="text-muted-foreground text-sm">
                                No sections yet
                            </span>
                        ) : (
                            sectionNames.map((name) => (
                                <div
                                    key={`${location}:${name}`}
                                    className={cn(
                                        "flex items-center justify-between gap-2 px-2 py-1 rounded cursor-pointer border",
                                        selectedSection === name
                                            ? "bg-accent border-accent"
                                            : "border-transparent hover:bg-accent/40",
                                    )}
                                    onClick={() => setSelectedSection(name)}>
                                    <span className="truncate text-sm">
                                        [{name}]
                                    </span>
                                    <Button
                                        size="tiny"
                                        variant="ghost"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            requestRemoveSection(name);
                                        }}>
                                        <Trash />
                                    </Button>
                                </div>
                            ))
                        )}
                    </div>
                </div>
                <div className="grow min-w-0">
                    {selectedSection ? (
                        <div className="flex flex-col gap-2 h-full">
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-muted-foreground text-nowrap overflow-x-auto">
                                    Editing section: [{selectedSection}] in{" "}
                                    {location}
                                </span>
                            </div>
                            <Textarea
                                className="h-40 overflow-y-auto"
                                value={localSections[selectedSection] ?? ""}
                                onChange={(e) =>
                                    updateText(selectedSection, e.target.value)
                                }
                                placeholder="Enter key=value pairs, one per line. Duplicate keys become lists."
                                wrap="off"
                                height="large"
                                resize={false}
                            />
                        </div>
                    ) : (
                        <div className="h-full flex items-center justify-center text-muted-foreground text-sm">
                            Select or add a section to edit.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

import { useCallback, useEffect, useRef, useState } from "react";
import type { SettingTemplate } from "@/store/serverStore.ts";

export type SearchResult = {
    id: string;
    name: string;
    category: string;
};

export function useSettingsSearch(
    processedTemplates: Record<string, SettingTemplate[]>,
) {
    const [results, setResults] = useState<SearchResult[]>([]);
    const [isVisible, setVisible] = useState(false);
    const searchContainerRef = useRef<HTMLDivElement | null>(null);

    const clearHighlights = useCallback(() => {
        document
            .querySelectorAll('[data-highlighted="true"]')
            .forEach((node) => node.removeAttribute("data-highlighted"));
    }, []);

    const scrollToResult = useCallback((id: string) => {
        const element = document.getElementById(id);
        if (!element) return;
        element.scrollIntoView({ behavior: "smooth", block: "center" });
        element.setAttribute("data-highlighted", "true");
    }, []);

    const onInputChange = useCallback(
        (value: string) => {
            const searchTerm = value.trim().toLowerCase();
            clearHighlights();
            if (!searchTerm) {
                setResults([]);
                setVisible(false);
                return;
            }

            const matches: SearchResult[] = Object.entries(
                processedTemplates ?? {},
            ).flatMap(([category, templates]) =>
                templates
                    .filter((template) =>
                        template.displayName.toLowerCase().includes(searchTerm),
                    )
                    .map((template) => ({
                        id: template.id,
                        name: template.displayName,
                        category,
                    })),
            );

            setResults(matches);
            setVisible(matches.length > 0);

            if (matches[0]) {
                clearHighlights();
                scrollToResult(matches[0].id);
            }
        },
        [processedTemplates, clearHighlights, scrollToResult],
    );

    const onResultSelect = useCallback(
        (match: SearchResult) => {
            clearHighlights();
            scrollToResult(match.id);
            setVisible(false);
        },
        [clearHighlights, scrollToResult],
    );

    const showResults = useCallback(() => {
        setVisible((visible) => visible || results.length > 0);
    }, [results.length]);

    useEffect(() => {
        const handleClick = (event: MouseEvent) => {
            const target = event.target as HTMLElement;
            if (target.closest('[data-highlighted="true"]')) return;
            if (
                searchContainerRef.current &&
                searchContainerRef.current.contains(target)
            ) {
                return;
            }
            clearHighlights();
            setVisible(false);
        };

        document.addEventListener("click", handleClick);
        return () => document.removeEventListener("click", handleClick);
    }, [clearHighlights]);

    return {
        results,
        isVisible,
        onInputChange,
        onResultSelect,
        showResults,
        searchContainerRef,
    };
}

import { memo, type RefObject, useEffect, useState } from "react";
import { CategoryButton } from "./CategoryButton.tsx";

export type CategoryButtonListProps = {
    categories: string[];
    containerRef: RefObject<HTMLDivElement | null>;
    categoryRefs: RefObject<Record<string, HTMLDivElement>>;
};

export const CategoryButtonList = memo(function CategoryButtonList(
    props: CategoryButtonListProps,
) {
    const [activeCategory, setActiveCategory] = useState<string | null>(null);

    useEffect(() => {
        const container = props.containerRef?.current;
        if (!container) return;

        const handleScroll = () => {
            const entries = Object.entries(props.categoryRefs.current).map(
                ([category, ref]) => {
                    const rect = ref.getBoundingClientRect();
                    const containerRect = container.getBoundingClientRect();
                    const top = rect.top - containerRect.top;
                    const bottom = rect.bottom - containerRect.top;
                    return { category, top, bottom };
                },
            );

            const candidates = entries.filter(
                (e) => e.top <= 0 && e.bottom > 40,
            );

            let current: (typeof entries)[number] | undefined;
            if (candidates.length > 0) {
                current = candidates.reduce((prev, curr) =>
                    curr.top > prev.top ? curr : prev,
                );
            } else {
                const above = entries.filter((e) => e.top > 0);
                if (above.length > 0) {
                    current = above.reduce((prev, curr) =>
                        curr.top < prev.top ? curr : prev,
                    );
                } else if (entries.length > 0) {
                    current = entries[entries.length - 1];
                }
            }

            if (current) setActiveCategory(current.category);
        };

        container.addEventListener("scroll", handleScroll);
        handleScroll();

        return () => container.removeEventListener("scroll", handleScroll);
    }, [props.categories, props.containerRef, props.categoryRefs]);

    const scrollTargetIntoContainer = (target: HTMLDivElement) => {
        const container = props.containerRef?.current;
        if (!container) return;
        const containerRect = container.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();

        const offsetTop =
            targetRect.top - containerRect.top + container.scrollTop;

        container.scrollTo({ top: offsetTop });
    };

    return (
        <>
            {props.categories.map((category) => (
                <CategoryButton
                    key={category}
                    category={category}
                    isActive={activeCategory === category}
                    onClick={() =>
                        scrollTargetIntoContainer(
                            props.categoryRefs.current[category],
                        )
                    }
                />
            ))}
        </>
    );
});

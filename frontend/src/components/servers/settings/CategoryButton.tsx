import { Button } from "@/components/ui/button.tsx";
import { memo } from "react";

export const CategoryButton = memo(function CategoryButton(props: {
    category: string;
    isActive: boolean;
    onClick: () => void;
}) {
    return (
        <Button
            className="block w-full uppercase text-lg text-left"
            size="lg"
            variant={props.isActive ? "tertiary" : "navbar"}
            onClick={props.onClick}>
            {props.category}
        </Button>
    );
});

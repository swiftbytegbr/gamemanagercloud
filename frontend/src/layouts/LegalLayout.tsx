import { Button } from "@/components/ui/button.tsx";
import { ArrowLeft, ExternalLink } from "lucide-react";
import { EmbeddedHtml } from "@/components/custom/EmbeddedHtml.tsx";

export function LegalLayout(props: { url: string }) {
    return (
        <div className="p-4 grid grid-cols-2 lg:grid-cols-[12rem_1fr_12rem] lg:h-screen lg:order-1">
            <Button onClick={() => window.history.back()} className="w-fit">
                <ArrowLeft className="size-5" />
                <span className="text-base uppercase">Back</span>
            </Button>
            <Button
                onClick={() => window.open(props.url, "_blank")}
                className="w-fit ml-auto lg:order-2"
                variant="outline">
                <ExternalLink className="size-5" />
                <span className="text-base uppercase">Text Version</span>
            </Button>
            <EmbeddedHtml url={props.url} />
        </div>
    );
}

import { Spinner } from "@/components/ui/spinner.tsx";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { sanitizeHtml } from "@/lib/sanitizeHtml.ts";

interface EmbeddedHtmlProps {
    url: string;
}

export function EmbeddedHtml(props: EmbeddedHtmlProps) {
    const [isLoading, setLoading] = useState<boolean>(true);
    const [htmlContent, setHtmlContent] = useState<string>("");

    useEffect(() => {
        setLoading(true);
        fetch(props.url)
            .then((response) => response.text())
            .then((html) => setHtmlContent(sanitizeHtml(html)))
            .catch(() => {
                toast.error(
                    `Failed to load ${props.url}, please use the text version.`,
                );
            })
            .finally(() => setLoading(false));
    }, [props.url]);

    return (
        <div
            id="embedded-html"
            className="mt-5 lg:order-1 col-span-2 lg:col-span-1 bg-secondary p-4 rounded-2xl">
            {isLoading ? (
                <div className="flex justify-center items-center h-full">
                    <Spinner size="large" />
                </div>
            ) : (
                <div dangerouslySetInnerHTML={{ __html: htmlContent }} />
            )}
        </div>
    );
}

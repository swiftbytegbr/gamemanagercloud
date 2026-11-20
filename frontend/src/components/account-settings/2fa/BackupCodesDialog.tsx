import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button.tsx";
import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { popDialog } from "@/store/dialogStore.tsx";

export function BackupCodesDialog({ codes }: { codes: string[] }) {
    const [copied, setCopied] = useState(false);
    const formattedCodes = useMemo(
        () => codes.map((code) => code.toUpperCase()),
        [codes],
    );

    async function copyCodes() {
        if (typeof navigator === "undefined" || !navigator.clipboard) return;
        try {
            await navigator.clipboard.writeText(formattedCodes.join("\n"));
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch {
            setCopied(false);
        }
    }

    function downloadCodes() {
        if (typeof document === "undefined") return;
        const blob = new Blob([formattedCodes.join("\n")], {
            type: "text/plain;charset=utf-8",
        });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "backup-codes.txt";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    return (
        <div className="flex flex-col gap-5">
            <DialogHeader className="text-left">
                <DialogTitle>Backup Codes</DialogTitle>
                <DialogDescription>
                    Store these single-use codes in a safe place. They allow you
                    to access your account if you can’t receive a 2FA code.
                </DialogDescription>
            </DialogHeader>
            <div className="grid gap-2 bg-secondary/50 rounded-md p-4 font-mono text-base text-center">
                {formattedCodes.map((code) => (
                    <span key={code} className="tracking-[0.3rem]">
                        {code}
                    </span>
                ))}
            </div>
            <DialogFooter className="flex flex-col gap-2">
                <Button variant="outline" type="button" onClick={downloadCodes}>
                    Download .txt File
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    onClick={copyCodes}
                    disabled={copied}>
                    {copied ? "Copied" : "Copy Codes"}
                </Button>
                <Button
                    type="button"
                    variant="tertiary"
                    onClick={() => popDialog()}>
                    Close
                </Button>
            </DialogFooter>
        </div>
    );
}

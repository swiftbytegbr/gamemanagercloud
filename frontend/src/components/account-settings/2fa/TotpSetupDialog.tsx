import { useState } from "react";
import QRCode from "react-qr-code";
import { Button } from "@/components/ui/button.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Label } from "@/components/ui/label.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { popDialog } from "@/store/dialogStore.tsx";
import { type TotpSetupResponse, verifyTotpTwoFA } from "@/api/userApi.ts";

export function TotpSetupDialog({
    setup,
    onVerified,
}: {
    setup: TotpSetupResponse;
    onVerified: (codes: string[]) => void | Promise<void>;
}) {
    const [code, setCode] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError(null);
        if (!code) {
            setError("Enter a code from your authenticator app.");
            return;
        }
        setLoading(true);
        try {
            const result = await verifyTotpTwoFA(code);
            popDialog();
            await onVerified(result.backupCodes ?? []);
        } catch {
            setLoading(false);
        }
    }

    return (
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
            <DialogHeader className="text-left gap-2">
                <DialogTitle>Connect an Authenticator App</DialogTitle>
                <DialogDescription>
                    Scan the QR code with your authenticator app or enter the
                    secret manually, then confirm with a 6-digit code.
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-4">
                <div className="flex-1 space-y-2">
                    <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide">
                            Secret
                        </p>
                        <p className="font-mono text-base break-all">
                            {setup.secret}
                        </p>
                    </div>
                    <p className="text-xs text-muted-foreground">
                        If you can’t scan the QR code, enter the secret above in
                        your authenticator app using the TOTP option.
                    </p>
                </div>
                <div className="flex justify-center w-full">
                    <div className="flex flex-col w-fit justify-center p-3 rounded-md border bg-secondary">
                        <QRCode value={setup.provisioningUri} size={168} />
                    </div>
                </div>
            </div>
            <div className="flex flex-col gap-2">
                <Label htmlFor="totp-code">Authenticator code</Label>
                <Input
                    id="totp-code"
                    value={code}
                    onChange={(e) => setCode(e.target.value.trim())}
                    placeholder="123456"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                />
                {error && <p className="text-sm text-destructive">{error}</p>}
            </div>
            <DialogFooter className="flex gap-2">
                <Button
                    type="button"
                    variant="tertiary"
                    onClick={() => popDialog()}>
                    Cancel
                </Button>
                <Button type="submit" disabled={loading}>
                    {loading ? <Spinner size="tiny" /> : "Enable Authenticator"}
                </Button>
            </DialogFooter>
        </form>
    );
}

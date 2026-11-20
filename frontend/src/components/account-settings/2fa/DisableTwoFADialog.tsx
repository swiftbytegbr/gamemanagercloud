import { useState } from "react";
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

type DisableTwoFaPayload = {
    twoFACode?: string;
    backupCode?: string;
};

export function DisableTwoFADialog({
    type,
    onConfirm,
}: {
    type: "EMAIL" | "TOTP";
    onConfirm: (payload: DisableTwoFaPayload) => Promise<void>;
}) {
    const [twoFACode, setTwoFACode] = useState("");
    const [backupCode, setBackupCode] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [showBackupInput, setShowBackupInput] = useState(false);

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError(null);
        if (!twoFACode && !backupCode) {
            setError(
                "Enter your current 2FA code or one of your backup codes.",
            );
            return;
        }
        setLoading(true);
        try {
            await onConfirm({
                twoFACode: twoFACode || undefined,
                backupCode: backupCode || undefined,
            });
            popDialog();
        } catch {
            setLoading(false);
        }
    }

    return (
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            <DialogHeader className="text-left gap-2">
                <DialogTitle>
                    Disable {type === "EMAIL" ? "Email" : "Authenticator"} 2FA
                </DialogTitle>
                <DialogDescription>
                    Confirm this action with a current code or a backup code.
                </DialogDescription>
                {type === "EMAIL" && (
                    <p className="text-sm text-muted-foreground">
                        We sent a fresh verification code to your email.
                    </p>
                )}
            </DialogHeader>
            <div className="flex flex-col gap-3">
                {!showBackupInput && (
                    <div className="flex flex-col gap-2">
                        <Label>
                            {type === "EMAIL"
                                ? "Email verification code"
                                : "Authenticator code"}
                        </Label>
                        <Input
                            value={twoFACode}
                            onChange={(e) =>
                                setTwoFACode(e.target.value.trim())
                            }
                            placeholder="123456"
                            inputMode="numeric"
                            autoComplete="one-time-code"
                        />
                    </div>
                )}
                {showBackupInput ? (
                    <div className="flex flex-col gap-2">
                        <Label htmlFor="backup-code">Backup code</Label>
                        <Input
                            id="backup-code"
                            value={backupCode}
                            onChange={(e) =>
                                setBackupCode(
                                    e.target.value.trim().toUpperCase(),
                                )
                            }
                            placeholder="XXXX-XXXX-XXXX"
                        />
                        <p className="text-xs text-muted-foreground">
                            Use one of the backup codes you saved earlier.
                        </p>
                    </div>
                ) : (
                    <p className="text-xs text-muted-foreground">
                        Lost you device?{" "}
                        <button
                            type="button"
                            className="text-primary underline underline-offset-2 font-medium"
                            onClick={() => {
                                setTwoFACode("");
                                setShowBackupInput(true);
                            }}>
                            Use a backup code instead.
                        </button>
                    </p>
                )}
                {error && <p className="text-sm text-destructive">{error}</p>}
            </div>
            <DialogFooter className="flex flex-col gap-2 sm:flex-row">
                <Button
                    type="button"
                    variant="tertiary"
                    onClick={() => popDialog()}>
                    Cancel
                </Button>
                <Button type="submit" variant="destructive" disabled={loading}>
                    {loading ? <Spinner size="tiny" /> : "Disable 2FA"}
                </Button>
            </DialogFooter>
        </form>
    );
}

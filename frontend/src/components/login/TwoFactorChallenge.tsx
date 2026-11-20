import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card.tsx";
import { LoginLayout } from "@/layouts/LoginLayout.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Label } from "@/components/ui/label.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";

type TwoFactorPayload = {
    twoFACode?: string;
    backupCode?: string;
};

type TwoFactorChallengeProps = {
    type: "EMAIL" | "TOTP";
    email: string;
    isLoading?: boolean;
    isResending?: boolean;
    onSubmit: (payload: TwoFactorPayload) => void;
    onCancel: () => void;
    onResend?: () => void;
    resetFieldsKey?: number;
};

export function TwoFactorChallenge({
    type,
    email,
    isLoading,
    onSubmit,
    onCancel,
    onResend,
    isResending,
    resetFieldsKey = 0,
}: TwoFactorChallengeProps) {
    const [useBackupCode, setUseBackupCode] = useState(false);
    const [twoFACode, setTwoFACode] = useState("");
    const [backupCode, setBackupCode] = useState("");
    const [localError, setLocalError] = useState<string | null>(null);
    const [resendTimer, setResendTimer] = useState(0);

    const instruction =
        type === "EMAIL"
            ? `We sent a verification code to ${email}. Enter it below to continue.`
            : "Open your authenticator app and enter the current code.";

    function resetInputs() {
        setTwoFACode("");
        setBackupCode("");
        setLocalError(null);
        setUseBackupCode(false);
    }

    function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setLocalError(null);
        const trimmedCode = useBackupCode
            ? backupCode.trim().toUpperCase()
            : twoFACode.trim();
        if (!trimmedCode) {
            setLocalError(
                useBackupCode
                    ? "Enter one of your backup codes."
                    : "Enter the current verification code.",
            );
            return;
        }
        onSubmit(
            useBackupCode
                ? { backupCode: trimmedCode }
                : { twoFACode: trimmedCode },
        );
    }

    function switchToBackup() {
        setUseBackupCode(true);
        setTwoFACode("");
        setLocalError(null);
    }

    function backToCode() {
        setUseBackupCode(false);
        setBackupCode("");
        setLocalError(null);
    }

    function startResendTimer() {
        setResendTimer(30);
    }

    function handleResendClick() {
        if (!onResend || resendTimer > 0 || isLoading) return;
        onResend();
        startResendTimer();
    }

    useEffect(() => {
        if (resendTimer <= 0) return;
        const interval = window.setInterval(
            () =>
                setResendTimer((prev) => {
                    if (prev <= 1) {
                        window.clearInterval(interval);
                        return 0;
                    }
                    return prev - 1;
                }),
            1000,
        );
        return () => window.clearInterval(interval);
    }, [resendTimer]);

    useEffect(() => {
        resetInputs();
        setResendTimer(0);
    }, [type, email, resetFieldsKey]);

    return (
        <LoginLayout>
            <form onSubmit={handleSubmit}>
                <Card className="lg:w-[400px] bg-secondary">
                    <CardContent className="pt-10">
                        <div className="flex flex-col gap-5">
                            <div className="flex flex-col gap-1 text-center">
                                <h2 className="text-xl font-semibold">
                                    Two-Factor Authentication
                                </h2>
                                <p className="text-sm text-muted-foreground">
                                    {instruction}
                                </p>
                            </div>
                            <div className="flex flex-col gap-3">
                                {useBackupCode ? (
                                    <>
                                        <Label htmlFor="backup-code">
                                            Backup code
                                        </Label>
                                        <Input
                                            id="backup-code"
                                            placeholder="XXXX-XXXX-XXXX"
                                            value={backupCode}
                                            onChange={(e) =>
                                                setBackupCode(
                                                    e.target.value
                                                        .toUpperCase()
                                                        .trim(),
                                                )
                                            }
                                            autoComplete="one-time-code"
                                        />
                                        <button
                                            type="button"
                                            className="text-xs text-primary underline underline-offset-2 text-left"
                                            onClick={backToCode}>
                                            Use a verification code instead
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        <Label htmlFor="twofa-code">
                                            Verification code
                                        </Label>
                                        <Input
                                            id="twofa-code"
                                            placeholder="123456"
                                            value={twoFACode}
                                            onChange={(e) =>
                                                setTwoFACode(
                                                    e.target.value.trim(),
                                                )
                                            }
                                            inputMode="numeric"
                                            autoComplete="one-time-code"
                                        />
                                        <button
                                            type="button"
                                            className="text-xs text-primary underline underline-offset-2 text-left"
                                            onClick={switchToBackup}>
                                            Lost your device? Use a backup code
                                            instead
                                        </button>
                                    </>
                                )}
                                {localError && (
                                    <p className="text-sm text-destructive">
                                        {localError}
                                    </p>
                                )}
                            </div>
                            <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
                                <Button
                                    type="button"
                                    variant="tertiary"
                                    onClick={onCancel}
                                    disabled={isLoading}>
                                    Back to login
                                </Button>
                                {type === "EMAIL" && onResend && (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        disabled={
                                            isResending ||
                                            isLoading ||
                                            resendTimer > 0
                                        }
                                        onClick={handleResendClick}>
                                        {isResending ? (
                                            <Spinner size="tiny" />
                                        ) : resendTimer > 0 ? (
                                            `Resend in ${resendTimer}s`
                                        ) : (
                                            "Resend code"
                                        )}
                                    </Button>
                                )}
                                <Button type="submit" disabled={isLoading}>
                                    {isLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        "Verify"
                                    )}
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </form>
        </LoginLayout>
    );
}

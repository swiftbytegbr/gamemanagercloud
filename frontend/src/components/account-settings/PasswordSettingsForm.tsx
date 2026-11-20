import { type ReactNode, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ManagedForm } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import {
    changePassword,
    disableEmailTwoFA,
    disableTotpTwoFA,
    enableEmailTwoFA,
    setupTotpTwoFA,
} from "@/api/userApi.ts";
import { closeDialog, dialog } from "@/store/dialogStore.tsx";
import { toast } from "sonner";
import { useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { changePasswordSchema } from "@/components/account-settings/schemas.ts";
import { BackupCodesDialog } from "@/components/account-settings/2fa/BackupCodesDialog.tsx";
import { DisableTwoFADialog } from "@/components/account-settings/2fa/DisableTwoFADialog.tsx";
import { TotpSetupDialog } from "@/components/account-settings/2fa/TotpSetupDialog.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { useUserQuery, refetchUserQuery } from "@/store/userStore.ts";

export function PasswordSettingsForm() {
    const [loading, setLoading] = useState(false);
    const [emailLoading, setEmailLoading] = useState(false);
    const [totpLoading, setTotpLoading] = useState(false);
    const router = useRouter();
    const { data: user, isLoading: isUserLoading } = useUserQuery();

    const form = useForm<z.infer<typeof changePasswordSchema>>({
        resolver: zodResolver(changePasswordSchema),
        defaultValues: {
            password: "",
            newPassword: "",
            passwordRepeat: "",
        },
    });

    const twoFAType = user?.twoFAType ?? "NONE";
    const emailEnabled = twoFAType === "EMAIL";
    const totpEnabled = twoFAType === "TOTP";

    const twoFAStatusLabel = useMemo(() => {
        if (twoFAType === "EMAIL") return "Email code";
        if (twoFAType === "TOTP") return "Authenticator app";
        return "Disabled";
    }, [twoFAType]);

    function onSubmit(data: z.infer<typeof changePasswordSchema>) {
        setLoading(true);
        changePassword(data.password, data.newPassword)
            .then(() => {
                closeDialog();
                router.navigate({ to: "/logout?all" });
                toast.success(
                    "Password changed successfully. Please log in again.",
                );
            })
            .finally(() => setLoading(false));
    }

    function showBackupCodes(codes?: string[]) {
        if (!codes?.length) return;
        dialog(<BackupCodesDialog codes={codes} />);
    }

    async function handleEnableEmail() {
        setEmailLoading(true);
        try {
            const response = await enableEmailTwoFA();
            showBackupCodes(response.backupCodes);
            toast.success("Email 2FA enabled.");
            await refetchUserQuery();
        } catch {
            // handled globally
        } finally {
            setEmailLoading(false);
        }
    }

    async function handleStartTotpSetup() {
        setTotpLoading(true);
        try {
            const setup = await setupTotpTwoFA();
            dialog(
                <TotpSetupDialog
                    setup={setup}
                    onVerified={async (codes) => {
                        toast.success("Authenticator 2FA enabled.");
                        await refetchUserQuery();
                        showBackupCodes(codes);
                    }}
                />,
            );
        } catch {
            // ignore
        } finally {
            setTotpLoading(false);
        }
    }

    function openDisableDialog(type: "EMAIL" | "TOTP") {
        if (type === "EMAIL") {
            disableEmailTwoFA().catch(() => {
                /* Requesting code - error already surfaced */
            });
        }
        dialog(
            <DisableTwoFADialog
                type={type}
                onConfirm={async (payload) => {
                    if (type === "EMAIL") {
                        await disableEmailTwoFA(payload);
                        toast.success("Email 2FA disabled.");
                    } else {
                        await disableTotpTwoFA(payload);
                        toast.success("Authenticator 2FA disabled.");
                    }
                    await refetchUserQuery();
                }}
            />,
        );
    }

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <div>
                <h3>Change Password</h3>
                <div className="flex flex-col gap-4 mt-2">
                    <FormInput
                        form={form}
                        label="Old Password"
                        name="password"
                        type="password"
                        required
                    />
                    <FormInput
                        form={form}
                        label="New Password"
                        name="newPassword"
                        type="password"
                        required
                    />
                    <FormInput
                        form={form}
                        label="Repeat Password"
                        name="passwordRepeat"
                        type="password"
                        required
                    />
                    <Button disabled={loading} type="submit">
                        {loading ? <Spinner /> : <>Change Password</>}
                    </Button>
                </div>
            </div>
            <hr />
            <div>
                <h3>Two Factor Authentication</h3>
                <div className="flex flex-col gap-4 mt-2">
                    <div className="flex items-center justify-between text-sm text-muted-foreground">
                        <span>Current status</span>
                        <Badge
                            variant={
                                twoFAType === "NONE" ? "secondary" : "default"
                            }>
                            {isUserLoading ? "Loading…" : twoFAStatusLabel}
                        </Badge>
                    </div>
                    <TwoFaCard
                        title="Email codes"
                        description={`Receive a one-time code via email when signing in. We’ll send the login code to ${user?.email || "your email"}`}
                        enabled={emailEnabled}
                        action={
                            emailEnabled ? (
                                <Button
                                    type="button"
                                    variant="destructive"
                                    onClick={() => openDisableDialog("EMAIL")}
                                    disabled={isUserLoading}>
                                    Disable Email 2FA
                                </Button>
                            ) : (
                                <Button
                                    type="button"
                                    onClick={handleEnableEmail}
                                    disabled={
                                        isUserLoading ||
                                        emailLoading ||
                                        totpEnabled
                                    }>
                                    {emailLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        "Enable Email 2FA"
                                    )}
                                </Button>
                            )
                        }
                        helperText={
                            totpEnabled && !emailEnabled
                                ? "Disable your authenticator 2FA before switching to email."
                                : undefined
                        }
                    />
                    <TwoFaCard
                        title="Authenticator app (TOTP)"
                        description="Use an app like Google Authenticator or 1Password to generate sign-in codes."
                        enabled={totpEnabled}
                        action={
                            totpEnabled ? (
                                <Button
                                    type="button"
                                    variant="destructive"
                                    onClick={() => openDisableDialog("TOTP")}
                                    disabled={isUserLoading}>
                                    Disable Authenticator
                                </Button>
                            ) : (
                                <Button
                                    type="button"
                                    onClick={handleStartTotpSetup}
                                    disabled={
                                        isUserLoading ||
                                        totpLoading ||
                                        emailEnabled
                                    }>
                                    {totpLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        "Enable Authenticator"
                                    )}
                                </Button>
                            )
                        }
                        helperText={
                            emailEnabled && !totpEnabled
                                ? "Disable email 2FA before enabling an authenticator."
                                : undefined
                        }
                    />
                </div>
            </div>
        </ManagedForm>
    );
}

function TwoFaCard({
    title,
    description,
    action,
    enabled,
    helperText,
}: {
    title: string;
    description: string;
    enabled: boolean;
    action: ReactNode;
    helperText?: string;
}) {
    return (
        <div className="border rounded-lg p-4 flex flex-col gap-3">
            <div className="flex flex-col gap-1 text-left">
                <div className="flex items-center justify-between gap-2">
                    <h4 className="font-semibold">{title}</h4>
                    <Badge variant={enabled ? "default" : "secondary"}>
                        {enabled ? "Enabled" : "Disabled"}
                    </Badge>
                </div>
                <p className="text-sm text-muted-foreground">{description}</p>
            </div>
            {action}
            {helperText && (
                <p className="text-xs text-muted-foreground">{helperText}</p>
            )}
        </div>
    );
}

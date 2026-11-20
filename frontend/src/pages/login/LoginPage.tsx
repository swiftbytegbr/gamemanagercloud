import { useAuthStore } from "@/store/authStore.ts";
import { toast } from "sonner";
import { useState } from "react";
import { LoginForm } from "@/components/login/LoginForm.tsx";
import { router } from "@/Router.tsx";
import { login } from "@/api/authApi.ts";
import { AuthGuard } from "@/controller/AuthGuard.tsx";
import { TwoFactorChallenge } from "@/components/login/TwoFactorChallenge.tsx";
import type { AxiosError } from "axios";

export function LoginPage() {
    const [loading, setLoading] = useState(false);
    const [twoFAState, setTwoFAState] = useState<{
        type: "EMAIL" | "TOTP";
        email: string;
        password: string;
        rememberMe: boolean;
    } | null>(null);
    const [twoFALoading, setTwoFALoading] = useState(false);
    const [resendLoading, setResendLoading] = useState(false);
    const [twoFAAttempt, setTwoFAAttempt] = useState(0);

    const markAuthenticated = useAuthStore((state) => state.login);

    function finishLogin() {
        markAuthenticated();
        router.navigate({ to: "/dashboard" });
        toast.success("Login successful");
    }

    function extractErrorKey(error: unknown) {
        const axiosError = error as AxiosError<{ errorKey?: string }>;
        return axiosError?.response?.data?.errorKey;
    }

    function mapTwoFaType(key?: string) {
        if (key === "auth.2fa_required_email") return "EMAIL";
        if (key === "auth.2fa_required_totp") return "TOTP";
        return null;
    }

    async function onSubmit(data: {
        email: string;
        password: string;
        rememberMe: boolean;
    }) {
        setLoading(true);
        try {
            await login(data);
            setTwoFAState(null);
            finishLogin();
        } catch (error) {
            const key = extractErrorKey(error);
            const twoFaType = mapTwoFaType(key);
            if (twoFaType) {
                setTwoFAState({
                    type: twoFaType,
                    email: data.email,
                    password: data.password,
                    rememberMe: data.rememberMe,
                });
                setTwoFAAttempt(0);
            }
        } finally {
            setLoading(false);
        }
    }

    async function handleTwoFASubmit(payload: {
        twoFACode?: string;
        backupCode?: string;
    }) {
        if (!twoFAState) return;
        setTwoFALoading(true);
        try {
            await login({
                email: twoFAState.email,
                password: twoFAState.password,
                rememberMe: twoFAState.rememberMe,
                ...payload,
            });
            setTwoFAState(null);
            setTwoFAAttempt(0);
            finishLogin();
        } catch (error) {
            const key = extractErrorKey(error);
            const shouldStayOnChallenge =
                key === "auth.invalid_2fa_token" ||
                key === "auth.invalid_backup_code" ||
                !!mapTwoFaType(key);
            if (!shouldStayOnChallenge) {
                setTwoFAState(null);
                setTwoFAAttempt(0);
            } else {
                setTwoFAAttempt((prev) => prev + 1);
            }
        } finally {
            setTwoFALoading(false);
        }
    }

    async function handleResendEmailCode() {
        if (!twoFAState || twoFAState.type !== "EMAIL") return;
        setResendLoading(true);
        try {
            await login({
                email: twoFAState.email,
                password: twoFAState.password,
                rememberMe: twoFAState.rememberMe,
            });
        } finally {
            setResendLoading(false);
        }
    }

    function handleCancelTwoFA() {
        setTwoFAState(null);
        setTwoFAAttempt(0);
    }

    return (
        <AuthGuard guard="unauthenticated">
            {twoFAState ? (
                <TwoFactorChallenge
                    type={twoFAState.type}
                    email={twoFAState.email}
                    isLoading={twoFALoading}
                    isResending={resendLoading}
                    onSubmit={handleTwoFASubmit}
                    onCancel={handleCancelTwoFA}
                    onResend={
                        twoFAState.type === "EMAIL"
                            ? handleResendEmailCode
                            : undefined
                    }
                    resetFieldsKey={twoFAAttempt}
                />
            ) : (
                <LoginForm onSubmit={onSubmit} isLoading={loading} />
            )}
        </AuthGuard>
    );
}

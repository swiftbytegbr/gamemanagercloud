import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import {
    LogOut,
    MonitorSmartphone,
    Settings,
    UserRoundCog,
} from "lucide-react";
import { useUserQuery } from "@/store/userStore.ts";
import { dialog } from "@/store/dialogStore.tsx";
import { useRouter } from "@tanstack/react-router";
import { AccountSettingsDialog } from "@/components/account-settings/AccountSettingsDialog.tsx";
import { SessionsDialog } from "@/components/layout/SessionsDialog.tsx";
import { ProfilePicture } from "@/components/custom/ProfilePicture.tsx";

export function UserDropDownMenu() {
    const { data: user } = useUserQuery();

    const router = useRouter();

    return (
        <div className="ml-auto">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <button>
                        <ProfilePicture className="w-10 h-10" user={user} />
                    </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="mr-4 mt-2">
                    <DropdownMenuLabel>
                        {user?.personalInformation.username}
                    </DropdownMenuLabel>
                    <DropdownMenuLabel className="text-muted-foreground">
                        {user?.email}
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                        onClick={() =>
                            dialog(
                                <AccountSettingsDialog
                                    user={user ?? undefined}
                                />,
                                {
                                    height: "lg",
                                    width: "lg",
                                },
                            )
                        }>
                        <Settings />
                        <p>Account Settings</p>
                    </DropdownMenuItem>
                    <DropdownMenuItem
                        onClick={() =>
                            dialog(<SessionsDialog />, {
                                height: "xl",
                                width: "xl",
                            })
                        }>
                        <MonitorSmartphone />
                        <p>Sessions</p>
                    </DropdownMenuItem>
                    {user?.accountType === "ADMIN" && (
                        <DropdownMenuItem
                            onClick={() => (window.location.href = "/admin")}>
                            <UserRoundCog />
                            <p>Admin Page</p>
                        </DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                        onClick={() => router.navigate({ to: "/logout" })}>
                        <LogOut />
                        <p>Logout</p>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

import { useState } from "react";
import {
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs.tsx";
import type { User } from "@/store/userStore.ts";
import { EmailSettingsForm } from "@/components/account-settings/EmailSettingsForm.tsx";
import { PersonalInfoForm } from "@/components/account-settings/PersonalInfoForm.tsx";
import { NotificationsForm } from "@/components/account-settings/NotificationsForm.tsx";
import { PasswordSettingsForm } from "@/components/account-settings/PasswordSettingsForm.tsx";
import { ChangeProfilePicture } from "@/components/account-settings/ChangeProfilePicture.tsx";

let accountSettingsTabMemory: "information" | "notifications" | "security" =
    "information";

export function AccountSettingsDialog({ user }: { user?: User }) {
    const [activeTab, setActiveTab] = useState(accountSettingsTabMemory);

    return (
        <div className="flex flex-col gap-4 min-h-0">
            <DialogHeader>
                <DialogTitle>Account Settings</DialogTitle>
                <DialogDescription className="sr-only">
                    Manage your personal information, notifications, and
                    security.
                </DialogDescription>
            </DialogHeader>
            <Tabs
                value={activeTab}
                onValueChange={(value) => {
                    if (
                        value === "information" ||
                        value === "notifications" ||
                        value === "security"
                    ) {
                        accountSettingsTabMemory = value;
                        setActiveTab(value);
                    }
                }}
                className="flex-1 min-h-0">
                <TabsList className="h-16 overflow-x-auto">
                    <TabsTrigger value="information">
                        Personal Information
                    </TabsTrigger>
                    <TabsTrigger value="notifications">
                        Notifications
                    </TabsTrigger>
                    <TabsTrigger value="security">Security</TabsTrigger>
                </TabsList>
                <div className="flex-1 min-h-0 overflow-y-auto">
                    <TabsContent
                        value="information"
                        className="flex flex-col gap-5 px-2">
                        <ChangeProfilePicture user={user} />
                        <hr className="border-muted" />
                        <EmailSettingsForm />
                        <hr className="border-muted" />
                        <PersonalInfoForm user={user} />
                    </TabsContent>
                    <TabsContent
                        value="notifications"
                        className="flex flex-col gap-5 px-2">
                        <NotificationsForm user={user} />
                    </TabsContent>
                    <TabsContent
                        value="security"
                        className="flex flex-col gap-5 px-2">
                        <PasswordSettingsForm />
                    </TabsContent>
                </div>
            </Tabs>
        </div>
    );
}

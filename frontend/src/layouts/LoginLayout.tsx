import type { ReactNode } from "react";
import DiscordLogo from "@/assets/discord-logo.svg";
import { appConfig } from "@/config.ts";
import { Centered } from "@/components/Centered.tsx";

interface LoginLayoutProps {
    children: ReactNode;
}

export function LoginLayout(props: LoginLayoutProps) {
    return (
        <div className="grid lg:grid-cols-2 h-full lg:h-screen">
            <div className="p-5">
                <Centered>{props.children}</Centered>
            </div>
            <div className="bg-secondary text-center p-5">
                <Centered className="flex-col">
                    <h2 className="text-5xl">Welcome to</h2>
                    <h1 className="text-7xl text-primary font-semibold">
                        Game Manager Cloud
                    </h1>
                    <h4 className="text-2xl text-muted-foreground font-light mt-1">
                        Host your game on your own server
                    </h4>
                    {appConfig.discordUrl && (
                        <a
                            className="mt-16"
                            href={appConfig.discordUrl}
                            target="_blank">
                            <img
                                className="h-24"
                                src={DiscordLogo}
                                alt="Discord Logo"
                            />
                        </a>
                    )}
                </Centered>
            </div>
        </div>
    );
}

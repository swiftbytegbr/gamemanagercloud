import { useState } from "react";

import { StatusIcon } from "@/components/custom/StatusIcon.tsx";
import { ServerSettingsHeaderButtons } from "@/components/servers/settings/ServerSettingsHeaderButtons.tsx";
import { CurrentPlayersElement } from "@/components/servers/CurrentPlayersElement.tsx";
import { ServerStatusElement } from "@/components/servers/ServerStatusElement.tsx";
import { ServerInfoPopoverContent } from "@/components/servers/settings/ServerInfoPopoverContent.tsx";
import { HoverPopover } from "@/components/custom/HoverPopover.tsx";
import type { Server } from "@/store/serverStore.ts";
import { Info } from "lucide-react";

type ServerSettingsHeaderProps = {
    server: Server;
    maxPlayers: number;
    isLoading: boolean;
    isProfileLoading: boolean;
    onSave?: () => void;
    hasChanges?: boolean;
    disableSettings?: boolean;
    disableBackups?: boolean;
};

export function ServerSettingsHeader(props: ServerSettingsHeaderProps) {
    const [isLoading, setLoading] = useState(false);

    const isAnyLoading = props.isLoading || isLoading;

    return (
        <>
            <div className="flex flex-col lg:flex-row gap-3 items-center justify-between">
                <div className="flex items-center gap-10">
                    <div className="flex items-center gap-4">
                        <StatusIcon
                            className="2xl:hidden"
                            status={props.server.state}
                        />
                        <HoverPopover
                            enableClickToOpen
                            delay={-1}
                            wide
                            content={
                                <ServerInfoPopoverContent
                                    server={props.server}
                                />
                            }>
                            <div className="flex gap-2 items-center">
                                <p className="text-2xl font-semibold leading-normal">
                                    {props.server.displayName}
                                    {props.hasChanges ? "*" : ""}
                                </p>
                                <Info />
                            </div>
                        </HoverPopover>
                    </div>
                </div>
                <div className="flex items-center gap-3 overflow-x-auto w-full sm:w-auto">
                    <ServerSettingsHeaderButtons
                        server={props.server}
                        isAnyLoading={isAnyLoading}
                        onSave={props.onSave}
                        setLoading={setLoading}
                        saveDisabled={!props.hasChanges}
                        disableBackups={props.disableBackups}
                        disableSettings={props.disableSettings}
                    />
                </div>
                <CurrentPlayersElement
                    server={props.server}
                    isProfileLoading={props.isProfileLoading}
                    maxPlayers={props.maxPlayers}
                    hiddenWhenSmall
                />
                <ServerStatusElement server={props.server} hiddenWhenSmall />
            </div>
        </>
    );
}

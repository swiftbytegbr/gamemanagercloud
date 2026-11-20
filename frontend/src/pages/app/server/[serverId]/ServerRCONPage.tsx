import { Input } from "@/components/ui/input.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Send } from "lucide-react";
import { useParams } from "@tanstack/react-router";
import { useActiveTeam } from "@/store/teamStore.ts";
import {
    type Server,
    useServerByIdQuery,
    useServerQuery,
    useSettingProfileQuery,
} from "@/store/serverStore.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { NotFoundPage } from "@/pages/NotFoundPage.tsx";
import { format } from "date-fns";
import { useEffect, useState } from "react";
import { sendCommand } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { useMaxPlayers } from "@/hooks/useMaxPlayers.ts";
import { cn } from "@/lib/utils.ts";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.tsx";
import { MultiSelect } from "@/components/ui/multi-select.tsx";
import { ServerSettingsHeader } from "@/components/servers/settings/ServerSettingsHeader.tsx";
import { ServerDirectoryChangeOverlay } from "@/components/servers/ServerDirectoryChangeOverlay.tsx";

type CommandMode =
    | "command"
    | "broadcast"
    | "serverchat"
    | "kickplayer"
    | "saveworld"
    | "destroywilddinos";

export function ServerRCONPage() {
    const [command, setCommand] = useState("");
    const [commandMode, setCommandMode] = useState<CommandMode>("command");
    const [selectedServers, setSelectedServers] = useState<string[]>([]);
    const [isLoading, setLoading] = useState(false);

    const params = useParams({ strict: false });
    const activeTeam = useActiveTeam();

    const { data: servers } = useServerQuery(activeTeam);
    const { data: server, isLoading: isServerLoading } = useServerByIdQuery(
        activeTeam,
        params.serverId ?? "",
    );

    const { data: profile, isLoading: isProfileLoading } =
        useSettingProfileQuery(activeTeam, server?.settingProfileId);

    useEffect(() => {
        setSelectedServers([]);
    }, [server]);
    useEffect(() => {
        if (
            server &&
            server.gameType !== "ARK_EVOLVED" &&
            commandMode === "broadcast"
        ) {
            setCommandMode("command");
        }
    }, [server, commandMode]);
    const resolvedCommandMode: CommandMode =
        !server || server.gameType === "ARK_EVOLVED"
            ? commandMode
            : commandMode === "broadcast"
              ? "command"
              : commandMode;

    const buildCommandForServer = (targetServer: Server) => {
        switch (resolvedCommandMode) {
            case "broadcast":
                return targetServer.gameType === "ARK_ASCENDED"
                    ? `serverchat ${command}`
                    : `broadcast ${command}`;
            case "serverchat":
                return `serverchat ${command}`;
            case "kickplayer":
                return `kickplayer ${command.trim()}`;
            case "saveworld":
                return "SaveWorld";
            case "destroywilddinos":
                return "DestroyWildDinos";
            default:
                return command;
        }
    };

    const onSubmit = () => {
        if (!server || server.state != "ONLINE") return;
        if (
            resolvedCommandMode === "kickplayer" &&
            command.trim().length === 0
        ) {
            toast.error("Please enter a target ID first.");
            return;
        }

        setLoading(true);

        selectedServers.forEach((serverId) => {
            const targetServer = servers?.find((it) => it.id === serverId);
            if (!targetServer) return;

            sendCommand(serverId, buildCommandForServer(targetServer)).then(
                () => {
                    toast.success(
                        `Command sent to server with id ${serverId} successfully!`,
                    );
                },
            );
        });

        sendCommand(server.id, buildCommandForServer(server))
            .then(() => {
                toast.success("Command sent successfully!");
                setCommand("");
            })
            .finally(() => setLoading(false));
    };

    const getColorizedCommandResponse = (
        cmd: string,
        isDefaultGray?: boolean,
    ) => {
        if (cmd.toLowerCase().includes("success")) {
            return <span className="text-green-600">{cmd}</span>;
        } else if (cmd.toLowerCase().includes("error")) {
            return <span className="text-red-600">{cmd}</span>;
        } else if (
            cmd
                .toLowerCase()
                .includes(
                    "Command received, but the server returned no output.".toLowerCase(),
                )
        ) {
            return (
                <span className="text-orange-500">
                    {"Command received, but the server returned no output."}
                </span>
            );
        } else {
            return (
                <span
                    className={cn(
                        isDefaultGray
                            ? "text-gray-400"
                            : "text-primary-foreground",
                    )}>
                    {cmd}
                </span>
            );
        }
    };

    const getColorizedCommand = (cmd: string) => {
        if (
            cmd.toLowerCase().includes("broadcast") ||
            cmd.toLowerCase().includes("serverchat")
        ) {
            return <span className="text-yellow-300">{cmd}</span>;
        } else {
            return <>{cmd}</>;
        }
    };

    const maxPlayers = useMaxPlayers(server, profile);

    if (isServerLoading) {
        return (
            <div className="w-full h-full flex justify-center items-center">
                <Spinner size="large" />
            </div>
        );
    }

    if (!server) {
        return <NotFoundPage />;
    }

    return (
        <ServerDirectoryChangeOverlay
            key={params.serverId}
            server={server}
            className="w-full h-full py-0 flex flex-col">
            <ServerSettingsHeader
                server={server}
                maxPlayers={maxPlayers}
                isLoading={isLoading}
                isProfileLoading={isProfileLoading}
                disableSettings
            />
            <div className="flex justify-center lg:justify-end h-9 items-center mb-4 gap-8 mt-5">
                <MultiSelect
                    className="hidden lg:block mr-auto w-[400px]"
                    placeholder="Select additional servers to receive commands"
                    emptyText="No online servers found"
                    onValueChange={setSelectedServers}
                    options={(servers ?? [])
                        .filter(
                            (it) => it.id != server.id && it.state === "ONLINE",
                        )
                        .map((it) => ({
                            label: it.displayName,
                            value: `${it.id}`,
                        }))}
                />
            </div>
            <div className="w-full h-0 grow bg-secondary rounded-md pt-4 flex flex-col">
                <div className="grow px-4 overflow-y-auto flex flex-col-reverse">
                    {(server.commands ?? [])
                        .slice()
                        .reverse()
                        .map((command) => {
                            return (
                                <div
                                    key={command.timestamp}
                                    className="font-mono">
                                    <p className="text-primary-foreground">
                                        <span className="text-gray-500 text-nowrap">
                                            {`[${format(
                                                new Date(command.timestamp),
                                                "MM/dd/yyyy HH:mm:ss",
                                            )}] `}
                                        </span>
                                        {getColorizedCommand(command.command)}
                                    </p>
                                    <p className="text-gray-400">
                                        <span className="text-gray-500 text-nowrap">
                                            {`[${format(
                                                new Date(command.timestamp),
                                                "MM/dd/yyyy HH:mm:ss",
                                            )}] `}
                                        </span>
                                        {getColorizedCommandResponse(
                                            command.response,
                                            true,
                                        )}
                                    </p>
                                </div>
                            );
                        })}
                </div>
                <form
                    className="p-4 lg:flex gap-3"
                    onSubmit={(event) => {
                        event.preventDefault();
                        onSubmit();
                    }}>
                    <Select
                        disabled={isLoading || server.state != "ONLINE"}
                        onValueChange={(value) =>
                            setCommandMode(value as CommandMode)
                        }
                        value={resolvedCommandMode}
                        defaultValue="command">
                        <SelectTrigger className="w-full mb-3 lg:w-60 lg:mb-0">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="command">
                                Mode: Command
                            </SelectItem>
                            {server.gameType === "ARK_EVOLVED" && (
                                <SelectItem value="broadcast">
                                    Mode: Broadcast
                                </SelectItem>
                            )}
                            <SelectItem value="serverchat">
                                Mode: ServerChat
                            </SelectItem>
                            <SelectItem value="kickplayer">
                                Mode: KickPlayer
                            </SelectItem>
                            <SelectItem value="saveworld">
                                Mode: SaveWorld
                            </SelectItem>
                            <SelectItem value="destroywilddinos">
                                Mode: DestroyWildDinos
                            </SelectItem>
                        </SelectContent>
                    </Select>
                    <div className="flex gap-3 w-full">
                        <Input
                            disabled={
                                isLoading ||
                                server.state != "ONLINE" ||
                                resolvedCommandMode === "saveworld" ||
                                resolvedCommandMode === "destroywilddinos"
                            }
                            placeholder={
                                resolvedCommandMode === "broadcast"
                                    ? "Write your message..."
                                    : resolvedCommandMode === "kickplayer"
                                      ? "Enter target ID..."
                                      : resolvedCommandMode === "saveworld" ||
                                          resolvedCommandMode ===
                                              "destroywilddinos"
                                        ? "No input required"
                                        : "Enter command..."
                            }
                            value={command}
                            onChange={(val) => setCommand(val.target.value)}
                            variant="background"
                            className="w-full"
                        />
                        <Button
                            aria-label="Send command"
                            disabled={
                                isLoading ||
                                server.state != "ONLINE" ||
                                (resolvedCommandMode === "kickplayer" &&
                                    command.trim().length === 0)
                            }
                            type="submit"
                            size="icon"
                            variant="input">
                            {isLoading ? <Spinner size="small" /> : <Send />}
                        </Button>
                    </div>
                </form>
            </div>
        </ServerDirectoryChangeOverlay>
    );
}

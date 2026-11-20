import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { useVersionInfoQuery } from "@/store/infoStore.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useServerQuery } from "@/store/serverStore.ts";
import { hasPermission, useActiveTeam } from "@/store/teamStore.ts";
import { ServersTable } from "@/components/servers/table/ServersTable.tsx";
import { useUserQuery } from "@/store/userStore.ts";
import { sanitizeHtml } from "@/lib/sanitizeHtml.ts";

export function DashboardPage() {
    const { data: versionInfo, isFetching: isLoadingVersionInfo } =
        useVersionInfoQuery();

    const { data: servers, isLoading: isLoadingServers } =
        useServerQuery(useActiveTeam());

    const { data: user } = useUserQuery();

    const activeTeam = useActiveTeam();

    return (
        <div className="h-full">
            <div className="h-full min-h-fit w-full grid lg:grid-cols-[1.5fr_1.5fr_1fr] lg:grid-rows-[1fr_1.5fr] lg:overflow-hidden gap-8">
                <Card className="min-h-fit bg-secondary overflow-hidden">
                    <CardHeader>
                        <CardTitle className="uppercase">
                            <h3>Online Maps</h3>
                        </CardTitle>
                        <CardDescription>
                            <h3 className="text-6xl font-normal">
                                {isLoadingServers ? (
                                    <Spinner />
                                ) : (
                                    servers?.filter(
                                        (server) => server.state === "ONLINE",
                                    ).length || 0
                                )}
                            </h3>
                        </CardDescription>
                    </CardHeader>
                </Card>
                <Card className="min-h-fit bg-secondary overflow-hidden">
                    <CardHeader>
                        <CardTitle className="uppercase">
                            <h3>Online Players</h3>
                        </CardTitle>
                        <CardDescription>
                            <h3 className="text-6xl font-normal">
                                {isLoadingServers ? (
                                    <Spinner />
                                ) : (
                                    servers?.reduce(
                                        (acc, server) =>
                                            acc + server.onlinePlayers,
                                        0,
                                    ) || 0
                                )}
                            </h3>
                        </CardDescription>
                    </CardHeader>
                </Card>
                <Card className="gap-2 overflow-hidden max-h-[400px] lg:max-h-full bg-tertiary">
                    {isLoadingVersionInfo ? (
                        <Spinner />
                    ) : (
                        <>
                            <div className="overflow-y-scroll">
                                <CardHeader>
                                    <CardTitle className="uppercase">
                                        <h3>Version</h3>
                                    </CardTitle>
                                    <CardDescription>
                                        <h3 className="text-6xl font-normal">
                                            {versionInfo?.version}
                                        </h3>
                                    </CardDescription>
                                </CardHeader>
                                <CardContent className="text-sm">
                                    <p>{versionInfo?.description}</p>
                                    <hr />
                                    <div
                                        dangerouslySetInnerHTML={{
                                            __html: sanitizeHtml(
                                                (
                                                    versionInfo?.changelog ?? ""
                                                ).replace(/\n/g, "<br />"),
                                            ),
                                        }}></div>
                                </CardContent>
                            </div>
                        </>
                    )}
                </Card>
                <div className="lg:col-span-3 bg-secondary rounded-md flex flex-col pt-2 overflow-y-auto">
                    {hasPermission(activeTeam, user?.id, "ACCESS_SERVERS") ? (
                        <ServersTable />
                    ) : (
                        <div className="flex w-full h-full p-16 justify-center items-center text-center">
                            <h2>
                                The Server Table is disabled because you are
                                missing the permission to view servers. Please
                                ask you team admin if you think this is a
                                mistake.
                            </h2>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

import { appTableFeatures, createColumnHelper } from "@/lib/table.ts";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { Link } from "@tanstack/react-router";
import {
    DEFAULT_ACTION_LOG_PAGE_SIZE,
    type LogEntry,
    type LogEntryType,
    type Team,
    type TeamMember,
    getTeamMemberMap,
    useTeamActionLogInfiniteQuery,
} from "@/store/teamStore.ts";
import { ProfilePicture } from "@/components/custom/ProfilePicture.tsx";
import { cn } from "@/lib/utils.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { flexRender, useTable } from "@tanstack/react-table";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.tsx";
import { differenceInMinutes, format, formatDistanceToNow } from "date-fns";
import { type Node, useNodeQuery } from "@/store/nodeStore.ts";
import { type Server, useServerQuery } from "@/store/serverStore.ts";
import { ExternalLink, Network, ServerIcon } from "lucide-react";
import { TeamPicture } from "@/components/custom/TeamPicture.tsx";

type ActionLogTableProps = {
    team: Team;
};

type ActionIntent = "added" | "edited" | "deleted" | "other";

type AffectedInfo =
    | {
          kind: "server" | "node";
          id: string;
          name: string;
          linkTarget: string | null;
      }
    | {
          kind: "user";
          name: string;
          imageId?: string;
      }
    | {
          kind: "team";
          name: string;
      }
    | {
          kind: "other";
          name: string;
      }
    | null;

type ActionLogRow = LogEntry & {
    intent: ActionIntent;
    typeLabel: string;
    issuer: TeamMember | null;
    affected: AffectedInfo;
};

const ADD_TYPES: LogEntryType[] = [
    "SERVER_CREATION",
    "SERVER_BACKUP",
    "NODE_CREATION",
    "NODE_REGISTRATION",
    "TEAM_CREATION",
    "TEAM_INVITATION",
    "TEAM_JOIN",
    "TEAM_APPLICATION_CREATION",
];

const DELETE_TYPES: LogEntryType[] = [
    "SERVER_DELETION",
    "SERVER_BACKUP_DELETION",
    "NODE_DELETION",
    "TEAM_LEAVE",
    "TEAM_KICK",
    "TEAM_APPLICATION_DELETION",
];

const EDIT_TYPES: LogEntryType[] = [
    "SERVER_SETTINGS_CHANGE",
    "SERVER_START",
    "SERVER_STOP",
    "SERVER_RESTART",
    "SERVER_RCON_COMMAND",
    "SERVER_ROLLBACK",
    "SERVER_UPDATE",
    "NODE_SETTINGS_CHANGE",
    "NODE_UPDATE",
    "TEAM_PERMISSION_CHANGE",
    "TEAM_NAME_CHANGE",
    "TEAM_ICON_CHANGE",
    "SETTING_PROFILE_UPDATE",
];

const addTypeSet = new Set<LogEntryType>(ADD_TYPES);
const deleteTypeSet = new Set<LogEntryType>(DELETE_TYPES);
const editTypeSet = new Set<LogEntryType>(EDIT_TYPES);

const intentColor: Record<ActionIntent, string> = {
    added: "bg-emerald-500",
    edited: "bg-sky-500",
    deleted: "bg-red-500",
    other: "bg-muted-foreground/70",
};

const intentLabel: Record<ActionIntent, string> = {
    added: "Added",
    edited: "Edited",
    deleted: "Deleted",
    other: "Action",
};

const getIntent = (type: LogEntryType): ActionIntent => {
    if (addTypeSet.has(type)) return "added";
    if (deleteTypeSet.has(type)) return "deleted";
    if (editTypeSet.has(type)) return "edited";
    return "other";
};

const toSentenceCase = (type: LogEntryType) => {
    return type
        .toLowerCase()
        .split("_")
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(" ");
};

const formatRelativeTime = (timestamp: string) => {
    const parsed = new Date(timestamp);
    if (Number.isNaN(parsed.getTime())) {
        return "Unknown time";
    }
    const minutesSince = differenceInMinutes(new Date(), parsed);
    if (minutesSince >= 60) {
        return format(parsed, "PPpp");
    }
    return formatDistanceToNow(parsed, { addSuffix: true });
};

const isTeamChange = (entry: LogEntry): boolean => {
    return entry.type.includes("TEAM_");
};

const actionLogColumnHelper = createColumnHelper<ActionLogRow>();

export function ActionLogTable(props: ActionLogTableProps) {
    const containerRef = useRef<HTMLDivElement | null>(null);
    const loadMoreRef = useRef<HTMLDivElement | null>(null);
    const { data: servers } = useServerQuery(props.team);
    const { data: nodes } = useNodeQuery(props.team);

    const {
        data,
        isLoading,
        isError,
        error,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        isFetching,
    } = useTeamActionLogInfiniteQuery(
        props.team.id,
        DEFAULT_ACTION_LOG_PAGE_SIZE,
    );

    const memberById = useMemo(
        () => getTeamMemberMap(props.team),
        [props.team],
    );

    const serverById = useMemo(() => {
        return new Map<string, Server>(
            (servers ?? []).map((server) => [server.id, server]),
        );
    }, [servers]);

    const nodeById = useMemo(() => {
        return new Map<string, Node>(
            (nodes ?? []).map((node) => [node.id, node]),
        );
    }, [nodes]);

    const flattenedEntries = useMemo<LogEntry[]>(() => {
        const pages = data?.pages ?? [];
        const entries = pages.flatMap((page) => page.content ?? []) ?? [];
        return entries
            .slice()
            .sort(
                (a, b) =>
                    new Date(b.timestamp).getTime() -
                    new Date(a.timestamp).getTime(),
            );
    }, [data?.pages]);

    const tableData = useMemo<ActionLogRow[]>(() => {
        return flattenedEntries.map((entry) => {
            const intent = getIntent(entry.type);
            const issuer = memberById.get(entry.issuerId) ?? null;

            let affected: AffectedInfo = null;
            if (entry.affectedUserId) {
                const member = memberById.get(entry.affectedUserId);
                affected = {
                    kind: "user",
                    name: member?.name ?? "Unknown User",
                    imageId: member?.imageId,
                };
            } else if (entry.affectedServerId) {
                const serverId = entry.affectedServerId;
                const server = serverById.get(serverId);
                affected = {
                    kind: "server",
                    id: serverId,
                    name: server?.displayName ?? `Unknown Server`,
                    linkTarget: server ? `/server/${serverId}` : null,
                };
            } else if (entry.affectedNodeId) {
                const nodeId = entry.affectedNodeId;
                const node = nodeById.get(nodeId);
                affected = {
                    kind: "node",
                    id: nodeId,
                    name: node?.settings.name ?? `Unknown Node`,
                    linkTarget: node ? `/node/${nodeId}` : null,
                };
            } else if (isTeamChange(entry)) {
                affected = {
                    kind: "team",
                    name: props.team.name,
                };
            } else if (entry.actionInformation) {
                affected = {
                    kind: "other",
                    name: entry.actionInformation,
                };
            }

            return {
                ...entry,
                intent,
                typeLabel: toSentenceCase(entry.type),
                issuer,
                affected,
            };
        });
    }, [flattenedEntries, memberById, nodeById, props.team.name, serverById]);

    useEffect(() => {
        const container = containerRef.current;
        const sentinel = loadMoreRef.current;
        if (!container || !sentinel) return;

        const observer = new IntersectionObserver(
            (entries) => {
                const entry = entries[0];
                if (
                    entry?.isIntersecting &&
                    hasNextPage &&
                    !isFetchingNextPage
                ) {
                    fetchNextPage();
                }
            },
            {
                root: container,
                rootMargin: "0px 0px 200px 0px",
                threshold: 0.1,
            },
        );

        observer.observe(sentinel);

        return () => {
            observer.disconnect();
        };
    }, [fetchNextPage, hasNextPage, isFetchingNextPage, props.team.id]);

    const hasEntries = tableData.length > 0;

    const renderAffected = useCallback(
        (affected: AffectedInfo) => {
            if (!affected) {
                return <span className="text-muted-foreground">—</span>;
            }

            if (affected.kind === "user") {
                return (
                    <div className="flex items-center gap-3">
                        <ProfilePicture
                            className="size-10"
                            imageId={affected.imageId}
                        />
                        <span className="font-medium leading-tight">
                            {affected.name}
                        </span>
                    </div>
                );
            }

            if (affected.kind === "team") {
                return (
                    <div className="flex items-center gap-3">
                        <TeamPicture className="size-10" team={props.team} />
                        <span className="font-medium leading-tight">
                            {affected.name}
                        </span>
                    </div>
                );
            }

            if (affected.kind === "server" || affected.kind === "node") {
                const Icon = affected.kind === "server" ? ServerIcon : Network;
                const target =
                    affected.kind === "server"
                        ? `/server/${affected.id}/settings`
                        : `/node/${affected.id}/settings`;

                return (
                    <div className="flex items-center gap-3">
                        <span className="flex size-10 items-center justify-center rounded-full bg-tertiary">
                            <Icon className="h-5 w-5" />
                        </span>
                        <div className="flex items-center gap-2">
                            <span className="font-medium leading-tight">
                                {affected.name}
                            </span>
                            <Link
                                to={target}
                                className="text-muted-foreground transition-colors hover:text-primary"
                                aria-label={`Open ${affected.kind} details`}>
                                <ExternalLink className="h-4 w-4" />
                            </Link>
                        </div>
                    </div>
                );
            }

            return (
                <div className="flex items-center gap-3">
                    <span className="font-medium leading-tight">
                        {affected.name}
                    </span>
                </div>
            );
        },
        [props.team],
    );

    const columns = useMemo(
        () =>
            actionLogColumnHelper.columns([
                actionLogColumnHelper.display({
                    id: "action",
                    header: "Action",
                    cell: (info) => {
                        const row = info.row.original;
                        return (
                            <div className="flex items-center gap-3">
                                <span
                                    className={cn(
                                        "h-3 w-3 rounded-full",
                                        intentColor[row.intent],
                                    )}
                                />
                                <div className="flex flex-col">
                                    <span className="font-medium leading-tight">
                                        {row.typeLabel}
                                    </span>
                                    <span className="text-xs text-muted-foreground">
                                        {intentLabel[row.intent]}
                                    </span>
                                </div>
                            </div>
                        );
                    },
                }),
                actionLogColumnHelper.display({
                    id: "time",
                    header: "Time",
                    cell: (info) => (
                        <span className="text-muted-foreground">
                            {formatRelativeTime(info.row.original.timestamp)}
                        </span>
                    ),
                }),
                actionLogColumnHelper.display({
                    id: "issuer",
                    header: "Actor",
                    cell: (info) => {
                        const row = info.row.original;
                        return (
                            <div className="flex items-center gap-3">
                                <ProfilePicture
                                    className="size-10"
                                    imageId={row.issuer?.imageId}
                                />
                                <div className="flex flex-col">
                                    <span className="font-medium leading-tight">
                                        {row.issuer?.name ?? "Unknown User"}
                                    </span>
                                    {row.issuer && (
                                        <span className="text-xs text-muted-foreground">
                                            {row.issuer.email}
                                        </span>
                                    )}
                                </div>
                            </div>
                        );
                    },
                }),
                actionLogColumnHelper.display({
                    id: "affected",
                    header: "Affected",
                    cell: (info) => renderAffected(info.row.original.affected),
                }),
                actionLogColumnHelper.display({
                    id: "details",
                    header: "Details",
                    cell: (info) => {
                        const details = info.row.original.actionInformation;
                        return details ? (
                            <span className="text-sm leading-tight">
                                {details}
                            </span>
                        ) : (
                            <span className="text-muted-foreground">—</span>
                        );
                    },
                }),
            ]),
        [renderAffected],
    );

    const table = useTable({
        features: appTableFeatures,
        manualPagination: true,
        data: tableData,
        columns,
    });

    return (
        <div className="h-full flex flex-col overflow-hidden">
            <div
                ref={containerRef}
                className="relative grow overflow-y-auto overflow-x-auto">
                {isFetching && !isFetchingNextPage && !isLoading && (
                    <div className="pointer-events-none absolute left-1/2 top-4 z-10 -translate-x-1/2 rounded-full bg-background/80 px-4 py-1 shadow">
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <Spinner size="small" />
                            <span>Refreshing…</span>
                        </div>
                    </div>
                )}
                <Table className="border-b overflow-x-hidden">
                    <TableHeader className="bg-primary-foreground/30">
                        {table.getHeaderGroups().map((headerGroup) => (
                            <TableRow key={headerGroup.id}>
                                {headerGroup.headers.map((header) => (
                                    <TableHead key={header.id}>
                                        {header.isPlaceholder
                                            ? null
                                            : flexRender(
                                                  header.column.columnDef
                                                      .header,
                                                  header.getContext(),
                                              )}
                                    </TableHead>
                                ))}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {isError && !hasEntries ? (
                            <TableRow>
                                <TableCell
                                    colSpan={5}
                                    className="h-24 text-center text-destructive">
                                    {error?.message ??
                                        "Failed to load action log."}
                                </TableCell>
                            </TableRow>
                        ) : isLoading && !hasEntries ? (
                            <TableRow>
                                <TableCell colSpan={5} className="h-32">
                                    <div className="flex items-center justify-center">
                                        <Spinner size="large" />
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : !hasEntries ? (
                            <TableRow>
                                <TableCell
                                    colSpan={5}
                                    className="h-24 text-center text-muted-foreground">
                                    No actions recorded yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            table.getRowModel().rows.map((row) => (
                                <TableRow key={row.id}>
                                    {row
                                        .getVisibleCells()
                                        .map((cell, index) => (
                                            <TableCell
                                                key={cell.id}
                                                className={cn(
                                                    index === 0 ? "pl-5" : "",
                                                )}>
                                                {flexRender(
                                                    cell.column.columnDef.cell,
                                                    cell.getContext(),
                                                )}
                                            </TableCell>
                                        ))}
                                </TableRow>
                            ))
                        )}
                        {isFetchingNextPage && hasEntries && (
                            <TableRow>
                                <TableCell colSpan={5}>
                                    <div className="flex items-center justify-center py-4">
                                        <Spinner size="medium" />
                                    </div>
                                </TableCell>
                            </TableRow>
                        )}
                        {!hasNextPage && hasEntries && (
                            <TableRow>
                                <TableCell
                                    colSpan={5}
                                    className="py-4 text-center text-xs text-muted-foreground">
                                    You have reached the end of the action log.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                <div ref={loadMoreRef} className="h-2 w-full" />
            </div>
        </div>
    );
}

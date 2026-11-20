export const queryKeys = {
    user: {
        all: ["user"] as const,
    },
    sessions: {
        all: ["sessions"] as const,
    },
    versionInfo: {
        all: ["versionInfo"] as const,
    },
    teams: {
        all: ["teams"] as const,
    },
    nodes: {
        all: ["nodes"] as const,
        list: (teamId?: string | null) =>
            ["nodes", { teamId: teamId ?? null }] as const,
    },
    nodeTasks: {
        all: ["nodeTasks"] as const,
        list: (teamId?: string | null, nodeId?: string | null) =>
            [
                "nodeTasks",
                { teamId: teamId ?? null, nodeId: nodeId ?? null },
            ] as const,
    },
    latestNodeVersion: {
        all: ["latestNodeVersion"] as const,
    },
    servers: {
        all: ["servers"] as const,
        list: (teamId?: string | null) =>
            ["servers", { teamId: teamId ?? null }] as const,
    },
    settingProfiles: {
        all: ["settingProfiles"] as const,
        detail: (teamId?: string | null, settingProfileId?: string | null) =>
            [
                "settingProfiles",
                {
                    teamId: teamId ?? null,
                    settingProfileId: settingProfileId ?? null,
                },
            ] as const,
    },
    settingTemplates: {
        all: ["settingTemplates"] as const,
        list: (gameType?: string | null) =>
            ["settingTemplates", { gameType: gameType ?? null }] as const,
    },
    apiKeys: {
        all: ["apiKeys"] as const,
        list: (teamId?: string | null) =>
            ["apiKeys", { teamId: teamId ?? null }] as const,
    },
    teamActionLog: {
        all: ["teamActionLog"] as const,
        list: (teamId?: string | null, pageSize?: number) =>
            ["teamActionLog", { teamId: teamId ?? null, pageSize }] as const,
    },
    automations: {
        all: ["automations"] as const,
        list: (teamId?: string | null) =>
            ["automations", { teamId: teamId ?? null }] as const,
    },
};

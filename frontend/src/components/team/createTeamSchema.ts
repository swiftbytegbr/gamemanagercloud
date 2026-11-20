import { z } from "zod";

export const createTeamSchema = z.object({
    teamName: z
        .string({
            message: "Team name is required",
        })
        .min(1, {
            message: "Team name is required",
        })
        .min(3, {
            message: "Team name must be at least 3 characters long",
        })
        .max(40, {
            message: "Team name must be at most 40 characters long",
        }),
});

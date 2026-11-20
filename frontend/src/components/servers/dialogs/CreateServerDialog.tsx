import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { FormSelect } from "@/components/custom/form/FormSelect.tsx";
import { useNodeQuery } from "@/store/nodeStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { Button } from "@/components/ui/button.tsx";
import { createServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/spinner.tsx";
import { useState } from "react";
import { closeDialog } from "@/store/dialogStore.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { queryKeys } from "@/store/queryKeys.ts";
import { upsertListItem } from "@/store/queryCache.ts";
import type { Server } from "@/store/serverStore.ts";

const createServerSchema = z.object({
    friendlyName: z
        .string({
            message: "Friendly name is required",
        })
        .min(1, {
            message: "Friendly name is required",
        })
        .min(3, {
            message: "Friendly name must be at least 3 characters long",
        })
        .max(16, {
            message: "Friendly name must be at most 16 characters long",
        }),
    selectGame: z
        .string({
            message: "You must select a game",
        })
        .min(1, {
            message: "You must select a game",
        }),
    enablePreaquaticaBeta: z.boolean().optional(),
    selectNode: z
        .string({
            message: "You must select a node",
        })
        .min(1, {
            message: "You must select a node",
        }),
    serverFolder: z.string().optional(),
});

export function CreateServerDialog() {
    const [isLoading, setLoading] = useState(false);

    const activeTeam = useActiveTeam();
    const { data: nodes } = useNodeQuery(activeTeam);

    const form = useForm<z.infer<typeof createServerSchema>>({
        resolver: zodResolver(createServerSchema),
        defaultValues: {
            friendlyName: "",
            serverFolder: "",
            enablePreaquaticaBeta: false,
        },
    });

    const onSubmit = (data: z.infer<typeof createServerSchema>) => {
        setLoading(true);
        createServer(
            data.selectNode,
            data.friendlyName,
            data.selectGame,
            data.enablePreaquaticaBeta ?? false,
            data.serverFolder,
        )
            .then((server: Server) => {
                if (activeTeam?.id) {
                    upsertListItem(
                        queryKeys.servers.list(activeTeam.id),
                        server,
                    );
                }
                toast.success("Server created successfully.");
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    const onSubmitAnother = (data: z.infer<typeof createServerSchema>) => {
        setLoading(true);
        createServer(
            data.selectNode,
            data.friendlyName,
            data.selectGame,
            data.enablePreaquaticaBeta ?? false,
            data.serverFolder,
        )
            .then((server: Server) => {
                if (activeTeam?.id) {
                    upsertListItem(
                        queryKeys.servers.list(activeTeam.id),
                        server,
                    );
                }
                toast.success("Server created successfully.");
            })
            .finally(() => setLoading(false));
    };

    const gameType = form.watch("selectGame");
    const node = form.watch("selectNode");

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Create a Server</DialogTitle>
                <DialogDescription>
                    Create a new game server / map on a node
                </DialogDescription>
            </DialogHeader>
            <div className="space-y-5">
                <FormInput
                    form={form}
                    label="Friendly Name"
                    name="friendlyName"
                    required
                />
                <FormSelect
                    form={form}
                    label="Select a game"
                    name="selectGame"
                    required
                    items={[
                        {
                            value: "ARK_EVOLVED",
                            label: "ARK: Survival Evolved",
                        },
                        {
                            value: "ARK_ASCENDED",
                            label: "ARK: Survival Ascended",
                        },
                    ]}
                />
                <FormSelect
                    form={form}
                    label="Select a node"
                    name="selectNode"
                    emptyText="No node was found that is online."
                    required
                    items={nodes
                        ?.filter((node) => node.status === "ONLINE")
                        .map((node) => ({
                            value: node.id,
                            label: node.settings.name,
                        }))}
                />
                <FormInput
                    form={form}
                    name="serverFolder"
                    label="Server directory"
                    placeholder={
                        nodes?.find((it) => it.id === node)?.settings
                            .defaultServerDirectory ?? ""
                    }
                />
                {gameType === "ARK_EVOLVED" && (
                    <FormCheckbox
                        form={form}
                        name="enablePreaquaticaBeta"
                        label="Enable Preaquatica Beta"
                        description="Check this to enable the Preaquatica Beta. You can always change this setting later, but it will require a server update."
                    />
                )}
            </div>
            <DialogFooter className="mt-5">
                <div className="w-full flex flex-col gap-3">
                    <Button
                        disabled={isLoading}
                        className="uppercase"
                        type="button"
                        variant="tertiary"
                        onClick={form.handleSubmit(onSubmitAnother)}>
                        {isLoading ? <Spinner /> : <>Add another server</>}
                    </Button>
                    <Button
                        disabled={isLoading}
                        className="uppercase"
                        type="submit">
                        {isLoading ? <Spinner /> : <>Create a server</>}
                    </Button>
                </div>
            </DialogFooter>
        </ManagedForm>
    );
}

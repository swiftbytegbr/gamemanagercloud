import {
    refetchNodeQuery,
    useLatestNodeVersionQuery,
    useNodeByIdQuery,
    useNodeQuery,
} from "@/store/nodeStore.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { useParams } from "@tanstack/react-router";
import { NotFoundPage } from "@/pages/NotFoundPage.tsx";
import { Button } from "@/components/ui/button.tsx";
import { CloudDownload, CloudUpload, Info } from "lucide-react";
import { statusColorTextMap } from "@/lib/utils.ts";
import { StatusIcon } from "@/components/custom/StatusIcon.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { FormCheckbox } from "@/components/custom/form/FormCheckbox.tsx";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { saveNodeSettings, updateNode } from "@/api/nodeApi.ts";
import { Spinner } from "@/components/ui/spinner.tsx";
import { CreateNodeDialog } from "@/components/nodes/CreateNodeDialog.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { NodeManagementDropdown } from "@/components/nodes/NodeManagementDropdown.tsx";
import { HoverPopover } from "@/components/custom/HoverPopover.tsx";
import { NodeInfoPopoverContent } from "@/components/nodes/NodeInfoPopoverContent.tsx";

const nodeSettingsSchema = z.object({
    nodeName: z
        .string({
            message: "Node name is required",
        })
        .min(1, {
            message: "Node name is required",
        })
        .min(3, {
            message: "Node name must be at least 3 characters long",
        })
        .max(16, {
            message: "Node name must be at most 16 characters long",
        }),
    defaultServerDirectory: z
        .string({
            message: "Default save location is required",
        })
        .min(1, {
            message: "Server save location is required",
        }),
    serverBackupsDirectory: z
        .string({
            message: "Server backup directory is required",
        })
        .min(1, {
            message: "Server save location is required",
        }),
    manageFirewallAutomatically: z.boolean({
        message: "Manage firewall automatically is required",
    }),
    enableAutoUpdate: z.boolean({
        message: "Enable auto update is required",
    }),
});

export function NodeSettingsPage() {
    const {
        data: nodes,
        isLoading: isLoadingNodes,
        isPending: isPendingNodes,
        isPlaceholderData: isPlaceholderDataNodes,
    } = useNodeQuery(useActiveTeam());

    const { data: latestNodeVersion } = useLatestNodeVersionQuery();

    const params = useParams({ strict: false });

    const [isLoading, setLoading] = useState(false);

    const { data: node, dataUpdatedAt: nodeDataUpdatedAt } = useNodeByIdQuery(
        useActiveTeam(),
        params.nodeId ?? "",
    );

    const defaultValues = useMemo(() => {
        return {
            nodeName: node?.settings.name ?? "",
            defaultServerDirectory: node?.settings.defaultServerDirectory ?? "",
            serverBackupsDirectory: node?.settings.serverBackupsDirectory ?? "",
            manageFirewallAutomatically:
                node?.settings.manageFirewallAutomatically ?? false,
            enableAutoUpdate: node?.settings.enableAutoUpdate ?? false,
        };
    }, [
        node?.settings.name,
        node?.settings.defaultServerDirectory,
        node?.settings.serverBackupsDirectory,
        node?.settings.manageFirewallAutomatically,
        node?.settings.enableAutoUpdate,
    ]);

    const form = useForm<z.infer<typeof nodeSettingsSchema>>({
        resolver: zodResolver(nodeSettingsSchema),
        defaultValues,
    });

    useEffect(() => {
        // Reset the form whenever the node query updates (even if values stay the same).
        form.reset(defaultValues);
    }, [form, defaultValues, nodeDataUpdatedAt]);

    const onSubmitSave = (data: z.infer<typeof nodeSettingsSchema>) => {
        setLoading(true);

        const nodeSettings = {
            name: data.nodeName,
            defaultServerDirectory: data.defaultServerDirectory,
            serverBackupsDirectory: data.serverBackupsDirectory,
            manageFirewallAutomatically: data.manageFirewallAutomatically,
            enableAutoUpdate: data.enableAutoUpdate,
        };

        saveNodeSettings(node?.id ?? "", nodeSettings)
            .then(() => {
                if (
                    data.serverBackupsDirectory !=
                    (node?.settings?.serverBackupsDirectory ?? "")
                ) {
                    toast.warning(
                        "Changes saved successfully. You changed the server backup directory. Please notice that it can take some time until all the backups are migrated to the new location.",
                        {
                            duration: 10000,
                        },
                    );
                } else toast.success("Changes saved successfully.");
                refetchNodeQuery().then();
            })
            .finally(() => setLoading(false));
    };

    const onSubmitUpdate = () => {
        setLoading(true);
        updateNode(node?.id ?? "")
            .then(() => {
                toast.success(
                    "The node is being updated in the background. Please wait a few seconds.",
                );
            })
            .finally(() => setLoading(false));
    };

    const canEdit = useMemo(() => {
        return node?.status === "ONLINE" && !isLoading;
    }, [isLoading, node?.status]);

    useEffect(() => {
        if (node && node.status === "CREATION") {
            dialog(<CreateNodeDialog node={node} setNode={() => {}} />, {
                tag: "createNodeDialog",
            });
        }
    }, [node]);

    if (isLoadingNodes || isPendingNodes || !nodes || isPlaceholderDataNodes) {
        return <Spinner size="large" />;
    }
    if (!node) {
        return <NotFoundPage />;
    }

    return (
        <>
            <div className="flex flex-col lg:flex-row gap-3 items-center justify-between">
                <div className="flex items-center gap-10">
                    <div className="flex items-center gap-2">
                        <StatusIcon
                            className="lg:hidden"
                            status={node.status}
                        />
                        <HoverPopover
                            enableClickToOpen
                            delay={-1}
                            wide
                            content={<NodeInfoPopoverContent node={node} />}>
                            <div className="flex gap-2 items-center">
                                <p className="text-2xl font-semibold">
                                    {node.settings.name}
                                </p>
                                <Info />
                            </div>
                        </HoverPopover>
                    </div>
                    <small className="text-muted-foreground">
                        {node.daemonVersion}
                    </small>
                </div>
                <div className="flex items-center gap-3">
                    <Button
                        disabled={
                            !canEdit || latestNodeVersion == node.daemonVersion
                        }
                        onClick={onSubmitUpdate}
                        size="tiny"
                        variant="tertiary">
                        {isLoading ? (
                            <Spinner size="small" />
                        ) : (
                            <>
                                <CloudDownload />
                                Update
                            </>
                        )}
                    </Button>
                    <Button
                        disabled={!canEdit}
                        onClick={form.handleSubmit(onSubmitSave)}
                        size="tiny">
                        {isLoading ? (
                            <Spinner size="small" />
                        ) : (
                            <>
                                <CloudUpload />
                                Save
                            </>
                        )}
                    </Button>
                    <NodeManagementDropdown node={node} />
                </div>
                <div className="hidden lg:flex items-center gap-3">
                    <p className="text-2xl font-semibold uppercase">Status</p>
                    <p className={statusColorTextMap[node.status]}>
                        {node.status}
                    </p>
                </div>
            </div>
            <div className="h-0 w-full pt-5 grow">
                <div className="overflow-y-auto w-full h-full bg-secondary rounded-md p-5">
                    <div className="grid grid-cols-1 lg:grid-cols-2 grid-rows-1 gap-5 h-fit">
                        <Form {...form}>
                            <div className="h-fit">
                                <FormInput
                                    form={form}
                                    label="Node Name"
                                    name="nodeName"
                                    className=""
                                    disabled={!canEdit}
                                />
                            </div>
                            <div />
                            <div className="h-fit">
                                <FormInput
                                    form={form}
                                    label="Default Server Directory"
                                    name="defaultServerDirectory"
                                    disabled={!canEdit}
                                />
                            </div>
                            <div className="h-fit">
                                <FormInput
                                    form={form}
                                    label="Server Backups Directory"
                                    name="serverBackupsDirectory"
                                    disabled={!canEdit}
                                />
                            </div>
                            <FormCheckbox
                                className="self-center lg:mt-5"
                                form={form}
                                label="Manage Firewall Automatically"
                                name="manageFirewallAutomatically"
                                reverseLabel
                                disabled={!canEdit}
                            />
                            <FormCheckbox
                                className="self-center lg:mt-5"
                                form={form}
                                label="Enable Auto Update"
                                name="enableAutoUpdate"
                                reverseLabel
                                disabled={!canEdit}
                            />
                        </Form>
                    </div>
                </div>
            </div>
        </>
    );
}

import { DialogClose, DialogFooter } from "@/components/ui/dialog.tsx";
import { appConfig } from "@/config.ts";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { CreateNodeDialogHeader } from "@/components/nodes/CreateNodeDialogHeader.tsx";
import { ClipboardCopy } from "lucide-react";
import { toast } from "sonner";
import { createNode } from "@/api/nodeApi.ts";
import { useActiveTeam } from "@/store/teamStore.ts";
import { type Node } from "@/store/nodeStore.ts";
import { queryKeys } from "@/store/queryKeys.ts";
import { upsertListItem } from "@/store/queryCache.ts";

const createNodeSchema = z.object({
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
});

type CreateNodeDialogOneProps = {
    node: Node | null;
    setNode: (node: Node | null) => void;
};

export function CreateNodeDialog(props: CreateNodeDialogOneProps) {
    const currentTeam = useActiveTeam();

    const [isLoading, setLoading] = useState(false);

    const [dialogNode, setDialogNode] = useState<Node | null>(props.node);

    const form = useForm<z.infer<typeof createNodeSchema>>({
        resolver: zodResolver(createNodeSchema),
        defaultValues: {
            friendlyName: "",
        },
    });

    useEffect(() => {
        setDialogNode(props.node);
    }, [props.node]);

    const onSubmit = (data: z.infer<typeof createNodeSchema>) => {
        setLoading(true);

        if (currentTeam === null) {
            toast.error("No active team found.");
            setLoading(false);
            return;
        }

        createNode(currentTeam.id, data.friendlyName)
            .then(async (node: Node) => {
                upsertListItem(queryKeys.nodes.list(currentTeam.id), node);

                setDialogNode(node);

                props.setNode(node);
                setLoading(false);
            })
            .finally(() => setLoading(false));
    };

    const copyToClipboard = (text: string) => {
        navigator.clipboard
            .writeText(text)
            .then(() => {
                toast.success(
                    "The invite token has been copied to the clipboard!",
                );
            })
            .catch(() => {
                toast.error(
                    "Failed to copy the invite token to the clipboard!",
                );
            });
    };

    const addHyphenBetweenDigits = (token: number | null) => {
        if (token === null) return "";
        const text = token.toString();
        return `${text.slice(0, 3)}-${text.slice(3)}`;
    };

    return (
        <>
            {dialogNode === null ? (
                <ManagedForm form={form} onSubmit={onSubmit}>
                    <CreateNodeDialogHeader />
                    <FormInput
                        form={form}
                        name="friendlyName"
                        label="Friendly Name"
                        required
                    />
                    <DialogFooter>
                        <div className="w-full flex flex-col gap-4 mt-5">
                            <div className="flex">
                                <Link
                                    className="w-full"
                                    to={appConfig.daemonDownloadUrl}
                                    target="_blank">
                                    <Button
                                        variant="outline"
                                        className="w-full uppercase">
                                        Download Daemon
                                    </Button>
                                </Link>
                            </div>
                            <Button
                                disabled={isLoading}
                                className="w-full uppercase"
                                type="submit">
                                {isLoading ? <Spinner /> : "Continue"}
                            </Button>
                        </div>
                    </DialogFooter>
                </ManagedForm>
            ) : (
                <>
                    <CreateNodeDialogHeader />
                    <div className="mt-5">
                        <h4 className="text-4xl mb-3">Invite Token</h4>
                        <div className="relative h-16 bg-input/30 border border-input rounded-md flex items-center justify-center">
                            <h1 className="font-mono">
                                {addHyphenBetweenDigits(dialogNode.inviteToken)}
                            </h1>
                            <Button
                                aria-label="Copy invite token"
                                onClick={() =>
                                    copyToClipboard(
                                        addHyphenBetweenDigits(
                                            dialogNode?.inviteToken ?? null,
                                        ),
                                    )
                                }
                                className="absolute right-0 mr-3"
                                size="icon"
                                variant="outline">
                                <ClipboardCopy />
                            </Button>
                        </div>
                    </div>
                    <DialogFooter>
                        <div className="w-full flex flex-col gap-4 mt-5">
                            <div className="flex">
                                <Link
                                    className="w-full"
                                    to={appConfig.daemonDownloadUrl}
                                    target="_blank">
                                    <Button
                                        variant="outline"
                                        className="w-full uppercase">
                                        Download Daemon
                                    </Button>
                                </Link>
                            </div>
                            <DialogClose asChild>
                                <Button className="uppercase w-full">
                                    Finish
                                </Button>
                            </DialogClose>
                        </div>
                    </DialogFooter>
                </>
            )}
        </>
    );
}

import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { ManagedForm } from "@/components/ui/form.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { type Server } from "@/store/serverStore.ts";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { restartServer, stopServer } from "@/api/serverApi.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";

type ServerPowerDialogProps = {
    action: "stop" | "restart";
    servers: Server[];
    isLoading: boolean;
    setLoading: (loading: boolean) => void;
    onComplete?: () => void;
};

const serverPowerSchema = z.object({
    delayMinutes: z.string().regex(/^\d*$/, {
        message: "Delay must be a whole number",
    }),
    overrideMessage: z.string().optional(),
});

export function ServerPowerDialog({
    action,
    servers,
    isLoading,
    setLoading,
    onComplete,
}: ServerPowerDialogProps) {
    const form = useForm<z.infer<typeof serverPowerSchema>>({
        resolver: zodResolver(serverPowerSchema),
        defaultValues: {
            delayMinutes: "",
            overrideMessage: "",
        },
    });

    const onSubmit = (data: z.infer<typeof serverPowerSchema>) => {
        setLoading(true);

        const parsedDelayMinutes = Number(data.delayMinutes);

        const actionPromises = servers.map((server) =>
            (action === "stop"
                ? stopServer(
                      server.id,
                      false,
                      parsedDelayMinutes,
                      data.overrideMessage,
                  )
                : restartServer(
                      server.id,
                      parsedDelayMinutes,
                      data.overrideMessage,
                  )
            ).then(() => {
                toast.success(
                    action === "stop"
                        ? `${server.displayName} is now stopping!`
                        : `${server.displayName} is now restarting!`,
                );
            }),
        );

        Promise.all(actionPromises)
            .then(() => {
                closeDialog();
            })
            .finally(() => {
                setLoading(false);
                onComplete?.();
            });
    };

    const serverCount = servers.length;
    const firstServer = servers[0];
    const targetLabel =
        serverCount === 1
            ? firstServer.displayName
            : `${serverCount} Server${serverCount > 1 ? "s" : ""}`;

    const actionCopy =
        action === "stop"
            ? {
                  title: `Stop ${targetLabel}`,
                  confirm:
                      serverCount === 1
                          ? "Stop"
                          : `Stop ${serverCount} Servers`,
                  description:
                      "Delay the stop if needed and optionally send a custom notification to players.",
                  buttonVariant: "serverStop" as const,
                  overrideLabel: "Override Stop Message",
              }
            : {
                  title: `Restart ${targetLabel}`,
                  confirm:
                      serverCount === 1
                          ? "Restart"
                          : `Restart ${serverCount} Servers`,
                  description:
                      "Delay the restart if needed and optionally send a custom notification to players.",
                  buttonVariant: "serverRestart" as const,
                  overrideLabel: "Override Restart Message",
              };

    const delayMinutes = Number(form.watch("delayMinutes"));

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>{actionCopy.title}</DialogTitle>
                <DialogDescription>{actionCopy.description}</DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-4">
                <FormInput
                    form={form}
                    name="delayMinutes"
                    label="Delay for minutes"
                    inputMode="numeric"
                    onlyDigits
                    placeholder="0"
                    description="Leave blank to run the action immediately."
                />
                {delayMinutes > 0 && (
                    <FormInput
                        form={form}
                        name="overrideMessage"
                        label={actionCopy.overrideLabel}
                        placeholder="Optional message for players"
                        description="Shown to players while the delayed stop or restart is pending. Use '{minutes}' to display the delay!"
                    />
                )}
            </div>
            <DialogFooter>
                <div className="flex flex-col gap-4 w-full mt-4">
                    <Button
                        disabled={isLoading}
                        type="submit"
                        variant={actionCopy.buttonVariant}>
                        {isLoading ? <Spinner /> : <>{actionCopy.confirm}</>}
                    </Button>
                    <DialogClose asChild>
                        <Button variant="tertiary" type="button">
                            Cancel
                        </Button>
                    </DialogClose>
                </div>
            </DialogFooter>
        </ManagedForm>
    );
}

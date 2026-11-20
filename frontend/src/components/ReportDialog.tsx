import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { ManagedForm } from "@/components/ui/form.tsx";
import {
    DialogClose,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { FormInput } from "@/components/custom/form/FormInput.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { z } from "zod";
import { FormTextArea } from "@/components/custom/form/FormTextArea.tsx";
import { useTeamQuery } from "@/store/teamStore.ts";
import { FormSelect } from "@/components/custom/form/FormSelect.tsx";
import { cn } from "@/lib/utils.ts";
import { reportBug } from "@/api/helpdeskApi.ts";
import { toast } from "sonner";
import { closeDialog } from "@/store/dialogStore.tsx";

export function ReportDialog() {
    const { data: teams } = useTeamQuery();

    const [isLoading, setLoading] = useState(false);

    const reportSchema = z.object({
        subject: z
            .string({
                message: "Subject is required",
            })
            .min(1, "Subject is required"),
        message: z
            .string({
                message: "Message is required",
            })
            .min(1, "Message is required"),
        team: z
            .string({
                message: "Team is required",
            })
            .min(1, "Team is required"),
        discordUsername: z.string().optional(),
    });

    const form = useForm<z.infer<typeof reportSchema>>({
        resolver: zodResolver(reportSchema),
        defaultValues: {
            subject: "",
            message: "",
            discordUsername: "",
            team: teams?.[0].id,
        },
    });

    const onSubmit = (data: z.infer<typeof reportSchema>) => {
        setLoading(true);
        reportBug(data.subject, data.message, data.team, data.discordUsername)
            .then(() => {
                toast.success(
                    "Your bug report has been sent successfully. We will process it as soon as possible.",
                );
                closeDialog();
            })
            .finally(() => setLoading(false));
    };

    return (
        <ManagedForm form={form} onSubmit={onSubmit}>
            <DialogHeader>
                <DialogTitle>Report a Bug</DialogTitle>
                <DialogDescription>
                    Help us improve our platform by reporting any bugs you
                    encounter.
                </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col mt-5 gap-5">
                <FormInput
                    form={form}
                    label="Subject"
                    name="subject"
                    required
                />
                <FormInput
                    form={form}
                    label="Discord Username"
                    name="discordUsername"
                />
                <div className={cn(teams?.length === 1 && "hidden")}>
                    <FormSelect
                        form={form}
                        name="team"
                        label="Team"
                        items={teams?.map((team) => ({
                            value: team.id,
                            label: team.name,
                        }))}
                        required
                    />
                </div>
                <FormTextArea
                    form={form}
                    label="Message"
                    name="message"
                    height="large"
                    className="mb-5"
                    required
                />
            </div>
            <DialogFooter>
                <DialogClose asChild>
                    <Button variant="tertiary">Cancel</Button>
                </DialogClose>
                <Button disabled={isLoading} type="submit">
                    {isLoading ? <Spinner /> : <>Report</>}
                </Button>
            </DialogFooter>
        </ManagedForm>
    );
}

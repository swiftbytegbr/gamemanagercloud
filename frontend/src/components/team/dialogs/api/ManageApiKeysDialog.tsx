import {
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { type Team } from "@/store/teamStore.ts";
import { dialog } from "@/store/dialogStore.tsx";
import { ApiKeysTable } from "@/components/team/ApiKeysTable.tsx";
import { CreateApiKeyDialog } from "@/components/team/dialogs/api/CreateApiKeyDialog.tsx";

type ManageApiKeysDialogProps = {
    team: Team;
};

export function ManageApiKeysDialog(props: ManageApiKeysDialogProps) {
    return (
        <div className="grow flex flex-col gap-6 min-h-0">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <DialogHeader>
                    <DialogTitle>API Keys</DialogTitle>
                    <DialogDescription>
                        Create and manage your API keys for {props.team.name}.
                    </DialogDescription>
                </DialogHeader>
                <Button
                    onClick={() =>
                        dialog(<CreateApiKeyDialog team={props.team} />, {
                            width: "auto",
                        })
                    }>
                    Create API Key
                </Button>
            </div>
            <div className="grow min-h-0">
                <ApiKeysTable team={props.team} />
            </div>
        </div>
    );
}

import { ActionLogTable } from "@/components/team/ActionLogTable.tsx";
import { useActiveTeam } from "@/store/teamStore.ts";

export function TeamActionLogPage() {
    const activeTeam = useActiveTeam();

    if (!activeTeam) return null;

    return (
        <div className="w-full h-full py-0">
            <div className="w-full h-full bg-secondary rounded-md pt-2 flex flex-col">
                <div className="grow overflow-hidden">
                    <ActionLogTable team={activeTeam} />
                </div>
            </div>
        </div>
    );
}

import { NodesTable } from "@/components/nodes/NodesTable.tsx";

export function NodePage() {
    return (
        <div className="w-full h-full py-0">
            <div className="w-full h-full bg-secondary rounded-md pt-2 flex flex-col">
                <NodesTable />
            </div>
        </div>
    );
}

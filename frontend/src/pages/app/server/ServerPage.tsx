import { ServersTable } from "@/components/servers/table/ServersTable.tsx";

export function ServerPage() {
    return (
        <div className="w-full h-full py-0">
            <div className="w-full h-full bg-secondary rounded-md pt-2 flex flex-col">
                <ServersTable />
            </div>
        </div>
    );
}

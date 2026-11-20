import { Button } from "@/components/ui/button.tsx";
import {
    ArrowLeft,
    ArrowLeftToLine,
    ArrowRight,
    ArrowRightToLine,
} from "lucide-react";
import type { RowData, Table } from "@/lib/table.ts";

type TableNavigationProps<TData extends RowData> = {
    table: Table<TData>;
};

export function TableNavigation<TData extends RowData>(
    props: TableNavigationProps<TData>,
) {
    return (
        <div className="flex items-center justify-center w-full my-2">
            <p className="text-sm pr-4">
                Page {props.table.state.pagination.pageIndex + 1} from{" "}
                {props.table.getPageCount()}
            </p>
            <Button
                aria-label="First page"
                variant="ghost"
                size="icon"
                disabled={!props.table.getCanPreviousPage()}
                onClick={props.table.firstPage}>
                <ArrowLeftToLine />
            </Button>
            <Button
                aria-label="Previous page"
                variant="ghost"
                size="icon"
                disabled={!props.table.getCanPreviousPage()}
                onClick={props.table.previousPage}>
                <ArrowLeft />
            </Button>
            <Button
                aria-label="Next page"
                variant="ghost"
                size="icon"
                disabled={!props.table.getCanNextPage()}
                onClick={props.table.nextPage}>
                <ArrowRight />
            </Button>
            <Button
                aria-label="Last page"
                variant="ghost"
                size="icon"
                disabled={!props.table.getCanNextPage()}
                onClick={props.table.lastPage}>
                <ArrowRightToLine />
            </Button>
        </div>
    );
}

import {
    columnFilteringFeature,
    columnVisibilityFeature,
    createColumnHelper as createTableColumnHelper,
    createFilteredRowModel,
    createPaginatedRowModel,
    createSortedRowModel,
    filterFns,
    globalFilteringFeature,
    rowPaginationFeature,
    rowSelectionFeature,
    rowSortingFeature,
    sortFns,
    tableFeatures,
    type ReactTable,
    type Row as TableRow,
    type RowData,
} from "@tanstack/react-table";

export type { RowData } from "@tanstack/react-table";

export const appTableFeatures = tableFeatures({
    columnFilteringFeature,
    columnVisibilityFeature,
    globalFilteringFeature,
    rowPaginationFeature,
    rowSelectionFeature,
    rowSortingFeature,
    filteredRowModel: createFilteredRowModel(),
    paginatedRowModel: createPaginatedRowModel(),
    sortedRowModel: createSortedRowModel(),
    filterFns,
    sortFns,
});

export function createColumnHelper<TData extends RowData>() {
    return createTableColumnHelper<typeof appTableFeatures, TData>();
}

export type Table<TData extends RowData> = ReactTable<
    typeof appTableFeatures,
    TData
>;
export type Row<TData extends RowData> = TableRow<
    typeof appTableFeatures,
    TData
>;

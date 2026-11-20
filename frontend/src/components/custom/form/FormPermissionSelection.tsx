import {
    FormControl,
    FormField,
    FormItem,
    FormMessage,
} from "@/components/ui/form.tsx";
import type { FieldValues, Path, UseFormReturn } from "react-hook-form";
import { Network, Server, Users } from "lucide-react";
import type { Permission } from "@/store/teamStore.ts";
import { FormLabelWithDescription } from "@/components/custom/form/FormLabelWithDescription";
import { cn } from "@/lib/utils.ts";

export type PermissionGroupKey = "manageTeam" | "manageNodes" | "manageServers";

export type PermissionSelectionValue = Record<PermissionGroupKey, boolean>;

interface FormPermissionSelectionProps<TFormValues extends FieldValues> {
    form: UseFormReturn<TFormValues>;
    name: Path<TFormValues>;
    className?: string;
    label: string;
    description?: string;
    required?: boolean;
    disableError?: boolean;
    disabled?: boolean;
}

const NODE_PERMISSIONS: Permission[] = ["ACCESS_NODES", "MANAGE_NODES"];
const SERVER_PERMISSIONS: Permission[] = [
    "ACCESS_SERVERS",
    "MANAGE_SERVERS",
    "ACCESS_PROFILES",
    "MANAGE_PROFILES",
];

type PermissionGroup = {
    key: PermissionGroupKey;
    label: string;
    description: string;
    permissions: Permission[];
    icon: typeof Users;
};

const PERMISSION_GROUPS: PermissionGroup[] = [
    {
        key: "manageTeam",
        label: "Manage Team",
        description: "Invite/kick members, rename, and manage team settings.",
        permissions: ["MANAGE_TEAM"],
        icon: Users,
    },
    {
        key: "manageNodes",
        label: "Manage Nodes",
        description: "Access and manage nodes connected to this team.",
        permissions: NODE_PERMISSIONS,
        icon: Network,
    },
    {
        key: "manageServers",
        label: "Manage Servers",
        description: "Access/manage servers and profiles within the team.",
        permissions: SERVER_PERMISSIONS,
        icon: Server,
    },
];

export function buildPermissionSelection(
    permissions: Permission[],
): PermissionSelectionValue {
    return {
        manageTeam: permissions.includes("MANAGE_TEAM"),
        manageNodes: NODE_PERMISSIONS.some((permission) =>
            permissions.includes(permission),
        ),
        manageServers: SERVER_PERMISSIONS.some((permission) =>
            permissions.includes(permission),
        ),
    };
}

function permissionsFromSelection(
    selection: PermissionSelectionValue,
): Permission[] {
    const selected = new Set<Permission>();
    if (selection.manageTeam) selected.add("MANAGE_TEAM");
    if (selection.manageNodes) {
        NODE_PERMISSIONS.forEach((permission) => selected.add(permission));
    }
    if (selection.manageServers) {
        SERVER_PERMISSIONS.forEach((permission) => selected.add(permission));
    }
    selected.add("ACCESS_TEAM");
    return Array.from(selected);
}

export function getPermissionGroupLabels(permissions: Permission[]): string[] {
    return PERMISSION_GROUPS.filter((group) =>
        group.permissions.some((permission) =>
            permissions.includes(permission),
        ),
    ).map((group) => group.label);
}

export function FormPermissionSelection<TFormValues extends FieldValues>({
    required = false,
    disableError = false,
    ...props
}: FormPermissionSelectionProps<TFormValues>) {
    return (
        <FormField
            disabled={props.disabled}
            control={props.form.control}
            name={props.name}
            render={({ field }) => {
                const selection = buildPermissionSelection(
                    (field.value ?? []) as Permission[],
                );

                return (
                    <FormItem className={cn("h-fit min-w-0", props.className)}>
                        <FormLabelWithDescription
                            label={props.label}
                            description={props.description}
                            required={required}
                        />
                        <FormControl>
                            <div
                                className={cn(
                                    "grid grid-cols-1 lg:grid-cols-3 gap-3 py-2 max-w-[400px] m-auto lg:m-0 lg:max-w-[870px]",
                                    props.disabled &&
                                        "pointer-events-none opacity-60",
                                )}>
                                {PERMISSION_GROUPS.map((group) => {
                                    const selected = selection[group.key];
                                    const Icon = group.icon;
                                    return (
                                        <div
                                            key={group.key}
                                            role="button"
                                            tabIndex={0}
                                            aria-pressed={selected}
                                            onClick={() =>
                                                field.onChange(
                                                    permissionsFromSelection({
                                                        ...selection,
                                                        [group.key]: !selected,
                                                    }),
                                                )
                                            }
                                            onKeyDown={(event) => {
                                                if (
                                                    event.key === " " ||
                                                    event.key === "Enter"
                                                ) {
                                                    event.preventDefault();
                                                    field.onChange(
                                                        permissionsFromSelection(
                                                            {
                                                                ...selection,
                                                                [group.key]:
                                                                    !selected,
                                                            },
                                                        ),
                                                    );
                                                }
                                            }}
                                            className={cn(
                                                "border rounded-md p-4 flex flex-col gap-2 cursor-pointer transition-colors",
                                                "hover:bg-secondary/60",
                                                selected &&
                                                    "ring-2 ring-primary bg-primary/5 border-primary/50",
                                            )}>
                                            <div className="flex items-center gap-3">
                                                <div
                                                    className={cn(
                                                        "rounded-md p-2",
                                                        selected
                                                            ? "bg-primary text-primary-foreground"
                                                            : "bg-secondary text-secondary-foreground",
                                                    )}>
                                                    <Icon className="size-5" />
                                                </div>
                                                <div className="font-medium">
                                                    {group.label}
                                                </div>
                                                <div
                                                    className={cn(
                                                        "ml-auto text-xs",
                                                        selected
                                                            ? "text-primary"
                                                            : "text-muted-foreground",
                                                    )}>
                                                    {selected
                                                        ? "Enabled"
                                                        : "Disabled"}
                                                </div>
                                            </div>
                                            <div className="text-sm text-muted-foreground">
                                                {group.description}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </FormControl>
                        {!disableError && <FormMessage />}
                    </FormItem>
                );
            }}
        />
    );
}

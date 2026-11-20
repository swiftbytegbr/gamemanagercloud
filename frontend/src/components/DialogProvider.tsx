import { useDialogStore } from "@/store/dialogStore.tsx";
import { Dialog, DialogContent } from "@/components/ui/dialog.tsx";
import { cn } from "@/lib/utils.ts";

export function DialogProvider() {
    const { stack, pop } = useDialogStore();

    const widthClasses = {
        auto: "sm:w-max sm:min-w-lg sm:max-w-[90vw]",
        sm: "sm:w-[420px] sm:max-w-[90vw]",
        md: "sm:w-[540px] sm:max-w-[90vw]",
        lg: "sm:w-[640px] sm:max-w-[90vw]",
        xl: "sm:w-[90vw]",
    } as const;
    const heightClasses = {
        auto: "h-auto sm:h-max sm:min-h-lg sm:max-h-[90vh]",
        sm: "sm:h-[320px] sm:max-h-[90vh]",
        md: "sm:h-[420px] sm:max-h-[90vh]",
        lg: "sm:h-[520px] sm:max-h-[90vh]",
        xl: "sm:h-[90vh]",
    } as const;

    return (
        <>
            {stack.map((entry, index) => {
                const isTop = index === stack.length - 1;
                const width = entry.width ?? "md";
                const height = entry.height ?? "auto";
                return (
                    <Dialog
                        key={entry.id}
                        open={entry.open}
                        onOpenChange={(open) => {
                            if (!open && isTop) {
                                if (entry.modal) return;
                                pop();
                            }
                        }}>
                        <DialogContent
                            disableCloseButton={entry.disableCloseButton}
                            onPointerDownOutside={(e) => {
                                if (entry.modal) e.preventDefault();
                            }}
                            onInteractOutside={(e) => {
                                if (entry.modal) e.preventDefault();
                            }}
                            onEscapeKeyDown={(e) => {
                                if (entry.modal) e.preventDefault();
                            }}
                            className={cn(
                                "max-h-[95vh] flex flex-col",
                                widthClasses[width],
                                heightClasses[height],
                            )}>
                            <div className="flex flex-col gap-4 overflow-y-auto px-2">
                                {entry.content}
                            </div>
                        </DialogContent>
                    </Dialog>
                );
            })}
        </>
    );
}

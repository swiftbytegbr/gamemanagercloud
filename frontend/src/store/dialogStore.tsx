import type { ReactNode } from "react";
import { create } from "zustand";

interface DialogOptions {
    disableCloseButton?: boolean;
    modal?: boolean;
    height?: "auto" | "sm" | "md" | "lg" | "xl";
    width?: "auto" | "sm" | "md" | "lg" | "xl";
    onOpen?: () => void;
    onClose?: () => void;
    tag?: string;
}

type DialogEntry = {
    id: string;
    content: ReactNode;
    disableCloseButton?: boolean;
    modal?: boolean;
    height?: "auto" | "sm" | "md" | "lg" | "xl";
    width?: "auto" | "sm" | "md" | "lg" | "xl";
    tag?: string;
    onOpen?: () => void;
    onClose?: () => void;
    open: boolean;
};

interface DialogState {
    stack: DialogEntry[];
    open: (content: ReactNode, options?: DialogOptions) => void;
    pop: () => void;
    close: () => void; // closes all dialogs
    closeByTag: (tag: string) => void;
}

function genId() {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export const useDialogStore = create<DialogState>((set, get) => ({
    stack: [],
    open: (content, options) => {
        const entry: DialogEntry = {
            id: genId(),
            content,
            disableCloseButton: options?.disableCloseButton,
            modal: options?.modal,
            height: options?.height,
            width: options?.width,
            tag: options?.tag,
            onOpen: options?.onOpen,
            onClose: options?.onClose,
            open: true,
        };
        options?.onOpen?.();
        set((state) => {
            const nextStack = state.stack.map((stackEntry, index) =>
                index === state.stack.length - 1
                    ? { ...stackEntry, open: false }
                    : stackEntry,
            );
            return { stack: [...nextStack, entry] };
        });
    },
    pop: () => {
        const { stack } = get();
        if (stack.length === 0) return;
        const top = stack[stack.length - 1];
        const id = top.id;

        set((state) => ({
            stack: state.stack.map((e) =>
                e.id === id ? { ...e, open: false } : e,
            ),
        }));
        setTimeout(() => {
            top.onClose?.();
            let reopened: DialogEntry | undefined;
            let shouldCallOnOpen = false;
            set((state) => {
                const filtered = state.stack.filter((e) => e.id !== id);
                if (filtered.length === 0) return { stack: filtered };

                const updated = filtered.map((entry, index) => {
                    if (index === filtered.length - 1) {
                        if (!entry.open) {
                            shouldCallOnOpen = true;
                            reopened = { ...entry, open: true };
                            return reopened;
                        }
                        reopened = entry;
                    }

                    return entry;
                });

                return { stack: updated };
            });
            if (shouldCallOnOpen) {
                reopened?.onOpen?.();
            }
        }, 200);
    },
    close: () => {
        const { stack } = get();
        if (stack.length === 0) return;
        set((state) => ({
            stack: state.stack.map((e) => ({ ...e, open: false })),
        }));
        setTimeout(() => {
            stack.forEach((e) => e.onClose?.());
            set({ stack: [] });
        }, 200);
    },
    closeByTag: (tag: string) => {
        const { stack } = get();
        const ids = stack.filter((e) => e.tag === tag).map((e) => e.id);
        if (ids.length === 0) return;
        set((state) => ({
            stack: state.stack.map((e) =>
                ids.includes(e.id) ? { ...e, open: false } : e,
            ),
        }));
        setTimeout(() => {
            stack
                .filter((e) => ids.includes(e.id))
                .forEach((e) => e.onClose?.());
            let reopened: DialogEntry | undefined;
            let shouldCallOnOpen = false;
            set((state) => {
                const filtered = state.stack.filter((e) => !ids.includes(e.id));
                if (filtered.length === 0) return { stack: filtered };

                const updated = filtered.map((entry, index) => {
                    if (index === filtered.length - 1) {
                        if (!entry.open) {
                            shouldCallOnOpen = true;
                            reopened = { ...entry, open: true };
                            return reopened;
                        }
                        reopened = entry;
                    }

                    return entry;
                });

                return { stack: updated };
            });
            if (shouldCallOnOpen) {
                reopened?.onOpen?.();
            }
        }, 200);
    },
}));

export function dialog(content: ReactNode, options?: DialogOptions) {
    useDialogStore.getState().open(content, options);
}

// Close all dialogs in the stack
export function closeDialog() {
    useDialogStore.getState().close();
}

export function popDialog() {
    useDialogStore.getState().pop();
}

export function closeDialogsByTag(tag: string) {
    useDialogStore.getState().closeByTag(tag);
}

export function hasDialogByTag(tag: string) {
    return useDialogStore.getState().stack.some((e) => e.tag === tag && e.open);
}

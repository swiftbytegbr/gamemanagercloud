import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { popDialog } from "@/store/dialogStore.tsx";
import { Link } from "@tanstack/react-router";

export function InstallCertInfoDialog() {
    return (
        <>
            <DialogHeader className="text-center">
                <DialogTitle className="mb-2">
                    Certificate Installation
                </DialogTitle>
                <DialogDescription>
                    The ASA (ASE Epic) servers{" "}
                    <strong>require a certificate</strong>. You can easily
                    install it with this script:
                </DialogDescription>
                <ol className="list-decimal list-inside my-3 text-muted-foreground text-sm">
                    <li>
                        Open{" "}
                        <Link
                            className="text-primary hover:text-primary/80"
                            to="https://github.com/Ch4r0ne/UnrealEngine_Dedicated_Server_Install_CA/blob/main/Install_Certificate.ps1"
                            target="_blank">
                            GitHub Repository
                        </Link>
                    </li>
                    <li>Run the script in PowerShell</li>
                    <li>Restart Node (Dedicated Server)</li>
                    <li>Start GMC Daemon</li>
                </ol>
            </DialogHeader>
            <DialogFooter>
                <Button className="w-full" onClick={() => popDialog()}>
                    I understand!
                </Button>
            </DialogFooter>
        </>
    );
}

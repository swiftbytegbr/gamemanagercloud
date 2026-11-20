import {
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { popDialog } from "@/store/dialogStore.tsx";

export function EarlyAccessDialog() {
    return (
        <>
            <DialogHeader className="text-center">
                <DialogTitle className="mb-2">
                    GameManager.Cloud is in Early Access
                </DialogTitle>
                <DialogDescription>
                    GameManager.Cloud (GMC) is currently available as an{" "}
                    <strong>Early Access release</strong>. This stage reflects
                    an active phase of development where our team is
                    continuously building, improving, and expanding the
                    platform’s features and systems. By using GMC in Early
                    Access,{" "}
                    <strong>
                        you’re getting a first look at what we’re creating
                    </strong>{" "}
                    — and the opportunity to help shape its future.
                    <br />
                    <br />
                    Please be aware that, as with any software in development,{" "}
                    <strong>
                        bugs, missing features, and occasional issues are to be
                        expected
                    </strong>
                    . We are committed to addressing these as quickly as
                    possible, and your feedback plays an important role in
                    identifying problems and guiding improvements.
                    <br />
                    <br />
                    During Early Access,{" "}
                    <strong>GMC is completely free to use</strong>. Once we
                    reach a full release, the platform will transition into a
                    paid service. However, there will always be a Free Tier
                    available for personal and small-scale use. Additional paid
                    plans will be offered for larger usage and advanced
                    features. We’ll make sure to communicate any upcoming
                    changes clearly and give our users ample notice before
                    adjustments take effect.
                    <br />
                    <br />
                    We appreciate your support and patience during this stage,
                    and we’re excited to build GameManager.Cloud together with
                    you.
                </DialogDescription>
            </DialogHeader>
            <DialogFooter>
                <Button className="w-full" onClick={() => popDialog()}>
                    I understand!
                </Button>
            </DialogFooter>
        </>
    );
}

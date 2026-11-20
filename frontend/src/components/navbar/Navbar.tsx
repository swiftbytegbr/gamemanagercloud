import { Menu } from "lucide-react";
import { useEffect, useState } from "react";
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from "@/components/ui/sheet.tsx";
import { NavbarNavigationElements } from "@/components/navbar/NavbarNavigationElements.tsx";
import { NavbarFooter } from "@/components/navbar/NavbarFooter.tsx";

export function Navbar() {
    const [isOpen, setOpen] = useState(false);

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth > 1024) {
                setOpen(false);
            }
        };

        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    return (
        <>
            <div className="hidden bg-secondary px-7 lg:flex flex-col gap-2 order-2">
                <NavbarNavigationElements />
                <NavbarFooter />
            </div>
            <div className="lg:hidden bg-secondary">
                <Sheet open={isOpen} onOpenChange={setOpen}>
                    <div className="h-full flex items-center ml-4">
                        <SheetTrigger
                            aria-label="Open menu"
                            className="bg-card p-2 rounded-md">
                            <Menu />
                        </SheetTrigger>
                    </div>
                    <SheetContent className="bg-secondary" side="left">
                        <SheetHeader className="mr-7 mt-7">
                            <SheetTitle className="sr-only">Menu</SheetTitle>
                            <SheetDescription className="sr-only">
                                Navigation menu
                            </SheetDescription>
                            <NavbarNavigationElements
                                onElementClick={() => setOpen(false)}
                            />
                        </SheetHeader>
                        <SheetFooter>
                            <NavbarFooter />
                        </SheetFooter>
                    </SheetContent>
                </Sheet>
            </div>
        </>
    );
}

import type { ErrorComponentProps } from "@tanstack/react-router";
import {
    Card,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.tsx";
import { Button } from "@/components/ui/button.tsx";

export function RootErrorPage({ error, reset }: ErrorComponentProps) {
    console.error(error);

    return (
        <div className="h-full flex items-center justify-center p-4">
            <Card className="bg-secondary w-full max-w-xl text-center">
                <CardHeader className="space-y-4">
                    <CardTitle className="uppercase">
                        Something went wrong
                    </CardTitle>
                    <CardDescription>
                        An unexpected error occurred.
                    </CardDescription>
                </CardHeader>
                <CardFooter className="flex flex-col gap-3 lg:flex-row lg:justify-center">
                    <Button
                        className="uppercase"
                        size="lg"
                        variant="outline"
                        onClick={() => reset()}>
                        Try again
                    </Button>
                </CardFooter>
            </Card>
        </div>
    );
}

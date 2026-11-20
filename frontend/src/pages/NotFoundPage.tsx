import {
    Card,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.tsx";
import { Button } from "@/components/ui/button.tsx";
import { router } from "@/Router.tsx";

export function NotFoundPage() {
    return (
        <div className="h-full flex items-center justify-center">
            <Card className="bg-secondary w-fit text-center">
                <CardHeader>
                    <CardTitle className="uppercase">Page not found</CardTitle>
                    <CardDescription>
                        The page you are looking for does not exist!
                    </CardDescription>
                </CardHeader>
                <CardFooter>
                    <div className="grid lg:grid-cols-2 gap-4">
                        <Button
                            onClick={() => window.history.back()}
                            className="uppercase"
                            size="lg"
                            variant="outline">
                            Back to last Page
                        </Button>
                        <Button
                            onClick={() =>
                                router.navigate({ to: "/dashboard" })
                            }
                            className="uppercase"
                            size="lg">
                            Back to Dashboard
                        </Button>
                    </div>
                </CardFooter>
            </Card>
        </div>
    );
}

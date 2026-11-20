import {
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.tsx";
import { Link } from "@tanstack/react-router";

export function LoginFormHeader() {
    return (
        <CardHeader>
            <CardTitle className="text-3xl uppercase">Login</CardTitle>
            <CardDescription>
                New User?
                <Link className="text-primary" to="/signup">
                    {" "}
                    Create an account
                </Link>
            </CardDescription>
        </CardHeader>
    );
}

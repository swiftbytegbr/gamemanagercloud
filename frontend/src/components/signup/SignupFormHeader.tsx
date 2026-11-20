import {
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.tsx";
import { Link } from "@tanstack/react-router";

export function SignupFormHeader() {
    return (
        <CardHeader>
            <CardTitle className="text-3xl uppercase">
                Create an account
            </CardTitle>
            <CardDescription>
                Already have an account?
                <Link className="text-primary" to="/login">
                    {" "}
                    Login
                </Link>
            </CardDescription>
        </CardHeader>
    );
}

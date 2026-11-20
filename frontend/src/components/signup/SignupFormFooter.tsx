import { Button } from "@/components/ui/button.tsx";
import { Link } from "@tanstack/react-router";
import { CardFooter } from "@/components/ui/card.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";

interface SignupFormFooterProps {
    submitButtonText: string;
    isLoading?: boolean;
}

export function SignupFormFooter(props: SignupFormFooterProps) {
    return (
        <CardFooter>
            <div className="w-full">
                <Button
                    disabled={props.isLoading}
                    className="w-full uppercase"
                    type="submit">
                    {props.isLoading ? (
                        <Spinner />
                    ) : props.submitButtonText ? (
                        props.submitButtonText
                    ) : (
                        "Continue"
                    )}
                </Button>
                <div className="flex text-muted-foreground font-light mt-3 text-sm justify-center">
                    <span>
                        <span>By continuing, you agree to accept our </span>
                        <Link
                            className="text-card-foreground"
                            to="/privacy-policy">
                            Privacy Policy
                        </Link>
                        <span> and </span>
                        <Link
                            className="text-card-foreground"
                            to="/terms-of-service">
                            Terms of Service
                        </Link>
                        <span>.</span>
                    </span>
                </div>
            </div>
        </CardFooter>
    );
}

import { Button } from "@/components/ui/button.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { CardFooter } from "@/components/ui/card.tsx";
import { FormFooterLinks } from "@/components/custom/form/FormFooterLinks.tsx";

interface SignupFormFooterProps {
    isLoading?: boolean;
}

export function LoginFormFooter(props: SignupFormFooterProps) {
    return (
        <CardFooter>
            <div className="w-full">
                <Button
                    disabled={props.isLoading}
                    className="w-full uppercase"
                    type="submit">
                    {props.isLoading ? <Spinner /> : "Login"}
                </Button>
                <FormFooterLinks />
            </div>
        </CardFooter>
    );
}

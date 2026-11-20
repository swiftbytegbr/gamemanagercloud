import { Link } from "@tanstack/react-router";

export function FormFooterLinks() {
    return (
        <div className="flex text-muted-foreground font-light mt-3 text-sm justify-center">
            <Link to="/imprint">Imprint</Link>
            <span className="mx-1">•</span>
            <Link to="/privacy-policy">Privacy Policy</Link>
            <span className="mx-1">•</span>
            <Link to="/terms-of-service">Terms of Service</Link>
        </div>
    );
}

import { Link } from "@tanstack/react-router";
import DiscordIcon from "@/assets/discord-icon.svg";
import FAQIcon from "@/assets/gmc-faq.svg";
import { Button } from "@/components/ui/button.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ReportDialog } from "@/components/ReportDialog.tsx";
import { useActiveTeam } from "@/store/teamStore.ts";
import { TeamPicture } from "@/components/custom/TeamPicture.tsx";
import { TeamSelectDialog } from "@/components/team/dialogs/TeamSelectDialog.tsx";
import { appConfig } from "@/config.ts";

export function NavbarFooter() {
    const activeTeam = useActiveTeam();

    return (
        <div className="mt-auto mb-3 text-left text-muted-secondary">
            {appConfig.discordUrl && (
                <Link
                    className="text-muted-foreground"
                    to={appConfig.discordUrl}
                    target="_blank">
                    <div className="flex items-center gap-1 justify-start h-6">
                        <img
                            className="size-4"
                            src={DiscordIcon}
                            alt="Discord Icon"
                        />
                        <small>Discord</small>
                    </div>
                </Link>
            )}
            {appConfig.faqUrl && (
                <Link
                    className="text-muted-foreground"
                    to={appConfig.faqUrl}
                    target="_blank">
                    <div className="flex items-center gap-1 justify-start h-6">
                        <img className="size-4" src={FAQIcon} alt="FAQ Icon" />
                        <small>FAQ</small>
                    </div>
                </Link>
            )}
            <Button
                onClick={() =>
                    dialog(<TeamSelectDialog />, {
                        height: "auto",
                        width: "md",
                    })
                }
                className="w-full h-12 mt-4"
                variant="cutout">
                <TeamPicture
                    className="h-full shrink-0 text-white"
                    team={activeTeam}
                />
                <span className="min-w-0 flex-1 truncate text-left">
                    {activeTeam?.name}
                </span>
            </Button>
            <Button
                className="w-full mt-4 mb-2"
                onClick={() => dialog(<ReportDialog />)}>
                Report Bug
            </Button>
            <div className="flex justify-center">
                <Link to="/terms-of-service">
                    <div>
                        <small>ToS</small>
                    </div>
                </Link>
                <span className="mx-1">•</span>
                <Link to="/privacy-policy">
                    <div>
                        <small>Privacy Policy</small>
                    </div>
                </Link>
                <span className="mx-1">•</span>
                <Link to="/imprint">
                    <div>
                        <small>Imprint</small>
                    </div>
                </Link>
            </div>
        </div>
    );
}

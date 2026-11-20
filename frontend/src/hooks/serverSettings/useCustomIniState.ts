import { useEffect, useMemo, useState } from "react";
import type { CustomIniSections } from "@/components/servers/settings/CustomIniEditor.tsx";
import {
    buildCustomIniSections,
    type CustomIniState,
    type MissingSetting,
} from "@/lib/serverSettings";

export function useCustomIniState(
    userCustomSettings: MissingSetting[],
): CustomIniState {
    const { game, gus } = useMemo(
        () => buildCustomIniSections(userCustomSettings),
        [userCustomSettings],
    );

    const [customGameIni, setCustomGameIni] = useState<CustomIniSections>(game);
    const [customGusIni, setCustomGusIni] = useState<CustomIniSections>(gus);

    useEffect(() => {
        setCustomGameIni(game);
        setCustomGusIni(gus);
    }, [game, gus]);

    return {
        game: customGameIni,
        gus: customGusIni,
        setGame: setCustomGameIni,
        setGus: setCustomGusIni,
    };
}

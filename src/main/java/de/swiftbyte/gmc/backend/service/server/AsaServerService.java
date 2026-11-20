package de.swiftbyte.gmc.backend.service.server;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.exception.ServerException;
import de.swiftbyte.gmc.backend.service.setting.SettingProfileService;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.model.SettingProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AsaServerService {

    @Autowired
    private SettingProfileService profileService;

    /**
     * Render the ASA admin list as a plain-text file content (one ID per line).
     * Uses the server's {@link SettingProfile} GMC settings key "Adminlist".
     */
    public String renderAdminlist(GameServer server) {
        if (server.getGameType() != GameType.ARK_ASCENDED) {
            throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);
        }

        SettingProfile profile = profileService.getOrThrow(server.getSettingProfileId());
        Map<String, Object> gmc = profile.getGmcSettings();
        if (gmc == null) return "";

        Object raw = gmc.get("Adminlist");
        if (raw == null) return "";

        // Accept common shapes: List<?> or any Collection of values; otherwise fallback to string
        if (raw instanceof Collection<?> col) {
            List<String> lines = col.stream()
                    .map(v -> v == null ? null : v.toString())
                    .map(s -> s == null ? null : s.trim())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toList());
            return String.join("\n", lines);
        }

        // Fallback: return the value as-is (normalize newlines, trim)
        String s = raw.toString();
        if (s == null || s.isBlank()) return "";
        // Normalize CRLF to LF to be consistent
        s = s.replace("\r\n", "\n").trim();
        return s;
    }
}


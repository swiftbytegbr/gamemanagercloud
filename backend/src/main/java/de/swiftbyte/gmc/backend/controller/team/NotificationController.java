package de.swiftbyte.gmc.backend.controller.team;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NotificationDto;
import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/team/{teamId}/notifications", "/api/teams/{teamId}/notifications"})
@Validated
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final PermissionService permissions;

    @GetMapping
    @ApiAccountAllowed
    public List<NotificationDto> list(
            @PathVariable @Identifier String teamId,
            @RequestParam(value = "onlyUnread", required = false, defaultValue = "false") boolean onlyUnread,
            @AuthenticationPrincipal User user
    ) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        return notificationService.listLatest(teamId, user, onlyUnread);
    }

    @Data
    public static class MarkReadRequest {
        @NotEmpty
        private List<String> ids;
    }

    @PostMapping("/mark-read")
    public void markRead(
            @PathVariable @Identifier String teamId,
            @RequestBody @Valid MarkReadRequest req,
            @AuthenticationPrincipal User user
    ) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        notificationService.markRead(teamId, user, req.getIds());
    }

    @PostMapping("/mark-all-read")
    public void markAllRead(
            @PathVariable @Identifier String teamId,
            @AuthenticationPrincipal User user
    ) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        notificationService.markAllRead(teamId, user);
    }

    @GetMapping("/unread-count")
    @ApiAccountAllowed
    public long unreadCount(
            @PathVariable @Identifier String teamId,
            @AuthenticationPrincipal User user
    ) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        return notificationService.countUnread(teamId, user.getId());
    }
}

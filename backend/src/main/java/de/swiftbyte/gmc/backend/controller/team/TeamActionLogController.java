package de.swiftbyte.gmc.backend.controller.team;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.PageDto;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.TeamActionLogService;
import de.swiftbyte.gmc.backend.service.TeamService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team/{teamId}/action-log")
@RequiredArgsConstructor
@Validated
public class TeamActionLogController {

    private final TeamService teamService;
    private final TeamActionLogService teamActionLogService;
    private final PermissionService permissions;

    @GetMapping
    @ApiAccountAllowed
    public PageDto<ActionLogItem> getActionLog(
            @PathVariable @Identifier String teamId,
            @RequestParam(value = "page", required = false) @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "100") @Min(1) @Max(1000) Integer size,
            @AuthenticationPrincipal User user
    ) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        return PageDto.from(teamActionLogService.getActionLog(teamId, page, size));
    }

    @DeleteMapping("/clear")
    public void clearActionLog(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.requireGmcAdmin(user);
        teamActionLogService.clearActionLog(team);
    }
}

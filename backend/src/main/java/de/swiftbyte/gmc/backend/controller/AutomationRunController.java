package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.automation.AutomationRunService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/automation-run")
@Validated
@ApiAccountAllowed
public class AutomationRunController {

    @Autowired
    private AutomationRunService runService;

    @Autowired
    private PermissionService permissions;

    @GetMapping("/by-team/{teamId}")
    public org.springframework.data.domain.Page<AutomationRun> listRunsByTeam(@PathVariable @Identifier String teamId,
                                                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                                                              @RequestParam(name = "size", defaultValue = "20") int size,
                                                                              @RequestParam(name = "automationId", required = false) String automationId,
                                                                              @RequestParam(name = "status", required = false) String statuses,
                                                                              @RequestParam(name = "createdFrom", required = false) String createdFrom,
                                                                              @RequestParam(name = "createdTo", required = false) String createdTo,
                                                                              @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_AUTOMATIONS);
        return runService.listByTeam(teamId, automationId, statuses, createdFrom, createdTo, page, size);
    }

    @GetMapping("/{runId}")
    public AutomationRun getRun(@PathVariable @Identifier String runId,
                                @AuthenticationPrincipal User user) {
        AutomationRun run = runService.getOrThrow(runId);
        permissions.requireByTeamId(user, run.getTeamId(), Permission.ACCESS_AUTOMATIONS);
        return run;
    }

    @PostMapping("/{runId}/delete")
    public void deleteRun(@PathVariable @Identifier String runId,
                          @AuthenticationPrincipal User user) {
        AutomationRun run = runService.getOrThrow(runId);
        permissions.requireByTeamId(user, run.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        runService.deleteIfTerminal(runId);
    }

    @PostMapping("/{runId}/cancel")
    public AutomationRun cancelRun(@PathVariable @Identifier String runId,
                                   @AuthenticationPrincipal User user) {
        AutomationRun run = runService.getOrThrow(runId);
        permissions.requireByTeamId(user, run.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        return runService.cancel(runId);
    }
}

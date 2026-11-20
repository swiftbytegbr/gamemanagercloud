package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.dto.request.AutomationCreateRequest;
import de.swiftbyte.gmc.backend.dto.request.AutomationUpdateRequest;
import lombok.Data;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.automation.AutomationService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/automation")
@Validated
@ApiAccountAllowed
public class AutomationController {

    @Autowired
    private AutomationService automationService;

    @Autowired
    private PermissionService permissions;

    @GetMapping("/{automationId}")
    public Automation getAutomation(@PathVariable @Identifier String automationId,
                                    @AuthenticationPrincipal User user) {
        Automation automation = automationService.getOrThrow(automationId);
        permissions.requireByTeamId(user, automation.getTeamId(), Permission.ACCESS_AUTOMATIONS);
        return automation;
    }

    @GetMapping("/by-team/{teamId}")
    public List<Automation> listAutomationsByTeam(@PathVariable @Identifier String teamId,
                                                  @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_AUTOMATIONS);
        return automationService.findByTeam(teamId);
    }

    @PostMapping("/create")
    public Automation createAutomation(@RequestBody @Valid AutomationCreateRequest request,
                                       @AuthenticationPrincipal User user) {
        // Require manage permission on the target team
        permissions.requireByTeamId(user, request.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        return automationService.create(request);
    }

    @PostMapping("/{automationId}/edit")
    public Automation editAutomation(@PathVariable @Identifier String automationId,
                                     @RequestBody @Valid AutomationUpdateRequest request,
                                     @AuthenticationPrincipal User user) {
        // permission by existing automation team
        Automation existing = automationService.getOrThrow(automationId);
        permissions.requireByTeamId(user, existing.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        return automationService.update(automationId, request);
    }

    @PostMapping("/{automationId}/delete")
    public void deleteAutomation(@PathVariable @Identifier String automationId,
                                 @AuthenticationPrincipal User user) {
        Automation existing = automationService.getOrThrow(automationId);
        permissions.requireByTeamId(user, existing.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        automationService.delete(automationId);
    }


    @Data
    public static class ManualTriggerRequest {
        private List<String> targetIds;
        private Instant nextFireAt;
    }

    @PostMapping("/{automationId}/trigger-manual")
    public AutomationRun triggerManual(@PathVariable @Identifier String automationId,
                                       @RequestBody(required = false) ManualTriggerRequest req,
                                       @AuthenticationPrincipal User user) {
        Automation existing = automationService.getOrThrow(automationId);
        permissions.requireByTeamId(user, existing.getTeamId(), Permission.MANAGE_AUTOMATIONS);
        return automationService.triggerManual(automationId, req != null ? req.getTargetIds() : null, req != null ? req.getNextFireAt() : null);
    }


}

package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.service.HelpdeskService;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/helpdesk")
@Validated
public class HelpdeskController {

    @Autowired
    private HelpdeskService helpdeskService;

    @Autowired
    private PermissionService permissions;

    @Data
    public static class BugReportRequest {
        @NotBlank
        @Size(max = 200)
        private String subject;
        @NotBlank
        @Size(min = 10, max = 5000, message = "Message must be between {min} and {max} characters long")
        private String message;
        @Identifier
        private String teamId;
        @Size(max = 64)
        private String discordUsername; // optional
    }

    @PostMapping("/bug-report")
    public void submitBugReport(@RequestBody @Valid BugReportRequest request, @AuthenticationPrincipal User user) {
        // ensure the user can at least view the specified team
        permissions.requireByTeamId(user, request.getTeamId(), Permission.ACCESS_TEAM);
        helpdeskService.submitBugReport(request.getSubject(), request.getMessage(), request.getTeamId(), request.getDiscordUsername(), user);
    }
}

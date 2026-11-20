package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.value.LimitsConfig;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import de.swiftbyte.gmc.backend.service.admin.AdminService.GmcVersion;
import de.swiftbyte.gmc.backend.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/admin")
@Validated
public class AdminController {

    @Autowired
    private PermissionService permissions;

    @Autowired
    private AdminService adminService;

    @Data
    public static class DaemonUpdateRequest {
        @NotBlank
        @Size(min = 1, max = 32)
        private String version;
    }

    @PostMapping("/daemon-update")
    public void daemonUpdate(@RequestBody @Valid DaemonUpdateRequest request, @AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        adminService.publishDaemonVersion(request.getVersion());
    }

    @Data
    public static class GmcUpdateRequest {
        @NotBlank
        @Size(min = 1, max = 32)
        private String version;
        @Size(max = 200)
        private String description;
        @Size(max = 10_000)
        private String changelog;
    }

    @PostMapping("/gmc-update")
    public void gmcUpdate(@RequestBody @Valid GmcUpdateRequest request, @AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        GmcVersion gmcVersion = new GmcVersion();
        gmcVersion.version = request.getVersion();
        gmcVersion.description = request.getDescription();
        gmcVersion.changelog = request.getChangelog();
        gmcVersion.releaseDate = Instant.now();
        adminService.publishGmcVersion(gmcVersion);
    }

    @GetMapping("/gmc-version")
    public GmcVersion getGmcVersion(@AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        return adminService.getGmcVersion();
    }

    @GetMapping("/limits")
    public LimitsConfig getLimits(@AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        return adminService.getLimitsConfig();
    }

    @PostMapping("/limits")
    public void updateLimits(@RequestBody LimitsConfig limits, @AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        adminService.updateLimitsConfig(limits);
    }

    @GetMapping("/daemon-version")
    public String getDaemonVersion(@AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        return adminService.getDaemonVersion();
    }

    @GetMapping("/dev-deployment-access")
    public AdminService.GmcAdminDevDeploymentAccess getDevDeploymentAccess(@AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        return adminService.getGmcAdminDevDeploymentAccess();
    }

    @PostMapping("/dev-deployment-access")
    public void updateDevDeploymentAccess(@RequestBody AdminService.GmcAdminDevDeploymentAccess body, @AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        adminService.updateGmcAdminDevDeploymentAccess(body);
    }

}

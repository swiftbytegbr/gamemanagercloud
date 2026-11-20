package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.AdminStatsDto;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import de.swiftbyte.gmc.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Validated
@RequiredArgsConstructor
public class SystemStatsController {

    private final PermissionService permissions;
    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsDto getStats(@AuthenticationPrincipal User user) {
        permissions.requireGmcAdmin(user);
        return adminService.getSystemStats();
    }
}


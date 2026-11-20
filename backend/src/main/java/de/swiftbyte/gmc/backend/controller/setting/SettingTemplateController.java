package de.swiftbyte.gmc.backend.controller.setting;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.setting.SettingTemplateService;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setting/template")
@Validated
public class SettingTemplateController {

    @Autowired
    SettingTemplateService service;
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public List<SettingTemplate> getByGameType(@RequestParam(value = "gameType", required = false) SettingTemplate.SettingGameType gameType) {
        if (gameType == null) {
            return service.findAll();
        }
        return service.findAllByGameType(gameType);
    }

    @PostMapping("/upload")
    public void uploadSettingTemplates(@RequestBody @NotNull @Size(min = 1, max = 10_000_000) byte[] file, @AuthenticationPrincipal User user) {
        permissionService.requireGmcAdmin(user);
        service.uploadSettingTemplates(file);
    }

}

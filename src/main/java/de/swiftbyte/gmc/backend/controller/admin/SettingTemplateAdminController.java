package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.setting.SettingTemplateService;
import de.swiftbyte.gmc.backend.service.setting.SettingProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setting/templates")
@Validated
@RequiredArgsConstructor
public class SettingTemplateAdminController {

    private final SettingTemplateService templateService;
    private final PermissionService permissions;
    private final SettingProfileService profiles;

    @GetMapping
    public List<SettingTemplate> list(
            @RequestParam(value = "gameType", required = false) SettingTemplate.SettingGameType gameType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "type", required = false) SettingTemplate.Type type,
            @RequestParam(value = "appearance", required = false) SettingTemplate.Appearance appearance,
            @RequestParam(value = "required", required = false) Boolean required,
            @RequestParam(value = "inline", required = false) Boolean inline,
            @RequestParam(value = "disableSync", required = false) Boolean disableSync,
            @RequestParam(value = "dropdownAllowCustom", required = false) Boolean dropdownAllowCustom,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sortBy", required = false, defaultValue = "category,position") String sortBy,
            @RequestParam(value = "order", required = false, defaultValue = "asc") String order,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        return templateService.findFiltered(
                gameType, category, location, section, key, type, appearance,
                required, inline, disableSync, dropdownAllowCustom, query, sortBy, order
        );
    }

    // Move upload to admin scope as requested
    @PostMapping("/upload")
    public void upload(@RequestBody @NotNull @Size(min = 1, max = 10_000_000) byte[] file,
                       @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        templateService.uploadSettingTemplates(file);
    }

    // New: JSON batch upload (List<SettingTemplate>) replacing all templates
    @Data
    public static class OperationResult {
        public int templatesAffected;
        public int profilesBackfilled;
        public SettingTemplate template; // set for single-create
    }

    @PostMapping("/batch")
    public OperationResult uploadJson(@RequestBody List<@Valid SettingTemplate> templates,
                           @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        templateService.uploadSettingTemplatesJson(templates);
        int prof = profiles.backfillDefaultsForAllProfilesAfterTemplateUpload();
        OperationResult res = new OperationResult();
        res.templatesAffected = templates != null ? templates.size() : 0;
        res.profilesBackfilled = prof;
        return res;
    }

    // Update all fields (except id) of a template; validation identical to XLSX rules
    @PutMapping("/{id}")
    public SettingTemplate replace(@PathVariable String id,
                                   @RequestBody @Valid SettingTemplate body,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        // enforce id immutability
        body.setId(id);
        templateService.validateTemplateObject(body);
        return templateService.save(body);
    }

    // PATCH variant: merge provided fields into existing entity then validate
    @Data
    public static class PatchSettingTemplateRequest {
        public SettingTemplate.SettingGameType gameType;
        public String location;
        public String section;
        public String key;
        public String category;
        public String displayName;
        public String description;
        public String placeholder;
        public SettingTemplate.Type type;
        public Object defaultValue;
        public Boolean required;
        public String verifyingRegex;
        public String regexMessage;
        public Integer rangeFrom;
        public Integer rangeTo;
        public Boolean disableSync;
        public Integer position;
        public Boolean inline;
        public List<String> dropdownValues;
        public Boolean dropdownAllowCustom;
        public SettingTemplate.Appearance appearance;
    }

    @PatchMapping("/{id}")
    public SettingTemplate patch(@PathVariable String id,
                                 @RequestBody PatchSettingTemplateRequest partial,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        SettingTemplate existing = templateService.getOrThrow(id);

        if (partial.gameType != null) existing.setGameType(partial.gameType);
        if (partial.location != null) existing.setLocation(partial.location);
        if (partial.section != null) existing.setSection(partial.section);
        if (partial.key != null) existing.setKey(partial.key);
        if (partial.category != null) existing.setCategory(partial.category);
        if (partial.displayName != null) existing.setDisplayName(partial.displayName);
        if (partial.description != null) existing.setDescription(partial.description);
        if (partial.placeholder != null) existing.setPlaceholder(partial.placeholder);
        if (partial.type != null) existing.setType(partial.type);
        if (partial.defaultValue != null) existing.setDefaultValue(partial.defaultValue);
        if (partial.required != null) existing.setRequired(partial.required);
        if (partial.verifyingRegex != null) existing.setVerifyingRegex(partial.verifyingRegex);
        if (partial.regexMessage != null) existing.setRegexMessage(partial.regexMessage);
        if (partial.rangeFrom != null) existing.setRangeFrom(partial.rangeFrom);
        if (partial.rangeTo != null) existing.setRangeTo(partial.rangeTo);
        if (partial.disableSync != null) existing.setDisableSync(partial.disableSync);
        if (partial.position != null) existing.setPosition(partial.position);
        if (partial.inline != null) existing.setInline(partial.inline);
        if (partial.dropdownValues != null) existing.setDropdownValues(partial.dropdownValues);
        if (partial.dropdownAllowCustom != null) existing.setDropdownAllowCustom(partial.dropdownAllowCustom);
        if (partial.appearance != null) existing.setAppearance(partial.appearance);

        templateService.validateTemplateObject(existing);
        return templateService.save(existing);
    }

    // New: create a single setting template
    @PostMapping
    public OperationResult create(@RequestBody @Valid SettingTemplate body,
                                  @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        SettingTemplate saved = templateService.createOne(body);
        int prof = profiles.backfillDefaultsForAllProfilesAfterTemplateUpload();
        OperationResult res = new OperationResult();
        res.templatesAffected = 1;
        res.profilesBackfilled = prof;
        res.template = saved;
        return res;
    }

    @Data
    public static class ReorderRequestItem {
        public String id;
        public int position;
    }

    @Data
    public static class ReorderRequest {
        public SettingTemplate.SettingGameType gameType; // optional; will accept both if ARK_BOTH
        public String category; // required; reorder happens within a category
        public List<ReorderRequestItem> items;
    }

    @PostMapping("/reorder")
    public void reorder(@RequestBody @Valid ReorderRequest body,
                        @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        templateService.reorderWithinCategory(body.gameType, body.category, body.items);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id,
                       @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        templateService.delete(id);
    }
}

package de.swiftbyte.gmc.backend.service.setting;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.backend.exception.XlsxParseException;
import de.swiftbyte.gmc.backend.exception.XlsxViolation;
import de.swiftbyte.gmc.backend.repository.SettingTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

@Slf4j
@Service
public class SettingTemplateService {

    @Autowired
    SettingTemplateRepository repository;

	@Autowired
	private SettingProfileService settingProfileService;

	// Column indices for XLSX template parsing
	private static final int CELL_LOCATION = 0;
	private static final int CELL_SECTION = 1;
	private static final int CELL_GAME_TYPE = 2;
	private static final int CELL_KEY = 3;
	private static final int CELL_CATEGORY = 4;
	private static final int CELL_POSITION = 5;
	private static final int CELL_DEFAULT_VALUE = 6;
	private static final int CELL_INLINE = 7;
	private static final int CELL_DISPLAY_NAME = 8;
	private static final int CELL_TYPE = 9;
	private static final int CELL_REQUIRED = 10;
	private static final int CELL_VERIFYING_REGEX = 11;
	private static final int CELL_REGEX_MESSAGE = 12;
	private static final int CELL_RANGE_FROM = 13;
	private static final int CELL_RANGE_TO = 14;
	private static final int CELL_APPEARANCE = 15;
	private static final int CELL_PLACEHOLDER = 16;
	private static final int CELL_DESCRIPTION = 17;
	private static final int CELL_DROPDOWN_ALLOW_CUSTOM = 18;
	private static final int CELL_DROPDOWN_VALUES = 19;
	private static final int CELL_DISABLE_SYNC = 20;

    // Human-readable column names for error context
    private static final String[] COLUMN_NAMES = new String[] {
            "Location","Section","GameType","Key","Category","Position (In Category)",
            "Default","Inline","Name","Type","Required","Regex","RegexMessage",
            "rangeFrom","rangeTo","Appearance","Placeholder","Description",
            "Dropdown Custom","Dropdown Values","disableSync"
    };


    public List<SettingTemplate> findAll() {
        return repository.findAll();
    }

    public List<SettingTemplate> findAllByGameType(SettingTemplate.SettingGameType gameType) {
        if (gameType == null) gameType = SettingTemplate.SettingGameType.ARK_BOTH;
        return repository.getAllByGameTypeOrGameType(gameType, SettingTemplate.SettingGameType.ARK_BOTH);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<SettingTemplate> findFiltered(
            SettingTemplate.SettingGameType gameType,
            String category,
            String location,
            String section,
            String key,
            SettingTemplate.Type type,
            SettingTemplate.Appearance appearance,
            Boolean required,
            Boolean inline,
            Boolean disableSync,
            Boolean dropdownAllowCustom,
            String q,
            String sortBy,
            String order
    ) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (gameType != null) {
            if (gameType == SettingTemplate.SettingGameType.ARK_BOTH) {
                // Return all templates that are ARK_BOTH
                criteria.add(Criteria.where("gameType").is(SettingTemplate.SettingGameType.ARK_BOTH));
            } else {
                // Match requested type or ARK_BOTH
                criteria.add(new Criteria().orOperator(
                        Criteria.where("gameType").is(gameType),
                        Criteria.where("gameType").is(SettingTemplate.SettingGameType.ARK_BOTH)
                ));
            }
        }

        if (category != null && !category.isBlank()) criteria.add(Criteria.where("category").is(category));
        if (location != null && !location.isBlank()) criteria.add(Criteria.where("location").is(location));
        if (section != null && !section.isBlank()) criteria.add(Criteria.where("section").is(section));
        if (key != null && !key.isBlank()) criteria.add(Criteria.where("key").is(key));
        if (type != null) criteria.add(Criteria.where("type").is(type));
        if (appearance != null) criteria.add(Criteria.where("appearance").is(appearance));
        if (required != null) criteria.add(Criteria.where("required").is(required));
        if (inline != null) criteria.add(Criteria.where("inline").is(inline));
        if (disableSync != null) criteria.add(Criteria.where("disableSync").is(disableSync));
        if (dropdownAllowCustom != null) criteria.add(Criteria.where("dropdownAllowCustom").is(dropdownAllowCustom));

        if (q != null && !q.isBlank()) {
            String regex = ".*" + java.util.regex.Pattern.quote(q) + ".*";
            criteria.add(new Criteria().orOperator(
                    Criteria.where("displayName").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("key").regex(regex, "i"),
                    Criteria.where("category").regex(regex, "i"),
                    Criteria.where("location").regex(regex, "i"),
                    Criteria.where("section").regex(regex, "i")
            ));
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        // Sorting: allow comma-separated fields; default category,position asc
        Sort.Direction direction = (order != null && order.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort;
        if (sortBy == null || sortBy.isBlank()) {
            sort = Sort.by(Sort.Direction.ASC, "category").and(Sort.by(Sort.Direction.ASC, "position"));
        } else {
            String[] fields = sortBy.split(",");
            sort = Sort.by(direction, fields[0].trim());
            for (int i = 1; i < fields.length; i++) {
                sort = sort.and(Sort.by(direction, fields[i].trim()));
            }
        }
        query.with(sort);

        return mongoTemplate.find(query, SettingTemplate.class);
    }

    // Public validation for admin updates: apply the same rules as XLSX
    public void validateTemplateObject(SettingTemplate template) {
        // Normalize for SUBTITLE appearance like the XLSX import does
        if (template != null && template.getAppearance() == SettingTemplate.Appearance.SUBTITLE) {
            // Clear disallowed fields to keep data consistent with XLSX behavior
            template.setLocation(null);
            template.setSection(null);
            template.setKey(null);
            template.setDefaultValue(null);
            template.setInline(false);
            template.setType(null);
            template.setRequired(false);
            template.setVerifyingRegex(null);
            template.setRegexMessage(null);
            template.setRangeFrom(0);
            template.setRangeTo(0);
            template.setPlaceholder(null);
            template.setDropdownAllowCustom(false);
            template.setDropdownValues(null);
            template.setDisableSync(false);
        }
        // Reuse strict validator logic (row index -1 indicates API origin)
        validateTemplate(template, -1);
    }

    /**
     * Replace all templates with the provided list (JSON upload semantics),
     * validating each entry with the same rules as XLSX import and then
     * backfilling defaults into existing profiles.
     */
    public void uploadSettingTemplatesJson(List<SettingTemplate> templates) {
        if (templates == null) templates = List.of();

        // Validate + normalize each template first to collect all errors early
        for (SettingTemplate t : templates) {
            validateTemplateObject(t);
        }

        repository.deleteAll();
        repository.saveAll(templates);
    }

    /**
     * Create a single template (admin API). Applies validation and normalization
     * identical to XLSX rules and triggers a defaults backfill.
     */
    public SettingTemplate createOne(SettingTemplate template) {
        if (template == null) throw new IllegalArgumentException("template_required");
        validateTemplateObject(template);

        // For non-subtitle entries, guard against duplicates by location+section+key
        if (template.getAppearance() != SettingTemplate.Appearance.SUBTITLE) {
            SettingTemplate existing = repository.findByLocationAndSectionAndKey(
                    template.getLocation(), template.getSection(), template.getKey());
            if (existing != null) {
                throw new de.swiftbyte.gmc.backend.exception.BadRequestException("template_already_exists");
            }
        }

        return repository.save(template);
    }

    public SettingTemplate getOrThrow(String id) {
        return repository.findById(id).orElseThrow(de.swiftbyte.gmc.backend.exception.NotFoundException::new);
    }

    public SettingTemplate save(SettingTemplate template) {
        return repository.save(template);
    }

    public void delete(String id) {
        if (id == null || id.isBlank()) return;
        repository.deleteById(id);
    }

    public void reorderWithinCategory(SettingTemplate.SettingGameType gameType, String category, List<de.swiftbyte.gmc.backend.controller.admin.SettingTemplateAdminController.ReorderRequestItem> items) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        if (items == null || items.isEmpty()) return;

        // Load existing docs for validation
        List<String> ids = new ArrayList<>();
        for (var it : items) if (it != null && it.id != null) ids.add(it.id);
        List<SettingTemplate> templates = repository.findAllById(ids);

        // Validate category and gameType membership
        for (SettingTemplate t : templates) {
            if (!category.equals(t.getCategory())) {
                throw new IllegalArgumentException("All items must belong to category: " + category);
            }
            if (gameType != null && gameType != SettingTemplate.SettingGameType.ARK_BOTH) {
                // allow ARK_BOTH templates for any game filter; otherwise require matching
                if (t.getGameType() != gameType && t.getGameType() != SettingTemplate.SettingGameType.ARK_BOTH) {
                    throw new IllegalArgumentException("Item " + t.getId() + " not in requested gameType scope");
                }
            }
        }

        // Apply positions
        for (var it : items) {
            for (SettingTemplate t : templates) {
                if (t.getId().equals(it.id)) {
                    t.setPosition(it.position);
                    break;
                }
            }
        }
        repository.saveAll(templates);
    }


    public void uploadSettingTemplates(byte[] file) {
        try {

            InputStream data = new ByteArrayInputStream(file);
            Workbook workbook = new XSSFWorkbook(data);
            Sheet sheet = workbook.getSheetAt(0);

            List<SettingTemplate> templates = new ArrayList<>();
            List<XlsxViolation> collected = new ArrayList<>();

            for (Row row : sheet) {

                try {
                    if (row.getRowNum() == 0) continue;


                    for (Cell cell : row) {
                        if (cell == null) continue;
                        cell.setCellType(CellType.STRING);
                    }

                    // Skip fully empty lines (no Location and no Appearance)
                    boolean locInvalid = isInvalid(row.getCell(CELL_LOCATION));
                    boolean appInvalid = isInvalid(row.getCell(CELL_APPEARANCE));
                    if (locInvalid && appInvalid) continue;

                    SettingTemplate template = new SettingTemplate();

                    // Read appearance first to branch logic for SUBTITLE entries
                    template.setAppearance(tryReadEnum(row, CELL_APPEARANCE, SettingTemplate.Appearance.class, collected));
                    boolean isSubtitle = template.getAppearance() == SettingTemplate.Appearance.SUBTITLE;

                    // For non-subtitle rows, skip lines that do not have a location (header/empty lines)
                    if (!isSubtitle && locInvalid) continue;

                    if (isSubtitle) {
                        // Allowed fields: gameType (required), category (required), position (required), name/displayName (required), description (optional)
                        template.setGameType(tryReadGameType(row, collected));
                        template.setCategory(tryReadString(row, CELL_CATEGORY, collected));

                        Integer pos = tryReadInt(row, CELL_POSITION, collected);
                        template.setPosition(pos != null ? pos : 0);

                        template.setDisplayName(tryReadString(row, CELL_DISPLAY_NAME, collected));
                        template.setDescription(tryReadOptionalNullable(row, CELL_DESCRIPTION, collected));

                        // All other fields must be null/empty or the literal placeholder "SUBTITLE" (any case).
                        // If provided as placeholder, we override to null/defaults; otherwise, record a violation.
                        enforceSubtitleOnlyOrPlaceholder(row, template, collected);

                    } else {
                        // Regular (non-subtitle) flow as before
                        template.setLocation(tryReadString(row, CELL_LOCATION, collected));
                        template.setSection(tryReadOptionalString(row, CELL_SECTION, "", collected));
                        template.setGameType(tryReadGameType(row, collected));
                        template.setKey(tryReadString(row, CELL_KEY, collected));
                        template.setCategory(tryReadOptionalString(row, CELL_CATEGORY, "Uncategorized", collected));
                        template.setPosition(tryReadOptionalInt(row, CELL_POSITION, 0, collected));
                        template.setInline(tryReadOptionalBoolean(row, CELL_INLINE, false, collected));
                        template.setDisplayName(tryReadString(row, CELL_DISPLAY_NAME, collected));
                        template.setType(tryReadEnum(row, CELL_TYPE, SettingTemplate.Type.class, collected));
                        template.setRequired(tryReadOptionalBoolean(row, CELL_REQUIRED, false, collected));
                        template.setVerifyingRegex(tryReadOptionalNullable(row, CELL_VERIFYING_REGEX, collected));
                        template.setRegexMessage(tryReadOptionalNullable(row, CELL_REGEX_MESSAGE, collected));
                        template.setRangeFrom(tryReadOptionalInt(row, CELL_RANGE_FROM, 0, collected));
                        template.setRangeTo(tryReadOptionalInt(row, CELL_RANGE_TO, 0, collected));
                        template.setPlaceholder(tryReadOptionalNullable(row, CELL_PLACEHOLDER, collected));
                        template.setDescription(tryReadOptionalNullable(row, CELL_DESCRIPTION, collected));
                        template.setDropdownAllowCustom(tryReadOptionalBoolean(row, CELL_DROPDOWN_ALLOW_CUSTOM, false, collected));
                        template.setDropdownValues(tryReadOptionalList(row, CELL_DROPDOWN_VALUES, collected));
                        template.setDisableSync(tryReadOptionalBoolean(row, CELL_DISABLE_SYNC, false, collected));

                        if (!isInvalid(row.getCell(CELL_DEFAULT_VALUE))) {
                            switch (template.getType()) {
                                case INTEGER -> template.setDefaultValue(tryReadInt(row, CELL_DEFAULT_VALUE, collected));
                                case STRING -> template.setDefaultValue(tryReadString(row, CELL_DEFAULT_VALUE, collected));
                                case FLOAT -> template.setDefaultValue(tryReadFloat(row, CELL_DEFAULT_VALUE, collected));
                                case BOOLEAN -> template.setDefaultValue(tryReadBoolean(row, CELL_DEFAULT_VALUE, collected));
                                case LIST -> template.setDefaultValue(tryReadList(row, CELL_DEFAULT_VALUE, collected));
                            }
                        }
                    }

                    validateTemplateCollect(template, row.getRowNum(), collected);
                    templates.add(template);
                } catch (Exception e) {
                    log.error("Parse failed at " + row.getRowNum() + ": ", e);
                    collected.add(toViolation(row.getRowNum(), -1, null, "unexpected_error", null));
                }
            }
            if (!collected.isEmpty()) {
                XlsxParseException ex = new XlsxParseException("parse_exception");
                for (XlsxViolation v : collected) ex.addViolation(v);
                throw ex;
            }
            repository.deleteAll();
            repository.saveAll(templates);

            // After updating templates, backfill new defaults into existing profiles
            // so profiles gain newly added settings similarly to default profile creation.
            // This keeps existing values intact and only fills missing keys.
            try {
                settingProfileService.backfillDefaultsForAllProfilesAfterTemplateUpload();
            } catch (Exception e) {
                // Do not fail the upload if backfilling encounters an issue; log and continue.
                log.error("Backfilling defaults after template upload failed", e);
            }

        } catch (IOException e) {
            log.error("Parse failed: ", e);
            throw new XlsxParseException().setFailedRow(-1);
        }
    }

    private boolean isInvalid(Cell cell) {
        try {
            return cell == null || cell.getStringCellValue().isBlank();
        } catch (IllegalStateException e) {
            log.info(cell.getRowIndex() + " " + cell.getColumnIndex());
            return true;
        }
    }

    // ---------- Helper methods (structure + validation) ----------

    private String colName(int idx) {
        if (idx >= 0 && idx < COLUMN_NAMES.length) return COLUMN_NAMES[idx];
        return "column_" + idx;
    }

    private XlsxParseException fail(int rowIndex, int colIndex, String code, Object value) {
        // Excel UI rows are 1-based; store 1-based row index
        return new XlsxParseException(code)
                .setFailedRow(rowIndex + 1)
                .setFailedColumnIndex(colIndex)
                .setFailedColumnName(colName(colIndex))
                .setFailedValue(value);
    }

    private XlsxViolation toViolation(int rowIndex, int colIndex, String colName, String code, Object value) {
        return new XlsxViolation()
                // Excel UI rows are 1-based; store 1-based row index
                .setRow(rowIndex + 1)
                .setColumnIndex(colIndex)
                .setColumnName(colName != null ? colName : colName(colIndex))
                .setCode(code)
                .setRejectedValue(value);
    }

    private String tryReadString(Row row, int idx, List<XlsxViolation> out) {
        try { return readString(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), null)); return null; }
    }

    private String tryReadOptionalString(Row row, int idx, String def, List<XlsxViolation> out) {
        try { return readOptionalString(row, idx, def); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), null)); return def; }
    }

    private String tryReadOptionalNullable(Row row, int idx, List<XlsxViolation> out) {
        try { return readOptionalNullable(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), null)); return null; }
    }

    private Integer tryReadInt(Row row, int idx, List<XlsxViolation> out) {
        try { return readInt(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private int tryReadOptionalInt(Row row, int idx, int def, List<XlsxViolation> out) {
        try { return readOptionalInt(row, idx, def); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return def; }
    }

    private Float tryReadFloat(Row row, int idx, List<XlsxViolation> out) {
        try { return readFloat(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private boolean tryReadOptionalBoolean(Row row, int idx, boolean def, List<XlsxViolation> out) {
        try { return readOptionalBoolean(row, idx, def); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return def; }
    }

    private Boolean tryReadBoolean(Row row, int idx, List<XlsxViolation> out) {
        try { return readBoolean(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private List<String> tryReadList(Row row, int idx, List<XlsxViolation> out) {
        try { return readList(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private List<String> tryReadOptionalList(Row row, int idx, List<XlsxViolation> out) {
        try { return readOptionalList(row, idx); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private SettingTemplate.SettingGameType tryReadGameType(Row row, List<XlsxViolation> out) {
        try { return readGameType(row); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), CELL_GAME_TYPE, null, e.getMessage(), row.getCell(CELL_GAME_TYPE) != null ? row.getCell(CELL_GAME_TYPE).getStringCellValue() : null)); return null; }
    }

    private <E extends Enum<E>> E tryReadEnum(Row row, int idx, Class<E> enumType, List<XlsxViolation> out) {
        try { return readEnum(row, idx, enumType); } catch (XlsxParseException e) { out.add(toViolation(row.getRowNum(), idx, null, e.getMessage(), row.getCell(idx) != null ? row.getCell(idx).getStringCellValue() : null)); return null; }
    }

    private void validateTemplateCollect(SettingTemplate t, int rowIndex, List<XlsxViolation> out) {
        if (t.getAppearance() == SettingTemplate.Appearance.SUBTITLE) {
            // For SUBTITLE rows, required-field violations (category/name/position) are captured
            // during parsing via tryReadString/tryReadInt as xlsx.required. Avoid duplicating them here.
            // Only add semantic checks not covered by parsing, e.g. non-negative position.
            if (t.getGameType() == null) out.add(toViolation(rowIndex, CELL_GAME_TYPE, null, "required", null));
            if (t.getPosition() < 0) out.add(toViolation(rowIndex, CELL_POSITION, null, "invalid_position", t.getPosition()));
            return;
        }

        // Regular validations
        if (t.getLocation() == null || t.getLocation().isBlank()) out.add(toViolation(rowIndex, CELL_LOCATION, null, "required", null));
        if (t.getKey() == null || t.getKey().isBlank()) out.add(toViolation(rowIndex, CELL_KEY, null, "required", null));
        if (t.getDisplayName() == null || t.getDisplayName().isBlank()) out.add(toViolation(rowIndex, CELL_DISPLAY_NAME, null, "required", null));
        if (t.getType() == null) out.add(toViolation(rowIndex, CELL_TYPE, null, "required", null));

        // Range validation: only allowed for INTEGER or FLOAT types
        if (t.getType() != null && t.getType() != SettingTemplate.Type.INTEGER && t.getType() != SettingTemplate.Type.FLOAT) {
            if (t.getRangeFrom() != 0) out.add(toViolation(rowIndex, CELL_RANGE_FROM, null, "range_unsupported_for_type", t.getType()));
            if (t.getRangeTo() != 0) out.add(toViolation(rowIndex, CELL_RANGE_TO, null, "range_unsupported_for_type", t.getType()));
        } else if (t.getType() == SettingTemplate.Type.INTEGER || t.getType() == SettingTemplate.Type.FLOAT) {
            if (t.getRangeFrom() != 0 && t.getRangeTo() != 0 && t.getRangeTo() < t.getRangeFrom()) {
                out.add(toViolation(rowIndex, CELL_RANGE_TO, null, "range_to_lt_from", t.getRangeFrom() + ":" + t.getRangeTo()));
            }
        }

        if (t.getAppearance() == null) {
            out.add(toViolation(rowIndex, CELL_APPEARANCE, null, "required", null));
        } else if (t.getType() != null) {
            switch (t.getType()) {
                case BOOLEAN -> {
                    if (t.getAppearance() != SettingTemplate.Appearance.CHECKBOX) {
                        out.add(toViolation(rowIndex, CELL_APPEARANCE, null, "appearance_mismatch", t.getType() + ":" + t.getAppearance()));
                    }
                }
                case STRING -> {
                    boolean ok = t.getAppearance() == SettingTemplate.Appearance.TEXT
                            || t.getAppearance() == SettingTemplate.Appearance.TEXT_AREA
                            || t.getAppearance() == SettingTemplate.Appearance.PASSWORD
                            || t.getAppearance() == SettingTemplate.Appearance.DROPDOWN;
                    if (!ok) out.add(toViolation(rowIndex, CELL_APPEARANCE, null, "appearance_mismatch", t.getType() + ":" + t.getAppearance()));
                }
                case INTEGER, FLOAT -> {
                    if (t.getAppearance() != SettingTemplate.Appearance.TEXT) {
                        out.add(toViolation(rowIndex, CELL_APPEARANCE, null, "appearance_mismatch", t.getType() + ":" + t.getAppearance()));
                    }
                }
                case LIST -> {
                    if (t.getAppearance() != SettingTemplate.Appearance.LIST) {
                        out.add(toViolation(rowIndex, CELL_APPEARANCE, null, "appearance_mismatch", t.getType() + ":" + t.getAppearance()));
                    }
                }
            }
        }
    }

    // For SUBTITLE rows: if a disallowed column contains a value other than empty/"subtitle",
    // record a violation. Then ensure the template's disallowed fields are nulled/defaulted.
    private void enforceSubtitleOnlyOrPlaceholder(Row row, SettingTemplate t, List<XlsxViolation> out) {
        // Helper to flag unexpected non-placeholder content
        IntConsumer ensureEmptyOrSubtitle = idx -> {
            Cell c = row.getCell(idx);
            if (c == null) return;
            String v;
            try { v = c.getStringCellValue(); } catch (Exception e) { v = null; }
            if (v == null || v.isBlank()) return;
            if (!"SUBTITLE".equalsIgnoreCase(v)) {
                out.add(toViolation(row.getRowNum(), idx, null, "must_be_empty_or_subtitle", v));
            }
        };

        // Disallowed columns: everything except gameType, category, position, displayName, description, appearance
        int[] disallowed = new int[] {
                CELL_LOCATION, CELL_SECTION, /* allow CELL_GAME_TYPE */ CELL_KEY, CELL_DEFAULT_VALUE,
                CELL_INLINE, CELL_TYPE, CELL_REQUIRED, CELL_VERIFYING_REGEX, CELL_REGEX_MESSAGE,
                CELL_RANGE_FROM, CELL_RANGE_TO, CELL_PLACEHOLDER, CELL_DROPDOWN_ALLOW_CUSTOM,
                CELL_DROPDOWN_VALUES, CELL_DISABLE_SYNC
        };
        for (int idx : disallowed) ensureEmptyOrSubtitle.accept(idx);

        // Null/clear all disallowed fields in the template
        t.setLocation(null);
        t.setSection(null);
        // keep gameType as provided for subtitles
        t.setKey(null);
        t.setDefaultValue(null);
        t.setInline(false);
        t.setType(null);
        t.setRequired(false);
        t.setVerifyingRegex(null);
        t.setRegexMessage(null);
        t.setRangeFrom(0);
        t.setRangeTo(0);
        t.setPlaceholder(null);
        t.setDropdownAllowCustom(false);
        t.setDropdownValues(null);
        t.setDisableSync(false);
    }

    private String readString(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) throw fail(row.getRowNum(), idx, "required", null);
        return c.getStringCellValue();
    }

    private String readOptionalString(Row row, int idx, String defaultValue) {
        Cell c = row.getCell(idx);
        return isInvalid(c) ? defaultValue : c.getStringCellValue();
    }

    private String readOptionalNullable(Row row, int idx) {
        Cell c = row.getCell(idx);
        return isInvalid(c) ? null : c.getStringCellValue();
    }

    private Integer readInt(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) throw fail(row.getRowNum(), idx, "required", null);
        try {
            return Integer.parseInt(c.getStringCellValue());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_integer", c.getStringCellValue());
        }
    }

    private int readOptionalInt(Row row, int idx, int defaultValue) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) return defaultValue;
        try {
            return Integer.parseInt(c.getStringCellValue());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_integer", c.getStringCellValue());
        }
    }

    private Float readFloat(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) throw fail(row.getRowNum(), idx, "required", null);
        try {
            return Float.parseFloat(c.getStringCellValue());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_float", c.getStringCellValue());
        }
    }


    private Boolean readBoolean(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) throw fail(row.getRowNum(), idx, "required", null);
        try {
            return Boolean.parseBoolean(c.getStringCellValue());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_boolean", c.getStringCellValue());
        }
    }

    private boolean readOptionalBoolean(Row row, int idx, boolean defaultValue) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) return defaultValue;
        try {
            return Boolean.parseBoolean(c.getStringCellValue());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_boolean", c.getStringCellValue());
        }
    }

    private List<String> readList(Row row, int idx) {
        String s = readString(row, idx);
        return Arrays.stream(s.split(";"))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .toList();
    }

    private List<String> readOptionalList(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (isInvalid(c)) return null;
        return Arrays.stream(c.getStringCellValue().split(";"))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .toList();
    }

    private SettingTemplate.SettingGameType readGameType(Row row) {
        String raw = readString(row, CELL_GAME_TYPE).replace("?", "");
        try {
            return SettingTemplate.SettingGameType.valueOf(raw);
        } catch (Exception ex) {
            throw fail(row.getRowNum(), CELL_GAME_TYPE, "invalid_enum", raw);
        }
    }

    private <E extends Enum<E>> E readEnum(Row row, int idx, Class<E> enumType) {
        String raw = readString(row, idx);
        try {
            return Enum.valueOf(enumType, raw.toUpperCase());
        } catch (Exception ex) {
            throw fail(row.getRowNum(), idx, "invalid_enum", raw);
        }
    }

    private void validateTemplate(SettingTemplate t, int rowIndex) {
        if (t.getGameType() == null) {
            throw fail(rowIndex, CELL_GAME_TYPE, "required", null);
        }
        if (t.getAppearance() == SettingTemplate.Appearance.SUBTITLE) {
            if (t.getCategory() == null || t.getCategory().isBlank())
                throw fail(rowIndex, CELL_CATEGORY, "required", null);
            if (t.getDisplayName() == null || t.getDisplayName().isBlank())
                throw fail(rowIndex, CELL_DISPLAY_NAME, "required", null);
            if (t.getPosition() < 0)
                throw fail(rowIndex, CELL_POSITION, "invalid_position", t.getPosition());
            return;
        }
        if (t.getLocation() == null || t.getLocation().isBlank())
            throw fail(rowIndex, CELL_LOCATION, "required", null);
        if (t.getKey() == null || t.getKey().isBlank())
            throw fail(rowIndex, CELL_KEY, "required", null);
        if (t.getDisplayName() == null || t.getDisplayName().isBlank())
            throw fail(rowIndex, CELL_DISPLAY_NAME, "required", null);
        if (t.getType() == null)
            throw fail(rowIndex, CELL_TYPE, "required", null);

        // Range validation: only allowed for INTEGER or FLOAT types
        if (t.getType() != SettingTemplate.Type.INTEGER && t.getType() != SettingTemplate.Type.FLOAT) {
            if (t.getRangeFrom() != 0) throw fail(rowIndex, CELL_RANGE_FROM, "range_unsupported_for_type", t.getType());
            if (t.getRangeTo() != 0) throw fail(rowIndex, CELL_RANGE_TO, "range_unsupported_for_type", t.getType());
        } else {
            if (t.getRangeFrom() != 0 && t.getRangeTo() != 0 && t.getRangeTo() < t.getRangeFrom()) {
                throw fail(rowIndex, CELL_RANGE_TO, "range_to_lt_from", t.getRangeFrom() + ":" + t.getRangeTo());
            }
        }

        // Appearance vs Type compatibility
        if (t.getAppearance() == null) {
            throw fail(rowIndex, CELL_APPEARANCE, "required", null);
        }
        switch (t.getType()) {
            case BOOLEAN -> {
                if (t.getAppearance() != SettingTemplate.Appearance.CHECKBOX) {
                    throw fail(rowIndex, CELL_APPEARANCE, "appearance_mismatch", t.getType() + ":" + t.getAppearance());
                }
            }
            case STRING -> {
                boolean ok = t.getAppearance() == SettingTemplate.Appearance.TEXT
                        || t.getAppearance() == SettingTemplate.Appearance.TEXT_AREA
                        || t.getAppearance() == SettingTemplate.Appearance.PASSWORD
                        || t.getAppearance() == SettingTemplate.Appearance.DROPDOWN;
                if (!ok) {
                    throw fail(rowIndex, CELL_APPEARANCE, "appearance_mismatch", t.getType() + ":" + t.getAppearance());
                }
            }
            case INTEGER, FLOAT -> {
                if (t.getAppearance() != SettingTemplate.Appearance.TEXT) {
                    throw fail(rowIndex, CELL_APPEARANCE, "appearance_mismatch", t.getType() + ":" + t.getAppearance());
                }
            }
            case LIST -> {
                if (t.getAppearance() != SettingTemplate.Appearance.LIST) {
                    throw fail(rowIndex, CELL_APPEARANCE, "appearance_mismatch", t.getType() + ":" + t.getAppearance());
                }
            }
        }
    }


}

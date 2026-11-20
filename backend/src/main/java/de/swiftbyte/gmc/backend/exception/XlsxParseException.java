package de.swiftbyte.gmc.backend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class XlsxParseException extends GmcException {

    private int failedRow;
    private Integer failedColumnIndex;
    private String failedColumnName;
    private Object failedValue;
    private List<XlsxViolation> violations;

    public XlsxParseException() {
    }

    public XlsxParseException(String message) {
        super("xlsx." + message);
    }

    public XlsxParseException addViolation(XlsxViolation v) {
        if (this.violations == null) this.violations = new ArrayList<>();
        this.violations.add(v);
        return this;
    }
}

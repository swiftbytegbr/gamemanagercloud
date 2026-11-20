package de.swiftbyte.gmc.backend.exception;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class XlsxViolation {
    private int row;
    private Integer columnIndex;
    private String columnName;
    private String code;
    private Object rejectedValue;
}


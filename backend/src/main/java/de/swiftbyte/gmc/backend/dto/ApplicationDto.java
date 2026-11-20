package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationDto {

    String applicationId;
    String name;
    List<Permission> permissions;


}

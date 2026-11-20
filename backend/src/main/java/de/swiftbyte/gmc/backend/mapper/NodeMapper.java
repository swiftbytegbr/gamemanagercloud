package de.swiftbyte.gmc.backend.mapper;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.controller.NodeController.NodeSettingsUpdateRequest;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NodeMapper {

    NodeDto toDto(Node node);

    NodeSettings toNodeSettings(NodeSettingsUpdateRequest request);

}

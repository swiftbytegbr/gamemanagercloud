package de.swiftbyte.gmc.backend.mapper;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.common.entity.GameServerDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameServerMapper {

    GameServerDto toCommonDto(GameServer server);

}


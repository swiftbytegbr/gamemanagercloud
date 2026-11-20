package de.swiftbyte.gmc.backend.mapper;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

}


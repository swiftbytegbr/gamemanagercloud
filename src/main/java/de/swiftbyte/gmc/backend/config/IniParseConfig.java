package de.swiftbyte.gmc.backend.config;

import de.swiftbyte.gmc.common.parser.IniConverter;
import de.swiftbyte.gmc.common.parser.IniParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IniParseConfig {

    @Bean
    IniParser iniParser() {
        return new IniParser();
    }


    @Bean
    IniConverter iniConverter() {
        return new IniConverter();
    }
}

package de.swiftbyte.gmc.backend.service.setting;

import de.swiftbyte.gmc.common.parser.IniConverter;
import de.swiftbyte.gmc.common.parser.IniParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;

@Service
public class IniService {

    @Autowired
    private IniParser iniParser;

    @Autowired
    private IniConverter iniConverter;

    public LinkedHashMap<String, LinkedHashMap<String, Object>> parseIni(String ini) {
        return iniParser.parseIni(ini);
    }

    public String generateIni(LinkedHashMap<String, LinkedHashMap<String, Object>> map) {
        return iniConverter.convertFromMap(map);
    }

    public String generateIni(String json) throws IOException {
        return iniConverter.convertFromJson(json);
    }


}

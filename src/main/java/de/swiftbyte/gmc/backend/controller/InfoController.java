package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import de.swiftbyte.gmc.backend.service.admin.AdminService.GmcVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
public class InfoController {

    @Autowired
    private AdminService adminService;

    @ApiAccountAllowed
    @GetMapping("/version")
    public GmcVersion version() {
        return adminService.getGmcVersion();
    }

}

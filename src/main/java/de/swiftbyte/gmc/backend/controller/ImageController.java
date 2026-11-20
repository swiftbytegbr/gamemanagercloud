package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.model.Image;
import de.swiftbyte.gmc.backend.service.ImageService;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.validation.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@ApiAccountAllowed
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping(path = "/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable @Identifier String imageId) {
        Optional<Image> imgOpt = imageService.findById(imageId);
        if (imgOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Image img = imgOpt.get();
        String filename = img.getId() + "." + (img.getFormat() == null ? "img" : img.getFormat());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : img.getContentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .lastModified(img.getCreatedAt() == null ? -1 : img.getCreatedAt().toEpochMilli())
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .body(img.getData());
    }
}

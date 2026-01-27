package com.bluemobility.bmpresence.controller;

import com.bluemobility.bmpresence.model.PresenceResource;
import com.bluemobility.bmpresence.service.PresenceResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PresenceResourceController {

    private final PresenceResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<PresenceResource>> getAllResources() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PresenceResource>> getActiveResources() {
        return ResponseEntity.ok(resourceService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PresenceResource> getResourceById(@PathVariable Integer id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PresenceResource> createResource(@RequestBody PresenceResource resource) {
        PresenceResource createdResource = resourceService.create(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdResource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PresenceResource> updateResource(@PathVariable Integer id,
            @RequestBody PresenceResource resource) {
        PresenceResource updatedResource = resourceService.update(id, resource);
        return ResponseEntity.ok(updatedResource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Integer id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteResource(@PathVariable Integer id) {
        resourceService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}

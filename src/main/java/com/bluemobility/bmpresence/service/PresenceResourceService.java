package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.model.PresenceResource;
import com.bluemobility.bmpresence.repository.PresenceResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceResourceService {

    private final PresenceResourceRepository resourceRepository;

    public List<PresenceResource> findAll() {
        return resourceRepository.findAll();
    }

    public List<PresenceResource> findAllActive() {
        return resourceRepository.findByActiveTrue();
    }

    public PresenceResource findById(Integer id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + id));
    }

    @Transactional
    public PresenceResource create(PresenceResource resource) {
        return resourceRepository.save(resource);
    }

    @Transactional
    public PresenceResource update(Integer id, PresenceResource resourceDetails) {
        PresenceResource resource = findById(id);

        resource.setName(resourceDetails.getName());
        resource.setBackground(resourceDetails.getBackground());
        resource.setForeground(resourceDetails.getForeground());
        resource.setActive(resourceDetails.getActive());

        return resourceRepository.save(resource);
    }

    @Transactional
    public void delete(Integer id) {
        PresenceResource resource = findById(id);
        resource.setActive(false);
        resourceRepository.save(resource);
    }

    @Transactional
    public void hardDelete(Integer id) {
        resourceRepository.deleteById(id);
    }
}

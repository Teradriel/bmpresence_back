package com.bluemobility.bmpresence.repository;

import com.bluemobility.bmpresence.model.PresenceResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PresenceResourceRepository extends JpaRepository<PresenceResource, Integer> {

    List<PresenceResource> findByActiveTrue();
}

package org.crisisconnect.repository;

import org.crisisconnect.model.entity.NeedUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NeedUpdateRepository extends JpaRepository<NeedUpdate, UUID> {
    List<NeedUpdate> findByNeedIdOrderByCreatedAtDesc(UUID needId);
}

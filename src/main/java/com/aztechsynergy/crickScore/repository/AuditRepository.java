package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface AuditRepository extends JpaRepository<Audit, Long> {
    public List<Audit> findAllByUserId(String userId);
    public List<Audit> findAllBylookup(String lookup);
}

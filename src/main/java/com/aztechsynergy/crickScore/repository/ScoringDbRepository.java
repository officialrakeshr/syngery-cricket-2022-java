package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.ScoringDB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface ScoringDbRepository extends JpaRepository<ScoringDB, Long> {
    public ScoringDB findScoringDBByLookup(String lookup);
    public List<ScoringDB> findScoringDBSByMatchNo(String matchNo);
}

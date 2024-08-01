package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Substitution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface SubstitutionRepository extends JpaRepository<Substitution, String> {
    @Query( "select o.matchNo from Substitution o where o.free = 100000 and o.username = ?1" )
    String findMatchByInfinitSubs(String username);

    List<Substitution> findAllByMatchNo(String matchNo);
}

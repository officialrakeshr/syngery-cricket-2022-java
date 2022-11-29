package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(exported = false)
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    public Tournament  findDistinctFirstByMatchNo(String matchNo);
}
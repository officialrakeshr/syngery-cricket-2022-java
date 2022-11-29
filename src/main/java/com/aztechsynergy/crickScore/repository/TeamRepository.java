package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Player;
import com.aztechsynergy.crickScore.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface TeamRepository extends JpaRepository<Team, String> {
}

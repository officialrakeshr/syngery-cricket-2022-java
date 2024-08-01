package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.LastMatchStartedDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface LastMatchStartedRepository extends JpaRepository<LastMatchStartedDetails, Long> {
}

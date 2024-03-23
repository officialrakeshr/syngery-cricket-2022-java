package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeam(String name);

    @Query( "select o from Player o where o.id in :ids" )
    List<Player> findByIdIsIn(@Param("ids") List<Long> ids);

    @Query( "select o from Player o " )
    List<Player> listPlayerNames();

    @Query( "select o from Player o where o.teamId = :id" )
    List<Player> findPlayerByTeamId(@Param("id") Integer id);

}

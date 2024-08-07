package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Tournament;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
  public Tournament findDistinctFirstByMatchNo(String matchNo);

  @Query(
    value = "SELECT s.lookup,p.name,p.team,s.total_player_point FROM score_db s join players p on p.id = s.player_id  where match_no = ?1 and total_player_point > 0",
    nativeQuery = true
  )
  List<Map<String, Object>> findPlayerPointsByMatch(String matchNo);

  @Query(
    value = "SELECT t from Tournament t where t.enable11 = true and t.matchdate = ?1 "
  )
  List<Tournament> findTournamentsByEnable11AndMatchdate(String matchDate);
}

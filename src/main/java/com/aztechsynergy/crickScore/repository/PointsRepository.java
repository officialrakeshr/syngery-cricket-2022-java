package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Points;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RepositoryRestResource(exported = false)
public interface PointsRepository extends JpaRepository<Points, Long> {
    List<Points> findByUsername(String username);
    Points findByLookUp(String lookup);
    List<Points> findPointsByMatchNo(String matchNo);
    @Query(value ="SELECT p FROM Points p WHERE p.username = ?1 ORDER BY p.lastUpdatedTime DESC")
    List<Points> findPointsInDescLastUpdatedPoints(String username);
    @Query(value ="SELECT u.name as username, u.phone as phone, p.rank_no as rank_no, p.total as total, p.matchNo as matchNo FROM Points p join User u on p.username = u.username where u.matchNumber = ?1 order by rank_no")
    List<Map<String, Object>> findAllRankForPlayers(String matchNo);

    @Query( "select o.username from Points o where o.matchNo = ?1" )
    List<String> findUsersByMatchNo(String matchNo);
    @Query(value ="SELECT u.name as username, u.phone as phone, sum(p.total) as total, u.house as house FROM Points p join User u on p.username = u.username group by p.username order by total desc")
    List<Map<String, Object>> findLeagueRankForPlayers();
}

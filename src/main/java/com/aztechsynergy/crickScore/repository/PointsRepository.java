package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.Points;
import com.aztechsynergy.crickScore.model.ScoringDB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RepositoryRestResource(exported = false)
public interface PointsRepository extends JpaRepository<Points, Long> {
    public Points findByUsername(String username);
    public List<Points> findPointsByMatchNo(String matchNo);

    @Query(value ="SELECT u.name as username, u.phone as phone, p.rank_no as rank_no, p.total as total, p.matchNo as matchNo FROM Points p join User u on p.username = u.username where u.matchNumber = ?1 order by rank_no")
    public List<Map> findAllRankForPlayers(String matchNo);
}

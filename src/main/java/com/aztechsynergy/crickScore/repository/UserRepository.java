package com.aztechsynergy.crickScore.repository;

import com.aztechsynergy.crickScore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(exported = false)
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);

    @Query( value = "select u.username from users u join user_roles ur on  ur.user_id = u.id where ur.role_id = 1" , nativeQuery = true)
    List<String> findAllUserIds();

}
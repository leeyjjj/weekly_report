package com.weekly.repository;

import com.weekly.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"team"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"team"})
    Optional<User> findByName(String name);

    @EntityGraph(attributePaths = {"team"})
    List<User> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"team"})
    List<User> findByTeamIdOrderByNameAsc(Long teamId);

    @EntityGraph(attributePaths = {"team"})
    Optional<User> findByOauthId(String oauthId);

    @EntityGraph(attributePaths = {"team"})
    Optional<User> findByEmail(String email);

    long countByTeamId(Long teamId);
}

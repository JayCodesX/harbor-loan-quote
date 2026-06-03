package com.jaycodesx.mortgage.auth.repository;

import com.jaycodesx.mortgage.auth.model.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    boolean existsByEmail(String email);

    Optional<AuthUser> findByEmail(String email);

    @Query("select a.role as role, count(a) as count from AuthUser a group by a.role")
    List<AuthUserRoleCount> countByRole();
}

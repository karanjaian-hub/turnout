package com.turnout.authservice.repository;

import com.turnout.authservice.entity.User;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<User> findByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);

// Pass status and role as strings to avoid Hibernate binding enums as bytea.
// CAST in the WHERE clause ensures Postgres compares VARCHAR to VARCHAR.
    @Query(value = """
        SELECT * FROM auth.users u
        WHERE u.role = :role
        AND (:status IS NULL OR u.status = :status)
        AND (:search IS NULL OR
             LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(*) FROM auth.users u
        WHERE u.role = :role
        AND (:status IS NULL OR u.status = :status)
        AND (:search IS NULL OR
             LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        nativeQuery = true)
    Page<User> searchOrganizers(
            @Param("role") String role,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}

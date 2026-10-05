package com.truecommunity.repo;

import com.truecommunity.model.Role;
import com.truecommunity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for users, authentication lookup, role administration,
 * and leaderboard ranking.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByUsernameAsc();

    /**
     * Returns users of a role for the admin role-management page.
     *
     * @param role the role to include
     * @return users ordered by username
     */
    List<User> findByRoleOrderByUsernameAsc(Role role);

    /**
     * Returns the top non-editor users for the trust-score leaderboard.
     *
     * @param role the role to exclude
     * @return at most ten users ordered by trust score
     */
    List<User> findTop10ByRoleNotOrderByTrustScoreDesc(Role role);

    /**
     * Returns all non-editor users for leaderboard sorts that are computed in memory.
     *
     * @param role the role to exclude
     * @return users whose role is not the supplied role
     */
    List<User> findByRoleNot(Role role);

    long countByRoleNotAndTrustScoreGreaterThan(Role role, Double trustScore);

    long countByRole(Role role);
}
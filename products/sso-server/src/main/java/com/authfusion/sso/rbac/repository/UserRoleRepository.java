package com.authfusion.sso.rbac.repository;

import com.authfusion.sso.rbac.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for user-to-role assignment entities.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    /**
     * Finds all role assignments for a given user.
     *
     * @param userId the user UUID
     * @return list of user-role assignment entities
     */
    List<UserRoleEntity> findByUserId(UUID userId);

    /**
     * Checks whether a specific user-role assignment exists.
     *
     * @param userId the user UUID
     * @param roleId the role UUID
     * @return true if the assignment exists
     */
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Deletes a specific user-role assignment.
     *
     * @param userId the user UUID
     * @param roleId the role UUID
     */
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}

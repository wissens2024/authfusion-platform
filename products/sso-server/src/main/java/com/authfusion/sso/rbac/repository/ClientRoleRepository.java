package com.authfusion.sso.rbac.repository;

import com.authfusion.sso.rbac.model.ClientRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for client-to-role assignment entities.
 */
@Repository
public interface ClientRoleRepository extends JpaRepository<ClientRoleEntity, UUID> {

    /**
     * Finds all role assignments for a given client.
     *
     * @param clientId the client UUID
     * @return list of client-role assignment entities
     */
    List<ClientRoleEntity> findByClientId(UUID clientId);

    /**
     * Checks whether a specific client-role assignment exists.
     *
     * @param clientId the client UUID
     * @param roleId   the role UUID
     * @return true if the assignment exists
     */
    boolean existsByClientIdAndRoleId(UUID clientId, UUID roleId);

    /**
     * Deletes a specific client-role assignment.
     *
     * @param clientId the client UUID
     * @param roleId   the role UUID
     */
    void deleteByClientIdAndRoleId(UUID clientId, UUID roleId);
}

package com.authfusion.sso.rbac.repository;

import com.authfusion.sso.rbac.model.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RBAC role entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    /**
     * Finds a role by its unique name.
     *
     * @param name the role name
     * @return the role entity if found
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * Checks whether a role with the given name already exists.
     *
     * @param name the role name to check
     * @return true if a role with the given name exists
     */
    boolean existsByName(String name);
}

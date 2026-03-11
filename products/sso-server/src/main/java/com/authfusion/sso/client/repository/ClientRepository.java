package com.authfusion.sso.client.repository;

import com.authfusion.sso.client.model.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for OAuth 2.0 client entities.
 */
@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    /**
     * Finds a client by its OAuth 2.0 client_id.
     *
     * @param clientId the client_id
     * @return the client entity if found
     */
    Optional<ClientEntity> findByClientId(String clientId);

    /**
     * Checks whether a client with the given client_id already exists.
     *
     * @param clientId the client_id to check
     * @return true if a client with the given client_id exists
     */
    boolean existsByClientId(String clientId);
}

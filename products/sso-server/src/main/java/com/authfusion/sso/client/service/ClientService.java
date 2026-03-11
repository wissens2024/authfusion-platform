package com.authfusion.sso.client.service;

import com.authfusion.sso.client.model.*;
import com.authfusion.sso.client.repository.ClientRepository;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceAlreadyExistsException;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service for OAuth 2.0 client lifecycle management.
 * Handles client registration, credential validation, and CRUD operations.
 */
@Slf4j
@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClientService(ClientRepository clientRepository, ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Registers a new OAuth 2.0 client.
     * Generates a unique client_id and client_secret, hashes the secret before storage.
     *
     * @param request the client creation request
     * @return the created client response with plain-text client secret
     */
    public ClientResponse createClient(ClientCreateRequest request) {
        log.info("Creating new client: name='{}'", request.getClientName());

        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();
        String clientSecretHash = passwordEncoder.encode(clientSecret);

        ClientEntity entity = ClientEntity.builder()
                .clientId(clientId)
                .clientSecretHash(clientSecretHash)
                .clientName(request.getClientName())
                .clientType(request.getClientType() != null ? request.getClientType() : ClientType.CONFIDENTIAL)
                .redirectUris(toJson(request.getRedirectUris()))
                .allowedScopes(toJson(request.getAllowedScopes()))
                .allowedGrantTypes(toJson(request.getAllowedGrantTypes()))
                .enabled(true)
                .requirePkce(request.isRequirePkce())
                .build();

        ClientEntity saved = clientRepository.save(entity);
        log.info("Client created: id={}, clientId='{}'", saved.getId(), saved.getClientId());

        ClientResponse response = toResponse(saved);
        response.setClientSecret(clientSecret);
        return response;
    }

    /**
     * Retrieves a client by its internal UUID.
     *
     * @param id the internal UUID
     * @return the client response
     * @throws ResourceNotFoundException if no client with the given id exists
     */
    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID id) {
        log.debug("Retrieving client by id={}", id);
        ClientEntity entity = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        return toResponse(entity);
    }

    /**
     * Retrieves a client by its OAuth 2.0 client_id.
     *
     * @param clientId the OAuth 2.0 client_id
     * @return the client response
     * @throws ResourceNotFoundException if no client with the given client_id exists
     */
    @Transactional(readOnly = true)
    public ClientResponse getByClientId(String clientId) {
        log.debug("Retrieving client by clientId='{}'", clientId);
        ClientEntity entity = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with clientId: " + clientId));
        return toResponse(entity);
    }

    /**
     * Lists all registered clients.
     *
     * @return list of all client responses
     */
    @Transactional(readOnly = true)
    public List<ClientResponse> listClients() {
        log.debug("Listing all clients");
        return clientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates an existing client's configuration.
     *
     * @param id      the internal UUID of the client to update
     * @param request the updated client configuration
     * @return the updated client response
     * @throws ResourceNotFoundException if no client with the given id exists
     */
    public ClientResponse updateClient(UUID id, ClientCreateRequest request) {
        log.info("Updating client: id={}", id);

        ClientEntity entity = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        entity.setClientName(request.getClientName());
        entity.setClientType(request.getClientType() != null ? request.getClientType() : entity.getClientType());
        entity.setRedirectUris(toJson(request.getRedirectUris()));
        entity.setAllowedScopes(toJson(request.getAllowedScopes()));
        entity.setAllowedGrantTypes(toJson(request.getAllowedGrantTypes()));
        entity.setRequirePkce(request.isRequirePkce());

        ClientEntity saved = clientRepository.save(entity);
        log.info("Client updated: id={}, clientId='{}'", saved.getId(), saved.getClientId());

        return toResponse(saved);
    }

    /**
     * Deletes a client by its internal UUID.
     *
     * @param id the internal UUID of the client to delete
     * @throws ResourceNotFoundException if no client with the given id exists
     */
    public void deleteClient(UUID id) {
        log.info("Deleting client: id={}", id);

        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client not found with id: " + id);
        }

        clientRepository.deleteById(id);
        log.info("Client deleted: id={}", id);
    }

    /**
     * Validates client credentials against the stored hashed secret.
     *
     * @param clientId     the OAuth 2.0 client_id
     * @param clientSecret the plain-text client secret to verify
     * @return true if credentials are valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateClientCredentials(String clientId, String clientSecret) {
        log.debug("Validating credentials for clientId='{}'", clientId);

        return clientRepository.findByClientId(clientId)
                .map(entity -> {
                    if (!entity.isEnabled()) {
                        log.warn("Client is disabled: clientId='{}'", clientId);
                        return false;
                    }
                    if (entity.getClientSecretHash() == null) {
                        log.warn("Client has no secret (PUBLIC client): clientId='{}'", clientId);
                        return false;
                    }
                    return passwordEncoder.matches(clientSecret, entity.getClientSecretHash());
                })
                .orElse(false);
    }

    // ---- Internal helpers ----

    /**
     * Converts a list of strings to a JSON array string.
     */
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list to JSON", e);
            throw new IllegalArgumentException("Failed to serialize list to JSON: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON array string to a list of strings.
     */
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to list: '{}'", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * Converts a ClientEntity to a ClientResponse with parsed JSON fields.
     */
    private ClientResponse toResponse(ClientEntity entity) {
        return ClientResponse.fromEntity(
                entity,
                fromJson(entity.getRedirectUris()),
                fromJson(entity.getAllowedScopes()),
                fromJson(entity.getAllowedGrantTypes())
        );
    }
}

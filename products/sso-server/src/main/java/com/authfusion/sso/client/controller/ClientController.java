package com.authfusion.sso.client.controller;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.client.model.ClientCreateRequest;
import com.authfusion.sso.client.model.ClientResponse;
import com.authfusion.sso.client.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for OAuth 2.0 client management.
 */
@ExtendedFeature("클라이언트 관리 API")
@ConditionalOnExtendedMode
@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Client Management", description = "OAuth 2.0 client registration and management")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @Operation(
            summary = "Register a new client",
            description = "Creates a new OAuth 2.0 client and returns the generated client_id and client_secret.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Client registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            }
    )
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientCreateRequest request) {
        log.info("POST /api/v1/clients - Creating client: name='{}'", request.getClientName());
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List all clients",
            description = "Returns a list of all registered OAuth 2.0 clients.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Clients retrieved successfully")
            }
    )
    public ResponseEntity<List<ClientResponse>> listClients() {
        log.info("GET /api/v1/clients - Listing all clients");
        List<ClientResponse> clients = clientService.listClients();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get client by ID",
            description = "Retrieves client information by its internal UUID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Client found"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            }
    )
    public ResponseEntity<ClientResponse> getClient(
            @Parameter(description = "Client UUID") @PathVariable UUID id) {
        log.info("GET /api/v1/clients/{}", id);
        ClientResponse response = clientService.getClient(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update client configuration",
            description = "Updates an existing client's configuration. Does not change client_id or client_secret.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Client updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            }
    )
    public ResponseEntity<ClientResponse> updateClient(
            @Parameter(description = "Client UUID") @PathVariable UUID id,
            @Valid @RequestBody ClientCreateRequest request) {
        log.info("PUT /api/v1/clients/{}", id);
        ClientResponse response = clientService.updateClient(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a client",
            description = "Permanently deletes a registered client.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            }
    )
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "Client UUID") @PathVariable UUID id) {
        log.info("DELETE /api/v1/clients/{}", id);
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}

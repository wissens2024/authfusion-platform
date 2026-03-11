package com.authfusion.sso.client.model;

/**
 * OAuth 2.0 client type classification.
 * CONFIDENTIAL clients can securely maintain a client secret.
 * PUBLIC clients (e.g., SPAs, mobile apps) cannot.
 */
public enum ClientType {
    CONFIDENTIAL,
    PUBLIC
}

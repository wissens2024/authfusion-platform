package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;

/**
 * Enumeration representing the possible states of a user account.
 */
@ToeScope(value = "사용자 상태 열거형", sfr = {"FIA_UAU.1"})
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    PENDING
}

package com.authfusion.sso.session.model;

import com.authfusion.sso.cc.ToeScope;

@ToeScope(value = "세션 상태 열거형", sfr = {"FIA_USB.1"})
public enum SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

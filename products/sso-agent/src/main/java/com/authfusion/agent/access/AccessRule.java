package com.authfusion.agent.access;

import com.authfusion.agent.cc.ToeScope;
import lombok.*;

import java.util.List;

@ToeScope(value = "접근 제어 규칙", sfr = {"FDP_ACC.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRule {

    private String pattern;
    private List<String> requiredRoles;
    private boolean authenticated;
}

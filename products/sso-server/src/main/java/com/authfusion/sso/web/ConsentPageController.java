package com.authfusion.sso.web;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ExtendedFeature("동의(Consent) 페이지")
@ConditionalOnExtendedMode
@Controller
public class ConsentPageController {

    @GetMapping("/consent")
    public String consentPage(
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "scope", required = false) String scope,
            Model model) {
        model.addAttribute("clientId", clientId);
        model.addAttribute("scope", scope);
        return "consent";
    }
}

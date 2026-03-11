package com.authfusion.sso.web;

import com.authfusion.sso.cc.ToeScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ToeScope(value = "에러 페이지 컨트롤러", sfr = {})
@Controller
public class ErrorPageController {

    @GetMapping("/error")
    public String errorPage(
            @RequestParam(value = "message", required = false, defaultValue = "An unexpected error occurred") String message,
            Model model) {
        model.addAttribute("errorMessage", message);
        return "error";
    }
}

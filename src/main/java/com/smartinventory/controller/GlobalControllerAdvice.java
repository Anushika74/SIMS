package com.smartinventory.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes shared values to every Thymeleaf view: the logged-in user's name and
 * role (for the navbar) and the configured currency symbol (for money displays).
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${app.currency.symbol:Rs.}")
    private String currencySymbol;

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "guest";
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Currency symbol available in every template as ${currency}. */
    @ModelAttribute("currency")
    public String currency() {
        return currencySymbol;
    }
}

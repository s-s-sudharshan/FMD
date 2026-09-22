package com.infy.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.infy.exception.UnauthorizedActionException;

/**
 * Fetches the logged-in user's username from the SecurityContext. Used by
 * Change Password (and, later, audit fields on other write actions) so
 * services never trust a caller-supplied username for "my own account"
 * operations.
 */
@Component
public class CurrentUserResolver {

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedActionException("No authenticated user found in the current session");
        }
        return authentication.getName();
    }
}

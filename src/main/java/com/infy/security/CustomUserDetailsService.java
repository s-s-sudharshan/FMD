package com.infy.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.infy.entity.User;
import com.infy.enums.UserState;
import com.infy.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Loads a User by username and maps it to a Spring Security UserDetails.
 * Deactivated accounts are surfaced as UserDetails.enabled = false, which
 * DaoAuthenticationProvider turns into a DisabledException during
 * authenticate() -- AuthServiceImpl catches that and re-throws it as the
 * SRS-specified UserDeactivatedException ("User is Deactivated"), kept
 * distinct from a plain bad-credentials failure.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getUserState() == UserState.DEACTIVATED)
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}

package com.cyclehaven.security;

import com.cyclehaven.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
                .map(CustomUserDetails::new)
                // Deliberately vague: saying "no account with that email" would let
                // an attacker enumerate which addresses are registered.
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}

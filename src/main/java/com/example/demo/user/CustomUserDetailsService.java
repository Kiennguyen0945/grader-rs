package com.example.demo.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return toPrincipal(user);
    }
    public CustomUserPrincipal loadUserById(String userIdText) {
        Long userId = Long.valueOf(userIdText);

        User user = userRepository.findById(userId)
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + userId));

        return toPrincipal(user);
    }
    private CustomUserPrincipal toPrincipal(User user) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new CustomUserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getPasswordHash(),
            authorities,
            user.getStatus() == User.Status.ACTIVE
        );
    }
}

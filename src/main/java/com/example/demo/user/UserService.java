package com.example.demo.user;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(
        String username,
        User.Role role,
        String password
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Username already exists"
            );
        }
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(
            username,
            role,
            User.Status.ACTIVE,
            encodedPassword
        );
        return userRepository.save(user);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
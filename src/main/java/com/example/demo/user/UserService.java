package com.example.demo.user;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(
        String username,
        User.Role role,
        String passwordHash
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                "Username already exists"
            );
        }
        User user = new User(
            username,
            role,
            User.Status.ACTIVE,
            passwordHash
        );
        return userRepository.save(user);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
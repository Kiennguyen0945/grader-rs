package com.example.demo.user;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(
            request.username(),
            request.role(),
            request.passwordHash()
        );
    }

    public record CreateUserRequest(
        String username,
        User.Role role,
        String passwordHash
    ) {
        
    }
}
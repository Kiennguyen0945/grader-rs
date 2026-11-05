package com.example.demo.user;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Slim projection: never serialize the JPA entity (it pulls in assignments,
     * submissions, course memberships and grading runs -> huge, lazily-loaded JSON).
     */
    @GetMapping
    public List<UserSummary> getUsers() {
        return userService.getAllUsers().stream().map(UserSummary::fromEntity).toList();
    }

    @PostMapping
    public UserSummary createUser(@RequestBody CreateUserRequest request) {
        return UserSummary.fromEntity(userService.createUser(
            request.username(),
            request.role(),
            request.passwordHash()
        ));
    }

    public record UserSummary(Long id, String username, User.Role role, User.Status status) {
        public static UserSummary fromEntity(User user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getRole(), user.getStatus());
        }
    }

    public record CreateUserRequest(
        String username,
        User.Role role,
        String passwordHash
    ) {
        
    }
}
//src/main/java/com/algoarena/algoarena/HomeController.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

// import java.util.Optional;

@RestController
public class HomeController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home() {
        return """
            <h1>Welcome to AlgoArena! 🚀</h1>
            <br><br>
            <a href='/oauth2/authorization/google' style='padding: 10px; background: #4285f4; color: white; text-decoration: none; margin: 10px;'>Login with Google</a>
            <br><br>
            <a href='/oauth2/authorization/github' style='padding: 10px; background: #333; color: white; text-decoration: none; margin: 10px;'>Login with GitHub</a>
            <br><br>
            <a href='/api/users' style='padding: 10px; background: #28a745; color: white; text-decoration: none; margin: 10px;'>View All Users</a>
            """;
    }
    
    @GetMapping("/api/health")
    public String health() {
        return "Server is running! ✅";
    }
    
    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint - no login required!";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {
        // Get user from session (set by our success handler)
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        
        if (currentUser != null) {
            // Show data from our database
            return String.format("""
                <h1>Welcome to your dashboard! 🎉</h1>
                <h2>Your Profile (from our database):</h2>
                <p><strong>Name:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Username:</strong> %s</p>
                <p><strong>Provider:</strong> %s</p>
                <p><strong>Provider ID:</strong> %s</p>
                <p><strong>Created At:</strong> %s</p>
                <p><strong>Last Login:</strong> %s</p>
                <p><strong>Avatar:</strong> <img src='%s' width='50' height='50' style='border-radius: 50%%;'></p>
                <br>
                <a href='/logout' style='padding: 10px; background: #dc3545; color: white; text-decoration: none;'>Logout</a>
                <br><br>
                <a href='/api/users' style='padding: 10px; background: #28a745; color: white; text-decoration: none;'>View All Users</a>
                """,
                currentUser.getName(),
                currentUser.getEmail() != null ? currentUser.getEmail() : "Not provided",
                currentUser.getUsername(),
                currentUser.getProvider(),
                currentUser.getProviderId(),
                currentUser.getCreatedAt(),
                currentUser.getLastLoginAt(),
                currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : ""
            );
        } else {
            // Fallback to OAuth2 data if session doesn't have user
            return "Welcome to your dashboard, " + principal.getAttribute("name") + "! 🎉 <br><br> " +
                   "<a href='/logout'>Logout</a>";
        }
    }
    
    @GetMapping("/api/users")
    public String getAllUsers() {
        Iterable<User> users = userService.getAllUsers();
        StringBuilder html = new StringBuilder();
        html.append("<h1>All Registered Users 👥</h1><br>");
        
        for (User user : users) {
            html.append(String.format("""
                <div style='border: 1px solid #ccc; padding: 15px; margin: 10px 0; border-radius: 5px;'>
                    <img src='%s' width='40' height='40' style='border-radius: 50%%; float: left; margin-right: 15px;'>
                    <strong>%s</strong> (%s) <br>
                    <small>Provider: %s | Joined: %s</small>
                </div>
                """,
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                user.getName(),
                user.getEmail() != null ? user.getEmail() : user.getUsername(),
                user.getProvider(),
                user.getCreatedAt()
            ));
        }
        
        html.append("<br><a href='/' style='padding: 10px; background: #007bff; color: white; text-decoration: none;'>Back to Home</a>");
        return html.toString();
    }
}
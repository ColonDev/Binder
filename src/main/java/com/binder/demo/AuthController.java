package com.binder.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    private final JdbcTemplate jdbcTemplate;

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email, @RequestParam String password, Model model) {
        String sql = "SELECT a.password_hash, u.full_name FROM users u " +
                     "JOIN authentications a ON u.user_id = a.user_id " +
                     "WHERE u.email = ? AND a.provider = 'LOCAL'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, email);

        if (!results.isEmpty()) {
            String hash = (String) results.get(0).get("password_hash");
            if (BCrypt.checkpw(password, hash)) {
                model.addAttribute("name", results.get(0).get("full_name"));
                return "dashboard";
            }
        }
        
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam String email, 
                                     @RequestParam String password, 
                                     @RequestParam String fullName,
                                     @RequestParam String role) {
        UUID userId = UUID.randomUUID();
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Insert into users
        jdbcTemplate.update("INSERT INTO users (user_id, email, full_name, role) VALUES (?, ?, ?, ?)",
                userId, email, fullName, role);

        // Insert into authentications
        jdbcTemplate.update("INSERT INTO authentications (user_id, provider, provider_id, password_hash) VALUES (?, 'LOCAL', ?, ?)",
                userId, email, hashedPassword);

        return "redirect:/login";
    }
}

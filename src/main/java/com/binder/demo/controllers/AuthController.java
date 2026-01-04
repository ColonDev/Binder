package com.binder.demo.controllers;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AuthController
 * This class manages user login and registration.
 * It connects to a database to store user information and
 * uses BCrypt to securely store and check passwords.
 */
@Controller
public class AuthController {

    /** Used to run SQL queries and updates on the database */
    private final JdbcTemplate jdbcTemplate;

    /** Email format checker */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Constructor that receives the database helper
    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {

        email = (email == null) ? "" : email.trim();
        password = (password == null) ? "" : password;

        if (email.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Please enter your email and password.");
            return "login";
        }

        String sql = "SELECT a.password_hash, u.full_name, u.user_id, u.email, u.role " +
                     "FROM users u " +
                     "JOIN authentications a ON u.user_id = a.user_id " +
                     "WHERE u.email = ? AND a.provider = 'LOCAL'";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, email);

        if (!results.isEmpty()) {
            String savedHash = (String) results.get(0).get("password_hash");

            if (savedHash != null && BCrypt.checkpw(password, savedHash)) {
                session.setAttribute("userId", results.get(0).get("user_id"));
                session.setAttribute("userName", results.get(0).get("full_name"));
                session.setAttribute("userEmail", results.get(0).get("email"));
                session.setAttribute("userRole", results.get(0).get("role"));
                return "redirect:/dashboard";
            }
        }

        model.addAttribute("error", "Invalid email or password");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("name", session.getAttribute("userName"));
        model.addAttribute("email", session.getAttribute("userEmail"));
        model.addAttribute("role", session.getAttribute("userRole"));

        // Fetch classrooms
        String classSql = 
            "SELECT c.class_id, c.name, c.description " +
            "FROM classrooms c " +
            "LEFT JOIN enrollments e ON c.class_id = e.class_id " +
            "LEFT JOIN classroom_teachers ct ON c.class_id = ct.class_id " +
            "WHERE e.student_id = ? OR ct.teacher_id = ? " +
            "GROUP BY c.class_id";

        List<Map<String, Object>> classrooms = jdbcTemplate.queryForList(classSql, userId, userId);
        model.addAttribute("classrooms", classrooms);

        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * Registration rules:
     * - Sanity check that a role for a teacher or student is provided.
     * - Email must look valid.
     * - Password must meet basic rules.
     * - Email must not already exist in the database.
     */
    @Transactional
    @PostMapping("/register")
    public String processRegistration(@RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String fullName,
                                      @RequestParam String role,
                                      Model model) {

        email = (email == null) ? "" : email.trim();
        fullName = (fullName == null) ? "" : fullName.trim();
        password = (password == null) ? "" : password;

        // Required fields
        if (email.isBlank() || fullName.isBlank() || password.isBlank() || role.isBlank()) {
            model.addAttribute("error", "Please fill in all fields.");
            return "register";
        }

        // Email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "register";
        }

        // Password rules
        if (password.length() < 8 ||
                !password.matches(".*[A-Za-z].*") ||
                !password.matches(".*\\d.*")) {
            model.addAttribute("error",
                    "Password must be at least 8 characters and include letters and numbers.");
            return "register";
        }

        // Check if email already exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );

        if (count != null && count > 0) {
            model.addAttribute("error", "An account with that email already exists.");
            return "register";
        }

        UUID userId = UUID.randomUUID();
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try {
            jdbcTemplate.update(
                    "INSERT INTO users (user_id, email, full_name, role) VALUES (?, ?, ?, ?)",
                    userId, email, fullName, role
            );

            jdbcTemplate.update(
                    "INSERT INTO authentications (user_id, provider, provider_id, password_hash) " +
                            "VALUES (?, 'LOCAL', ?, ?)",
                    userId, email, hashedPassword
            );
        } catch (DataAccessException ex) {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }

        return "redirect:/login";
    }
}

package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.User;
import com.newsstand.security.PasswordSecurity;
import com.newsstand.security.RBACManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthController — handles all authentication endpoints.
 *
 * Routes
 * ──────
 *   POST /auth/login     – authenticate user, open session
 *   POST /auth/register  – create new account (password hashed with BCrypt)
 *   POST /auth/logout    – invalidate session
 *   GET  /auth/me        – return current session user info + permissions
 *   PUT  /auth/update    – update own profile (re-hashes new password)
 */
@WebServlet("/auth/*")
public class AuthController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ── Session attribute key ─────────────────────────────────────────────────
    public static final String SESSION_USER = "sessionUser";

    // ── POST dispatcher ───────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo(); // e.g. "/login"
        if (path == null) path = "/";

        switch (path) {
            case "/login"    -> handleLogin(req, resp);
            case "/register" -> handleRegister(req, resp);
            case "/logout"   -> handleLogout(req, resp);
            default          -> sendError(resp, 404, "Unknown auth endpoint: " + path);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if ("/me".equals(path)) {
            handleMe(req, resp);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if ("/update".equals(path)) {
            handleUpdate(req, resp);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    // ── /auth/login ───────────────────────────────────────────────────────────

    /**
     * Authenticate a user.
     *
     * Security measures applied:
     *  1. PreparedStatement prevents SQL injection entirely.
     *  2. BCrypt.checkpw() performs constant-time comparison — no timing leak.
     *  3. Session is regenerated after login to prevent session fixation.
     *
     * Expected JSON body: { "userEmail": "...", "userPassword": "..." }
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String email    = (String) body.get("userEmail");
        String password = (String) body.get("userPassword");

        if (email == null || password == null) {
            sendError(resp, 400, "Email and password are required.");
            return;
        }

        try {
            Connection conn = DBManager.getInstance().getConnection();

            // ── PreparedStatement: no string concatenation → no SQL injection ──
            String sql = "SELECT userId, userName, userEmail, userPassword, userRole, languageId " +
                         "FROM user WHERE userEmail = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    // Use the SAME generic message for both "no user" and "wrong password"
                    // to avoid user enumeration
                    sendError(resp, 401, "Invalid email or password.");
                    return;
                }

                String storedHash = rs.getString("userPassword");

                // ── BCrypt verification (salt is embedded in storedHash) ────────
                if (!PasswordSecurity.verifyPassword(password, storedHash)) {
                    sendError(resp, 401, "Invalid email or password.");
                    return;
                }

                // ── Build User object from result set ──────────────────────────
                User user = new User();
                user.setUserId(rs.getInt("userId"));
                user.setUserName(rs.getString("userName"));
                user.setUserEmail(rs.getString("userEmail"));
                user.setUserRole(rs.getInt("userRole"));
                user.setLanguageId(rs.getInt("languageId"));
                // NOTE: we do NOT put the password hash into the session

                // ── Session management ─────────────────────────────────────────
                // Invalidate the old session and create a new one to prevent
                // session-fixation attacks.
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) oldSession.invalidate();

                HttpSession session = req.getSession(true);
                session.setAttribute(SESSION_USER, user);
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("userId",   user.getUserId());
                response.put("userName", user.getUserName());
                response.put("userRole", user.getUserRole());
                response.put("permissions",
                        RBACManager.getPermissionsForRole(user.getUserRole())
                                   .stream().map(Enum::name).toList());

                sendJson(resp, 200, response);
            }

        } catch (SQLException e) {
            sendError(resp, 500, "Database error during login.");
        }
    }

    // ── /auth/register ────────────────────────────────────────────────────────

    /**
     * Register a new user.
     *
     * Security measures applied:
     *  1. Password is hashed with BCrypt (STRENGTH=12) before storing.
     *  2. PreparedStatement used throughout — no SQL injection possible.
     *  3. Duplicate email check uses a PreparedStatement.
     *
     * Expected JSON body:
     *   { "userName": "...", "userEmail": "...", "userPassword": "...", "languageId": 1 }
     */
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String name       = (String)  body.get("userName");
        String email      = (String)  body.get("userEmail");
        String password   = (String)  body.get("userPassword");
        int    languageId = body.get("languageId") != null
                            ? (int) body.get("languageId") : 1;

        if (name == null || email == null || password == null) {
            sendError(resp, 400, "Name, email, and password are required.");
            return;
        }

        if (password.length() < 8) {
            sendError(resp, 400, "Password must be at least 8 characters.");
            return;
        }

        try {
            Connection conn = DBManager.getInstance().getConnection();

            // ── Check for duplicate email ──────────────────────────────────────
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT userId FROM user WHERE userEmail = ?")) {
                check.setString(1, email);
                if (check.executeQuery().next()) {
                    sendError(resp, 409, "This email is already registered.");
                    return;
                }
            }

            // ── Hash the password BEFORE inserting into the database ───────────
            //    BCrypt.hashpw() automatically generates a unique salt each call,
            //    so two users with the same password get different hash strings.
            String hashedPassword = PasswordSecurity.hashPassword(password);

            // ── Insert new user (role 2 = REGULAR by default) ─────────────────
            String insert = "INSERT INTO user (userName, userEmail, userPassword, userRole, languageId) " +
                            "VALUES (?, ?, ?, 2, ?)";

            try (PreparedStatement ps = conn.prepareStatement(
                    insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hashedPassword);  // ← hashed, NOT plain text
                ps.setInt(4, languageId);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                int newId = keys.next() ? keys.getInt(1) : -1;

                User user = new User();
                user.setUserId(newId);
                user.setUserName(name);
                user.setUserEmail(email);
                user.setUserRole(2);
                user.setLanguageId(languageId);

                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) oldSession.invalidate();

                HttpSession session = req.getSession(true);
                session.setAttribute(SESSION_USER, user);
                session.setMaxInactiveInterval(30 * 60);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Registration successful");
                response.put("userId", newId);
                sendJson(resp, 201, response);
            }

        } catch (SQLException e) {
            sendError(resp, 500, "Database error during registration.");
        }
    }

    // ── /auth/logout ──────────────────────────────────────────────────────────

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sendJson(resp, 200, Map.of("message", "Logged out successfully."));
    }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    /** Returns current session user info + their resolved permission list. */
    private void handleMe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER) == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        User user = (User) session.getAttribute(SESSION_USER);
        String roleName = "Unknown";
        try (Connection conn = DBManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT roleName FROM role WHERE roleId = ?")) {
            ps.setInt(1, user.getUserRole());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) roleName = rs.getString("roleName");
        } catch (SQLException ignore) { }

        Map<String, Object> response = new HashMap<>();
        response.put("userId",      user.getUserId());
        response.put("userName",    user.getUserName());
        response.put("userEmail",   user.getUserEmail());
        response.put("userRole",    user.getUserRole());
        response.put("roleName",    roleName);               // ← added
        response.put("languageId",  user.getLanguageId());
        response.put("permissions",
                RBACManager.getPermissionsForRole(user.getUserRole())
                        .stream().map(Enum::name).toList());

        sendJson(resp, 200, response);
    }

    // ── PUT /auth/update ──────────────────────────────────────────────────────

    /**
     * Update own profile. Re-hashes the new password if provided.
     *
     * Expected JSON body (all fields optional):
     *   { "userName": "...", "userEmail": "...", "userPassword": "..." }
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER) == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }
        User sessionUser = (User) session.getAttribute(SESSION_USER);

        Map<?, ?> body    = JSON.readValue(req.getReader(), Map.class);
        String newName    = (String) body.get("userName");
        String newEmail   = (String) body.get("userEmail");
        String newPassword = (String) body.get("userPassword");

        try {
            Connection conn = DBManager.getInstance().getConnection();
            StringBuilder sql = new StringBuilder("UPDATE user SET ");
            boolean first = true;

            if (newName != null && !newName.isBlank()) {
                sql.append("userName = ?");
                first = false;
            }
            if (newEmail != null && !newEmail.isBlank()) {
                if (!first) sql.append(", ");
                sql.append("userEmail = ?");
                first = false;
            }
            if (newPassword != null && !newPassword.isBlank()) {
                if (!first) sql.append(", ");
                sql.append("userPassword = ?");
            }
            sql.append(" WHERE userId = ?");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (newName     != null && !newName.isBlank())     ps.setString(idx++, newName);
                if (newEmail    != null && !newEmail.isBlank())    ps.setString(idx++, newEmail);
                if (newPassword != null && !newPassword.isBlank()) {
                    // Re-hash the new password before storing
                    ps.setString(idx++, PasswordSecurity.hashPassword(newPassword));
                }
                ps.setInt(idx, sessionUser.getUserId());
                ps.executeUpdate();
            }

            // Refresh session with latest DB values
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT userName, userEmail, userRole, languageId FROM user WHERE userId = ?")) {
                ps2.setInt(1, sessionUser.getUserId());
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    sessionUser.setUserName(rs.getString("userName"));
                    sessionUser.setUserEmail(rs.getString("userEmail"));
                    sessionUser.setUserRole(rs.getInt("userRole"));
                    session.setAttribute(SESSION_USER, sessionUser);
                }
            }

            sendJson(resp, 200, Map.of("message", "Profile updated successfully."));

        } catch (SQLException e) {
            sendError(resp, 500, "Database error during profile update.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendJson(HttpServletResponse resp, int status, Object payload)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            JSON.writeValue(out, payload);
        }
    }

    private void sendError(HttpServletResponse resp, int status, String message)
            throws IOException {
        sendJson(resp, status, Map.of("error", message));
    }
}

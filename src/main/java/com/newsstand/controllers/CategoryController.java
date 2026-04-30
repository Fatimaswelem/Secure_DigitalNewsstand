package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.User;
import com.newsstand.security.RBACManager;
import com.newsstand.security.RBACManager.Permission;
import com.newsstand.utils.AuditLogger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/api/categories/*")
public class CategoryController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TABLE_NAME = "category";

    // ── Helper to obtain a live connection ──────────────────────────────
    private Connection getValidConnection() throws SQLException {
        return DBManager.getInstance().getConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("sessionUser") == null) {
                sendError(resp, 401, "Not authenticated.");
                return;
            }

            List<Map<String, Object>> categories = new ArrayList<>();
            String sql = "SELECT categoryId, categoryName, icon FROM " + TABLE_NAME + " ORDER BY categoryId";

            try (Connection conn = getValidConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    cat.put("id", rs.getInt(1));       // categoryId
                    cat.put("name", rs.getString(2));  // categoryName
                    String icon = rs.getString(3);     // icon
                    cat.put("icon", icon != null ? icon : "📂");
                    categories.add(cat);
                }
            } catch (SQLException e) {
                sendError(resp, 500, "SQL Error: " + e.getMessage());
                return;
            }
            sendJson(resp, 200, categories);
        } catch (Exception e) {
            sendError(resp, 500, "Error: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    // ── POST /api/categories (admin only) ───────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User currentUser = (User) req.getSession().getAttribute("sessionUser");
        if (currentUser == null || !RBACManager.hasPermission(currentUser.getUserRole(), Permission.MANAGE_CATEGORIES)) {
            sendError(resp, 403, "Access denied.");
            return;
        }

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String name = (String) body.get("categoryName");
        String icon = (String) body.get("icon");

        if (name == null || name.isBlank()) {
            sendError(resp, 400, "Category name required.");
            return;
        }

        try (Connection conn = getValidConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO " + TABLE_NAME + " (categoryName, icon) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            if (icon != null && !icon.isBlank()) {
                ps.setString(2, icon.trim());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;

            AuditLogger.log(currentUser.getUserId(), currentUser.getUserName(),
                    "CREATE_CATEGORY", "category", newId,
                    "Created category '" + name + "' with icon '" + icon + "'");

            sendJson(resp, 201, Map.of("message", "Category added", "id", newId));
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(resp, 500, "Database error adding category.");
        }
    }

    // ── DELETE /api/categories/{id} (admin only) ────────────────────────
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User currentUser = (User) req.getSession().getAttribute("sessionUser");
        if (currentUser == null || !RBACManager.hasPermission(currentUser.getUserRole(), Permission.MANAGE_CATEGORIES)) {
            sendError(resp, 403, "Access denied.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) {
            sendError(resp, 400, "Category id required.");
            return;
        }
        int catId;
        try { catId = Integer.parseInt(path.substring(1)); }
        catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid id.");
            return;
        }

        try (Connection conn = getValidConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM " + TABLE_NAME + " WHERE categoryId = ?")) {
            ps.setInt(1, catId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                sendError(resp, 404, "Category not found.");
            } else {
                AuditLogger.log(currentUser.getUserId(), currentUser.getUserName(),
                        "DELETE_CATEGORY", "category", catId, "Deleted category");
                sendJson(resp, 200, Map.of("message", "Category deleted."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(resp, 500, "Database error deleting category.");
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────
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
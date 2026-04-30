package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/api/favorites/sections")
public class FavoriteSectionsController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ── GET /api/favorites/sections ───────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute("sessionUser");
        if (user == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        List<Map<String, Object>> sections = new ArrayList<>();
        String sql = "SELECT c.categoryId, c.categoryName, c.icon FROM category c " +
                     "JOIN user_followed_sections ufs ON c.categoryId = ufs.categoryId " +
                     "WHERE ufs.userId = ? ORDER BY c.categoryName";

        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sec = new LinkedHashMap<>();
                    sec.put("id", rs.getInt("categoryId"));
                    sec.put("name", rs.getString("categoryName"));
                    sec.put("icon", rs.getString("icon") != null ? rs.getString("icon") : "📌");
                    sections.add(sec);
                }
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error.");
            return;
        }
        sendJson(resp, 200, sections);
    }

    // ── POST /api/favorites/sections ──────────────────────────────────────
    // Body: { "sectionId": 3, "action": "follow" }   OR   "action": "unfollow"
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute("sessionUser");
        if (user == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        int sectionId = (int) body.get("sectionId");
        String action = (String) body.get("action");

        if (action == null || (!action.equals("follow") && !action.equals("unfollow"))) {
            sendError(resp, 400, "Action must be 'follow' or 'unfollow'.");
            return;
        }

        try (Connection conn = DBManager.getInstance().getConnection()) {
            if ("follow".equals(action)) {
                String insert = "INSERT IGNORE INTO user_followed_sections (userId, categoryId) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, user.getUserId());
                    ps.setInt(2, sectionId);
                    ps.executeUpdate();
                }
            } else {
                String delete = "DELETE FROM user_followed_sections WHERE userId = ? AND categoryId = ?";
                try (PreparedStatement ps = conn.prepareStatement(delete)) {
                    ps.setInt(1, user.getUserId());
                    ps.setInt(2, sectionId);
                    ps.executeUpdate();
                }
            }
            sendJson(resp, 200, Map.of("message", action.equals("follow") ? "Followed" : "Unfollowed"));
        } catch (SQLException e) {
            sendError(resp, 500, "Database error.");
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────
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
package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.User;
import com.newsstand.utils.AuditLogger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/subscribe")
public class SubscriptionController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_USER) == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        User sessionUser = (User) session.getAttribute(AuthController.SESSION_USER);

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        int planId = body.get("planId") != null ? (int) body.get("planId") : 0;

        // Map plan IDs to role IDs (from subscription.html)
        int newRoleId = switch (planId) {
            case 1 -> 3;   // Basic
            case 2 -> 4;   // Standard
            case 3 -> 5;   // Premium
            default -> 0;
        };

        if (newRoleId == 0) {
            sendError(resp, 400, "Invalid plan id.");
            return;
        }

        // Prevent admin subscription changes (optional)
        if (sessionUser.getUserRole() == 1) {
            sendError(resp, 400, "Admins cannot subscribe to a plan.");
            return;
        }

        try (Connection conn = DBManager.getInstance().getConnection()) {
            // Fetch old role for audit
            int oldRoleId = sessionUser.getUserRole();
            String oldRoleName = "";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT roleName FROM role WHERE roleId = ?")) {
                ps.setInt(1, oldRoleId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) oldRoleName = rs.getString("roleName");
            }

            // Update user role
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE user SET userRole = ? WHERE userId = ?")) {
                ps.setInt(1, newRoleId);
                ps.setInt(2, sessionUser.getUserId());
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    sendError(resp, 500, "Failed to update role.");
                    return;
                }
            }

            // Refresh session user role
            sessionUser.setUserRole(newRoleId);
            session.setAttribute(AuthController.SESSION_USER, sessionUser);

            // Fetch new role name for response
            String newRoleName = "";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT roleName FROM role WHERE roleId = ?")) {
                ps.setInt(1, newRoleId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) newRoleName = rs.getString("roleName");
            }

            // Audit log
            AuditLogger.log(
                sessionUser.getUserId(),
                sessionUser.getUserName(),
                "SUBSCRIPTION_CHANGE",
                "user",
                sessionUser.getUserId(),
                "Changed role from " + oldRoleName + " (ID " + oldRoleId +
                ") to " + newRoleName + " (ID " + newRoleId + ")"
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Subscription updated to " + newRoleName);
            response.put("newRoleId", newRoleId);
            response.put("roleName", newRoleName);
            sendJson(resp, 200, response);
        } catch (Exception e) {
            sendError(resp, 500, "Database error during subscription update.");
        }
    }

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
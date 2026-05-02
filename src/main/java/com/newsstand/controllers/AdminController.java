package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.Role;
import com.newsstand.models.User;
import com.newsstand.security.RBACManager;
import com.newsstand.security.RBACManager.Permission;
import com.newsstand.utils.AuditLogger;   // new import for audit logging
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

/**
 * AdminController — admin-only endpoints for user management.
 *
 * Every handler performs a secondary RBAC check on the specific permission it needs,
 * even though AuthFilter already requires VIEW_ALL_USERS for /api/admin/*.
 * This is defence-in-depth: if the filter is ever mis-configured, the
 * controller itself still refuses unauthorised requests.
 *
 * Routes
 * ──────
 *   GET    /api/admin/users            – list all users
 *   PUT    /api/admin/users/role       – change a user's role
 *   DELETE /api/admin/users/{id}       – delete a user
 */
@WebServlet("/api/admin/*")
public class AdminController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ── HTTP method dispatchers ──────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if ("/users".equals(path) || "/users/".equals(path)) {
            listUsers(req, resp);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if ("/users/role".equals(path)) {
            changeUserRole(req, resp);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/users/")) {
            String idStr = path.substring("/users/".length());
            deleteUser(req, resp, idStr);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────
    private void listUsers(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!checkPermission(req, resp, Permission.VIEW_ALL_USERS)) return;

        try {
            Connection conn = DBManager.getInstance().getConnection();
            String sql = "SELECT u.userId, u.userName, u.userEmail, u.userRole, " +
                         "r.roleName, u.languageId FROM user u " +
                         "JOIN role r ON u.userRole = r.roleId " +
                         "ORDER BY u.userId";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                List<Map<String, Object>> users = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId",     rs.getInt("userId"));
                    row.put("userName",   rs.getString("userName"));
                    row.put("userEmail",  rs.getString("userEmail"));
                    row.put("userRole",   rs.getInt("userRole"));
                    row.put("roleName",   rs.getString("roleName"));
                    row.put("languageId", rs.getInt("languageId"));
                    // permissions this user holds
                    row.put("permissions",
                            RBACManager.getPermissionsForRole(rs.getInt("userRole"))
                                       .stream().map(Enum::name).toList());
                    users.add(row);
                }
                sendJson(resp, 200, users);
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error fetching users.");
        }
    }

    // ── PUT /api/admin/users/role ─────────────────────────────────────────
    private void changeUserRole(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!checkPermission(req, resp, Permission.CHANGE_USER_ROLE)) return;

        User currentAdmin = (User) req.getAttribute("currentUser");
        Map<?, ?> body  = JSON.readValue(req.getReader(), Map.class);
        int targetId    = (int) body.get("userId");
        int newRoleId   = (int) body.get("newRoleId");

        try { Role.fromId(newRoleId); }
        catch (IllegalArgumentException e) {
            sendError(resp, 400, "Invalid roleId: " + newRoleId);
            return;
        }

        try {
            Connection conn = DBManager.getInstance().getConnection();
            // Get old role for logging
            int oldRoleId = 0;
            try (PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT userRole FROM user WHERE userId = ?")) {
                psCheck.setInt(1, targetId);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) oldRoleId = rs.getInt("userRole");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE user SET userRole = ? WHERE userId = ?")) {
                ps.setInt(1, newRoleId);
                ps.setInt(2, targetId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    sendError(resp, 404, "User not found.");
                } else {
                    // ── Audit log ──
                    AuditLogger.log(
                        currentAdmin.getUserId(),
                        currentAdmin.getUserName(),
                        "CHANGE_USER_ROLE",
                        "user",
                        targetId,
                        "Changed role from " + oldRoleId + " to " + newRoleId
                    );

                    sendJson(resp, 200, Map.of(
                            "message",    "Role updated successfully.",
                            "userId",     targetId,
                            "newRoleId",  newRoleId,
                            "permissions",
                            RBACManager.getPermissionsForRole(newRoleId)
                                       .stream().map(Enum::name).toList()
                    ));
                }
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error changing role.");
        }
    }

    // ── DELETE /api/admin/users/{id} ──────────────────────────────────────
    private void deleteUser(HttpServletRequest req, HttpServletResponse resp, String idStr)
            throws IOException {
        if (!checkPermission(req, resp, Permission.DELETE_USER)) return;

        User currentAdmin = (User) req.getAttribute("currentUser");
        int targetId;
        try { targetId = Integer.parseInt(idStr); }
        catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid user id.");
            return;
        }

        // Prevent admins from deleting themselves
        if (currentAdmin != null && currentAdmin.getUserId() == targetId) {
            sendError(resp, 400, "You cannot delete your own account.");
            return;
        }

        try {
            Connection conn = DBManager.getInstance().getConnection();
            // Get user info for logging before deletion
            String targetName = null;
            try (PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT userName FROM user WHERE userId = ?")) {
                psCheck.setInt(1, targetId);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) targetName = rs.getString("userName");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM user WHERE userId = ?")) {
                ps.setInt(1, targetId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    sendError(resp, 404, "User not found.");
                } else {
                    // ── Audit log ──
                    AuditLogger.log(
                        currentAdmin.getUserId(),
                        currentAdmin.getUserName(),
                        "DELETE_USER",
                        "user",
                        targetId,
                        "Deleted user: " + (targetName != null ? targetName : "ID " + targetId)
                    );

                    sendJson(resp, 200, Map.of("message", "User deleted."));
                }
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error deleting user.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private boolean checkPermission(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    Permission required) throws IOException {
        User user = (User) req.getAttribute("currentUser");
        if (user == null || !RBACManager.hasPermission(user.getUserRole(), required)) {
            sendError(resp, 403, "Access denied. Required permission: " + required.name());
            return false;
        }
        return true;
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
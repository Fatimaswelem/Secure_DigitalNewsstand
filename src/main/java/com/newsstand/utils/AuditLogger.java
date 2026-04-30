package com.newsstand.utils;

import com.newsstand.database.DBManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {

    /**
     * Logs an admin action.
     *
     * @param adminId    ID of the admin performing the action (from session)
     * @param adminName  Name of the admin (from session)
     * @param action     Action type (e.g. "DELETE_USER")
     * @param targetType Entity affected (e.g. "user")
     * @param targetId   ID of affected entity (can be null)
     * @param details    Extra info (can be null)
     */
    public static void log(int adminId, String adminName, String action,
                           String targetType, Integer targetId, String details) {
        String sql = "INSERT INTO audit_log (admin_id, admin_name, action, target_type, target_id, details, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, adminId);
            ps.setString(2, adminName);
            ps.setString(3, action);
            ps.setString(4, targetType);
            if (targetId != null) {
                ps.setInt(5, targetId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setString(6, details);
            ps.setString(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.executeUpdate();
        } catch (SQLException e) {
            // In production, log this to a file; for now, stderr
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    // Convenience overload
    public static void log(int adminId, String adminName, String action, String targetType, int targetId, String details) {
        log(adminId, adminName, action, targetType, (Integer) targetId, details);
    }
}
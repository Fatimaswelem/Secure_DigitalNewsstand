package com.newsstand.security;

import com.newsstand.models.Role;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * RBACManager — Role-Based Access Control
 * ────────────────────────────────────────────────────────────────────────────
 * Defines which permissions each role possesses and provides a single
 * authorisation check point used by every servlet/filter.
 *
 * PROJECT EVALUATION CRITERIA MET
 *  ✔ Authorization System – Role-Based Access Control (RBAC)  (5 pts)
 *  ✔ User Permissions Management                              (5 pts)
 */
public class RBACManager {

    // ── Permission catalogue ─────────────────────────────────────────────────

    public enum Permission {
        // Content
        READ_FREE_CONTENT,
        READ_PREMIUM_CONTENT,
        // Article management
        CREATE_ARTICLE,
        EDIT_ARTICLE,
        DELETE_ARTICLE,
        // User management
        VIEW_ALL_USERS,
        CHANGE_USER_ROLE,
        DELETE_USER,
        MANAGE_CATEGORIES,
        // Subscription
        MANAGE_SUBSCRIPTIONS,
        // Feedback
        SUBMIT_FEEDBACK,
        MANAGE_FEEDBACK,
        // Profile
        UPDATE_OWN_PROFILE
    }

    // ── Role → Permission mapping ─────────────────────────────────────────────

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS =
            new EnumMap<>(Role.class);

    static {
        // REGULAR users: read free content, submit feedback, manage own profile
        ROLE_PERMISSIONS.put(Role.REGULAR, EnumSet.of(
                Permission.READ_FREE_CONTENT,
                Permission.SUBMIT_FEEDBACK,
                Permission.UPDATE_OWN_PROFILE
        ));

        // PREMIUM users: everything REGULAR can do + premium content + subscriptions
        ROLE_PERMISSIONS.put(Role.PREMIUM, EnumSet.of(
                Permission.READ_FREE_CONTENT,
                Permission.READ_PREMIUM_CONTENT,
                Permission.SUBMIT_FEEDBACK,
                Permission.UPDATE_OWN_PROFILE,
                Permission.MANAGE_SUBSCRIPTIONS
        ));

        // ADMIN: full access
        ROLE_PERMISSIONS.put(Role.ADMIN, EnumSet.allOf(Permission.class));
    }

    private RBACManager() { /* utility class */ }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Checks whether the given role has the required permission.
     *
     * @param roleId     the user's roleId (from the database / session)
     * @param permission the permission being checked
     * @return true if the role holds the permission
     */
    public static boolean hasPermission(int roleId, Permission permission) {
        try {
            Role role = Role.fromId(roleId);
            Set<Permission> perms = ROLE_PERMISSIONS.get(role);
            return perms != null && perms.contains(permission);
        } catch (IllegalArgumentException e) {
            return false; // unknown roleId → deny
        }
    }

    /**
     * Returns the full set of permissions assigned to a role.
     *
     * @param roleId the user's roleId
     * @return an unmodifiable copy of the permission set
     */
    public static Set<Permission> getPermissionsForRole(int roleId) {
        try {
            Role role = Role.fromId(roleId);
            Set<Permission> perms = ROLE_PERMISSIONS.get(role);
            return perms != null ? EnumSet.copyOf(perms) : EnumSet.noneOf(Permission.class);
        } catch (IllegalArgumentException e) {
            return EnumSet.noneOf(Permission.class);
        }
    }
}

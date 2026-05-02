package com.newsstand.security;

import com.newsstand.models.Role;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Role-Based Access Control
 *
 * Defines which permissions each role possesses and provides a single
 * authorisation check point used by every servlet/filter.
 *
 * ROLES CURRENTLY IN THE SYSTEM
 *   Admin    (1) – system management only (no user‑facing actions)
 *   Regular  (2) – base user, no subscription
 *   Basic    (3) – $9/mo  (same base permissions)
 *   Standard (4) – $19/mo (same base permissions)
 *   Premium  (5) – $29/mo (extra premium content + subscription management)
 */
public class RBACManager {

    public enum Permission {
        // Content access
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

        // Category management
        MANAGE_CATEGORIES,

        // Subscription (for users & admin)
        MANAGE_SUBSCRIPTIONS,

        // Feedback
        SUBMIT_FEEDBACK,
        MANAGE_FEEDBACK,

        // Profile
        UPDATE_OWN_PROFILE
    }

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS =
            new EnumMap<>(Role.class);

    static {
        // ── Regular (not subscribed) ──────────────────────────────────────
        ROLE_PERMISSIONS.put(Role.REGULAR, EnumSet.of(
            Permission.READ_FREE_CONTENT,
            Permission.SUBMIT_FEEDBACK,
            Permission.UPDATE_OWN_PROFILE
        ));

        // ── Basic ─────────────────────────────────────────────────────────
        ROLE_PERMISSIONS.put(Role.BASIC, EnumSet.of(
            Permission.READ_FREE_CONTENT,
            Permission.SUBMIT_FEEDBACK,
            Permission.UPDATE_OWN_PROFILE
        ));

        // ── Standard ──────────────────────────────────────────────────────
        ROLE_PERMISSIONS.put(Role.STANDARD, EnumSet.of(
            Permission.READ_FREE_CONTENT,
            Permission.SUBMIT_FEEDBACK,
            Permission.UPDATE_OWN_PROFILE
        ));

        // ── Premium ───────────────────────────────────────────────────────
        ROLE_PERMISSIONS.put(Role.PREMIUM, EnumSet.of(
            Permission.READ_FREE_CONTENT,
            Permission.READ_PREMIUM_CONTENT,
            Permission.SUBMIT_FEEDBACK,
            Permission.UPDATE_OWN_PROFILE,
            Permission.MANAGE_SUBSCRIPTIONS
        ));

        // ── Admin (management only – no user‑level permissions) ───────────
        ROLE_PERMISSIONS.put(Role.ADMIN, EnumSet.of(
            Permission.CREATE_ARTICLE,
            Permission.EDIT_ARTICLE,
            Permission.DELETE_ARTICLE,
            Permission.VIEW_ALL_USERS,
            Permission.CHANGE_USER_ROLE,
            Permission.DELETE_USER,
            Permission.MANAGE_CATEGORIES,
            Permission.MANAGE_SUBSCRIPTIONS,
            Permission.MANAGE_FEEDBACK
        ));
    }

    private RBACManager() { /* utility class */ }

    public static boolean hasPermission(int roleId, Permission permission) {
        try {
            Role role = Role.fromId(roleId);
            Set<Permission> perms = ROLE_PERMISSIONS.get(role);
            return perms != null && perms.contains(permission);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

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

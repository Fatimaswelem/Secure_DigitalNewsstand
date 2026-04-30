package com.newsstand.models;

/**
 * User model — mirrors the `user` table.
 * Passwords are NEVER stored in plain text; only BCrypt hashes are persisted.
 */
public class User {

    private int    userId;
    private String userName;
    private String userEmail;
    private String userPassword;   // BCrypt hash when loaded from DB
    private int    userRole;       // FK → role.roleId  (1=admin, 2=regular, 3=premium)
    private int    languageId;

    // ── Constructors ────────────────────────────────────────────────────────

    public User() {}

    public User(String userName, String userEmail, String userPassword, int languageId) {
        this.userName     = userName;
        this.userEmail    = userEmail;
        this.userPassword = userPassword;
        this.languageId   = languageId;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int    getUserId()       { return userId; }
    public void   setUserId(int id) { this.userId = id; }

    public String getUserName()             { return userName; }
    public void   setUserName(String n)     { this.userName = n; }

    public String getUserEmail()            { return userEmail; }
    public void   setUserEmail(String e)    { this.userEmail = e; }

    public String getUserPassword()         { return userPassword; }
    public void   setUserPassword(String p) { this.userPassword = p; }

    public int    getUserRole()             { return userRole; }
    public void   setUserRole(int r)        { this.userRole = r; }

    public int    getLanguageId()           { return languageId; }
    public void   setLanguageId(int l)      { this.languageId = l; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", email=" + userEmail + ", role=" + userRole + "}";
    }
}

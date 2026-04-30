package com.newsstand.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBManager – provides fresh JDBC connections on demand.
 *
 * In production you would use a pool (HikariCP, Tomcat JDBC, etc.).
 * For this project, a simple factory that returns a new connection
 * each time is reliable and avoids dead‑connection issues.
 */
public class DBManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/digital_newsstand"
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Root@1234";

    private static DBManager instance;   // still a singleton, but now only for convenience

    private DBManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public static synchronized DBManager getInstance() {
        if (instance == null) {
            instance = new DBManager();
        }
        return instance;
    }

    /**
     * Returns a <b>brand new</b> connection every time.
     * The caller is responsible for closing it (preferably with try‑with‑resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /** No longer needed – connections are closed by the caller. */
    public void close() {
        // nothing to do
    }
}

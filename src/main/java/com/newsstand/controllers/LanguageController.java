package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/api/languages")
public class LanguageController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("sessionUser") == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        List<Map<String, Object>> langs = new ArrayList<>();
        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT languageId, languageName FROM language ORDER BY languageId");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> lang = new LinkedHashMap<>();
                lang.put("id", rs.getInt("languageId"));
                lang.put("name", rs.getString("languageName"));
                langs.add(lang);
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
            return;
        }
        sendJson(resp, 200, langs);
    }

    private void sendJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) { JSON.writeValue(out, payload); }
    }

    private void sendError(HttpServletResponse resp, int status, String msg) throws IOException {
        sendJson(resp, status, Map.of("error", msg));
    }
}
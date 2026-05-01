package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import com.newsstand.models.User;
import com.newsstand.security.RBACManager;
import com.newsstand.security.RBACManager.Permission;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/api/articles/*")
public class ArticleController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ── GET /api/articles?categoryId=...  OR  /api/articles/{id} ──
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("sessionUser") == null) {
            sendError(resp, 401, "Not authenticated.");
            return;
        }

        String path = req.getPathInfo();

        // Single article: /api/articles/15
        if (path != null && path.length() > 1) {
            int articleId;
            try { articleId = Integer.parseInt(path.substring(1)); }
            catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid article id.");
                return;
            }
            getSingleArticle(resp, articleId);
            return;
        }

        // List by category: /api/articles?categoryId=3
        String categoryIdParam = req.getParameter("categoryId");
        if (categoryIdParam == null) {
            sendError(resp, 400, "categoryId required.");
            return;
        }
        int categoryId;
        try { categoryId = Integer.parseInt(categoryIdParam); }
        catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid category id.");
            return;
        }
        listArticlesByCategory(resp, categoryId);
    }

    private void listArticlesByCategory(HttpServletResponse resp, int categoryId) throws IOException {
        List<Map<String, Object>> articles = new ArrayList<>();
        String sql = "SELECT a.articleId, a.articleTitle, a.articleContent, a.articleImg, " +
                     "a.articlePublicationDate, a.articleAuthor, c.categoryName, l.languageName " +
                     "FROM article a " +
                     "LEFT JOIN category c ON a.categoryId = c.categoryId " +
                     "LEFT JOIN language l ON a.languageId = l.languageId " +
                     "WHERE a.categoryId = ? ORDER BY a.articlePublicationDate DESC";

        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> art = new LinkedHashMap<>();
                art.put("id", rs.getInt("articleId"));
                art.put("title", rs.getString("articleTitle"));
                art.put("image", rs.getString("articleImg"));
                String fullContent = rs.getString("articleContent");
                String excerpt = fullContent != null && fullContent.length() > 120
                                 ? fullContent.substring(0, 120) + "..." : fullContent;
                art.put("excerpt", excerpt);
                art.put("author", rs.getString("articleAuthor"));
                art.put("categoryName", rs.getString("categoryName"));
                art.put("languageName", rs.getString("languageName"));
                art.put("date", rs.getTimestamp("articlePublicationDate"));
                articles.add(art);
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
            return;
        }
        sendJson(resp, 200, articles);
    }

    private void getSingleArticle(HttpServletResponse resp, int articleId) throws IOException {
        String sql = "SELECT a.*, c.categoryName, l.languageName " +
                     "FROM article a " +
                     "LEFT JOIN category c ON a.categoryId = c.categoryId " +
                     "LEFT JOIN language l ON a.languageId = l.languageId " +
                     "WHERE a.articleId = ?";
        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                sendError(resp, 404, "Article not found.");
                return;
            }
            Map<String, Object> art = new LinkedHashMap<>();
            art.put("id", rs.getInt("articleId"));
            art.put("title", rs.getString("articleTitle"));
            art.put("content", rs.getString("articleContent"));
            art.put("imageUrl", rs.getString("articleImg"));
            art.put("categoryId", rs.getInt("categoryId"));
            art.put("categoryName", rs.getString("categoryName"));
            art.put("languageId", rs.getInt("languageId"));
            art.put("languageName", rs.getString("languageName"));
            art.put("author", rs.getString("articleAuthor"));
            art.put("createdAt", rs.getTimestamp("articlePublicationDate"));
            sendJson(resp, 200, art);
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
        }
    }

    // ── POST /api/articles (admin only) ─────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("sessionUser");
        if (user == null || !RBACManager.hasPermission(user.getUserRole(), Permission.CREATE_ARTICLE)) {
            sendError(resp, 403, "Access denied.");
            return;
        }

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String title   = (String) body.get("title");
        String content = (String) body.get("content");
        String imageUrl= (String) body.get("imageUrl");
        int categoryId = body.get("categoryId") != null ? (int) body.get("categoryId") : 0;
        int languageId = body.get("languageId") != null ? (int) body.get("languageId") : 1;

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            sendError(resp, 400, "Title and content required.");
            return;
        }

        try (Connection conn = DBManager.getInstance().getConnection()) {
            String sql = "INSERT INTO article (userId, articleTitle, articleContent, articleImg, categoryId, languageId, articlePublicationDate) " +
                         "VALUES (?, ?, ?, ?, ?, ?, CURDATE())";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, user.getUserId());
            ps.setString(2, title.trim());
            ps.setString(3, content.trim());
            ps.setString(4, imageUrl);
            if (categoryId > 0) ps.setInt(5, categoryId); else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, languageId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;
            sendJson(resp, 201, Map.of("message", "Article created", "id", newId));
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
        }
    }

    // ── PUT /api/articles/{id} (admin) ─────────────────────
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("sessionUser");
        if (user == null || !RBACManager.hasPermission(user.getUserRole(), Permission.EDIT_ARTICLE)) {
            sendError(resp, 403, "Access denied.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) {
            sendError(resp, 400, "Article id required.");
            return;
        }
        int articleId;
        try { articleId = Integer.parseInt(path.substring(1)); }
        catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid id.");
            return;
        }

        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String title   = (String) body.get("title");
        String content = (String) body.get("content");
        String imageUrl= (String) body.get("imageUrl");
        int categoryId = body.get("categoryId") != null ? (int) body.get("categoryId") : 0;
        int languageId = body.get("languageId") != null ? (int) body.get("languageId") : 1;

        try (Connection conn = DBManager.getInstance().getConnection()) {
            String sql = "UPDATE article SET articleTitle=?, articleContent=?, articleImg=?, categoryId=?, languageId=? WHERE articleId=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, title.trim());
            ps.setString(2, content.trim());
            ps.setString(3, imageUrl);
            if (categoryId > 0) ps.setInt(4, categoryId); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, languageId);
            ps.setInt(6, articleId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                sendError(resp, 404, "Article not found.");
            } else {
                sendJson(resp, 200, Map.of("message", "Article updated"));
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
        }
    }

    // ── DELETE /api/articles/{id} (admin) ──────────────────
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("sessionUser");
        if (user == null || !RBACManager.hasPermission(user.getUserRole(), Permission.DELETE_ARTICLE)) {
            sendError(resp, 403, "Access denied.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) {
            sendError(resp, 400, "Article id required.");
            return;
        }
        int articleId;
        try { articleId = Integer.parseInt(path.substring(1)); }
        catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid id.");
            return;
        }

        try (Connection conn = DBManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM article WHERE articleId = ?")) {
            ps.setInt(1, articleId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                sendError(resp, 404, "Article not found.");
            } else {
                sendJson(resp, 200, Map.of("message", "Article deleted"));
            }
        } catch (SQLException e) {
            sendError(resp, 500, "Database error: " + e.getMessage());
        }
    }

    // ── helpers ────────────────────────────────────────────
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
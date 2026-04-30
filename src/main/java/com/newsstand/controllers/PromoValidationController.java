package com.newsstand.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsstand.database.DBManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/promo/*")
public class PromoValidationController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if ("/validate".equals(path)) {
            validatePromoCode(req, resp);
        } else {
            sendError(resp, 404, "Not found");
        }
    }

    private void validatePromoCode(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Map<?, ?> body = JSON.readValue(req.getReader(), Map.class);
        String promoCode = (String) body.get("promoCode");
        int planId = body.get("planId") != null ? (int) body.get("planId") : 0;

        if (promoCode == null || promoCode.isBlank()) {
            sendError(resp, 400, "Promo code is required.");
            return;
        }

        try (Connection conn = DBManager.getInstance().getConnection()) {
            String sql = "SELECT * FROM promo_codes WHERE code = ? AND is_active = TRUE";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, promoCode.toUpperCase().trim());
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    sendJson(resp, 200, Map.of("valid", false, "message", "Invalid promo code."));
                    return;
                }

                // Expiry check
                Date expiryDate = rs.getDate("expiry_date");
                if (expiryDate != null && expiryDate.before(new Date(System.currentTimeMillis()))) {
                    sendJson(resp, 200, Map.of("valid", false, "message", "Promo code expired."));
                    return;
                }

                // Usage limit check
                int maxUses = rs.getInt("max_uses");
                int timesUsed = rs.getInt("times_used");
                if (maxUses > 0 && timesUsed >= maxUses) {
                    sendJson(resp, 200, Map.of("valid", false, "message", "Promo code usage limit reached."));
                    return;
                }

                // Code is valid – calculate discount
                String discountType = rs.getString("discount_type");
                double discountValue = rs.getDouble("discount_value");
                double basePrice = getBasePriceForPlan(planId);
                double discountedPrice = basePrice;

                if ("percentage".equals(discountType)) {
                    discountedPrice = basePrice * (1 - discountValue / 100.0);
                } else if ("fixed".equals(discountType)) {
                    discountedPrice = Math.max(0, basePrice - discountValue);
                }

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("valid", true);
                response.put("discountType", discountType);
                response.put("discountValue", discountValue);
                response.put("discountedPrice", Math.round(discountedPrice * 100.0) / 100.0);
                sendJson(resp, 200, response);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(resp, 500, "Database error validating promo code.");
        }
    }

    /** Hardcoded plan prices – replace with a DB lookup if you have a plans table */
    private double getBasePriceForPlan(int planId) {
        switch (planId) {
            case 1: return 9.0;
            case 2: return 19.0;
            case 3: return 29.0;
            default: return 0.0;
        }
    }

    // ----- helpers (same as your other controllers) -----
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
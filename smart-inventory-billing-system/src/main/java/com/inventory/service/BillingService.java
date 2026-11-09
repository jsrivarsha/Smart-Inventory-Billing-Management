package com.inventory.service;

import com.inventory.dao.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BillingService {

    public int createSale(
            String customerName,
            BigDecimal discount,
            Map<String, Integer> cart
    ) throws SQLException {

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        if (discount == null || discount.signum() < 0) {
            throw new IllegalArgumentException("Invalid discount.");
        }

        Map<String, BigDecimal> prices = new HashMap<>();
        Map<String, Integer> previousQuantities = new HashMap<>();

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                BigDecimal subtotal = BigDecimal.ZERO;

                // Check products, prices and available stock
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {

                    String productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    if (productId == null || quantity == null || quantity <= 0) {
                        throw new IllegalArgumentException(
                                "Invalid product or quantity."
                        );
                    }

                    String sql =
                            "SELECT price, quantity FROM products " +
                            "WHERE product_id = ? FOR UPDATE";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setString(1, productId);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product not found: " + productId
                                );
                            }

                            BigDecimal price = rs.getBigDecimal("price");
                            int available = rs.getInt("quantity");

                            if (available < quantity) {
                                throw new SQLException(
                                        "Insufficient stock for product: " +
                                        productId
                                );
                            }

                            prices.put(productId, price);
                            previousQuantities.put(productId, available);

                            BigDecimal itemTotal = price.multiply(
                                    BigDecimal.valueOf(quantity)
                            );

                            subtotal = subtotal.add(itemTotal);
                        }
                    }
                }

                if (discount.compareTo(subtotal) > 0) {
                    throw new IllegalArgumentException(
                            "Discount cannot exceed subtotal."
                    );
                }

                BigDecimal finalAmount = subtotal.subtract(discount);

                // Insert sale
                String saleSql =
                        "INSERT INTO sales " +
                        "(customer_name, subtotal, discount, final_amount) " +
                        "VALUES (?, ?, ?, ?)";

                int saleId;

                try (PreparedStatement ps = con.prepareStatement(
                        saleSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, customerName);
                    ps.setBigDecimal(2, subtotal);
                    ps.setBigDecimal(3, discount);
                    ps.setBigDecimal(4, finalAmount);

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not generate invoice number."
                            );
                        }

                        saleId = keys.getInt(1);
                    }
                }

                // Insert sale items
                String itemSql =
                        "INSERT INTO sale_items " +
                        "(sale_id, product_id, quantity, unit_price, total_price) " +
                        "VALUES (?, ?, ?, ?, ?)";

                // Reduce product stock
                String stockSql =
                        "UPDATE products SET quantity = quantity - ? " +
                        "WHERE product_id = ? AND quantity >= ?";

                // Record stock movement
                String movementSql =
                        "INSERT INTO stock_movements " +
                        "(product_id, movement_type, quantity, " +
                        "previous_quantity, new_quantity, sale_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                try (
                        PreparedStatement itemPs = con.prepareStatement(itemSql);
                        PreparedStatement stockPs = con.prepareStatement(stockSql);
                        PreparedStatement movementPs =
                                con.prepareStatement(movementSql)
                ) {

                    for (Map.Entry<String, Integer> entry : cart.entrySet()) {

                        String productId = entry.getKey();
                        int quantity = entry.getValue();

                        BigDecimal price = prices.get(productId);
                        int previousQuantity =
                                previousQuantities.get(productId);

                        int newQuantity = previousQuantity - quantity;

                        BigDecimal total = price.multiply(
                                BigDecimal.valueOf(quantity)
                        );

                        // Save sale item
                        itemPs.setInt(1, saleId);
                        itemPs.setString(2, productId);
                        itemPs.setInt(3, quantity);
                        itemPs.setBigDecimal(4, price);
                        itemPs.setBigDecimal(5, total);
                        itemPs.executeUpdate();

                        // Update product stock
                        stockPs.setInt(1, quantity);
                        stockPs.setString(2, productId);
                        stockPs.setInt(3, quantity);

                        if (stockPs.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Stock update failed for " + productId
                            );
                        }

                        // Record stock movement
                        movementPs.setString(1, productId);
                        movementPs.setString(2, "SALE");
                        movementPs.setInt(3, quantity);
                        movementPs.setInt(4, previousQuantity);
                        movementPs.setInt(5, newQuantity);
                        movementPs.setInt(6, saleId);

                        movementPs.executeUpdate();
                    }
                }

                con.commit();
                return saleId;

            } catch (Exception e) {

                con.rollback();

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }

                throw new SQLException(e);

            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}

package com.inventory.service;

import com.inventory.dao.DBConnection;
import com.inventory.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {

    // Add Product
    public void addProduct(Product product) throws SQLException {

        String sql = "INSERT INTO products VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, product.getProductId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setDouble(4, product.getPrice());
            ps.setInt(5, product.getQuantity());

            ps.executeUpdate();
        }
    }

    // View All Products
    public List<Product> getAllProducts() throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql = "SELECT * FROM products";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product(
                    rs.getString("product_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                );

                products.add(p);
            }
        }

        return products;
    }

    // Search Product by ID
    public Product searchProduct(String productId) throws SQLException {

        String sql = "SELECT * FROM products WHERE product_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return new Product(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                    );
                }
            }
        }

        return null;
    }

    // Update Product
    public void updateProduct(Product product) throws SQLException {

        String sql = "UPDATE products SET name=?, category=?, price=?, quantity=? WHERE product_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getQuantity());
            ps.setString(5, product.getProductId());

            ps.executeUpdate();
        }
    }

    // Delete Product
    public void deleteProduct(String productId) throws SQLException {

        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ps.executeUpdate();
        }
    }
    
public List<Product> searchProductsByName(String name) throws SQLException {

    List<Product> products = new ArrayList<>();

    String sql = "SELECT * FROM products WHERE name LIKE ?";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, "%" + name + "%");

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Product p = new Product(
                    rs.getString("product_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                );

                products.add(p);
            }
        }
    }

    return products;
}

}

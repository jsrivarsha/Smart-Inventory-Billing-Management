
package com.inventory.service;

import com.inventory.dao.DBConnection;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    // CHECK WHETHER USERS EXIST

    public boolean hasUsers() throws SQLException {

        String sql = "SELECT COUNT(*) FROM users";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    // REGISTER USER

    public void registerUser(
            String username,
            String password,
            String role
    ) throws Exception {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        if (!role.equals("ADMIN") && !role.equals("STAFF")) {
            throw new IllegalArgumentException("Invalid user role.");
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        String saltText = Base64.getEncoder().encodeToString(salt);
        String hash = hashPassword(password, salt);

        String sql =
                "INSERT INTO users " +
                "(username, password_hash, password_salt, role) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, username.trim());
            ps.setString(2, hash);
            ps.setString(3, saltText);
            ps.setString(4, role);

            ps.executeUpdate();
        }
    }

    // LOGIN VALIDATION

    public String login(String username, String password)
            throws Exception {

        String sql =
                "SELECT password_hash, password_salt, role " +
                "FROM users WHERE username = ?";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                String storedHash = rs.getString("password_hash");
                String storedSalt = rs.getString("password_salt");
                String role = rs.getString("role");

                byte[] salt =
                        Base64.getDecoder().decode(storedSalt);

                String enteredHash = hashPassword(password, salt);

                if (java.security.MessageDigest.isEqual(
                        storedHash.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        ),
                        enteredHash.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                )) {

                    return role;
                }

                return null;
            }
        }
    }

    // PASSWORD HASHING

    private String hashPassword(
            String password,
            byte[] salt
    ) throws Exception {

        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hash);
    }
}

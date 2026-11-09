
package com.inventory.dao;

import java.sql.Connection;

public class DBConnectionTest {

    public static void main(String[] args) {

        try (Connection connection = DBConnection.getConnection()) {

            if (connection != null) {
                System.out.println("MySQL Connected Successfully!");
            }

        } catch (Exception e) {
            System.out.println("MySQL Connection Failed!");
            e.printStackTrace();
        }
    }
}

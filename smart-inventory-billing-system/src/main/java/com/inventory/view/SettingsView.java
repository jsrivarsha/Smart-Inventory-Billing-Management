
package com.inventory.view;

import com.inventory.dao.DBConnection;
import java.sql.Connection;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SettingsView {

    private Label connectionStatus;

    public void show() {

        Stage stage = new Stage();
        stage.setTitle("Settings - Smart Inventory");

        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label title = new Label("Settings");
        title.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
            "Manage application information and database settings."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        // DATABASE SETTINGS
        VBox databaseCard = new VBox(15);
        databaseCard.setPadding(new Insets(22));
        databaseCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;"
        );

        Label databaseTitle = new Label("Database Settings");
        databaseTitle.setStyle(
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;"
        );

        Label databaseName = new Label("Database: inventory_db");
        Label databaseServer = new Label("Server: localhost:3306");
        Label databaseUser = new Label("Username: root");

        connectionStatus = new Label("Checking connection...");
        connectionStatus.setStyle(
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #64748B;"
        );

        Button testButton = new Button("Test Database Connection");
        testButton.setStyle(
            "-fx-background-color: #4338CA;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 18;" +
            "-fx-background-radius: 7;"
        );

        testButton.setOnAction(e -> testConnection());

        databaseCard.getChildren().addAll(
            databaseTitle,
            databaseName,
            databaseServer,
            databaseUser,
            connectionStatus,
            testButton
        );

        // INVENTORY SETTINGS
        VBox inventoryCard = new VBox(12);
        inventoryCard.setPadding(new Insets(22));
        inventoryCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;"
        );

        Label inventoryTitle = new Label("Inventory Settings");
        inventoryTitle.setStyle(
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;"
        );

        Label lowStock = new Label(
            "Low Stock Alert Threshold: 10 units"
        );

        Label lowStockDescription = new Label(
            "Products with 10 or fewer units are considered low stock."
        );
        lowStockDescription.setStyle(
            "-fx-text-fill: #64748B;"
        );

        inventoryCard.getChildren().addAll(
            inventoryTitle,
            lowStock,
            lowStockDescription
        );

        // APPLICATION INFORMATION
        VBox aboutCard = new VBox(12);
        aboutCard.setPadding(new Insets(22));
        aboutCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;"
        );

        Label aboutTitle = new Label("Application Information");
        aboutTitle.setStyle(
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;"
        );

        Label appName = new Label(
            "Application: Smart Inventory and Billing Management System"
        );

        Label technology = new Label(
            "Technology: Java 21, JavaFX, JDBC and MySQL"
        );

        Label version = new Label("Version: 1.0");

        aboutCard.getChildren().addAll(
            aboutTitle,
            appName,
            technology,
            version
        );

        root.getChildren().addAll(
            title,
            subtitle,
            databaseCard,
            inventoryCard,
            aboutCard
        );

        testConnection();

        Scene scene = new Scene(root, 650, 650);
        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMinHeight(550);
        stage.show();
    }

    // TEST MYSQL CONNECTION
    private void testConnection() {

        try (Connection con = DBConnection.getConnection()) {

            if (con != null && !con.isClosed()) {

                connectionStatus.setText(
                    "Connection Status: Connected Successfully"
                );

                connectionStatus.setStyle(
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #16A34A;"
                );
            }

        } catch (Exception e) {

            connectionStatus.setText(
                "Connection Status: Connection Failed"
            );

            connectionStatus.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #DC2626;"
            );
        }
    }
}

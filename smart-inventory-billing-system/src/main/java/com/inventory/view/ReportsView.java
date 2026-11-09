
package com.inventory.view;

import com.inventory.dao.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ReportsView extends Stage {

    private Label productsValue;
    private Label stockValue;
    private Label lowStockValue;
    private Label revenueValue;
    private Label transactionsValue;

    private BarChart<String, Number> salesChart;

    public ReportsView() {

        setTitle("Smart Inventory - Reports & Analytics");

        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Reports & Analytics");
        heading.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
            "Monitor inventory and business performance."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        GridPane summary = createSummaryCards();

        salesChart = createSalesChart();

        VBox chartPanel = new VBox(15);
        chartPanel.setPadding(new Insets(20));
        chartPanel.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;"
        );

        Label chartHeading = new Label("Monthly Sales Revenue");
        chartHeading.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;"
        );

        chartPanel.getChildren().addAll(
            chartHeading,
            salesChart
        );

        Button refreshButton = new Button("Refresh Reports");
        refreshButton.setStyle(
            "-fx-background-color: #4338CA;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 7;"
        );

        refreshButton.setOnAction(e -> loadReports());

        root.getChildren().addAll(
            heading,
            subtitle,
            summary,
            chartPanel,
            refreshButton
        );

        loadReports();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);

        setScene(new Scene(scroll, 1100, 750));
        setMinWidth(850);
        setMinHeight(650);
    }

    // SUMMARY CARDS
    private GridPane createSummaryCards() {

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        productsValue = new Label("0");
        stockValue = new Label("0");
        lowStockValue = new Label("0");
        revenueValue = new Label("₹0.00");
        transactionsValue = new Label("0");

        grid.add(createCard(
            "Total Products", productsValue, "#DBEAFE"
        ), 0, 0);

        grid.add(createCard(
            "Total Stock Units", stockValue, "#DCFCE7"
        ), 1, 0);

        grid.add(createCard(
            "Low Stock Products", lowStockValue, "#FEE2E2"
        ), 2, 0);

        grid.add(createCard(
            "Total Revenue", revenueValue, "#EDE9FE"
        ), 0, 1);

        grid.add(createCard(
            "Total Transactions", transactionsValue, "#FEF3C7"
        ), 1, 1);

        return grid;
    }

    private VBox createCard(
        String title, Label value, String color
    ) {

        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefSize(220, 110);

        card.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 12;"
        );

        Label heading = new Label(title);
        heading.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #475569;"
        );

        value.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1E293B;"
        );

        card.getChildren().addAll(heading, value);

        return card;
    }

    // MONTHLY SALES CHART
    private BarChart<String, Number> createSalesChart() {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (₹)");

        BarChart<String, Number> chart =
            new BarChart<>(xAxis, yAxis);

        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(350);

        return chart;
    }

    // LOAD DATABASE REPORTS
    private void loadReports() {

        String summarySql =
            "SELECT " +
            "(SELECT COUNT(*) FROM products) AS total_products, " +
            "(SELECT COALESCE(SUM(quantity), 0) FROM products) AS total_stock, " +
            "(SELECT COUNT(*) FROM products WHERE quantity <= 10) AS low_stock, " +
            "(SELECT COALESCE(SUM(final_amount), 0) FROM sales) AS revenue, " +
            "(SELECT COUNT(*) FROM sales) AS transactions";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(summarySql);
            ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {

                productsValue.setText(
                    String.valueOf(rs.getInt("total_products"))
                );

                stockValue.setText(
                    String.valueOf(rs.getInt("total_stock"))
                );

                lowStockValue.setText(
                    String.valueOf(rs.getInt("low_stock"))
                );

                BigDecimal revenue =
                    rs.getBigDecimal("revenue");

                revenueValue.setText(
                    "₹" + revenue.setScale(
                        2, java.math.RoundingMode.HALF_UP
                    ).toPlainString()
                );

                transactionsValue.setText(
                    String.valueOf(rs.getInt("transactions"))
                );
            }

            loadMonthlySales(con);

        } catch (SQLException e) {

            showError(
                "Unable to Load Reports",
                e.getMessage()
            );
        }
    }

    // MONTHLY SALES DATA
    private void loadMonthlySales(Connection con)
            throws SQLException {

        salesChart.getData().clear();

        XYChart.Series<String, Number> series =
            new XYChart.Series<>();

        String sql =
            "SELECT DATE_FORMAT(sale_date, '%Y-%m') AS month, " +
            "SUM(final_amount) AS revenue " +
            "FROM sales " +
            "GROUP BY DATE_FORMAT(sale_date, '%Y-%m') " +
            "ORDER BY month";

        try (
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                String month = rs.getString("month");

                BigDecimal revenue =
                    rs.getBigDecimal("revenue");

                series.getData().add(
                    new XYChart.Data<>(
                        month,
                        revenue.doubleValue()
                    )
                );
            }
        }

        salesChart.getData().add(series);
    }

    // ERROR MESSAGE
    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}

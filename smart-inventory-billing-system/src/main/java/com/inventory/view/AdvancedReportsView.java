
package com.inventory.view;

import com.inventory.dao.DBConnection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AdvancedReportsView extends Stage {

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private Label revenueLabel;
    private Label transactionsLabel;
    private Label averageLabel;
    private Label itemsSoldLabel;

    private TableView<TopProduct> topProductsTable;
    private TableView<DailySale> dailySalesTable;

    private final ObservableList<TopProduct> topProducts =
            FXCollections.observableArrayList();

    private final ObservableList<DailySale> dailySales =
            FXCollections.observableArrayList();

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private int totalTransactions = 0;
    private int totalItemsSold = 0;

    public AdvancedReportsView() {

        setTitle("Smart Inventory - Advanced Sales Reports");

        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Advanced Sales Reports");
        heading.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
                "Analyse revenue, transactions and product performance."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        root.getChildren().addAll(
                heading,
                subtitle,
                createFilterSection(),
                createSummarySection(),
                createDailySalesSection(),
                createTopProductsSection()
        );

        loadReports();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);

        setScene(new Scene(scrollPane, 1100, 800));
        setMinWidth(900);
        setMinHeight(650);
    }

    // DATE FILTERS

    private VBox createFilterSection() {

        VBox section = createPanel();

        Label title = new Label("Report Date Range");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        startDatePicker = new DatePicker(
                LocalDate.now().withDayOfMonth(1)
        );

        endDatePicker = new DatePicker(LocalDate.now());

        Button applyButton = createButton(
                "Apply Filter", "#4338CA"
        );

        Button resetButton = createButton(
                "This Month", "#64748B"
        );

        Button exportButton = createButton(
                "Export CSV", "#16A34A"
        );

        applyButton.setOnAction(e -> applyDateFilter());

        resetButton.setOnAction(e -> {
            startDatePicker.setValue(
                    LocalDate.now().withDayOfMonth(1)
            );
            endDatePicker.setValue(LocalDate.now());
            loadReports();
        });

        exportButton.setOnAction(e -> exportCsv());

        HBox row = new HBox(
                10,
                new Label("From:"),
                startDatePicker,
                new Label("To:"),
                endDatePicker,
                applyButton,
                resetButton,
                exportButton
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);

        section.getChildren().addAll(title, row);

        return section;
    }

    // SUMMARY CARDS

    private HBox createSummarySection() {

        revenueLabel = new Label("INR 0.00");
        transactionsLabel = new Label("0");
        averageLabel = new Label("INR 0.00");
        itemsSoldLabel = new Label("0");

        HBox cards = new HBox(15);

        cards.getChildren().addAll(
                createSummaryCard("Total Revenue", revenueLabel),
                createSummaryCard("Transactions", transactionsLabel),
                createSummaryCard("Average Sale", averageLabel),
                createSummaryCard("Items Sold", itemsSoldLabel)
        );

        return cards;
    }

    private VBox createSummaryCard(String title, Label value) {

        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.CENTER_LEFT);

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;"
        );

        Label heading = new Label(title);
        heading.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-size: 13px;"
        );

        value.setStyle(
                "-fx-font-size: 21px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        card.getChildren().addAll(heading, value);

        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    // DAILY SALES TABLE AND CHART

    private VBox createDailySalesSection() {

        VBox section = createPanel();

        Label title = new Label("Daily Revenue");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (INR)");

        BarChart<String, Number> chart =
                new BarChart<>(xAxis, yAxis);

        chart.setTitle("Revenue by Date");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        chart.getData().add(series);

        dailySalesTable = new TableView<>(dailySales);
        dailySalesTable.setPrefHeight(200);

        dailySalesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<DailySale, String> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("date")
        );

        TableColumn<DailySale, String> revenueColumn =
                new TableColumn<>("Revenue");

        revenueColumn.setCellValueFactory(
                new PropertyValueFactory<>("revenue")
        );

        TableColumn<DailySale, Integer> countColumn =
                new TableColumn<>("Transactions");

        countColumn.setCellValueFactory(
                new PropertyValueFactory<>("transactions")
        );

        dailySalesTable.getColumns().addAll(
                dateColumn,
                revenueColumn,
                countColumn
        );

        dailySalesTable.setUserData(series);

        section.getChildren().addAll(
                title,
                chart,
                dailySalesTable
        );

        return section;
    }

    // TOP-SELLING PRODUCTS

    private VBox createTopProductsSection() {

        VBox section = createPanel();

        Label title = new Label("Top-Selling Products");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        topProductsTable = new TableView<>(topProducts);
        topProductsTable.setPrefHeight(250);

        topProductsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<TopProduct, String> idColumn =
                new TableColumn<>("Product ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("productId")
        );

        TableColumn<TopProduct, String> nameColumn =
                new TableColumn<>("Product Name");

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("productName")
        );

        TableColumn<TopProduct, Integer> quantityColumn =
                new TableColumn<>("Quantity Sold");

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantitySold")
        );

        TableColumn<TopProduct, String> revenueColumn =
                new TableColumn<>("Revenue");

        revenueColumn.setCellValueFactory(
                new PropertyValueFactory<>("revenue")
        );

        topProductsTable.getColumns().addAll(
                idColumn,
                nameColumn,
                quantityColumn,
                revenueColumn
        );

        section.getChildren().addAll(
                title,
                topProductsTable
        );

        return section;
    }

    // APPLY FILTER

    private void applyDateFilter() {

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showError("Select both start and end dates.");
            return;
        }

        if (start.isAfter(end)) {
            showError("Start date cannot be after end date.");
            return;
        }

        loadReports();
    }

    // LOAD ALL REPORT DATA

    private void loadReports() {

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            return;
        }

        loadSummary(start, end);
        loadDailySales(start, end);
        loadTopProducts(start, end);
    }

    // SUMMARY QUERY

    private void loadSummary(LocalDate start, LocalDate end) {

        String sql =
                "SELECT " +
                "COALESCE(SUM(final_amount), 0) AS revenue, " +
                "COUNT(*) AS transactions " +
                "FROM sales " +
                "WHERE sale_date >= ? AND sale_date < ?";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(start.atStartOfDay())
            );

            ps.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    totalRevenue = rs.getBigDecimal("revenue");
                    totalTransactions = rs.getInt("transactions");
                }
            }

            String itemSql =
                    "SELECT COALESCE(SUM(si.quantity), 0) AS items " +
                    "FROM sale_items si " +
                    "JOIN sales s ON si.sale_id = s.sale_id " +
                    "WHERE s.sale_date >= ? AND s.sale_date < ?";

            try (PreparedStatement itemPs =
                    con.prepareStatement(itemSql)) {

                itemPs.setTimestamp(
                        1,
                        java.sql.Timestamp.valueOf(start.atStartOfDay())
                );

                itemPs.setTimestamp(
                        2,
                        java.sql.Timestamp.valueOf(
                                end.plusDays(1).atStartOfDay()
                        )
                );

                try (ResultSet rs = itemPs.executeQuery()) {

                    if (rs.next()) {
                        totalItemsSold = rs.getInt("items");
                    }
                }
            }

            BigDecimal average = BigDecimal.ZERO;

            if (totalTransactions > 0) {
                average = totalRevenue.divide(
                        BigDecimal.valueOf(totalTransactions),
                        2,
                        java.math.RoundingMode.HALF_UP
                );
            }

            revenueLabel.setText(
                    "INR " + totalRevenue.setScale(
                            2,
                            java.math.RoundingMode.HALF_UP
                    )
            );

            transactionsLabel.setText(
                    String.valueOf(totalTransactions)
            );

            averageLabel.setText("INR " + average);

            itemsSoldLabel.setText(
                    String.valueOf(totalItemsSold)
            );

        } catch (SQLException e) {

            showError("Unable to load summary: " + e.getMessage());
        }
    }

    // DAILY SALES QUERY

    private void loadDailySales(LocalDate start, LocalDate end) {

        dailySales.clear();

        String sql =
                "SELECT DATE(sale_date) AS sale_day, " +
                "SUM(final_amount) AS revenue, " +
                "COUNT(*) AS transactions " +
                "FROM sales " +
                "WHERE sale_date >= ? AND sale_date < ? " +
                "GROUP BY DATE(sale_date) " +
                "ORDER BY sale_day";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(start.atStartOfDay())
            );

            ps.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    LocalDate date = rs.getDate("sale_day").toLocalDate();

                    BigDecimal revenue =
                            rs.getBigDecimal("revenue");

                    int transactions =
                            rs.getInt("transactions");

                    dailySales.add(new DailySale(
                            date.toString(),
                            revenue.toPlainString(),
                            transactions
                    ));
                }
            }

            updateDailyChart();

        } catch (SQLException e) {

            showError("Unable to load daily sales: " + e.getMessage());
        }
    }

    // UPDATE BAR CHART

    private void updateDailyChart() {

        @SuppressWarnings("unchecked")
        XYChart.Series<String, Number> series =
                (XYChart.Series<String, Number>)
                        dailySalesTable.getUserData();

        if (series == null) {
            return;
        }

        series.getData().clear();

        for (DailySale sale : dailySales) {

            series.getData().add(
                    new XYChart.Data<>(
                            sale.getDate(),
                            new BigDecimal(sale.getRevenue())
                    )
            );
        }
    }

    // TOP PRODUCTS QUERY

    private void loadTopProducts(LocalDate start, LocalDate end) {

        topProducts.clear();

        String sql =
                "SELECT p.product_id, p.name, " +
                "SUM(si.quantity) AS quantity_sold, " +
                "SUM(si.total_price) AS revenue " +
                "FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "JOIN products p ON si.product_id = p.product_id " +
                "WHERE s.sale_date >= ? AND s.sale_date < ? " +
                "GROUP BY p.product_id, p.name " +
                "ORDER BY quantity_sold DESC " +
                "LIMIT 10";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(start.atStartOfDay())
            );

            ps.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    topProducts.add(new TopProduct(
                            rs.getString("product_id"),
                            rs.getString("name"),
                            rs.getInt("quantity_sold"),
                            rs.getBigDecimal("revenue").toPlainString()
                    ));
                }
            }

        } catch (SQLException e) {

            showError("Unable to load top products: " + e.getMessage());
        }
    }

    // EXPORT CSV

    private void exportCsv() {

        FileChooser chooser = new FileChooser();

        chooser.setTitle("Save Sales Report");

        chooser.setInitialFileName(
                "Sales_Report_" + LocalDate.now() + ".csv"
        );

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "CSV Files",
                        "*.csv"
                )
        );

        File file = chooser.showSaveDialog(this);

        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {

            writer.write("ADVANCED SALES REPORT\n");
            writer.write(
                    "From," + startDatePicker.getValue() + "\n"
            );
            writer.write(
                    "To," + endDatePicker.getValue() + "\n\n"
            );

            writer.write("SUMMARY\n");
            writer.write("Total Revenue," + totalRevenue + "\n");
            writer.write("Transactions," + totalTransactions + "\n");
            writer.write("Items Sold," + totalItemsSold + "\n\n");

            writer.write("DAILY SALES\n");
            writer.write("Date,Revenue,Transactions\n");

            for (DailySale sale : dailySales) {

                writer.write(
                        sale.getDate() + "," +
                        sale.getRevenue() + "," +
                        sale.getTransactions() + "\n"
                );
            }

            writer.write("\nTOP SELLING PRODUCTS\n");
            writer.write(
                    "Product ID,Product Name,Quantity Sold,Revenue\n"
            );

            for (TopProduct product : topProducts) {

                writer.write(
                        csvValue(product.getProductId()) + "," +
                        csvValue(product.getProductName()) + "," +
                        product.getQuantitySold() + "," +
                        product.getRevenue() + "\n"
                );
            }

            showInformation(
                    "Sales report exported successfully:\n" +
                    file.getAbsolutePath()
            );

        } catch (IOException e) {

            showError("Unable to export CSV: " + e.getMessage());
        }
    }

    private String csvValue(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // COMMON PANEL

    private VBox createPanel() {

        VBox panel = new VBox(15);
        panel.setPadding(new Insets(18));

        panel.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;"
        );

        return panel;
    }

    // BUTTON DESIGN

    private Button createButton(String text, String color) {

        Button button = new Button(text);

        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 10 16;" +
                "-fx-background-radius: 7;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // ALERTS

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sales Reports");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sales Reports");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // DAILY SALE MODEL

    public static class DailySale {

        private final String date;
        private final String revenue;
        private final int transactions;

        public DailySale(
                String date,
                String revenue,
                int transactions
        ) {
            this.date = date;
            this.revenue = revenue;
            this.transactions = transactions;
        }

        public String getDate() {
            return date;
        }

        public String getRevenue() {
            return revenue;
        }

        public int getTransactions() {
            return transactions;
        }
    }

    // TOP PRODUCT MODEL

    public static class TopProduct {

        private final String productId;
        private final String productName;
        private final int quantitySold;
        private final String revenue;

        public TopProduct(
                String productId,
                String productName,
                int quantitySold,
                String revenue
        ) {
            this.productId = productId;
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantitySold() {
            return quantitySold;
        }

        public String getRevenue() {
            return revenue;
        }
    }
}

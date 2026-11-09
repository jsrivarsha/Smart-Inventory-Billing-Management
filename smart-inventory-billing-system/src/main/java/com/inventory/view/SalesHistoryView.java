package com.inventory.view;

import com.inventory.dao.DBConnection;
import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SalesHistoryView extends Stage {

    private TableView<SaleRecord> salesTable;
    private TextField searchField;

    private final ObservableList<SaleRecord> sales =
            FXCollections.observableArrayList();

    public SalesHistoryView() {

        setTitle("Sales History");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Sales History");
        heading.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
                "View, search and monitor all completed sales."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        searchField = new TextField();
        searchField.setPromptText(
                "Search by Invoice ID or Customer Name..."
        );
        searchField.setPrefWidth(320);

        Button searchButton = new Button("Search");
        styleButton(searchButton, "#4338CA");

        Button refreshButton = new Button("Refresh");
        styleButton(refreshButton, "#64748B");

        Button exportButton = new Button("Export CSV");
        styleButton(exportButton, "#0F766E");

        Button openInvoiceButton =
                new Button("Open / Print Invoice");

        styleButton(openInvoiceButton, "#16A34A");

        searchButton.setOnAction(e -> searchSales());
        searchField.setOnAction(e -> searchSales());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            loadSales();
        });

        exportButton.setOnAction(e -> exportSalesToCSV());

        openInvoiceButton.setOnAction(e -> openInvoice());

        HBox searchBar = new HBox(
                10,
                searchField,
                searchButton,
                refreshButton,
                exportButton
        );

        salesTable = new TableView<>();
        salesTable.setItems(sales);

        salesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        salesTable.setPrefHeight(450);

        TableColumn<SaleRecord, Integer> idColumn =
                new TableColumn<>("Invoice ID");

        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getSaleId()
                ).asObject()
        );

        TableColumn<SaleRecord, String> customerColumn =
                new TableColumn<>("Customer");

        customerColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCustomerName()
                )
        );

        TableColumn<SaleRecord, String> dateColumn =
                new TableColumn<>("Sale Date");

        dateColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getSaleDate()
                )
        );

        TableColumn<SaleRecord, BigDecimal> subtotalColumn =
                new TableColumn<>("Subtotal (₹)");

        subtotalColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getSubtotal()
                )
        );

        TableColumn<SaleRecord, BigDecimal> discountColumn =
                new TableColumn<>("Discount (₹)");

        discountColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getDiscount()
                )
        );

        TableColumn<SaleRecord, BigDecimal> totalColumn =
                new TableColumn<>("Final Amount (₹)");

        totalColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getFinalAmount()
                )
        );

        salesTable.getColumns().addAll(
                idColumn,
                customerColumn,
                dateColumn,
                subtotalColumn,
                discountColumn,
                totalColumn
        );

        VBox tablePanel = new VBox(15);
        tablePanel.setPadding(new Insets(20));

        tablePanel.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;"
        );

        Label tableHeading =
                new Label("Previous Transactions");

        tableHeading.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        tablePanel.getChildren().addAll(
                tableHeading,
                salesTable
        );

        root.getChildren().addAll(
                heading,
                subtitle,
                searchBar,
                openInvoiceButton,
                tablePanel
        );

        loadSales();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 1100, 700);

        setScene(scene);
        setMinWidth(850);
        setMinHeight(600);
    }

    // BUTTON DESIGN
    private void styleButton(Button button, String color) {

        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 7;" +
                "-fx-cursor: hand;"
        );
    }

    // LOAD SALES FROM MYSQL
    private void loadSales() {

        sales.clear();

        String sql =
                "SELECT sale_id, customer_name, sale_date, " +
                "subtotal, discount, final_amount " +
                "FROM sales ORDER BY sale_date DESC";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            while (result.next()) {

                SaleRecord record = new SaleRecord(
                        result.getInt("sale_id"),
                        result.getString("customer_name"),
                        result.getString("sale_date"),
                        result.getBigDecimal("subtotal"),
                        result.getBigDecimal("discount"),
                        result.getBigDecimal("final_amount")
                );

                sales.add(record);
            }

            salesTable.setItems(sales);

        } catch (SQLException e) {

            showError(
                    "Unable to Load Sales",
                    e.getMessage()
            );
        }
    }

    // SEARCH SALES BY INVOICE ID OR CUSTOMER NAME
    private void searchSales() {

        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            salesTable.setItems(sales);
            return;
        }

        ObservableList<SaleRecord> filtered =
                FXCollections.observableArrayList();

        for (SaleRecord sale : sales) {

            boolean matchesId =
                    String.valueOf(sale.getSaleId()).contains(search);

            boolean matchesCustomer =
                    sale.getCustomerName() != null &&
                    sale.getCustomerName()
                            .toLowerCase()
                            .contains(search.toLowerCase());

            if (matchesId || matchesCustomer) {
                filtered.add(sale);
            }
        }

        salesTable.setItems(filtered);

        if (filtered.isEmpty()) {
            showMessage("No matching invoices found.");
        }
    }

    // OPEN PREVIOUS INVOICE PDF
    private void openInvoice() {

        SaleRecord selected =
                salesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {

            showError(
                    "No Invoice Selected",
                    "Please select an invoice from the table."
            );

            return;
        }

        try {

            Path invoicePath = Paths.get(
                    System.getProperty("user.home"),
                    "Documents",
                    "SmartInventoryInvoices",
                    "Invoice_" + selected.getSaleId() + ".pdf"
            );

            if (!Files.exists(invoicePath)) {

                showError(
                        "Invoice Not Found",
                        "The PDF for invoice #" +
                        selected.getSaleId() +
                        " does not exist."
                );

                return;
            }

            if (!Desktop.isDesktopSupported()) {

                showError(
                        "Not Supported",
                        "Your system does not support opening PDF files."
                );

                return;
            }

            Desktop.getDesktop().open(invoicePath.toFile());

        } catch (IOException e) {

            showError(
                    "Unable to Open Invoice",
                    e.getMessage()
            );

        } catch (Exception e) {

            showError("Error", e.getMessage());
        }
    }

    // EXPORT DISPLAYED SALES TO CSV
    private void exportSalesToCSV() {

        ObservableList<SaleRecord> displayedSales =
                salesTable.getItems();

        if (displayedSales == null || displayedSales.isEmpty()) {

            showError(
                    "Export Failed",
                    "No sales records available to export."
            );

            return;
        }

        Path folder = Paths.get(
                System.getProperty("user.home"),
                "Documents",
                "SmartInventoryInvoices"
        );

        try {

            Files.createDirectories(folder);

            Path file = folder.resolve("Sales_History.csv");

            try (BufferedWriter writer =
                    Files.newBufferedWriter(file)) {

                writer.write(
                        "Invoice ID,Customer Name,Sale Date," +
                        "Subtotal,Discount,Final Amount"
                );

                writer.newLine();

                for (SaleRecord sale : displayedSales) {

                    String customer = sale.getCustomerName() == null
                            ? ""
                            : sale.getCustomerName().replace("\"", "\"\"");

                    String date = sale.getSaleDate() == null
                            ? ""
                            : sale.getSaleDate().replace("\"", "\"\"");

                    writer.write(
                            sale.getSaleId() + "," +
                            "\"" + customer + "\"," +
                            "\"" + date + "\"," +
                            sale.getSubtotal() + "," +
                            sale.getDiscount() + "," +
                            sale.getFinalAmount()
                    );

                    writer.newLine();
                }
            }

            showMessage(
                    "Sales history exported successfully!\n\n" +
                    "Location: " + file
            );

        } catch (IOException e) {

            showError(
                    "Unable to Export Sales",
                    e.getMessage()
            );
        }
    }

    // SUCCESS MESSAGE
    private void showMessage(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ERROR MESSAGE
    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // SALE RECORD MODEL
    public static class SaleRecord {

        private final int saleId;
        private final String customerName;
        private final String saleDate;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal finalAmount;

        public SaleRecord(
                int saleId,
                String customerName,
                String saleDate,
                BigDecimal subtotal,
                BigDecimal discount,
                BigDecimal finalAmount
        ) {

            this.saleId = saleId;
            this.customerName = customerName;
            this.saleDate = saleDate;
            this.subtotal = subtotal;
            this.discount = discount;
            this.finalAmount = finalAmount;
        }

        public int getSaleId() {
            return saleId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getSaleDate() {
            return saleDate;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }
    }
}
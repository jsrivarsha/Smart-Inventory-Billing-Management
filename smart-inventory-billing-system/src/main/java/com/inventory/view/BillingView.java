package com.inventory.view;

import com.inventory.dao.DBConnection;
import com.inventory.model.Product;
import com.inventory.service.InventoryService;
import com.inventory.service.BillingService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class BillingView extends Stage {

    private final InventoryService inventoryService =
            new InventoryService();

    private final BillingService billingService =
            new BillingService();

    private final ObservableList<CartItem> cart =
            FXCollections.observableArrayList();

    private ComboBox<Product> productBox;
    private ComboBox<String> customerBox;

    private TextField quantityField;
    private TextField customerField;
    private TextField discountField;

    private TableView<CartItem> cartTable;

    private Label subtotalLabel;
    private Label totalLabel;

    public BillingView() {

        setTitle("Smart Inventory - Billing");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Create New Bill");
        heading.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
                "Select a customer, add products and generate an invoice."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        root.getChildren().addAll(
                heading,
                subtitle,
                createCustomerSection(),
                createProductSection(),
                createCartSection(),
                createTotalSection()
        );

        loadCustomers();
        loadProducts();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 1050, 720);

        setScene(scene);
        setMinWidth(850);
        setMinHeight(650);
    }

    // CUSTOMER DETAILS

    private VBox createCustomerSection() {

        VBox section = createPanel();

        Label heading = new Label("Customer Details");
        heading.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold;"
        );

        customerBox = new ComboBox<>();
        customerBox.setPromptText("Select a registered customer");
        customerBox.setPrefWidth(350);

        customerBox.setOnAction(e -> {
            String selected = customerBox.getValue();

            if (selected != null) {
                customerField.setText(selected);
            }
        });

        customerField = new TextField();
        customerField.setPromptText(
                "Enter customer name for a new customer"
        );

        Label note = new Label(
                "Select a registered customer or enter a name manually."
        );
        note.setStyle("-fx-text-fill: #64748B;");

        section.getChildren().addAll(
                heading,
                new Label("Registered Customers:"),
                customerBox,
                new Label("Customer Name:"),
                customerField,
                note
        );

        return section;
    }

    // LOAD REGISTERED CUSTOMERS

    private void loadCustomers() {

        customerBox.getItems().clear();

        String sql =
                "SELECT customer_name FROM customers ORDER BY customer_name";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                customerBox.getItems().add(
                        rs.getString("customer_name")
                );
            }

        } catch (SQLException e) {

            showError(
                    "Unable to Load Customers",
                    "Check whether the customers table exists.\n\n" +
                    e.getMessage()
            );
        }
    }

    // PRODUCT SELECTION

    private VBox createProductSection() {

        VBox section = createPanel();

        Label heading = new Label("Select Products");
        heading.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold;"
        );

        productBox = new ComboBox<>();
        productBox.setPromptText("Select a product");
        productBox.setPrefWidth(350);

        productBox.setConverter(new StringConverter<Product>() {

            @Override
            public String toString(Product product) {

                if (product == null) {
                    return "";
                }

                return product.getName() + " (" +
                        product.getProductId() + ") - INR " +
                        product.getPrice() +
                        " | Stock: " + product.getQuantity();
            }

            @Override
            public Product fromString(String text) {
                return null;
            }
        });

        quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        quantityField.setPrefWidth(100);

        Button addButton = createButton(
                "Add to Cart", "#4338CA"
        );

        addButton.setOnAction(e -> addToCart());

        HBox row = new HBox(
                12, productBox, quantityField, addButton
        );

        row.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(heading, row);

        return section;
    }

    // CART TABLE

    private VBox createCartSection() {

        VBox section = createPanel();

        Label heading = new Label("Bill Items");
        heading.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold;"
        );

        cartTable = new TableView<>(cart);
        cartTable.setPrefHeight(230);

        cartTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<CartItem, String> idColumn =
                new TableColumn<>("Product ID");

        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getProductId()
                )
        );

        TableColumn<CartItem, String> nameColumn =
                new TableColumn<>("Product");

        nameColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()
                )
        );

        TableColumn<CartItem, Integer> quantityColumn =
                new TableColumn<>("Quantity");

        quantityColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getQuantity()
                ).asObject()
        );

        TableColumn<CartItem, Double> priceColumn =
                new TableColumn<>("Unit Price");

        priceColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(
                        data.getValue().getPrice()
                ).asObject()
        );

        TableColumn<CartItem, Double> totalColumn =
                new TableColumn<>("Total");

        totalColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(
                        data.getValue().getTotal()
                ).asObject()
        );

        cartTable.getColumns().addAll(
                idColumn,
                nameColumn,
                quantityColumn,
                priceColumn,
                totalColumn
        );

        Button removeButton = createButton(
                "Remove Selected Item", "#DC2626"
        );

        removeButton.setOnAction(e -> {

            CartItem selected =
                    cartTable.getSelectionModel().getSelectedItem();

            if (selected != null) {
                cart.remove(selected);
                updateTotals();
            } else {
                showError("Select an item to remove.");
            }
        });

        section.getChildren().addAll(
                heading,
                cartTable,
                removeButton
        );

        return section;
    }

    // BILL TOTALS

    private VBox createTotalSection() {

        VBox section = createPanel();

        subtotalLabel = new Label("Subtotal: INR 0.00");

        discountField = new TextField("0");
        discountField.setPromptText("Discount amount");

        totalLabel = new Label("Final Amount: INR 0.00");
        totalLabel.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4338CA;"
        );

        Button generateButton = createButton(
                "Generate Bill & PDF Invoice", "#16A34A"
        );

        discountField.textProperty().addListener(
                (observable, oldValue, newValue) -> updateTotals()
        );

        generateButton.setOnAction(e -> generateBill());

        section.getChildren().addAll(
                subtotalLabel,
                new Label("Discount (INR):"),
                discountField,
                totalLabel,
                generateButton
        );

        return section;
    }

    // COMMON PANEL DESIGN

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
                "-fx-padding: 10 18;" +
                "-fx-background-radius: 7;" +
                "-fx-font-size: 13px;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // LOAD PRODUCTS FROM MYSQL

    private void loadProducts() {

        try {

            productBox.getItems().setAll(
                    inventoryService.getAllProducts()
            );

            productBox.getItems().removeIf(
                    product -> product.getQuantity() <= 0
            );

        } catch (Exception e) {

            showError(
                    "Unable to Load Products",
                    e.getMessage()
            );
        }
    }

    // ADD PRODUCT TO CART

    private void addToCart() {

        Product selected = productBox.getValue();

        if (selected == null) {
            showError("Select a product first.");
            return;
        }

        try {

            int quantity = Integer.parseInt(
                    quantityField.getText().trim()
            );

            if (quantity <= 0) {
                showError("Quantity must be greater than zero.");
                return;
            }

            int alreadyAdded = 0;

            for (CartItem item : cart) {

                if (item.getProductId().equals(
                        selected.getProductId()
                )) {
                    alreadyAdded = item.getQuantity();
                    break;
                }
            }

            if (alreadyAdded + quantity >
                    selected.getQuantity()) {

                showError(
                        "Requested quantity exceeds available stock."
                );
                return;
            }

            boolean found = false;

            for (CartItem item : cart) {

                if (item.getProductId().equals(
                        selected.getProductId()
                )) {

                    item.setQuantity(
                            item.getQuantity() + quantity
                    );

                    found = true;
                    break;
                }
            }

            if (!found) {

                cart.add(new CartItem(
                        selected.getProductId(),
                        selected.getName(),
                        quantity,
                        selected.getPrice()
                ));
            }

            cartTable.refresh();
            quantityField.clear();

            updateTotals();

        } catch (NumberFormatException e) {

            showError("Enter a valid whole-number quantity.");
        }
    }

    // CALCULATE TOTALS

    private void updateTotals() {

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart) {

            BigDecimal itemTotal =
                    BigDecimal.valueOf(item.getPrice())
                            .multiply(
                                    BigDecimal.valueOf(item.getQuantity())
                            );

            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;

        try {

            String value = discountField.getText().trim();

            if (!value.isEmpty()) {
                discount = new BigDecimal(value);
            }

            if (discount.signum() < 0 ||
                    discount.scale() > 2) {
                discount = BigDecimal.ZERO;
            }

        } catch (NumberFormatException e) {
            discount = BigDecimal.ZERO;
        }

        BigDecimal finalAmount = subtotal.subtract(discount);

        if (finalAmount.signum() < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        subtotalLabel.setText(
                String.format("Subtotal: INR %.2f", subtotal)
        );

        totalLabel.setText(
                String.format("Final Amount: INR %.2f", finalAmount)
        );
    }

    // GENERATE BILL

    private void generateBill() {

        if (cart.isEmpty()) {
            showError("Add at least one product to the bill.");
            return;
        }

        String customer = customerField.getText().trim();

        if (customer.isEmpty()) {
            showError("Enter the customer name.");
            return;
        }

        try {

            String discountText = discountField.getText().trim();

            if (discountText.isEmpty()) {
                discountText = "0";
            }

            BigDecimal discount = new BigDecimal(discountText);

            if (discount.signum() < 0 ||
                    discount.scale() > 2) {

                showError("Enter a valid discount amount.");
                return;
            }

            BigDecimal subtotal = BigDecimal.ZERO;

            for (CartItem item : cart) {

                subtotal = subtotal.add(
                        BigDecimal.valueOf(item.getPrice())
                                .multiply(
                                        BigDecimal.valueOf(item.getQuantity())
                                )
                );
            }

            if (discount.compareTo(subtotal) > 0) {
                showError("Discount cannot exceed the subtotal.");
                return;
            }

            BigDecimal finalAmount = subtotal.subtract(discount);

            Map<String, Integer> items = new LinkedHashMap<>();

            for (CartItem item : cart) {
                items.put(
                        item.getProductId(),
                        item.getQuantity()
                );
            }

            // SAVE SALE TO MYSQL

            int saleId = billingService.createSale(
                    customer,
                    discount,
                    items
            );

            // GENERATE PDF INVOICE

            Path pdfPath = null;
            String pdfMessage;

            try {

                pdfPath = createInvoicePdf(
                        saleId,
                        customer,
                        subtotal,
                        discount,
                        finalAmount
                );

                pdfMessage = "PDF Invoice: " + pdfPath;

            } catch (IOException pdfError) {

                pdfMessage =
                        "The bill was saved, but PDF generation failed: " +
                        pdfError.getMessage();
            }

            Alert success = new Alert(
                    Alert.AlertType.INFORMATION
            );

            success.setTitle("Bill Generated");
            success.setHeaderText("Invoice #" + saleId);

            success.setContentText(
                    "Bill saved successfully!\n" +
                    "Customer: " + customer + "\n" +
                    "Final Amount: INR " + finalAmount + "\n\n" +
                    pdfMessage
            );

            success.showAndWait();

            cart.clear();
            cartTable.refresh();

            customerField.clear();
            customerBox.getSelectionModel().clearSelection();
            discountField.setText("0");

            updateTotals();
            loadProducts();

        } catch (NumberFormatException e) {

            showError("Enter a valid discount amount.");

        } catch (Exception e) {

            showError(
                    "Unable to Generate Bill",
                    e.getMessage()
            );
        }
    }

    // CREATE PDF INVOICE

    private Path createInvoicePdf(
            int saleId,
            String customer,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal finalAmount
    ) throws IOException {

        Path folder = Paths.get(
                System.getProperty("user.home"),
                "Documents",
                "SmartInventoryInvoices"
        );

        Files.createDirectories(folder);

        Path file = folder.resolve(
                "Invoice_" + saleId + ".pdf"
        );

        try (PDDocument document = new PDDocument()) {

            int itemsPerPage = 22;
            int totalPages = (int) Math.ceil(
                    (double) cart.size() / itemsPerPage
            );

            for (int pageNumber = 0;
                    pageNumber < totalPages;
                    pageNumber++) {

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream content =
                        new PDPageContentStream(document, page)) {

                    float y = 790;

                    drawText(
                            content,
                            "SMART INVENTORY AND BILLING SYSTEM",
                            50, y, 16, true
                    );

                    y -= 35;

                    drawText(
                            content,
                            "CUSTOMER INVOICE",
                            50, y, 15, true
                    );

                    y -= 25;

                    drawText(
                            content,
                            "Invoice Number: " + saleId,
                            50, y, 10, false
                    );

                    y -= 18;

                    drawText(
                            content,
                            "Date: " + LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern(
                                            "dd-MM-yyyy HH:mm:ss"
                                    )
                            ),
                            50, y, 10, false
                    );

                    y -= 18;

                    drawText(
                            content,
                            "Customer: " + customer,
                            50, y, 10, false
                    );

                    y -= 30;

                    drawText(content, "Product", 50, y, 10, true);
                    drawText(content, "Qty", 300, y, 10, true);
                    drawText(content, "Price", 360, y, 10, true);
                    drawText(content, "Total", 450, y, 10, true);

                    y -= 20;

                    int start = pageNumber * itemsPerPage;
                    int end = Math.min(
                            start + itemsPerPage,
                            cart.size()
                    );

                    for (int i = start; i < end; i++) {

                        CartItem item = cart.get(i);

                        drawText(
                                content,
                                item.getName(),
                                50, y, 9, false
                        );

                        drawText(
                                content,
                                String.valueOf(item.getQuantity()),
                                300, y, 9, false
                        );

                        drawText(
                                content,
                                String.format("%.2f", item.getPrice()),
                                360, y, 9, false
                        );

                        drawText(
                                content,
                                String.format("%.2f", item.getTotal()),
                                450, y, 9, false
                        );

                        y -= 20;
                    }

                    if (pageNumber == totalPages - 1) {

                        y -= 25;

                        drawText(
                                content,
                                "Subtotal: INR " + subtotal,
                                350, y, 10, false
                        );

                        y -= 20;

                        drawText(
                                content,
                                "Discount: INR " + discount,
                                350, y, 10, false
                        );

                        y -= 25;

                        drawText(
                                content,
                                "FINAL AMOUNT: INR " + finalAmount,
                                300, y, 13, true
                        );

                        y -= 40;

                        drawText(
                                content,
                                "Thank you for your purchase!",
                                170, y, 11, true
                        );
                    }
                }
            }

            document.save(file.toFile());
        }

        return file;
    }

    // WRITE TEXT INTO PDF

    private void drawText(
            PDPageContentStream content,
            String text,
            float x,
            float y,
            int fontSize,
            boolean bold
    ) throws IOException {

        String safeText = text.replaceAll(
                "[^\\x20-\\x7E]",
                "?"
        );

        content.beginText();

        content.setFont(
                bold ? PDType1Font.HELVETICA_BOLD
                     : PDType1Font.HELVETICA,
                fontSize
        );

        content.newLineAtOffset(x, y);
        content.showText(safeText);
        content.endText();
    }

    // ERROR MESSAGE

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("Billing Error");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null ? "Unknown error." : message
        );

        alert.showAndWait();
    }

    // CART ITEM CLASS

    private static class CartItem {

        private final String productId;
        private final String name;
        private int quantity;
        private final double price;

        public CartItem(
                String productId,
                String name,
                int quantity,
                double price
        ) {

            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getProductId() {
            return productId;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public double getTotal() {
            return price * quantity;
        }
    }
}
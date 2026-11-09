package com.inventory.view;

import com.inventory.dao.DBConnection;
import com.inventory.model.Product;
import com.inventory.service.InventoryService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class Dashboard extends BorderPane {

    private final InventoryService service = new InventoryService();

    private TableView<Product> productTable;
    private TableView<Product> lowStockTable;
    private TextField searchField;

    private Label totalProductsValue;
    private Label totalStockValue;
    private Label lowStockValue;
    private Label totalSalesValue;

    public Dashboard() {

        setLeft(createSidebar());

        VBox mainContent = new VBox(25);
        mainContent.setPadding(new Insets(30));
        mainContent.setStyle("-fx-background-color: #F8FAFC;");

        mainContent.getChildren().addAll(
                createHeader(),
                createSummaryCards(),
                createLowStockSection(),
                createProductSection()
        );

        ScrollPane scroll = new ScrollPane(mainContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F8FAFC;");

        setCenter(scroll);

        loadDashboardStatistics();
    }

    // SIDEBAR
    private VBox createSidebar() {

        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(25));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #172554;");

        Label title = new Label("Smart Inventory");
        title.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle = new Label("& Billing System");
        subtitle.setStyle(
                "-fx-text-fill: #A5B4FC;" +
                "-fx-font-size: 12px;"
        );

       String[] names = {
        "Dashboard",
        "Products",
        "Billing",
        "Sales History",
        "Stock History",
        "Reports",
        "Settings"
};
        for (int i = 0; i < names.length; i++) {

            Button button = new Button(names[i]);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefHeight(42);

            button.setStyle(
                    "-fx-background-color: " +
                    (i == 0 ? "#4338CA" : "transparent") + ";" +
                    "-fx-text-fill: " +
                    (i == 0 ? "white" : "#E0E7FF") + ";" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-alignment: CENTER_LEFT;" +
                    "-fx-padding: 10 15;" +
                    "-fx-background-radius: 8;"
            );

            if (names[i].equals("Billing")) {
                button.setOnAction(e -> new BillingView().show());
            }

            if (names[i].equals("Sales History")) {
                button.setOnAction(e -> new SalesHistoryView().show());
            }

           if (names[i].equals("Reports")) {
    button.setOnAction(e -> new AdvancedReportsView().show());
}

            if (names[i].equals("Dashboard")) {
                button.setOnAction(e -> refreshDashboard());
            }

            if (names[i].equals("Settings")) {
                button.setOnAction(e -> new SettingsView().show());
            }
            if (names[i].equals("Stock History")) {
    button.setOnAction(e ->
            new StockMovementHistoryView().show()
    );
    if (names[i].equals("Customers")) {
    button.setOnAction(e -> new CustomersView().show());
}
if (names[i].equals("Reports")) {
    button.setOnAction(e -> new AdvancedReportsView().show());
}
}

            sidebar.getChildren().add(button);
        }

        VBox branding = new VBox(5, title, subtitle);
        sidebar.getChildren().add(0, branding);

        return sidebar;
    }

    // HEADER
    private HBox createHeader() {

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox heading = new VBox(5);

        Label title = new Label("Dashboard Overview");
        title.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label welcome = new Label("Welcome back, Admin!");
        welcome.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #64748B;"
        );

        heading.getChildren().addAll(title, welcome);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton =
                createButton("Refresh Overview", "#4338CA");

        refreshButton.setOnAction(e -> refreshDashboard());

        Label admin = new Label("Admin");
        admin.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4338CA;"
        );

        header.getChildren().addAll(
                heading, spacer, refreshButton, admin
        );

        return header;
    }

    // SUMMARY CARDS
    private GridPane createSummaryCards() {

        GridPane grid = new GridPane();
        grid.setHgap(18);

        String[] titles = {
                "Total Products",
                "Total Stock Units",
                "Low Stock Products",
                "Total Sales"
        };

        String[] colors = {
                "#DBEAFE", "#DCFCE7", "#FEE2E2", "#EDE9FE"
        };

        for (int i = 0; i < titles.length; i++) {

            VBox card = new VBox(12);
            card.setPrefHeight(115);
            card.setMinWidth(150);
            card.setPadding(new Insets(18));

            card.setStyle(
                    "-fx-background-color: " + colors[i] + ";" +
                    "-fx-background-radius: 12;"
            );

            Label heading = new Label(titles[i]);
            heading.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: #475569;"
            );

            Label value = new Label("Loading...");
            value.setStyle(
                    "-fx-font-size: 24px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #1E293B;"
            );

            switch (i) {
                case 0:
                    totalProductsValue = value;
                    break;
                case 1:
                    totalStockValue = value;
                    break;
                case 2:
                    lowStockValue = value;
                    break;
                case 3:
                    totalSalesValue = value;
                    break;
            }

            card.getChildren().addAll(heading, value);
            grid.add(card, i, 0);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }

        return grid;
    }

    // LOAD DASHBOARD STATISTICS FROM MYSQL
    private void loadDashboardStatistics() {

        String sql =
                "SELECT " +
                "(SELECT COUNT(*) FROM products) AS total_products, " +
                "(SELECT COALESCE(SUM(quantity), 0) FROM products) AS total_stock, " +
                "(SELECT COUNT(*) FROM products WHERE quantity <= 10) AS low_stock, " +
                "(SELECT COALESCE(SUM(final_amount), 0) FROM sales) AS total_sales";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {

                int totalProducts = rs.getInt("total_products");
                int totalStock = rs.getInt("total_stock");
                int lowStock = rs.getInt("low_stock");
                double totalSales = rs.getDouble("total_sales");

                totalProductsValue.setText(
                        String.valueOf(totalProducts)
                );

                totalStockValue.setText(
                        String.valueOf(totalStock)
                );

                lowStockValue.setText(
                        String.valueOf(lowStock)
                );

                totalSalesValue.setText(
                        String.format("₹%,.2f", totalSales)
                );
            }

        } catch (Exception e) {

            showError(
                    "Dashboard Loading Failed",
                    e.getMessage()
            );
        }
    }

    // REFRESH ALL DASHBOARD DATA
    private void refreshDashboard() {

        loadProducts();
        loadDashboardStatistics();
        loadLowStockProducts();
    }

    // LOW STOCK ALERT SECTION
    private VBox createLowStockSection() {

        VBox section = new VBox(15);
        section.setPadding(new Insets(22));

        section.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14;"
        );

        Label heading = new Label("Low Stock Alerts");
        heading.setStyle(
                "-fx-font-size: 21px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #DC2626;"
        );

        Label description = new Label(
                "Products with 10 or fewer units remaining."
        );
        description.setStyle("-fx-text-fill: #64748B;");

        lowStockTable = new TableView<>();
        lowStockTable.setPrefHeight(180);

        lowStockTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<Product, String> idColumn =
                new TableColumn<>("Product ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("productId")
        );

        TableColumn<Product, String> nameColumn =
                new TableColumn<>("Product Name");

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        TableColumn<Product, Integer> quantityColumn =
                new TableColumn<>("Remaining Quantity");

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        lowStockTable.getColumns().addAll(
                idColumn, nameColumn, quantityColumn
        );

        lowStockTable.setPlaceholder(
                new Label("All products have sufficient stock.")
        );

        section.getChildren().addAll(
                heading, description, lowStockTable
        );

        loadLowStockProducts();

        return section;
    }

    // LOAD LOW STOCK PRODUCTS
    private void loadLowStockProducts() {

        try {

            List<Product> products = service.getAllProducts();

            lowStockTable.getItems().clear();

            for (Product product : products) {

                if (product.getQuantity() <= 10) {
                    lowStockTable.getItems().add(product);
                }
            }

        } catch (Exception e) {

            showError(
                    "Low Stock Loading Failed",
                    e.getMessage()
            );
        }
    }

    // PRODUCT INVENTORY
    private VBox createProductSection() {

        VBox section = new VBox(18);
        section.setPadding(new Insets(22));

        section.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14;"
        );

        Label heading = new Label("Product Inventory");
        heading.setStyle(
                "-fx-font-size: 21px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label description = new Label(
                "Manage and monitor your available products."
        );

        description.setStyle("-fx-text-fill: #64748B;");

        searchField = new TextField();
        searchField.setPromptText("Search by Product ID or Name...");
        searchField.setPrefWidth(270);
        searchField.setPrefHeight(38);

        Button searchButton = createButton("Search", "#4338CA");
        Button refreshButton = createButton("Refresh", "#64748B");

        Button addButton = createButton("+ Add Product", "#16A34A");
        Button editButton = createButton("Edit Product", "#4338CA");
        Button deleteButton = createButton("Delete Product", "#DC2626");
        Button restockButton = createButton("Restock Product", "#0F766E");

        HBox searchBar = new HBox(
                12, searchField, searchButton, refreshButton
        );

        searchBar.setAlignment(Pos.CENTER_LEFT);

        HBox actionBar = new HBox(
                12, addButton, editButton, deleteButton, restockButton
        );

        actionBar.setAlignment(Pos.CENTER_LEFT);

        productTable = new TableView<>();

        productTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        productTable.setPrefHeight(320);

        TableColumn<Product, String> idColumn =
                new TableColumn<>("Product ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("productId")
        );

        TableColumn<Product, String> nameColumn =
                new TableColumn<>("Product Name");

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        TableColumn<Product, String> categoryColumn =
                new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(
                new PropertyValueFactory<>("category")
        );

        TableColumn<Product, Double> priceColumn =
                new TableColumn<>("Price (₹)");

        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>("price")
        );

        TableColumn<Product, Integer> quantityColumn =
                new TableColumn<>("Quantity");

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        productTable.getColumns().addAll(
                idColumn, nameColumn, categoryColumn,
                priceColumn, quantityColumn
        );

        addButton.setOnAction(e -> addProduct());
        editButton.setOnAction(e -> editProduct());
        deleteButton.setOnAction(e -> deleteProduct());
        restockButton.setOnAction(e -> restockProduct());

        searchButton.setOnAction(e -> searchProducts());
        searchField.setOnAction(e -> searchProducts());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            refreshDashboard();
        });

        section.getChildren().addAll(
                heading, description, searchBar,
                actionBar, productTable
        );

        loadProducts();

        return section;
    }

    // BUTTON DESIGN
    private Button createButton(String text, String color) {

        Button button = new Button(text);

        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 7;" +
                "-fx-padding: 10 18;" +
                "-fx-font-size: 13px;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // LOAD PRODUCTS
    private void loadProducts() {

        try {

            List<Product> products = service.getAllProducts();
            productTable.getItems().setAll(products);

        } catch (Exception e) {

            showError("Loading Failed", e.getMessage());
        }
    }

    // SEARCH PRODUCTS
    private void searchProducts() {

        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            loadProducts();
            return;
        }

        try {

            Product product = service.searchProduct(search);

            if (product != null) {

                productTable.getItems().setAll(product);

            } else {

                List<Product> products =
                        service.searchProductsByName(search);

                productTable.getItems().setAll(products);
            }

            if (productTable.getItems().isEmpty()) {
                showMessage("No matching products found.");
            }

        } catch (Exception e) {

            showError("Search Failed", e.getMessage());
        }
    }

    // ADD PRODUCT
    private void addProduct() {

        Dialog<Product> dialog =
                createProductDialog("Add New Product", null);

        dialog.showAndWait().ifPresent(product -> {

            try {

                service.addProduct(product);
                refreshDashboard();

                showMessage("Product added successfully!");

            } catch (Exception e) {

                showError("Unable to Add Product", e.getMessage());
            }
        });
    }

    // EDIT PRODUCT
    private void editProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {

            showError("No Selection", "Select a product to edit.");
            return;
        }

        Dialog<Product> dialog =
                createProductDialog("Edit Product", selected);

        dialog.showAndWait().ifPresent(product -> {

            try {

                service.updateProduct(product);
                refreshDashboard();

                showMessage("Product updated successfully!");

            } catch (Exception e) {

                showError("Unable to Update Product", e.getMessage());
            }
        });
    }

    // DELETE PRODUCT
    private void deleteProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {

            showError("No Selection", "Select a product to delete.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Delete Product");

        confirmation.setHeaderText(
                "Delete " + selected.getName() + "?"
        );

        confirmation.setContentText(
                "Are you sure you want to delete this product?"
        );

        confirmation.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                try {

                    service.deleteProduct(selected.getProductId());
                    refreshDashboard();

                    showMessage("Product deleted successfully!");

                } catch (Exception e) {

                    showError("Unable to Delete Product", e.getMessage());
                }
            }
        });
    }

    // RESTOCK PRODUCT AND RECORD STOCK MOVEMENT
    private void restockProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("No Selection", "Select a product to restock.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Restock Product");
        dialog.setHeaderText("Restock " + selected.getName());
        dialog.setContentText(
                "Current stock: " + selected.getQuantity() +
                "\nEnter quantity to add:"
        );

        dialog.showAndWait().ifPresent(input -> {

            int addedQuantity;

            try {

                addedQuantity = Integer.parseInt(input.trim());

                if (addedQuantity <= 0) {
                    showError(
                            "Invalid Quantity",
                            "Enter a quantity greater than zero."
                    );
                    return;
                }

            } catch (NumberFormatException e) {

                showError(
                        "Invalid Input",
                        "Please enter a valid whole number."
                );
                return;
            }

            String selectSql =
                    "SELECT quantity FROM products " +
                    "WHERE product_id = ? FOR UPDATE";

            String updateSql =
                    "UPDATE products SET quantity = ? WHERE product_id = ?";

            String movementSql =
                    "INSERT INTO stock_movements " +
                    "(product_id, movement_type, quantity, " +
                    "previous_quantity, new_quantity) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                try {

                    int previousQuantity;

                    try (PreparedStatement ps =
                            con.prepareStatement(selectSql)) {

                        ps.setString(1, selected.getProductId());

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException("Product not found.");
                            }

                            previousQuantity = rs.getInt("quantity");
                        }
                    }

                    int newQuantity = Math.addExact(
                            previousQuantity,
                            addedQuantity
                    );

                    try (PreparedStatement ps =
                            con.prepareStatement(updateSql)) {

                        ps.setInt(1, newQuantity);
                        ps.setString(2, selected.getProductId());

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Stock update failed.");
                        }
                    }

                    try (PreparedStatement ps =
                            con.prepareStatement(movementSql)) {

                        ps.setString(1, selected.getProductId());
                        ps.setString(2, "RESTOCK");
                        ps.setInt(3, addedQuantity);
                        ps.setInt(4, previousQuantity);
                        ps.setInt(5, newQuantity);

                        ps.executeUpdate();
                    }

                    con.commit();

                    refreshDashboard();

                    showMessage(
                            "Stock updated successfully!\n\n" +
                            "Product: " + selected.getName() + "\n" +
                            "Added quantity: " + addedQuantity + "\n" +
                            "New stock: " + newQuantity
                    );

                } catch (Exception e) {

                    con.rollback();
                    throw e;

                } finally {
                    con.setAutoCommit(true);
                }

            } catch (ArithmeticException e) {

                showError(
                        "Quantity Too Large",
                        "The resulting stock quantity is too large."
                );

            } catch (Exception e) {

                showError(
                        "Unable to Restock Product",
                        e.getMessage()
                );
            }
        });
    }

    // ADD / EDIT PRODUCT FORM
    private Dialog<Product> createProductDialog(
            String title, Product existing
    ) {

        Dialog<Product> dialog = new Dialog<>();

        dialog.setTitle(title);
        dialog.setHeaderText(title);

        ButtonType saveButton = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton, ButtonType.CANCEL
        );

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField priceField = new TextField();
        TextField quantityField = new TextField();

        idField.setPromptText("Enter Product ID");
        nameField.setPromptText("Enter Product Name");
        categoryField.setPromptText("Enter Category");
        priceField.setPromptText("Enter Price");
        quantityField.setPromptText("Enter Quantity");

        if (existing != null) {

            idField.setText(existing.getProductId());
            idField.setDisable(true);

            nameField.setText(existing.getName());
            categoryField.setText(existing.getCategory());
            priceField.setText(String.valueOf(existing.getPrice()));
            quantityField.setText(String.valueOf(existing.getQuantity()));
        }

        GridPane form = new GridPane();

        form.setHgap(12);
        form.setVgap(15);
        form.setPadding(new Insets(20));

        form.addRow(0, new Label("Product ID:"), idField);
        form.addRow(1, new Label("Product Name:"), nameField);
        form.addRow(2, new Label("Category:"), categoryField);
        form.addRow(3, new Label("Price:"), priceField);
        form.addRow(4, new Label("Quantity:"), quantityField);

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {

            if (button == saveButton) {

                try {

                    String id = idField.getText().trim();
                    String name = nameField.getText().trim();
                    String category = categoryField.getText().trim();

                    String priceText = priceField.getText().trim();
                    String quantityText = quantityField.getText().trim();

                    if (id.isEmpty() || name.isEmpty()
                            || category.isEmpty()
                            || priceText.isEmpty()
                            || quantityText.isEmpty()) {

                        showError(
                                "Validation Error",
                                "Please fill in all fields."
                        );

                        return null;
                    }

                    double price = Double.parseDouble(priceText);
                    int quantity = Integer.parseInt(quantityText);

                    if (!Double.isFinite(price)
                            || price < 0 || quantity < 0) {

                        showError(
                                "Invalid Values",
                                "Price and quantity cannot be negative."
                        );

                        return null;
                    }

                    return new Product(
                            id, name, category, price, quantity
                    );

                } catch (NumberFormatException e) {

                    showError(
                            "Invalid Input",
                            "Enter a valid numeric price and quantity."
                    );

                    return null;
                }
            }

            return null;
        });

        return dialog;
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
}

package com.inventory.view;

import com.inventory.dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class CustomersView extends Stage {

    private final ObservableList<Customer> customers =
            FXCollections.observableArrayList();

    private TableView<Customer> table;

    private TextField nameField;
    private TextField phoneField;
    private TextField emailField;
    private TextField searchField;

    private int selectedCustomerId = -1;

    public CustomersView() {

        setTitle("Smart Inventory - Customer Management");

        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Customer Management");
        heading.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
                "Register and manage your customers."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        root.getChildren().addAll(
                heading,
                subtitle,
                createForm(),
                createCustomerTable()
        );

        loadCustomers();

        Scene scene = new Scene(root, 1000, 700);

        setScene(scene);
        setMinWidth(850);
        setMinHeight(600);
    }

    // CUSTOMER FORM

    private VBox createForm() {

        VBox section = createPanel();

        Label title = new Label("Customer Details");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        nameField = new TextField();
        nameField.setPromptText("Enter customer name");

        phoneField = new TextField();
        phoneField.setPromptText("Enter phone number");

        emailField = new TextField();
        emailField.setPromptText("Enter email address");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);

        form.add(new Label("Customer Name:"), 0, 0);
        form.add(nameField, 1, 0);

        form.add(new Label("Phone:"), 2, 0);
        form.add(phoneField, 3, 0);

        form.add(new Label("Email:"), 0, 1);
        form.add(emailField, 1, 1, 3, 1);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(110);

        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().addAll(
                labelColumn,
                inputColumn,
                labelColumn,
                inputColumn
        );

        Button addButton = createButton("Add Customer", "#16A34A");
        Button updateButton = createButton("Update", "#2563EB");
        Button deleteButton = createButton("Delete", "#DC2626");
        Button clearButton = createButton("Clear", "#64748B");

        addButton.setOnAction(e -> addCustomer());
        updateButton.setOnAction(e -> updateCustomer());
        deleteButton.setOnAction(e -> deleteCustomer());
        clearButton.setOnAction(e -> clearForm());

        HBox buttons = new HBox(
                10, addButton, updateButton, deleteButton, clearButton
        );
        buttons.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(
                title,
                form,
                buttons
        );

        return section;
    }

    // CUSTOMER TABLE

    private VBox createCustomerTable() {

        VBox section = createPanel();

        Label title = new Label("Registered Customers");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Search by name, phone or email");

        Button searchButton = createButton("Search", "#4338CA");
        Button refreshButton = createButton("Refresh", "#64748B");

        searchButton.setOnAction(e -> searchCustomers());
        refreshButton.setOnAction(e -> {
            searchField.clear();
            loadCustomers();
        });

        HBox searchRow = new HBox(
                10, searchField, searchButton, refreshButton
        );
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.setOnAction(e -> searchCustomers());

        table = new TableView<>(customers);
        table.setPrefHeight(350);
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<Customer, Integer> idColumn =
                new TableColumn<>("Customer ID");
        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("customerId")
        );

        TableColumn<Customer, String> nameColumn =
                new TableColumn<>("Name");
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("customerName")
        );

        TableColumn<Customer, String> phoneColumn =
                new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        TableColumn<Customer, String> emailColumn =
                new TableColumn<>("Email");
        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("email")
        );

        TableColumn<Customer, String> dateColumn =
                new TableColumn<>("Registered On");
        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdAt")
        );

        table.getColumns().addAll(
                idColumn,
                nameColumn,
                phoneColumn,
                emailColumn,
                dateColumn
        );

        table.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, selected) -> {

                    if (selected != null) {
                        selectedCustomerId = selected.getCustomerId();

                        nameField.setText(selected.getCustomerName());
                        phoneField.setText(selected.getPhone());
                        emailField.setText(selected.getEmail());
                    }
                });

        section.getChildren().addAll(
                title,
                searchRow,
                table
        );

        return section;
    }

    // ADD CUSTOMER

    private void addCustomer() {

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty()) {
            showError("Customer name is required.");
            return;
        }

        if (!validateDetails(phone, email)) {
            return;
        }

        String sql =
                "INSERT INTO customers " +
                "(customer_name, phone, email) VALUES (?, ?, ?)";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, name);
            ps.setString(2, phone.isEmpty() ? null : phone);
            ps.setString(3, email.isEmpty() ? null : email);

            ps.executeUpdate();

            showInformation("Customer added successfully.");

            clearForm();
            loadCustomers();

        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                showError("This phone number is already registered.");
            } else {
                showError("Unable to add customer: " + e.getMessage());
            }
        }
    }

    // UPDATE CUSTOMER

    private void updateCustomer() {

        if (selectedCustomerId == -1) {
            showError("Select a customer from the table first.");
            return;
        }

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty()) {
            showError("Customer name is required.");
            return;
        }

        if (!validateDetails(phone, email)) {
            return;
        }

        String sql =
                "UPDATE customers SET customer_name = ?, " +
                "phone = ?, email = ? WHERE customer_id = ?";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, name);
            ps.setString(2, phone.isEmpty() ? null : phone);
            ps.setString(3, email.isEmpty() ? null : email);
            ps.setInt(4, selectedCustomerId);

            ps.executeUpdate();

            showInformation("Customer updated successfully.");

            clearForm();
            loadCustomers();

        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                showError("This phone number is already registered.");
            } else {
                showError("Unable to update customer: " + e.getMessage());
            }
        }
    }

    // DELETE CUSTOMER

    private void deleteCustomer() {

        if (selectedCustomerId == -1) {
            showError("Select a customer from the table first.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Delete Customer");
        confirmation.setHeaderText("Delete selected customer?");
        confirmation.setContentText(
                "This action cannot be undone."
        );

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        String sql =
                "DELETE FROM customers WHERE customer_id = ?";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, selectedCustomerId);
            ps.executeUpdate();

            showInformation("Customer deleted successfully.");

            clearForm();
            loadCustomers();

        } catch (SQLException e) {

            showError(
                    "Unable to delete customer. " +
                    "The customer may be linked to existing records.\n\n" +
                    e.getMessage()
            );
        }
    }

    // LOAD ALL CUSTOMERS

    private void loadCustomers() {

        customers.clear();

        String sql =
                "SELECT customer_id, customer_name, phone, email, " +
                "created_at FROM customers ORDER BY customer_id DESC";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                customers.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("created_at")
                ));
            }

        } catch (SQLException e) {

            showError(
                    "Unable to load customers: " + e.getMessage()
            );
        }
    }

    // SEARCH CUSTOMERS

    private void searchCustomers() {

        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            loadCustomers();
            return;
        }

        customers.clear();

        String sql =
                "SELECT customer_id, customer_name, phone, email, " +
                "created_at FROM customers " +
                "WHERE customer_name LIKE ? " +
                "OR phone LIKE ? OR email LIKE ? " +
                "ORDER BY customer_id DESC";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            String value = "%" + search + "%";

            ps.setString(1, value);
            ps.setString(2, value);
            ps.setString(3, value);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    customers.add(new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("created_at")
                    ));
                }
            }

        } catch (SQLException e) {

            showError("Search failed: " + e.getMessage());
        }
    }

    // VALIDATE CUSTOMER DETAILS

    private boolean validateDetails(String phone, String email) {

        if (!phone.isEmpty() && !phone.matches("[0-9]{10,15}")) {
            showError("Enter a valid phone number (10 to 15 digits).");
            return false;
        }

        if (!email.isEmpty() &&
                !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError("Enter a valid email address.");
            return false;
        }

        return true;
    }

    // CLEAR FORM

    private void clearForm() {

        selectedCustomerId = -1;

        nameField.clear();
        phoneField.clear();
        emailField.clear();

        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
    }

    // PANEL DESIGN

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
                "-fx-cursor: hand;"
        );

        return button;
    }

    // ALERTS

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Customer Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Customer Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // CUSTOMER MODEL

    public static class Customer {

        private final int customerId;
        private final String customerName;
        private final String phone;
        private final String email;
        private final String createdAt;

        public Customer(
                int customerId,
                String customerName,
                String phone,
                String email,
                String createdAt
        ) {

            this.customerId = customerId;
            this.customerName = customerName;
            this.phone = phone == null ? "" : phone;
            this.email = email == null ? "" : email;
            this.createdAt = createdAt == null ? "" : createdAt;
        }

        public int getCustomerId() {
            return customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}

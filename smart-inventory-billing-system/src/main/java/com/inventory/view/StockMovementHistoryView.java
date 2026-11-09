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
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class StockMovementHistoryView extends Stage {

    private TableView<MovementRecord> movementTable;
    private TextField searchField;

    private final ObservableList<MovementRecord> movements =
            FXCollections.observableArrayList();

    public StockMovementHistoryView() {

        setTitle("Stock Movement History");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label heading = new Label("Stock Movement History");
        heading.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label(
                "Track all stock additions and deductions."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        // SEARCH FIELD
        searchField = new TextField();
        searchField.setPromptText(
                "Search by product, movement type or invoice ID..."
        );
        searchField.setPrefWidth(350);

        Button searchButton = new Button("Search");
        styleButton(searchButton, "#4338CA");

        Button refreshButton = new Button("Refresh");
        styleButton(refreshButton, "#64748B");

        searchButton.setOnAction(e -> searchMovements());
        searchField.setOnAction(e -> searchMovements());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            loadMovements();
        });

        HBox searchBar = new HBox(
                10, searchField, searchButton, refreshButton
        );
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // MOVEMENT TABLE
        movementTable = new TableView<>();
        movementTable.setItems(movements);

        movementTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        movementTable.setPrefHeight(450);

        TableColumn<MovementRecord, Integer> idColumn =
                new TableColumn<>("Movement ID");

        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getMovementId()
                ).asObject()
        );

        TableColumn<MovementRecord, String> productIdColumn =
                new TableColumn<>("Product ID");

        productIdColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getProductId()
                )
        );

        TableColumn<MovementRecord, String> nameColumn =
                new TableColumn<>("Product Name");

        nameColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getProductName()
                )
        );

        TableColumn<MovementRecord, String> typeColumn =
                new TableColumn<>("Movement Type");

        typeColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getMovementType()
                )
        );

        TableColumn<MovementRecord, Integer> quantityColumn =
                new TableColumn<>("Quantity");

        quantityColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getQuantity()
                ).asObject()
        );

        TableColumn<MovementRecord, Integer> previousColumn =
                new TableColumn<>("Previous Stock");

        previousColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getPreviousQuantity()
                ).asObject()
        );

        TableColumn<MovementRecord, Integer> newColumn =
                new TableColumn<>("Updated Stock");

        newColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getNewQuantity()
                ).asObject()
        );

        TableColumn<MovementRecord, Integer> saleColumn =
                new TableColumn<>("Invoice ID");

        saleColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getSaleId()
                )
        );

        TableColumn<MovementRecord, String> dateColumn =
                new TableColumn<>("Date and Time");

        dateColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getMovementDate()
                )
        );

        movementTable.getColumns().addAll(
                idColumn,
                productIdColumn,
                nameColumn,
                typeColumn,
                quantityColumn,
                previousColumn,
                newColumn,
                saleColumn,
                dateColumn
        );

        movementTable.setPlaceholder(
                new Label("No stock movements recorded yet.")
        );

        VBox tablePanel = new VBox(15);
        tablePanel.setPadding(new Insets(20));
        tablePanel.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;"
        );

        Label tableHeading = new Label("Movement Records");
        tableHeading.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        tablePanel.getChildren().addAll(
                tableHeading,
                movementTable
        );

        root.getChildren().addAll(
                heading,
                subtitle,
                searchBar,
                tablePanel
        );

        loadMovements();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 1250, 700);

        setScene(scene);
        setMinWidth(950);
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

    // LOAD MOVEMENT RECORDS FROM MYSQL
    private void loadMovements() {

        movements.clear();

        String sql =
                "SELECT sm.movement_id, sm.product_id, p.name, " +
                "sm.movement_type, sm.quantity, " +
                "sm.previous_quantity, sm.new_quantity, " +
                "sm.sale_id, sm.movement_date " +
                "FROM stock_movements sm " +
                "LEFT JOIN products p ON sm.product_id = p.product_id " +
                "ORDER BY sm.movement_date DESC, sm.movement_id DESC";

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                MovementRecord record = new MovementRecord(
                        rs.getInt("movement_id"),
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("movement_type"),
                        rs.getInt("quantity"),
                        rs.getInt("previous_quantity"),
                        rs.getInt("new_quantity"),
                        (Integer) rs.getObject("sale_id"),
                        String.valueOf(rs.getTimestamp("movement_date"))
                );

                movements.add(record);
            }

        } catch (SQLException e) {

            showError(
                    "Unable to Load Stock History",
                    e.getMessage()
            );
        }
    }

    // SEARCH MOVEMENT RECORDS
    private void searchMovements() {

        String search = searchField.getText()
                .trim()
                .toLowerCase();

        if (search.isEmpty()) {
            movementTable.setItems(movements);
            return;
        }

        ObservableList<MovementRecord> filtered =
                FXCollections.observableArrayList();

        for (MovementRecord record : movements) {

            boolean matches =
                    record.getProductId().toLowerCase().contains(search)
                    || record.getProductName().toLowerCase().contains(search)
                    || record.getMovementType().toLowerCase().contains(search)
                    || String.valueOf(record.getMovementId()).contains(search)
                    || (record.getSaleId() != null &&
                        String.valueOf(record.getSaleId()).contains(search));

            if (matches) {
                filtered.add(record);
            }
        }

        movementTable.setItems(filtered);

        if (filtered.isEmpty()) {
            showMessage("No matching stock movements found.");
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

    // MOVEMENT RECORD MODEL
    public static class MovementRecord {

        private final int movementId;
        private final String productId;
        private final String productName;
        private final String movementType;
        private final int quantity;
        private final int previousQuantity;
        private final int newQuantity;
        private final Integer saleId;
        private final String movementDate;

        public MovementRecord(
                int movementId,
                String productId,
                String productName,
                String movementType,
                int quantity,
                int previousQuantity,
                int newQuantity,
                Integer saleId,
                String movementDate
        ) {

            this.movementId = movementId;
            this.productId = productId;
            this.productName = productName == null
                    ? "Unknown Product" : productName;
            this.movementType = movementType;
            this.quantity = quantity;
            this.previousQuantity = previousQuantity;
            this.newQuantity = newQuantity;
            this.saleId = saleId;
            this.movementDate = movementDate;
        }

        public int getMovementId() {
            return movementId;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public String getMovementType() {
            return movementType;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getPreviousQuantity() {
            return previousQuantity;
        }

        public int getNewQuantity() {
            return newQuantity;
        }

        public Integer getSaleId() {
            return saleId;
        }

        public String getMovementDate() {
            return movementDate;
        }
    }
}
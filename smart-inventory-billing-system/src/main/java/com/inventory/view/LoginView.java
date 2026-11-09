
package com.inventory.view;

import com.inventory.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView extends Stage {

    private final Stage mainStage;
    private final AuthService authService = new AuthService();

    private TextField usernameField;
    private PasswordField passwordField;

    private Button loginButton;
    private Button createAdminButton;

    public LoginView(Stage mainStage) {

        this.mainStage = mainStage;

        setTitle("Smart Inventory - Login");

        VBox root = new VBox(18);
        root.setPadding(new Insets(35));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F1F5F9;");

        Label heading = new Label("Smart Inventory");
        heading.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label subtitle = new Label("Login to continue");
        subtitle.setStyle("-fx-text-fill: #64748B;");

        usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(300);

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(300);

        loginButton = createButton("Login", "#4338CA");
        loginButton.setMaxWidth(300);
        loginButton.setOnAction(e -> login());

        createAdminButton = createButton(
                "Create First Admin Account", "#16A34A"
        );
        createAdminButton.setMaxWidth(300);
        createAdminButton.setOnAction(e -> createFirstAdmin());

        root.getChildren().addAll(
                heading,
                subtitle,
                new Label("Username"),
                usernameField,
                new Label("Password"),
                passwordField,
                loginButton,
                createAdminButton
        );

        refreshRegistrationButton();

        setScene(new Scene(root, 450, 500));
        setResizable(false);
    }

    // LOGIN

    private void login() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Enter both username and password.");
            return;
        }

        try {

            String role = authService.login(username, password);

            if (role == null) {
                showError("Invalid username or password.");
                return;
            }

            mainStage.setTitle(
                    "Smart Inventory & Billing System - " + role
            );

            mainStage.setScene(
                    new Scene(new Dashboard(), 1200, 750)
            );

            mainStage.setMinWidth(1000);
            mainStage.setMinHeight(650);
            mainStage.show();

            close();

        } catch (Exception e) {

            showError(
                    "Login failed: " + e.getMessage()
            );
        }
    }

    // FIRST ADMIN REGISTRATION

    private void createFirstAdmin() {

        try {

            if (authService.hasUsers()) {
                showError(
                        "An account already exists. " +
                        "Ask an administrator to create additional accounts."
                );
                refreshRegistrationButton();
                return;
            }

        } catch (Exception e) {

            showError("Unable to check users: " + e.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Create First Admin");
        dialog.setHeaderText("Set up your administrator account");

        ButtonType createButton = new ButtonType(
                "Create Account",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                createButton,
                ButtonType.CANCEL
        );

        TextField username = new TextField();
        username.setPromptText("Choose username");

        PasswordField password = new PasswordField();
        password.setPromptText("Choose password");

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm password");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(20));

        form.add(new Label("Username:"), 0, 0);
        form.add(username, 1, 0);

        form.add(new Label("Password:"), 0, 1);
        form.add(password, 1, 1);

        form.add(new Label("Confirm Password:"), 0, 2);
        form.add(confirmPassword, 1, 2);

        dialog.getDialogPane().setContent(form);

        dialog.showAndWait().ifPresent(result -> {

            if (result == createButton) {

                if (username.getText().trim().isEmpty()) {
                    showError("Enter a username.");
                    return;
                }

                if (!password.getText().equals(
                        confirmPassword.getText()
                )) {
                    showError("Passwords do not match.");
                    return;
                }

                try {

                    authService.registerUser(
                            username.getText(),
                            password.getText(),
                            "ADMIN"
                    );

                    showInformation(
                            "Admin account created successfully. " +
                            "You can now log in."
                    );

                    usernameField.setText(
                            username.getText().trim()
                    );

                    refreshRegistrationButton();

                } catch (Exception e) {

                    showError(
                            "Unable to create account: " +
                            e.getMessage()
                    );
                }
            }
        });
    }

    // SHOW REGISTRATION BUTTON ONLY WHEN NO USERS EXIST

    private void refreshRegistrationButton() {

        try {

            boolean hasUsers = authService.hasUsers();

            createAdminButton.setVisible(!hasUsers);
            createAdminButton.setManaged(!hasUsers);

        } catch (Exception e) {

            createAdminButton.setVisible(false);
            createAdminButton.setManaged(false);

            showError(
                    "Unable to connect to the database: " +
                    e.getMessage()
            );
        }
    }

    // BUTTON DESIGN

    private Button createButton(String text, String color) {

        Button button = new Button(text);

        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 11 20;" +
                "-fx-background-radius: 7;" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // ALERTS

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Setup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

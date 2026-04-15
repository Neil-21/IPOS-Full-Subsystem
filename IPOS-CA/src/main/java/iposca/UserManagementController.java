package iposca;

import iposca.dao.UserDAO;
import iposca.model.User;
import iposca.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class UserManagementController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colActive;
    @FXML private TextField searchField;

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        if (!AuthService.isAdmin()) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(null);
            a.setContentText("Access denied. Admin only.");
            a.showAndWait();
            javafx.application.Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) userTable.getScene().getWindow();
                    Utils.switchScene(stage, "/loggedIn.fxml", "Dashboard");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        }
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        userTable.setItems(userList);
        loadUsers();
        setupSearch();
    }

    private void loadUsers() {
        try {
            List<User> users = userDAO.getAll();
            userList.setAll(users);
        } catch (Exception e) {
            showError("Could not load users: " + e.getMessage());
        }
    }

    private void setupSearch() {
        if (searchField == null) return;
        FilteredList<User> filtered = new FilteredList<>(userList, p -> true);
        userTable.setItems(filtered);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(u -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return u.getUsername().toLowerCase().contains(newVal.toLowerCase())
                        || u.getFullName().toLowerCase().contains(newVal.toLowerCase());
            });
        });
    }

    @FXML
    void handleAddUser() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add User Account");
        dialog.setHeaderText("Create a new IPOS-CA staff account");

        TextField usernameField = new TextField();
        TextField passwordField = new TextField();
        TextField fullNameField = new TextField();
        TextField emailField = new TextField();
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Admin", "Pharmacist", "Manager");
        roleBox.setPromptText("Select role");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Username:"), 0, 0);  grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);  grid.add(passwordField, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2); grid.add(fullNameField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);     grid.add(emailField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);      grid.add(roleBox, 1, 4);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveType) {
                if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()
                        || fullNameField.getText().isEmpty() || roleBox.getValue() == null) {
                    showError("All fields are required.");
                    return;
                }
                try {
                    User user = new User(
                            usernameField.getText().trim(),
                            passwordField.getText().trim(),
                            roleBox.getValue(),
                            fullNameField.getText().trim()
                    );
                    user.setEmail(emailField.getText().trim());
                    userDAO.insert(user);
                    loadUsers();
                } catch (Exception e) {
                    showError("Could not create user: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void handleChangeRole() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a user.");
            return;
        }
        if (selected.getUsername().equals(AuthService.getCurrentUser().getUsername())) {
            showError("You cannot change your own role.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                selected.getRole(), "Admin", "Pharmacist", "Manager");
        dialog.setTitle("Change Role");
        dialog.setHeaderText("Change role for: " + selected.getFullName());
        dialog.setContentText("New role:");

        dialog.showAndWait().ifPresent(newRole -> {
            try {
                selected.setRole(newRole);
                userDAO.update(selected);
                loadUsers();
            } catch (Exception e) {
                showError("Could not update role: " + e.getMessage());
            }
        });
    }

    @FXML
    void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a user.");
            return;
        }
        if (selected.getUsername().equals(AuthService.getCurrentUser().getUsername())) {
            showError("You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setContentText("Delete account for " + selected.getFullName() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userDAO.delete(selected.getUserID());
                    loadUsers();
                } catch (Exception e) {
                    showError("Could not delete user: " + e.getMessage());
                }
            }
        });
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML void home(MouseEvent event) throws IOException { switchTo(event, "/loggedIn.fxml", "Dashboard"); }
    @FXML void sales(MouseEvent event) throws IOException { switchTo(event, "/SalesCheckout.fxml", "Sales Checkout"); }
    @FXML void stock(MouseEvent event) throws IOException { switchTo(event, "/stock.fxml", "Stock"); }
    @FXML void orders(MouseEvent event) throws IOException { switchTo(event, "/Orders.fxml", "Orders"); }
    @FXML void customers(MouseEvent event) throws IOException { switchTo(event, "/Customers.fxml", "Account Holders"); }
    @FXML public void reports(MouseEvent event) throws IOException { switchTo(event, "/Reports.fxml", "Reports"); }
    @FXML public void users(MouseEvent event) throws IOException { switchTo(event, "/UserManagement.fxml", "User Management"); }
    @FXML public void templates(MouseEvent event) throws IOException { switchTo(event, "/Templates.fxml", "Reminder Templates"); }

    private void switchTo(MouseEvent event, String fxml, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, fxml, title);
    }
}
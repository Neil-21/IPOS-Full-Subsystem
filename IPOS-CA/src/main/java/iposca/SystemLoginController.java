package iposca;

import java.util.prefs.Preferences;
import iposca.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.io.IOException;

public class SystemLoginController {

    @FXML private PasswordField passField;
    @FXML private TextField userField;
    @FXML private CheckBox rememberMe;
    private static final String PREF_NODE = "iposca.login";
    private static final String PREF_USER = "rememberedUsername";

    @FXML
    public void initialize() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String saved = prefs.get(PREF_USER, "");
        if (!saved.isEmpty()) {
            userField.setText(saved);
            if (rememberMe != null) rememberMe.setSelected(true);
        }
    }
/*
    @FXML
    void forgotPass(MouseEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter your username and a new password");

        TextField userInput = new TextField();
        userInput.setPromptText("Username");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New password");
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm new password");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Username:"), 0, 0);         grid.add(userInput, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);     grid.add(newPass, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2); grid.add(confirmPass, 1, 2);
        dialog.getDialogPane().setContent(grid);

        ButtonType resetType = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response != resetType) return;
            String username = userInput.getText().trim();
            String pass = newPass.getText();
            String confirm = confirmPass.getText();
            if (username.isEmpty() || pass.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }
            if (!pass.equals(confirm)) {
                showError("Passwords do not match.");
                return;
            }
            if (pass.length() < 4) {
                showError("Password must be at least 4 characters.");
                return;
            }
            try {
                iposca.dao.UserDAO dao = new iposca.dao.UserDAO();
                iposca.model.User user = dao.findByUsername(username);
                if (user == null) {
                    showError("No user found with that username.");
                    return;
                }
                if (dao.updatePassword(username, pass)) {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setHeaderText(null);
                    ok.setContentText("Password updated successfully.");
                    ok.showAndWait();
                } else {
                    showError("Could not update password.");
                }
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        });
    }
*/
    @FXML
    void logIn(MouseEvent event) throws IOException {
        login();
    }

    @FXML
    void logInKey(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            login();
        }
    }

    private void login() throws IOException {

        String username = userField.getText();
        String password = passField.getText();

        //general error handling
        if (username.trim().isEmpty() || password.trim().isEmpty()) {

            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Login Error");
            alert.setHeaderText(null);
            alert.setContentText("Please check you have filled both the username and password fields, and try to log in again");

            alert.showAndWait();

        } else {

            if (AuthService.login(username, password)) {
                Preferences prefs = Preferences.userRoot().node(PREF_NODE);
                if (rememberMe != null && rememberMe.isSelected()) {
                    prefs.put(PREF_USER, username);
                } else {
                    prefs.remove(PREF_USER);
                }
                Stage stage = (Stage) passField.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/loggedin.fxml"));
                Parent root = loader.load();

                DashController controller = loader.getController();

                stage.setTitle("Dashboard");
                stage.setScene(new Scene(root));
                stage.show();
            }

            else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText(null);
                alert.setContentText("Password or Username incorrect.");

                alert.showAndWait();
            }
        }
    }
}

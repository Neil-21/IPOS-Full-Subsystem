package iposca;

import iposca.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

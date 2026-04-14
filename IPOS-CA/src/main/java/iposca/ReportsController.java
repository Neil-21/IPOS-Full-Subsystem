package iposca;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ReportsController {

    @FXML private DatePicker fromDate;
    @FXML private TextArea reportOutput;
    @FXML private ComboBox<?> reportTypeBox;
    @FXML private DatePicker toDate;

    @FXML
    void handleGenerateReport(ActionEvent event) {

    }

    @FXML
    void handlePrint(ActionEvent event) {

    }

    @FXML
    void home(MouseEvent event) throws IOException {
        switchTo(event, "/loggedIn.fxml", "Dashboard");
    }

    @FXML
    void sales(MouseEvent event) throws IOException {
        switchTo(event, "/SalesCheckout.fxml", "Sales Checkout");
    }

    @FXML
    void stock(MouseEvent event) throws IOException {
        switchTo(event, "/stock.fxml", "Stock");
    }

    @FXML
    void orders(MouseEvent event) throws IOException {
        switchTo(event, "/Orders.fxml", "Orders");
    }

    @FXML
    void customers(MouseEvent event) throws IOException {
        switchTo(event, "/Customers.fxml", "Account Holders");
    }

    @FXML public void users(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/UserManagement.fxml", "User Management");
    }

    @FXML public void templates(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Templates.fxml", "Reminder Templates");
    }

    private void switchTo(MouseEvent event, String fxml, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, fxml, title);
    }

}

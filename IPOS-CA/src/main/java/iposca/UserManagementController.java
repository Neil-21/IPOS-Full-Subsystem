package iposca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class UserManagementController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colActive;

    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        userTable.setItems(userList);
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

    @FXML public void reports(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Reports.fxml", "Reports");
    }

    private void switchTo(MouseEvent event, String fxml, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, fxml, title);
    }
    @FXML
    public void users(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/UserManagement.fxml", "User Management");
    }
    @FXML public void templates(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Templates.fxml", "Reminder Templates");
    }
}
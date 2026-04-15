package iposca;

import iposca.dao.ReminderDAO;
import iposca.model.ReminderTemplate;
import iposca.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;

public class TemplatesController {

    @FXML private TextArea firstReminderBody;
    @FXML private TextField firstReminderSubject;
    @FXML private TextArea secondReminderBody;
    @FXML private TextField secondReminderSubject;

    private final ReminderDAO reminderDAO = new ReminderDAO();

    @FXML
    public void initialize() {
        if (!AuthService.isManager() && !AuthService.isAdmin()) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(null);
            a.setContentText("Access denied. Manager or Admin only.");
            a.showAndWait();
            javafx.application.Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) firstReminderBody.getScene().getWindow();
                    Utils.switchScene(stage, "/loggedIn.fxml", "Dashboard");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        }
        loadTemplates();
    }

    private void loadTemplates() {
        try {
            ReminderTemplate first = reminderDAO.getTemplate("1st Reminder");
            if (first != null) {
                firstReminderSubject.setText(first.getSubject());
                firstReminderBody.setText(first.getBodyText());
            }
            ReminderTemplate second = reminderDAO.getTemplate("2nd Reminder");
            if (second != null) {
                secondReminderSubject.setText(second.getSubject());
                secondReminderBody.setText(second.getBodyText());
            }
        } catch (Exception e) {
            showError("Could not load templates: " + e.getMessage());
        }
    }

    @FXML
    void handleSaveTemplates(ActionEvent event) {
        try {
            ReminderTemplate first = new ReminderTemplate();
            first.setTemplateType("1st Reminder");
            first.setSubject(firstReminderSubject.getText().trim());
            first.setBodyText(firstReminderBody.getText().trim());
            first.setUpdatedBy(AuthService.getCurrentUser().getUserID());
            reminderDAO.updateTemplate(first);

            ReminderTemplate second = new ReminderTemplate();
            second.setTemplateType("2nd Reminder");
            second.setSubject(secondReminderSubject.getText().trim());
            second.setBodyText(secondReminderBody.getText().trim());
            second.setUpdatedBy(AuthService.getCurrentUser().getUserID());
            reminderDAO.updateTemplate(second);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Saved");
            success.setContentText("Templates saved successfully.");
            success.showAndWait();
        } catch (Exception e) {
            showError("Could not save templates: " + e.getMessage());
        }
    }

    @FXML
    void handlePreview(ActionEvent event) {
        String preview = firstReminderBody.getText()
                .replace("{customer_name}", "John Smith")
                .replace("{account_id}", "ACC0001")
                .replace("{amount_owed}", "150.00")
                .replace("{merchant_name}", "Cosymed Ltd")
                .replace("{payment_due_date}", LocalDate.now().plusDays(7).toString())
                .replace("{first_reminder_date}", LocalDate.now().minusDays(15).toString());

        TextArea ta = new TextArea(preview);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(450, 350);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("1st Reminder Preview");
        alert.setHeaderText("Subject: " + firstReminderSubject.getText());
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
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
    @FXML public void users(MouseEvent event) throws IOException { switchTo(event, "/UserManagement.fxml", "User Management"); }
    @FXML public void reports(MouseEvent event) throws IOException { switchTo(event, "/Reports.fxml", "Reports"); }
    @FXML public void templates(MouseEvent event) throws IOException { switchTo(event, "/Templates.fxml", "Templates"); }

    private void switchTo(MouseEvent event, String fxml, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, fxml, title);
    }
}
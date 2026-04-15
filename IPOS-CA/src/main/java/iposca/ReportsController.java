package iposca;

import iposca.dao.ReportDAO;
import iposca.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;

public class ReportsController {

    @FXML private DatePicker fromDate;
    @FXML private TextArea reportOutput;
    @FXML private ComboBox<String> reportTypeBox;
    @FXML private DatePicker toDate;

    private final ReportDAO reportDAO = new ReportDAO();

    @FXML
    public void initialize() {
        reportTypeBox.getItems().addAll(
                "Turnover Report",
                "Stock Availability Report",
                "Account Holder Debt Report"
        );
        reportTypeBox.setPromptText("Select report type");
        fromDate.setValue(LocalDate.now().minusMonths(1));
        toDate.setValue(LocalDate.now());
    }

    @FXML
    void handleGenerateReport(ActionEvent event) {
        if (reportTypeBox.getValue() == null) {
            showError("Please select a report type.");
            return;
        }
        if (fromDate.getValue() == null || toDate.getValue() == null) {
            showError("Please select a date range.");
            return;
        }
        try {
            String report = switch (reportTypeBox.getValue()) {
                case "Turnover Report" ->
                        reportDAO.getTurnoverReport(fromDate.getValue(), toDate.getValue());
                case "Stock Availability Report" ->
                        reportDAO.getStockAvailabilityReport();
                case "Account Holder Debt Report" ->
                        reportDAO.getDebtReport(fromDate.getValue(), toDate.getValue());
                default -> "Unknown report type.";
            };
            reportOutput.setText(report);
        } catch (Exception e) {
            showError("Could not generate report: " + e.getMessage());
        }
    }

    @FXML
    void handlePrint(ActionEvent event) {
        if (reportOutput.getText().isEmpty()) {
            showError("Generate a report first.");
            return;
        }
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(reportOutput.getScene().getWindow())) {
            boolean success = job.printPage(reportOutput);
            if (success) job.endJob();
        }
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
    @FXML public void templates(MouseEvent event) throws IOException { switchTo(event, "/Templates.fxml", "Reminder Templates"); }

    private void switchTo(MouseEvent event, String fxml, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, fxml, title);
    }
}
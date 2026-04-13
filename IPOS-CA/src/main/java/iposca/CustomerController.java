package iposca;

import iposca.model.AccountHolder;
import iposca.model.DiscountPlan;
import iposca.model.Reminder;
import iposca.service.AccountService;
import iposca.service.AuthService;
import iposca.service.ReminderService;
import iposca.dao.AccountHolderDAO;
import iposca.dao.DiscountPlanDAO;
import javafx.beans.property.SimpleStringProperty;
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
import java.math.BigDecimal;
import java.util.List;

public class CustomerController {

    @FXML private TextField searchField;

    @FXML private TableView<AccountHolder> customerTable;
    @FXML private TableColumn<AccountHolder, String> colName;
    @FXML private TableColumn<AccountHolder, Double> colLimit;
    @FXML private TableColumn<AccountHolder, Double> colBalance;
    @FXML private TableColumn<AccountHolder, String> colStatus;

    @FXML private TableView<AccountHolder> overdueTable;
    @FXML private TableColumn<AccountHolder, String> colOverdueName;
    @FXML private TableColumn<AccountHolder, Double> colOverdueAmount;
    @FXML private TableColumn<AccountHolder, String> colOverdueDate;
    @FXML private TableColumn<AccountHolder, String> colOverdueReminder;

    private final ObservableList<AccountHolder> customerList =
            FXCollections.observableArrayList();
    private final AccountHolderDAO accountDAO = new AccountHolderDAO();
    private final DiscountPlanDAO discountDAO = new DiscountPlanDAO();

    @FXML
    public void initialize() {
        setupCustomerTable();
        setupOverdueTable();
        loadCustomers();
        setupSearch();

        // run status updates every time this screen loads
        try {
            AccountService.updateAccountStatuses();
            loadCustomers(); // reload after status update
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCustomerTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colLimit.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getCreditLimit().doubleValue()));
        colBalance.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getCurrentBalance().doubleValue()));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("accountStatus"));

        // colour code status, Normal=green, Suspended=orange, In Default=red
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Normal"     -> setStyle(
                                "-fx-text-fill: green; -fx-font-weight: bold;");
                        case "Suspended"  -> setStyle(
                                "-fx-text-fill: orange; -fx-font-weight: bold;");
                        case "In Default" -> setStyle(
                                "-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        customerTable.setItems(customerList);
    }

    private void setupOverdueTable() {
        colOverdueName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colOverdueAmount.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getCurrentBalance().doubleValue()));
        colOverdueDate.setCellValueFactory(cellData -> {
            AccountHolder ah = cellData.getValue();
            String date = ah.getDate1stReminder() != null
                    ? ah.getDate1stReminder().toString() : "N/A";
            return new SimpleStringProperty(date);
        });
        colOverdueReminder.setCellValueFactory(cellData -> {
            AccountHolder ah = cellData.getValue();
            String status = "None";
            if ("sent".equals(ah.getStatus2ndReminder())) status = "2nd Sent";
            else if ("due".equals(ah.getStatus2ndReminder())) status = "2nd Due";
            else if ("sent".equals(ah.getStatus1stReminder())) status = "1st Sent";
            else if ("due".equals(ah.getStatus1stReminder())) status = "1st Due";
            return new SimpleStringProperty(status);
        });

        // overdue table only shows suspended or in default accounts
        FilteredList<AccountHolder> overdueData = new FilteredList<>(customerList,
                ah -> !ah.getAccountStatus().equals("Normal"));
        overdueTable.setItems(overdueData);
    }

    private void loadCustomers() {
        try {
            List<AccountHolder> accounts = accountDAO.getAll();
            customerList.setAll(accounts);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load customers: " + e.getMessage());
        }
    }

    private void setupSearch() {
        FilteredList<AccountHolder> filteredData =
                new FilteredList<>(customerList, p -> true);
        customerTable.setItems(filteredData);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(ah -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return ah.getFullName().toLowerCase()
                            .contains(newVal.toLowerCase())
                            || ah.getAccountID().toLowerCase()
                            .contains(newVal.toLowerCase());
                });
            });
        }
    }

    @FXML
    void handleAddAccount() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Account Holder");
        dialog.setHeaderText("Enter the details for the new customer account");

        TextField accountIdField = new TextField();
        accountIdField.setPromptText("e.g. ACC0003");
        TextField nameField = new TextField();
        TextField addressField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();
        TextField limitField = new TextField();

        // discount plan picker
        ComboBox<DiscountPlan> planComboBox = new ComboBox<>();
        try {
            planComboBox.getItems().setAll(discountDAO.getAll());
            planComboBox.setPromptText("Select discount plan");
            planComboBox.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(DiscountPlan p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty ? null : p.getPlanName());
                }
            });
            planComboBox.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(DiscountPlan p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty ? null : p.getPlanName());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Account ID:"), 0, 0);   grid.add(accountIdField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);    grid.add(nameField, 1, 1);
        grid.add(new Label("Address:"), 0, 2);      grid.add(addressField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);        grid.add(phoneField, 1, 3);
        grid.add(new Label("Email:"), 0, 4);        grid.add(emailField, 1, 4);
        grid.add(new Label("Credit Limit (£):"), 0, 5); grid.add(limitField, 1, 5);
        grid.add(new Label("Discount Plan:"), 0, 6); grid.add(planComboBox, 1, 6);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveType) {
                try {
                    if (accountIdField.getText().trim().isEmpty() ||
                            nameField.getText().trim().isEmpty() ||
                            limitField.getText().trim().isEmpty()) {
                        showError("Account ID, name and credit limit are required.");
                        return;
                    }
                    AccountHolder ah = new AccountHolder();
                    ah.setAccountID(accountIdField.getText().trim());
                    ah.setFullName(nameField.getText().trim());
                    ah.setAddress(addressField.getText().trim());
                    ah.setPhone(phoneField.getText().trim());
                    ah.setEmail(emailField.getText().trim());
                    ah.setCreditLimit(new BigDecimal(limitField.getText().trim()));
                    ah.setCurrentBalance(BigDecimal.ZERO);
                    ah.setAccountStatus("Normal");
                    ah.setStatus1stReminder("no_need");
                    ah.setStatus2ndReminder("no_need");
                    if (planComboBox.getValue() != null) {
                        ah.setDiscountPlanID(planComboBox.getValue().getDiscountPlanID());
                    }
                    accountDAO.insert(ah);
                    loadCustomers();
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number for the credit limit.");
                } catch (Exception e) {
                    showError("Could not save account: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void handleSetLimit() {
        AccountHolder selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSetLimitDialog();
            return;
        }
        showSetLimitDialogForAccount(selected);
    }

    private void showSetLimitDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Credit Limit");
        dialog.setHeaderText("Update a customer's credit limit");

        ComboBox<AccountHolder> combo = new ComboBox<>(customerList);
        combo.setPromptText("Select an account holder");
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(AccountHolder ah, boolean empty) {
                super.updateItem(ah, empty);
                setText(empty ? null : ah.getFullName() + " (" + ah.getAccountID() + ")");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(AccountHolder ah, boolean empty) {
                super.updateItem(ah, empty);
                setText(empty ? null : ah.getFullName() + " (" + ah.getAccountID() + ")");
            }
        });

        TextField limitField = new TextField();
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Account Holder:"), 0, 0); grid.add(combo, 1, 0);
        grid.add(new Label("New Limit (£):"), 0, 1);  grid.add(limitField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveType && combo.getValue() != null) {
                showSetLimitDialogForAccount(combo.getValue());
            }
        });
    }

    private void showSetLimitDialogForAccount(AccountHolder ah) {
        TextInputDialog dialog = new TextInputDialog(
                ah.getCreditLimit().toPlainString());
        dialog.setTitle("Set Credit Limit");
        dialog.setHeaderText("New credit limit for " + ah.getFullName());
        dialog.setContentText("Amount (£):");
        dialog.showAndWait().ifPresent(input -> {
            try {
                BigDecimal newLimit = new BigDecimal(input.trim());
                ah.setCreditLimit(newLimit);
                accountDAO.update(ah);
                customerTable.refresh();
                overdueTable.refresh();
            } catch (NumberFormatException e) {
                showError("Please enter a valid number.");
            } catch (Exception e) {
                showError("Could not update limit: " + e.getMessage());
            }
        });
    }

    // delete selected account holder
    @FXML
    void handleDeleteAccount() {
        AccountHolder selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an account holder to delete.");
            return;
        }
        if (selected.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            showError("Cannot delete account with outstanding balance of £"
                    + selected.getCurrentBalance().toPlainString()
                    + ". Balance must be cleared first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setContentText("Are you sure you want to delete the account for "
                + selected.getFullName() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    accountDAO.delete(selected.getAccountID());
                    loadCustomers();
                } catch (Exception e) {
                    showError("Could not delete account: " + e.getMessage());
                }
            }
        });
    }

    // record a payment from an account holder — reduces their balance
    @FXML
    void handleRecordPayment() {
        AccountHolder selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an account holder.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Payment");
        dialog.setHeaderText("Record payment for " + selected.getFullName()
                + "\nCurrent Balance: £" + selected.getCurrentBalance().toPlainString());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount received (£)");
        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("Cash", "Card", "Bank Transfer");
        methodBox.setPromptText("Payment method");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Amount (£):"), 0, 0);       grid.add(amountField, 1, 0);
        grid.add(new Label("Payment Method:"), 0, 1);   grid.add(methodBox, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveType) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText().trim());
                    AccountService.recordPayment(selected.getAccountID(), amount);
                    loadCustomers();
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Payment Recorded");
                    success.setContentText("Payment of £" + amount.toPlainString()
                            + " recorded for " + selected.getFullName());
                    success.showAndWait();
                } catch (NumberFormatException e) {
                    showError("Please enter a valid amount.");
                } catch (Exception e) {
                    showError("Could not record payment: " + e.getMessage());
                }
            }
        });
    }

    // generate reminders for all accounts that have one due, currently not implemented or used anywhere
    //gonna comment out the function for now as it was causing me issues
    /*@FXML
    void handleGenerateReminders() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Generate Reminders");
        confirm.setContentText(
                "This will generate reminders for all overdue accounts. Continue?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int count = ReminderService.generateReminders();
                    loadCustomers();
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Reminders Generated");
                    success.setContentText(count + " reminder(s) generated successfully.");
                    success.showAndWait();
                } catch (Exception e) {
                    showError("Could not generate reminders: " + e.getMessage());
                }
            }
        });
    }
*/

    // restore an In Default account to Normal, manager only
    @FXML
    void handleRestoreAccount() {
        if (!AuthService.isManager()) {
            showError("Only a Manager can restore an 'In Default' account.");
            return;
        }
        AccountHolder selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an account holder.");
            return;
        }
        if (!selected.getAccountStatus().equals("In Default")) {
            showError("This account is not 'In Default'. Only In Default accounts can be restored.");
            return;
        }
        if (selected.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            showError("Balance must be cleared before restoring this account.\n"
                    + "Outstanding: £" + selected.getCurrentBalance().toPlainString());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Account");
        confirm.setContentText("Restore account for " + selected.getFullName()
                + " to Normal status?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = AccountService.restoreFromDefault(
                            selected.getAccountID());
                    if (success) {
                        loadCustomers();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Account Restored");
                        alert.setContentText(selected.getFullName()
                                + "'s account has been restored to Normal.");
                        alert.showAndWait();
                    } else {
                        showError("Could not restore account.");
                    }
                } catch (Exception e) {
                    showError("Error restoring account: " + e.getMessage());
                }
            }
        });
    }

    private void deleteFromDAO(String accountId) throws Exception {
        accountDAO.delete(accountId);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML void home(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/loggedIn.fxml", "Dashboard");
    }
    @FXML void sales(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/SalesCheckout.fxml", "Sales Checkout");
    }
    @FXML void stock(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/stock.fxml", "Stock");
    }
    @FXML public void orders(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Orders.fxml", "Orders");
    }
}
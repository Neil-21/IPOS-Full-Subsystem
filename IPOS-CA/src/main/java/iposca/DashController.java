package iposca;

import iposca.dao.SAIntegrationDAO;
import iposca.model.Sale;
import iposca.model.StockItem;
import iposca.service.AccountService;
import iposca.service.AuthService;
import iposca.service.SalesService;
import iposca.service.StockService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.List;

public class DashController {

    @FXML private Label lowstockWarning;
    @FXML private Label paymentReminder;
    @FXML private Label salesTodayNo;
    @FXML private Label lowStockItemNo;
    @FXML private Label pendingOrderNo;
    @FXML private Label supplierOrderNo;
    @FXML private Label helloLabel;

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colCustomer;
    @FXML private TableColumn<Transaction, String> colItems;
    @FXML private TableColumn<Transaction, String> colPaymentType;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colTime;

    private final ObservableList<Transaction> recentTransactions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set welcome label from logged in user
        if (AuthService.getCurrentUser() != null) {
            helloLabel.setText("Welcome, " + AuthService.getCurrentUser().getFullName());
        }

        // Run account status updates on dashboard load
        try {
            AccountService.updateAccountStatuses();
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadDashboardStats();
        loadLowStockWarning();
        loadRecentTransactions();
    }

    private void loadDashboardStats() {
        try {
            //sales today count
            List<Sale> todaySales = SalesService.getSalesToday();
            salesTodayNo.setText(String.valueOf(todaySales.size()));

            //low stock count
            List<StockItem> lowStock = StockService.getLowStockItems();
            lowStockItemNo.setText(String.valueOf(lowStock.size()));

            //pending orders - orders placed with SA not yet delivered
            int pendingOrders = iposca.service.OrderService.getPendingOrderCount();
            pendingOrderNo.setText(String.valueOf(pendingOrders));

            //supplier orders this month
            int supplierOrders = iposca.service.OrderService.getThisMonthOrderCount();
            supplierOrderNo.setText(String.valueOf(supplierOrders));

        } catch (Exception e) {
            e.printStackTrace();
            salesTodayNo.setText("--");
            lowStockItemNo.setText("--");
            pendingOrderNo.setText("--");
            supplierOrderNo.setText("--");
        }

        try {
            SAIntegrationDAO saDAO = new SAIntegrationDAO();
            String saStatus = saDAO.getCosymedAccountStatus();
            double saBalance = saDAO.getCosymedBalance();

            if ("SUSPENDED".equals(saStatus) || "IN_DEFAULT".equals(saStatus)) {
                paymentReminder.setText(
                        "⚠ Your InfoPharma account is " + saStatus +
                                ". Outstanding balance: £" + String.format("%.2f", saBalance) +
                                ". No new orders can be placed until payment is received.");
                paymentReminder.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (saBalance > 0) {
                paymentReminder.setText(
                        "Outstanding balance with InfoPharma: £" +
                                String.format("%.2f", saBalance));
                paymentReminder.setStyle("-fx-text-fill: orange;");
            } else {
                paymentReminder.setText("No outstanding balance with InfoPharma.");
                paymentReminder.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            paymentReminder.setText("Could not retrieve SA account status.");
        }
    }

    private void loadLowStockWarning() {
        try {
            List<StockItem> lowStock = StockService.getLowStockItems();
            if (lowStock.isEmpty()) {
                lowstockWarning.setText("All stock levels are healthy.");
                lowstockWarning.setStyle("-fx-text-fill: green;");
            } else {
                StringBuilder sb = new StringBuilder();
                for (StockItem item : lowStock) {
                    sb.append(item.getProductName())
                            .append(" — ")
                            .append(item.getCurrentStock())
                            .append(" units remaining  ");
                }
                lowstockWarning.setText(sb.toString().trim());
                lowstockWarning.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lowstockWarning.setText("Could not load stock data.");
        }
    }

    private void loadRecentTransactions() {
        try {
            colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
            colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
            colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colPaymentType.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

            // Load real sales from DB and map to Transaction display objects
            List<Sale> sales = SalesService.getRecentSales(10);
            recentTransactions.clear();
            for (Sale sale : sales) {
                recentTransactions.add(new Transaction(
                        sale.getSaleDate().toLocalTime().toString().substring(0, 5),
                        sale.getAccountID() != null ? sale.getAccountID() : "Walk-in Customer",
                        "Sale " + sale.getSaleReference(),
                        sale.getTotalAmount().doubleValue(),
                        sale.getPaymentMethod(),
                        sale.getPaymentStatus()
                ));
            }
            transactionTable.setItems(recentTransactions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleOpenSales(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/SalesCheckout.fxml", "Sales Checkout");
    }

    @FXML
    void handleViewStock(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/stock.fxml", "Stock");
    }

    @FXML
    void sales(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/SalesCheckout.fxml", "Sales Checkout");
    }

    @FXML
    void stock(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/stock.fxml", "Stock");
    }

    @FXML
    public void orders(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Orders.fxml", "Orders");
    }

    @FXML
    public void customers(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Customers.fxml", "Account Holders");
    }
}
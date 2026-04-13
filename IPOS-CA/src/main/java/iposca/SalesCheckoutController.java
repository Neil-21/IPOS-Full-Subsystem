package iposca;

import iposca.model.SaleItem;
import iposca.model.StockItem;
import iposca.service.AuthService;
import iposca.service.SalesService;
import iposca.service.StockService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalesCheckoutController {

    @FXML private ChoiceBox<String> paymentMethodBox;
    @FXML private TableView<StockItem> productTable;
    @FXML private TableColumn<StockItem, String> colProductName;
    @FXML private TableColumn<StockItem, Double> colPrice;
    @FXML private TableColumn<StockItem, Integer> colStock;

    @FXML private TableView<StockItem> cartTable;
    @FXML private TableColumn<StockItem, String> colItem;
    @FXML private TableColumn<StockItem, Integer> colQty;
    @FXML private TableColumn<StockItem, Double> colCartPrice;

    @FXML private Label totalLabel;
    @FXML private TextField searchField;
    @FXML private TextField discountField;
    @FXML private Label validateCreditField;

    @FXML private TextField accountIDField;

    private ObservableList<StockItem> masterData = FXCollections.observableArrayList();
    private ObservableList<StockItem> cartData = FXCollections.observableArrayList();

    // this tracks quantities separately since stockitem doesn't have qty
    private java.util.Map<String, Integer> cartQuantities = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadProductsFromDatabase();
        setupSearch();

        paymentMethodBox.getItems().addAll(
                Enums.PaymentMethod.CARD.name(),
                Enums.PaymentMethod.CASH.name(),
                Enums.PaymentMethod.CREDITS.name()
        );
        paymentMethodBox.setValue("Select Payment Method");
        totalLabel.setText("Total price: £0.00");
    }

    private void setupTableColumns() {
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQty.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cartQuantities.getOrDefault(cellData.getValue().getProductID(), 1)));
        colCartPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
    }

    private void loadProductsFromDatabase() {
        try {
            masterData.clear();
            List<StockItem> items = StockService.getAllStock();
            masterData.setAll(items);
            productTable.setItems(masterData);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load products: " + e.getMessage());
        }
    }

    private void setupSearch() {
        FilteredList<StockItem> filteredData = new FilteredList<>(masterData, p -> true);
        productTable.setItems(filteredData);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(item -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return item.getProductName().toLowerCase().contains(newVal.toLowerCase());
                });
            });
        }
    }

    @FXML
    public void addToCart() {
        StockItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String pid = selected.getProductID();
        if (cartQuantities.containsKey(pid)) {
            cartQuantities.put(pid, cartQuantities.get(pid) + 1);
            cartTable.refresh();
        } else {
            cartQuantities.put(pid, 1);
            cartData.add(selected);
        }
        cartTable.setItems(cartData);
        updateTotalPrice();
    }

    @FXML
    public void removeSelectedItem() {
        StockItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String pid = selected.getProductID();
        int qty = cartQuantities.getOrDefault(pid, 1);
        if (qty > 1) {
            cartQuantities.put(pid, qty - 1);
            cartTable.refresh();
        } else {
            cartQuantities.remove(pid);
            cartData.remove(selected);
        }
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (StockItem item : cartData) {
            int qty = cartQuantities.getOrDefault(item.getProductID(), 1);
            total += item.getRetailPrice().doubleValue() * qty;
        }
        totalLabel.setText(String.format("Total price: £%.2f", total));
    }

    @FXML
    void moveToPayment(MouseEvent event) {
        String method = paymentMethodBox.getValue();
        if (method == null || method.equals("Select Payment Method")) {
            showError("Please select a payment method.");
            return;
        }
        if (cartData.isEmpty()) {
            showError("Cart is empty.");
            return;
        }

        if (method.equals(Enums.PaymentMethod.CARD.name())) {
            openCreditCardPopup();
        } else if (method.equals(Enums.PaymentMethod.CASH.name())) {
            completeSale("Cash", null, null, null, 0, 0);
        } else if (method.equals(Enums.PaymentMethod.CREDITS.name())) {
            String accountId = accountIDField != null ? accountIDField.getText().trim() : null;
            if (accountId == null || accountId.isEmpty()) {
                showError("Please enter the account holder ID for credit payment.");
                return;
            }
            completeSale("Credit", accountId, null, null, 0, 0);
        }
    }

    // called by CreditCardController after card details are confirmed
    public void completeSale(String paymentMethod, String accountId,
                             String cardType, String cardLast4,
                             int expiryMonth, int expiryYear) {
        try {
            List<SaleItem> items = new ArrayList<>();
            for (StockItem item : cartData) {
                int qty = cartQuantities.getOrDefault(item.getProductID(), 1);
                SaleItem si = new SaleItem();
                si.setProductID(item.getProductID());
                si.setQuantity(qty);
                si.setUnitPrice(item.getRetailPrice());
                si.setLineTotal(item.getRetailPrice().multiply(BigDecimal.valueOf(qty)));
                items.add(si);
            }

            String customerType = accountId != null ? "Account Holder" : "Occasional Customer";

            String ref = SalesService.recordSale(
                    customerType, accountId, items, paymentMethod,
                    cardType, null, cardLast4, expiryMonth, expiryYear
            );

            // clears cart after successful sale
            cartData.clear();
            cartQuantities.clear();
            totalLabel.setText("Total price: £0.00");

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Sale Complete");
            success.setContentText("Sale recorded successfully.\nReference: " + ref);
            success.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not complete sale: " + e.getMessage());
        }
    }

    private void openCreditCardPopup() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/CreditCardPopup.fxml"));
            javafx.scene.Parent root = loader.load();

            CreditCardController controller = loader.getController();
            controller.receiveTotal(totalLabel.getText());
            // passing reference back so card controller can call completeSale
            controller.setSalesController(this);

            Stage stage = new Stage();
            stage.setTitle("Enter Card Details");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML public void handleDiscount() {
        try {
            String input = discountField.getText();
            if (input.isEmpty()) return;
            double discountPercent = Double.parseDouble(input);
            double subtotal = 0.0;
            for (StockItem item : cartData) {
                int qty = cartQuantities.getOrDefault(item.getProductID(), 1);
                subtotal += item.getRetailPrice().doubleValue() * qty;
            }
            double discountAmount = subtotal * (discountPercent / 100.0);
            double finalTotal = subtotal - discountAmount;
            totalLabel.setText(String.format("Total price: £%.2f\n(Discount applied: %.0f%%)",
                    Math.round(finalTotal * 100.0) / 100.0, discountPercent));
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for the discount.");
        }
    }

    @FXML public void home(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/loggedIn.fxml", "Logged In");
    }
    @FXML public void stock() throws IOException {
        Stage stage = (Stage) totalLabel.getScene().getWindow();
        java.net.URL fileUrl = getClass().getClassLoader().getResource("stock.fxml");
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fileUrl);
        javafx.scene.Parent root = loader.load();
        stage.setScene(new javafx.scene.Scene(root));
        stage.setTitle("Stock"); stage.show();
    }
    @FXML public void orders(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Orders.fxml", "Orders");
    }
    @FXML public void customers(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Customers.fxml", "Account Holders");
    }
}
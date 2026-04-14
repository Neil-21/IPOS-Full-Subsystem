package iposca;

import iposca.model.StockItem;
import iposca.service.StockService;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.List;

public class StockController {

    @FXML private TextField searchField;
    @FXML private Label lowStockLabel;
    @FXML private TableView<StockItem> stockTable;
    @FXML private TableColumn<StockItem, String> colName;
    @FXML private TableColumn<StockItem, Integer> colQuantity;
    @FXML private TableColumn<StockItem, Integer> colThreshold;
    @FXML private TableColumn<StockItem, String> colStatus;

    // changed from Product to StockItem
    private ObservableList<StockItem> stockList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearch();
        loadStockFromDatabase();
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colThreshold.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        colStatus.setCellValueFactory(cellData -> {
            StockItem item = cellData.getValue();
            if (item.getCurrentStock() <= item.getReorderLevel()) {
                return new SimpleStringProperty("LOW STOCK");
            } else {
                return new SimpleStringProperty("OK");
            }
        });
    }

    private void loadStockFromDatabase() {
        try {
            stockList.clear();
            List<StockItem> items = StockService.getAllStock();
            stockList.setAll(items);
            updateLowStockWarnings();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load stock from database: " + e.getMessage());
        }
    }

    private void setupSearch() {
        FilteredList<StockItem> filteredData = new FilteredList<>(stockList, p -> true);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(item -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return item.getProductName().toLowerCase().contains(newVal.toLowerCase())
                            || item.getProductID().toLowerCase().contains(newVal.toLowerCase());
                });
            });
        }
        if (stockTable != null) stockTable.setItems(filteredData);
    }

    public void updateLowStockWarnings() {
        try {
            List<StockItem> lowStock = StockService.getLowStockItems();
            if (lowStock.isEmpty()) {
                lowStockLabel.setText("All stock levels are healthy.");
                lowStockLabel.setStyle("-fx-text-fill: green;");
            } else {
                StringBuilder sb = new StringBuilder();
                for (StockItem item : lowStock) {
                    sb.append(item.getProductName())
                            .append(" (")
                            .append(item.getCurrentStock())
                            .append(" remaining), ");
                }
                String msg = sb.toString().replaceAll(", $", "");
                lowStockLabel.setText(msg);
                lowStockLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lowStockLabel.setText("Could not check stock levels.");
        }
    }

    @FXML
    void handleSetThreshold() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Low Stock Threshold");
        dialog.setHeaderText("Choose a product and set its new low stock threshold");

        ComboBox<StockItem> productComboBox = new ComboBox<>(stockList);
        productComboBox.setPromptText("Select a product");
        productComboBox.setPrefWidth(200);
        TextField thresholdField = new TextField();
        thresholdField.setPromptText("e.g. 10");

        productComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getProductName());
            }
        });
        productComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getProductName());
            }
        });

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Product:"), 0, 0);
        grid.add(productComboBox, 1, 0);
        grid.add(new Label("New Threshold:"), 0, 1);
        grid.add(thresholdField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                StockItem selected = productComboBox.getValue();
                if (selected == null || thresholdField.getText().isEmpty()) return;
                try {
                    int newThreshold = Integer.parseInt(thresholdField.getText());
                    selected.setReorderLevel(newThreshold);
                    // save to database
                    StockService.updateStockItem(selected);
                    stockTable.refresh();
                    updateLowStockWarnings();
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number.");
                } catch (Exception e) {
                    showError("Could not save threshold: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void handleAddStock() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter the details for the new product");

        TextField nameField = new TextField();
        TextField productIdField = new TextField();
        TextField wholesaleField = new TextField();
        TextField retailField = new TextField();
        TextField stockField = new TextField();
        TextField thresholdField = new TextField();

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Product ID:"), 0, 0);    grid.add(productIdField, 1, 0);
        grid.add(new Label("Product Name:"), 0, 1);  grid.add(nameField, 1, 1);
        grid.add(new Label("Wholesale Cost (£):"), 0, 2); grid.add(wholesaleField, 1, 2);
        grid.add(new Label("Retail Price (£):"), 0, 3);   grid.add(retailField, 1, 3);
        grid.add(new Label("Initial Stock:"), 0, 4);  grid.add(stockField, 1, 4);
        grid.add(new Label("Low Stock Threshold:"), 0, 5); grid.add(thresholdField, 1, 5);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                try {
                    StockItem newItem = new StockItem();
                    newItem.setProductID(productIdField.getText().trim());
                    newItem.setProductName(nameField.getText().trim());
                    newItem.setWholesaleCost(new BigDecimal(wholesaleField.getText().trim()));
                    newItem.setRetailPrice(new BigDecimal(retailField.getText().trim()));
                    newItem.setCurrentStock(Integer.parseInt(stockField.getText().trim()));
                    newItem.setReorderLevel(Integer.parseInt(thresholdField.getText().trim()));
                    newItem.setActive(true);

                    StockService.addNewStockItem(newItem);
                    stockList.add(newItem);
                    updateLowStockWarnings();
                } catch (NumberFormatException e) {
                    showError("Please ensure all numeric fields are valid.");
                } catch (Exception e) {
                    showError("Could not save product: " + e.getMessage());
                }
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML public void home(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/loggedIn.fxml", "Logged In");
    }
    @FXML public void sales(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/SalesCheckout.fxml", "SalesCheckout");
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
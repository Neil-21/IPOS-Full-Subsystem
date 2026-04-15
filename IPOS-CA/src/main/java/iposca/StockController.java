package iposca;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.stage.FileChooser;
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

import java.awt.event.ActionEvent;
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
    void handleExportStockReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Stock Report");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("stock_report_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".pdf");

        Stage stage = (Stage) stockTable.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        Document document = new Document();
        try (FileOutputStream out = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Stock Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph date = new Paragraph("Generated: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    smallFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(15f);
            document.add(date);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 3f, 1.2f, 1.5f, 1.2f});
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            for (String h : new String[]{"Product ID", "Product Name", "Quantity", "Low Threshold", "Status"}) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
                cell.setBackgroundColor(new Color(51, 102, 153));
                cell.setPadding(6f);
                table.addCell(cell);
            }

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (StockItem item : stockList) {
                String status = item.getCurrentStock() <= item.getReorderLevel() ? "LOW STOCK" : "OK";
                table.addCell(new PdfPCell(new Paragraph(item.getProductID(), bodyFont)));
                table.addCell(new PdfPCell(new Paragraph(item.getProductName(), bodyFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(item.getCurrentStock()), bodyFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(item.getReorderLevel()), bodyFont)));
                PdfPCell statusCell = new PdfPCell(new Paragraph(status, bodyFont));
                if ("LOW STOCK".equals(status)) statusCell.setBackgroundColor(new Color(255, 220, 220));
                table.addCell(statusCell);
            }
            document.add(table);
            document.close();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setHeaderText(null);
            info.setContentText("Stock report saved:\n" + file.getAbsolutePath());
            info.showAndWait();
        } catch (Exception e) {
            if (document.isOpen()) document.close();
            showError("Could not generate PDF: " + e.getMessage());
        }
    }

    @FXML
    void handleShowLowStock() {
        try {
            List<StockItem> lowStock = StockService.getLowStockItems();
            if (lowStock.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setContentText("All stock levels are healthy.");
                a.showAndWait();
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Items below reorder level:\n\n");
            for (StockItem item : lowStock) {
                sb.append(String.format("%-30s Current: %-5d Threshold: %d%n",
                        item.getProductName(),
                        item.getCurrentStock(),
                        item.getReorderLevel()));
            }
            TextArea ta = new TextArea(sb.toString());
            ta.setEditable(false);
            ta.setPrefSize(450, 300);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Low Stock Report");
            alert.setHeaderText("The following items are below their reorder level:");
            alert.getDialogPane().setContent(ta);
            alert.showAndWait();
        } catch (Exception e) {
            showError("Could not generate low stock list: " + e.getMessage());
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

    @FXML
    void handleModifyStock() {
        StockItem selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a stock item to modify.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modify Stock Quantity");
        dialog.setHeaderText("Modify quantity for: " + selected.getProductName());

        TextField quantityField = new TextField(String.valueOf(selected.getCurrentStock()));

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Current Stock:"), 0, 0);
        grid.add(new Label(String.valueOf(selected.getCurrentStock())), 1, 0);
        grid.add(new Label("New Quantity:"), 0, 1);
        grid.add(quantityField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveType) {
                try {
                    int newQty = Integer.parseInt(quantityField.getText().trim());
                    if (newQty < 0) {
                        showError("Quantity cannot be negative.");
                        return;
                    }
                    selected.setCurrentStock(newQty);
                    StockService.updateStockQuantity(selected.getProductID(), newQty);
                    stockTable.refresh();
                    updateLowStockWarnings();
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number.");
                } catch (Exception e) {
                    showError("Could not update stock: " + e.getMessage());
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

    @FXML public void users(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/UserManagement.fxml", "User Management");
    }

    @FXML public void reports(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Reports.fxml", "Reports");
    }
    @FXML public void templates(MouseEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Utils.switchScene(stage, "/Templates.fxml", "Reminder Templates");
    }
}
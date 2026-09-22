package com.kainanresto.controllers;

import com.kainanresto.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for inventory-view.fxml.
 *
 * Holds only the wiring the layout needs: column bindings, status-chip
 * rendering and the current date. Data loading is left to the server/database
 * layer — {@link #setProducts(ObservableList)} is the entry point.
 */
public class InventoryController {

    /* ----- Sidebar ----- */
    @FXML private VBox sidebarRoot;
    @FXML private Label restaurantNameLabel;
    @FXML private Label portalTypeLabel;
    @FXML private Button navDashboardBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button logoutBtn;

    /* ----- Header ----- */
    @FXML private Label currentDateLabel;

    /* ----- Card / filters ----- */
    @FXML private VBox inventoryCard;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private TextField itemNameSearchField;
    @FXML private Button deleteBtn;

    /* ----- Table ----- */
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> itemNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> uomColumn;
    @FXML private TableColumn<Product, String> qtyOnHandColumn;
    @FXML private TableColumn<Product, String> parLevelColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    private final ObservableList<Product> inventoryItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        currentDateLabel.setText(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));

        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        uomColumn.setCellValueFactory(new PropertyValueFactory<>("uom"));
        qtyOnHandColumn.setCellValueFactory(new PropertyValueFactory<>("qtyOnHand"));
        parLevelColumn.setCellValueFactory(new PropertyValueFactory<>("parLevel"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        applyTextStyle(skuColumn, "cell-medium");
        applyTextStyle(itemNameColumn, "cell-strong");
        applyTextStyle(qtyOnHandColumn, "cell-strong");
        statusColumn.setCellFactory(column -> new StatusChipCell());

        inventoryTable.setItems(inventoryItems);
    }

    /** Replace the visible rows with server/database supplied data. */
    public void setProducts(ObservableList<Product> items) {
        inventoryItems.setAll(items);
    }

    /** Populate the category filter with server/database supplied values. */
    public void setCategoryOptions(ObservableList<String> categories) {
        categoryFilterCombo.setItems(categories);
    }

    /** Applies a text style class to every cell of a plain text column. */
    private void applyTextStyle(TableColumn<Product, String> column, String styleClass) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().remove(styleClass);
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        });
    }

    /** Renders the status column as a coloured dot plus label. */
    private static final class StatusChipCell extends TableCell<Product, String> {

        private final Label dot = new Label("\u25CF");
        private final Label text = new Label();
        private final HBox chip = new HBox(6.0, dot, text);

        private StatusChipCell() {
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("status-chip");
            dot.getStyleClass().add("status-dot");
            text.getStyleClass().add("status-text");
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setText(null);
            if (empty || value == null || value.isBlank()) {
                setGraphic(null);
                return;
            }
            text.setText(value);
            chip.getStyleClass().removeAll("status-in-stock", "status-low-stock", "status-out-of-stock");
            chip.getStyleClass().add(styleClassFor(value));
            setGraphic(chip);
        }

        private String styleClassFor(String value) {
            String normalized = value.trim().toLowerCase(Locale.ENGLISH);
            if (normalized.startsWith("out")) {
                return "status-out-of-stock";
            }
            if (normalized.startsWith("low")) {
                return "status-low-stock";
            }
            return "status-in-stock";
        }
    }

    /* ----- Action stubs: wire to navigation / data logic elsewhere ----- */

    @FXML private void onNavDashboard() { /* TODO: navigation */ }

    @FXML private void onNavInventory() { /* TODO: navigation */ }

    @FXML private void onNavMenu() { /* TODO: navigation */ }

    @FXML private void onNavSalesOrders() { /* TODO: navigation */ }

    @FXML private void onNavAccountManagement() { /* TODO: navigation */ }

    @FXML private void onNavSettings() { /* TODO: navigation */ }

    @FXML private void onLogout() { /* TODO: session handling */ }

    @FXML private void onDelete() { /* TODO: delete selected inventory record */ }
}
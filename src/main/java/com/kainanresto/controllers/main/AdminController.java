package com.kainanresto.controllers.main;

import com.kainanresto.model.Product;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AdminController {

    /* ============================== SIDEBAR ============================== */
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

    /* ============================== SHARED TOPBAR ============================== */
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;

    /* ============================== VIEW SWITCHING ============================== */
    @FXML private BorderPane appRoot;
    @FXML private StackPane viewStack;
    @FXML private ScrollPane dashboardView;
    @FXML private VBox inventoryView;
    private static final String NAV_ACTIVE = "nav-item-active";

    /* ============================== DASHBOARD: STAT CARDS ============================== */
    @FXML private HBox statCardsRow;
    @FXML private Label todaysSalesValueLabel;
    @FXML private Label todaysSalesDeltaLabel;
    @FXML private Label transactionsValueLabel;
    @FXML private Label transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel;
    @FXML private Label completedOrdersSubLabel;
    @FXML private Label totalMenuItemsValueLabel;
    @FXML private Label totalMenuItemsSubLabel;

    /* ============================== DASHBOARD: ANALYTICS ============================== */
    @FXML private Button rangeTodayBtn;
    @FXML private Button rangeWeekBtn;
    @FXML private Button rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel;
    @FXML private Label weeklyOrdersCountLabel;
    @FXML private Label avgOrderValueLabel;

    /* ============================== DASHBOARD: BEST SELLERS / ALERTS ============================== */
    @FXML private VBox bestSellingItemsContainer;
    @FXML private VBox lowStockAlertsContainer;
    @FXML private VBox outOfStockAlertsContainer;

    /* ============================== INVENTORY: CARD / FILTERS ============================== */
    @FXML private VBox inventoryCard;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private TextField itemNameSearchField;
    @FXML private Button deleteBtn;

    /* ============================== INVENTORY: TABLE ============================== */
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> itemNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> uomColumn;
    @FXML private TableColumn<Product, String> qtyOnHandColumn;
    @FXML private TableColumn<Product, String> parLevelColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    private final ObservableList<Product> inventoryItems = FXCollections.observableArrayList();

    /* ============================== LIFECYCLE ============================== */

    @FXML
    public void initialize() {
        currentDateLabel.setText(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));

        setupInventoryTable();
        showDashboard();

        Platform.runLater(() -> {
            Stage stage = (Stage) appRoot.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent closeEvent) -> {
                closeEvent.consume();
                SessionManager.clearSession();
                NavigationUtil.switchScene(
                        closeEvent,
                        "/com/kainanresto/views/id/LoginView.fxml",
                        "Kainan Ni Juan POS - Login"
                );
            });
        });
    }

    private void setupInventoryTable() {
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

    /* ============================== DATA ENTRY POINTS ============================== */

    /** Replace the visible inventory rows with server/database supplied data. */
    public void setProducts(ObservableList<Product> items) {
        inventoryItems.setAll(items);
    }

    /** Populate the category filter with server/database supplied values. */
    public void setCategoryOptions(ObservableList<String> categories) {
        categoryFilterCombo.setItems(categories);
    }

    /* ============================== NAVIGATION ============================== */

    @FXML
    private void onNavDashboard() {
        showDashboard();
    }

    @FXML
    private void onNavInventory() {
        showInventory();
    }

    @FXML
    private void onNavMenu() {
        // TODO: navigate to Menu view.
    }

    @FXML
    private void onNavSalesOrders() {
        // TODO: navigate to Sales & Orders view.
    }

    @FXML
    private void onNavAccountManagement() {
        // TODO: navigate to Account Management view.
    }

    @FXML
    private void onNavSettings() {
        // TODO: navigate to Settings view.
    }

    @FXML
    private void onLogout(ActionEvent event) {
        SessionManager.clearSession();
        NavigationUtil.switchScene(
                event,
                "/com/kainanresto/views/id/LoginView.fxml",
                "Kainan Ni Juan - Login"
        );
    }

    private void showDashboard() {
        dashboardView.setVisible(true);
        dashboardView.setManaged(true);
        inventoryView.setVisible(false);
        inventoryView.setManaged(false);

        pageTitleLabel.setText("Dashboard");
        pageSubtitleLabel.setText("Overview of restaurant health and inventory diagnostics");

        setActiveNav(navDashboardBtn);
    }

    private void showInventory() {
        inventoryView.setVisible(true);
        inventoryView.setManaged(true);
        dashboardView.setVisible(false);
        dashboardView.setManaged(false);

        pageTitleLabel.setText("Inventory");
        pageSubtitleLabel.setText("Monitor stock levels and manage your ingredients");

        setActiveNav(navInventoryBtn);
    }

    /** Clears nav-item-active from every sidebar button and applies it to the given one. */
    private void setActiveNav(Button active) {
        for (Button btn : new Button[] {
                navDashboardBtn, navInventoryBtn, navMenuBtn,
                navSalesOrdersBtn, navAccountManagementBtn, navSettingsBtn
        }) {
            btn.getStyleClass().remove(NAV_ACTIVE);
        }
        if (!active.getStyleClass().contains(NAV_ACTIVE)) {
            active.getStyleClass().add(NAV_ACTIVE);
        }
    }

    /* ============================== DASHBOARD: RANGE TOGGLE ============================== */

    @FXML
    private void onRangeToday() {
        // TODO: reload chart + summary data for "Today".
    }

    @FXML
    private void onRangeWeek() {
        // TODO: reload chart + summary data for "This Week".
    }

    @FXML
    private void onRangeMonth() {
        // TODO: reload chart + summary data for "This Month".
    }

    /* ============================== INVENTORY: ACTIONS ============================== */

    @FXML
    private void onDelete() {
        // TODO: delete selected inventory record.
    }

    /* ============================== CELL RENDERING HELPERS ============================== */

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
}
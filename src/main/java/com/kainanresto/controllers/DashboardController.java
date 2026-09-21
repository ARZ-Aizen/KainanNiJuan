package com.kainanresto.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for Dashboard.fxml.
 *
 * This class only wires up the fx:id fields declared in the FXML and stubs
 * out the event handlers referenced there. It intentionally contains NO
 * database/server logic or sample-data generation — data population is left
 * for the service/data layer to call into (e.g. via public setter methods
 * you can add here later, such as setTodaysSales(...), setBestSellingItems(...), etc.).
 */
public class DashboardController {

    // ---------- Sidebar ----------
    @FXML private VBox sidebarRoot;
    @FXML private Label restaurantNameLabel;
    @FXML private Label portalTypeLabel;

    @FXML private Button navDashboardBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navTeamManagementBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button logoutBtn;

    // ---------- Header ----------
    @FXML private Label currentDateLabel;

    // ---------- Stat cards ----------
    @FXML private HBox statCardsRow;
    @FXML private Label todaysSalesValueLabel;
    @FXML private Label todaysSalesDeltaLabel;
    @FXML private Label transactionsValueLabel;
    @FXML private Label transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel;
    @FXML private Label completedOrdersSubLabel;
    @FXML private Label totalMenuItemsValueLabel;
    @FXML private Label totalMenuItemsSubLabel;

    // ---------- Weekly analytics ----------
    @FXML private Button rangeTodayBtn;
    @FXML private Button rangeWeekBtn;
    @FXML private Button rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel;
    @FXML private Label weeklyOrdersCountLabel;
    @FXML private Label avgOrderValueLabel;

    // ---------- Best-selling items ----------
    @FXML private VBox bestSellingItemsContainer;

    // ---------- Inventory alerts ----------
    @FXML private VBox lowStockAlertsContainer;
    @FXML private VBox outOfStockAlertsContainer;

    @FXML
    private void initialize() {
        // Populate currentDateLabel, stat values, chart series, and list containers
        // here once real data is available (e.g. from a service/repository call).
    }

    // ---------- Navigation handlers (stubs) ----------

    @FXML
    private void onNavDashboard() {
        // Already on Dashboard — no-op or refresh.
    }

    @FXML
    private void onNavInventory() {
        // TODO: navigate to Inventory view.
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
    private void onNavTeamManagement() {
        // TODO: navigate to Team Management view.
    }

    @FXML
    private void onNavSettings() {
        // TODO: navigate to Settings view.
    }

    @FXML
    private void onLogout() {
        // TODO: perform logout / return to login screen.
    }

    // ---------- Weekly range toggle handlers (stubs) ----------

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
}
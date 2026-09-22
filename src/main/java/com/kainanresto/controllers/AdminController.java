package com.kainanresto.controllers;

import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AdminController {

    //SIDEBAR
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

    //HEADER
    @FXML private Label currentDateLabel;

    //STAT CARD
    @FXML private HBox statCardsRow;
    @FXML private Label todaysSalesValueLabel;
    @FXML private Label todaysSalesDeltaLabel;
    @FXML private Label transactionsValueLabel;
    @FXML private Label transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel;
    @FXML private Label completedOrdersSubLabel;
    @FXML private Label totalMenuItemsValueLabel;
    @FXML private Label totalMenuItemsSubLabel;

    //ANALYTICS
    @FXML private Button rangeTodayBtn;
    @FXML private Button rangeWeekBtn;
    @FXML private Button rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel;
    @FXML private Label weeklyOrdersCountLabel;
    @FXML private Label avgOrderValueLabel;

    //BEST SELL
    @FXML private VBox bestSellingItemsContainer;

    //INV ALERTS
    @FXML private VBox lowStockAlertsContainer;
    @FXML private VBox outOfStockAlertsContainer;

    @FXML private void onNavDashboard() {
        //
    }

    @FXML private void onNavInventory() {
        // TODO: navigate to Inventory view.
    }

    @FXML private void onNavMenu() {
        // TODO: navigate to Menu view.
    }

    @FXML private void onNavSalesOrders() {
        // TODO: navigate to Sales & Orders view.
    }

    @FXML private void onNavTeamManagement() {
        // TODO: navigate to Team Management view.
    }

    @FXML private void onNavSettings() {
        // TODO: navigate to Settings view.
    }

    @FXML private void onLogout() {
        // TODO: perform logout / return to login screen.
    }

    @FXML private void onRangeToday() {
        // TODO: reload chart + summary data for "Today".
    }

    @FXML private void onRangeWeek() {
        // TODO: reload chart + summary data for "This Week".
    }

    @FXML private void onRangeMonth() {
        // TODO: reload chart + summary data for "This Month".
    }

    @FXML private AnchorPane rootPane;

    @FXML public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent closeEvent) -> {
                closeEvent.consume();
                SessionManager.clearSession();
                NavigationUtil.switchScene(
                        closeEvent,
                        "/com/kainanresto/ui/LoginView.fxml",
                        "Kainan Ni Juan POS - Login"
                );
            });
        });
    }

    @FXML private void handleLogout(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/ui/LoginView.fxml", "Kainan Ni Juan - Login");
    }
}
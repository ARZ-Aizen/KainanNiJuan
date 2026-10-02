package com.kainanresto.controllers.main.admin;

import com.kainanresto.model.account.User;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainAdminController {

    @FXML private BorderPane appRoot;
    @FXML private ImageView restaurantLogoImage;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private StackPane contentArea;

    /* NAV BUTTONS */
    @FXML private Button navDashboardBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private Button navSettingsBtn;

    /* ICONS */
    @FXML private SVGPath dashboardIcon, menuIcon, salesOrdersIcon, accountManagementIcon, settingsIcon, logoutIcon, calendarIcon;

    // View Cache: prevents FXML from reloading every time you click a tab (saves memory and state)
    private final Map<String, Node> viewCache = new HashMap<>();
    private static final String NAV_ACTIVE = "nav-item-active";

    @FXML
    public void initialize() {
        if (currentDateLabel != null) {
            currentDateLabel.setText(com.kainanresto.controllers.util.SystemTimeManager.getCurrentLocalDateTime().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        }
        if (restaurantLogoImage != null) {
            restaurantLogoImage.setClip(new Circle(34, 34, 34));
        }

        initializeIcons();

        // Load the Dashboard by default
        onNavDashboard();

        Platform.runLater(() -> {
            if (appRoot.getScene() == null) return;
            Stage stage = (Stage) appRoot.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent closeEvent) -> {
                closeEvent.consume();
                SessionManager.clearSession();
                NavigationUtil.switchScene(closeEvent, "/com/kainanresto/views/id/LoginView.fxml", "Kainan Ni Juan POS - Login");
            });
        });
    }

    private void initializeIcons() {
        setIconAndScale(dashboardIcon, Icons.NAV_DASHBOARD, 34.0);
        setIconAndScale(menuIcon, Icons.NAV_MENU, 34.0);
        setIconAndScale(salesOrdersIcon, Icons.NAV_SALES_ORDERS, 34.0);
        setIconAndScale(accountManagementIcon, Icons.NAV_ACCOUNTS, 34.0);
        setIconAndScale(settingsIcon, Icons.NAV_SETTINGS, 34.0);
        setIconAndScale(logoutIcon, Icons.NAV_LOGOUT, 30.0);
        setIconAndScale(calendarIcon, Icons.CALENDAR, 16.0);
    }

    private void setIconAndScale(SVGPath icon, String content, double targetSize) {
        if (icon != null && content != null) {
            icon.setContent(content);
            double scale = targetSize / 24.0; // 24 is the lucide grid size
            icon.setScaleX(scale);
            icon.setScaleY(scale);
            icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
        }
    }

    /* ================= ROUTING LOGIC ================= */
    private void switchView(String fxmlPath, String title, String subtitle, Button activeNavBtn) {
        try {
            if (!viewCache.containsKey(fxmlPath)) {
                Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
                viewCache.put(fxmlPath, view);
            }
            contentArea.getChildren().setAll(viewCache.get(fxmlPath));
            pageTitleLabel.setText(title);
            pageSubtitleLabel.setText(subtitle);
            setActiveNav(activeNavBtn);
        } catch (IOException e) {
            e.printStackTrace();
            com.kainanresto.util.AlertUtil.showError("Navigation Error", "Could not load module: " + fxmlPath);
        }
    }

    private void setActiveNav(Button active) {
        for (Button btn : new Button[]{navDashboardBtn, navMenuBtn, navSalesOrdersBtn, navAccountManagementBtn, navSettingsBtn}) {
            btn.getStyleClass().remove(NAV_ACTIVE);
        }
        if (!active.getStyleClass().contains(NAV_ACTIVE)) active.getStyleClass().add(NAV_ACTIVE);
    }

    @FXML private void onNavDashboard() {
        switchView("/com/kainanresto/views/main/admin/dashboard/DashboardView.fxml", "Dashboard", "Overview of restaurant health and inventory diagnostics", navDashboardBtn);
    }

    @FXML private void onNavMenu() {
        switchView("/com/kainanresto/views/main/admin/menu/MenuView.fxml", "Menu Management", "Configure your dishes, pricing tiers, and categorizations", navMenuBtn);
    }

    @FXML private void onNavSalesOrders() {
        switchView("/com/kainanresto/views/main/admin/sales/SalesView.fxml", "Sales & Orders", "Manage customer transactions, kitchen states, and order processing logs", navSalesOrdersBtn);
    }

    @FXML private void onNavAccountManagement() {
        switchView("/com/kainanresto/views/main/admin/accounts/AccountView.fxml", "Account Management", "Configure system access, roles, and security permissions", navAccountManagementBtn);
    }

    @FXML private void onNavSettings() {
        switchView("/com/kainanresto/views/main/admin/settings/SettingsView.fxml", "Settings", "Configure your restaurant preferences, system defaults, and security configurations", navSettingsBtn);
    }

    @FXML private void onLogout(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            new com.kainanresto.dao.UserDAO().setAccountInactive(currentUser.getUserId());
        }
        SessionManager.clearSession();
        NavigationUtil.switchScene(event, "/com/kainanresto/views/id/LoginView.fxml", "Kainan Ni Juan - Login");
    }
}
package com.kainanresto.controllers.main.admin;

import com.kainanresto.model.account.User;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainAdminController {

    /* ROOT / LAYERS */
    @FXML private StackPane appRoot;
    @FXML private BorderPane mainLayout;
    @FXML private VBox sidebarRoot;
    @FXML private Region sidebarScrim;
    @FXML private VBox expandedSidebar;

    /* BRANDING (placeholders until loaded from the database) */
    @FXML private ImageView restaurantLogoImage, expandedLogoImage;
    @FXML private Label logoPlaceholderLabel, expandedLogoPlaceholderLabel;
    @FXML private Label expandedRestaurantNameLabel, expandedPortalLabel;

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private StackPane contentArea;

    /* COMPACT NAV BUTTONS */
    @FXML private Button navDashboardBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private Button navSettingsBtn;

    /* EXPANDED NAV BUTTONS */
    @FXML private Button expNavDashboardBtn;
    @FXML private Button expNavMenuBtn;
    @FXML private Button expNavSalesOrdersBtn;
    @FXML private Button expNavAccountManagementBtn;
    @FXML private Button expNavSettingsBtn;

    /* ICONS (compact) */
    @FXML private SVGPath dashboardIcon, menuIcon, salesOrdersIcon, accountManagementIcon, settingsIcon, logoutIcon, calendarIcon;
    /* ICONS (expanded): a Node can only have one parent, so these are separate instances */
    @FXML private SVGPath expDashboardIcon, expMenuIcon, expSalesOrdersIcon, expAccountManagementIcon, expSettingsIcon, expLogoutIcon;

    private static final String NAV_ACTIVE = "nav-item-active";
    private static final String EXP_NAV_ACTIVE = "exp-nav-item-active";

    private static final int NAV_DASHBOARD = 0, NAV_MENU = 1, NAV_SALES = 2, NAV_ACCOUNTS = 3, NAV_SETTINGS = 4;

    /* Placeholders: replaced by DB values via applyBranding() */
    private static final String PLACEHOLDER_NAME = "Restaurant Name";
    private static final String PORTAL_LABEL = "Admin Portal";

    private static final double COMPACT_WIDTH = 93.0;
    private static final double EXPANDED_WIDTH = 304.0;
    private static final Duration ANIM_DURATION = Duration.millis(260);

    private final Map<String, Node> viewCache = new HashMap<>();
    private Button[] compactNav;
    private Button[] expandedNav;

    private boolean sidebarExpanded = false;
    private final Rectangle sidebarClip = new Rectangle(COMPACT_WIDTH, 0);
    private Timeline sidebarAnimation;

    @FXML
    public void initialize() {
        if (currentDateLabel != null) {
            currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        }
        restaurantLogoImage.setClip(new Circle(34, 34, 34));
        expandedLogoImage.setClip(new Circle(34, 34, 34));

        compactNav = new Button[]{navDashboardBtn, navMenuBtn, navSalesOrdersBtn, navAccountManagementBtn, navSettingsBtn};
        expandedNav = new Button[]{expNavDashboardBtn, expNavMenuBtn, expNavSalesOrdersBtn, expNavAccountManagementBtn, expNavSettingsBtn};

        initializeIcons();

        // Start collapsed: the expanded layer is clipped to the compact width and invisible
        sidebarClip.heightProperty().bind(expandedSidebar.heightProperty());
        expandedSidebar.setClip(sidebarClip);
        expandedSidebar.setOpacity(0);
        sidebarScrim.setOpacity(0);

        // Placeholder branding until real data is loaded
        applyBranding(PLACEHOLDER_NAME, null);
        loadBranding();

        // Load the Dashboard by default
        onNavDashboard();

        Platform.runLater(() -> {
            if (appRoot.getScene() == null) return;

            // ESC closes the expanded sidebar
            appRoot.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE && sidebarExpanded) {
                    collapseSidebar();
                    e.consume();
                }
            });

            Stage stage = (Stage) appRoot.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent closeEvent) -> {
                closeEvent.consume();
                SessionManager.clearSession();
                NavigationUtil.switchScene(closeEvent, "/com/kainanresto/views/id/LoginView.fxml", "Kainan Ni Juan POS - Login");
            });
        });
    }

    /* ================= BRANDING (DB-driven) ================= */

    /**
         * Hook for pulling the restaurant name/logo from the database.
     * Call applyBranding(...) from here once your DAO/service is ready, e.g.:
     *   RestaurantInfo info = new RestaurantDAO().getRestaurantInfo();
     *   applyBranding(info.getName(), info.getLogoImage());
     * Ideally run it off the FX thread (Task) and call applyBranding inside Platform.runLater.
     */
    private void loadBranding() {
        // TODO: fetch from database / settings
    }

    /** Public so other controllers (e.g. Settings after saving) can refresh the sidebar. */
    public void applyBranding(String restaurantName, Image logo) {
        String name = (restaurantName == null || restaurantName.isBlank()) ? PLACEHOLDER_NAME : restaurantName.trim();

        expandedRestaurantNameLabel.setText(name);
        expandedPortalLabel.setText(PORTAL_LABEL);

        boolean hasLogo = logo != null;
        restaurantLogoImage.setImage(logo);
        expandedLogoImage.setImage(logo);

        // Placeholder shows the first letter of the name when no logo exists
        String initial = name.substring(0, 1).toUpperCase(Locale.ENGLISH);
        logoPlaceholderLabel.setText(initial);
        expandedLogoPlaceholderLabel.setText(initial);
        logoPlaceholderLabel.setVisible(!hasLogo);
        expandedLogoPlaceholderLabel.setVisible(!hasLogo);
    }

    /* ================= ICONS ================= */

    private void initializeIcons() {
        setIconAndScale(dashboardIcon, Icons.NAV_DASHBOARD, 34.0);
        setIconAndScale(menuIcon, Icons.NAV_MENU, 34.0);
        setIconAndScale(salesOrdersIcon, Icons.NAV_SALES_ORDERS, 34.0);
        setIconAndScale(accountManagementIcon, Icons.NAV_ACCOUNTS, 34.0);
        setIconAndScale(settingsIcon, Icons.NAV_SETTINGS, 34.0);
        setIconAndScale(logoutIcon, Icons.NAV_LOGOUT, 30.0);
        setIconAndScale(calendarIcon, Icons.CALENDAR, 16.0);

        // Expanded sidebar uses a 30px icon host per the design
        setIconAndScale(expDashboardIcon, Icons.NAV_DASHBOARD, 30.0);
        setIconAndScale(expMenuIcon, Icons.NAV_MENU, 30.0);
        setIconAndScale(expSalesOrdersIcon, Icons.NAV_SALES_ORDERS, 30.0);
        setIconAndScale(expAccountManagementIcon, Icons.NAV_ACCOUNTS, 30.0);
        setIconAndScale(expSettingsIcon, Icons.NAV_SETTINGS, 30.0);
        setIconAndScale(expLogoutIcon, Icons.NAV_LOGOUT, 30.0);
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

    /* ================= SIDEBAR EXPAND / COLLAPSE ================= */

    @FXML
    private void onSidebarClicked(MouseEvent event) {
        if (isInsideButton(event.getTarget(), sidebarRoot)) return; // buttons keep their normal behavior
        expandSidebar();
    }

    @FXML
    private void onExpandedSidebarClicked(MouseEvent event) {
        if (isInsideButton(event.getTarget(), expandedSidebar)) return;
        collapseSidebar();
    }

    @FXML
    private void onScrimClicked(MouseEvent event) {
        collapseSidebar();
    }

    /** True if the click originated on (or inside) a button, e.g. its icon or text. */
    private boolean isInsideButton(Object target, Node boundary) {
        Node node = (target instanceof Node) ? (Node) target : null;
        while (node != null && node != boundary) {
            if (node instanceof ButtonBase) return true;
            node = node.getParent();
        }
        return false;
    }

    private void expandSidebar() {
        if (sidebarExpanded) return;
        sidebarExpanded = true;
        animateSidebar(true);
    }

    private void collapseSidebar() {
        if (!sidebarExpanded) return;
        sidebarExpanded = false;
        animateSidebar(false);
    }

    private void animateSidebar(boolean show) {
        if (sidebarAnimation != null) sidebarAnimation.stop();

        if (show) {
            sidebarScrim.setVisible(true);
            expandedSidebar.setVisible(true);
        }

        Interpolator ease = Interpolator.EASE_BOTH;

        // Width grows/shrinks over the full duration, along with the scrim
        KeyFrame end = new KeyFrame(ANIM_DURATION,
                new KeyValue(sidebarClip.widthProperty(), show ? EXPANDED_WIDTH : COMPACT_WIDTH, ease),
                new KeyValue(sidebarScrim.opacityProperty(), show ? 1.0 : 0.0, ease));

        // Content cross-fades with the compact sidebar underneath:
        // fades in during the first 40% on expand, fades out during the last 40% on collapse
        KeyFrame fade = show
                ? new KeyFrame(ANIM_DURATION.multiply(0.4), new KeyValue(expandedSidebar.opacityProperty(), 1.0))
                : new KeyFrame(ANIM_DURATION.multiply(0.6), new KeyValue(expandedSidebar.opacityProperty(), 1.0));
        KeyFrame fadeEnd = show
                ? null
                : new KeyFrame(ANIM_DURATION, new KeyValue(expandedSidebar.opacityProperty(), 0.0));

        sidebarAnimation = (fadeEnd == null)
                ? new Timeline(fade, end)
                : new Timeline(fade, end, fadeEnd);

        if (!show) {
            sidebarAnimation.setOnFinished(e -> {
                sidebarScrim.setVisible(false);
                expandedSidebar.setVisible(false);
            });
        }
        sidebarAnimation.play();
    }

    /* ================= ROUTING LOGIC ================= */

    private void switchView(String fxmlPath, String title, String subtitle, int navIndex) {
        try {
            if (!viewCache.containsKey(fxmlPath)) {
                Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
                viewCache.put(fxmlPath, view);
            }
            contentArea.getChildren().setAll(viewCache.get(fxmlPath));
            pageTitleLabel.setText(title);
            pageSubtitleLabel.setText(subtitle);
            setActiveNav(navIndex);
            collapseSidebar(); // no-op if already collapsed
        } catch (IOException e) {
            e.printStackTrace();
            com.kainanresto.util.AlertUtil.showError("Navigation Error", "Could not load module: " + fxmlPath);
        }
    }

    /** Keeps the compact and expanded sidebars in sync. */
    private void setActiveNav(int index) {
        for (int i = 0; i < compactNav.length; i++) {
            compactNav[i].getStyleClass().remove(NAV_ACTIVE);
            expandedNav[i].getStyleClass().remove(EXP_NAV_ACTIVE);
        }
        compactNav[index].getStyleClass().add(NAV_ACTIVE);
        expandedNav[index].getStyleClass().add(EXP_NAV_ACTIVE);
    }

    @FXML private void onNavDashboard() {
        switchView("/com/kainanresto/views/main/admin/dashboard/DashboardView.fxml", "Dashboard", "Overview of restaurant health and inventory diagnostics", NAV_DASHBOARD);
    }

    @FXML private void onNavMenu() {
        switchView("/com/kainanresto/views/main/admin/menu/MenuView.fxml", "Menu Management", "Configure your dishes, pricing tiers, and categorizations", NAV_MENU);
    }

    @FXML private void onNavSalesOrders() {
        switchView("/com/kainanresto/views/main/admin/sales/SalesView.fxml", "Sales & Orders", "Manage customer transactions, kitchen states, and order processing logs", NAV_SALES);
    }

    @FXML private void onNavAccountManagement() {
        switchView("/com/kainanresto/views/main/admin/accounts/AccountView.fxml", "Account Management", "Configure system access, roles, and security permissions", NAV_ACCOUNTS);
    }

    @FXML private void onNavSettings() {
        switchView("/com/kainanresto/views/main/admin/settings/SettingsView.fxml", "Settings", "Configure your restaurant preferences, system defaults, and security configurations", NAV_SETTINGS);
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
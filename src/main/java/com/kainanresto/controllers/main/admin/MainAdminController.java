package com.kainanresto.controllers.main.admin;

import com.kainanresto.controllers.main.client.om.ClientOrderManagementController;
import com.kainanresto.dao.OrderDAO;
import com.kainanresto.dao.ProductDAO;
import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.User;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainAdminController {

    @FXML private StackPane appRoot;
    @FXML private BorderPane mainLayout;
    @FXML private VBox sidebarRoot;
    @FXML private Region sidebarScrim;
    @FXML private VBox expandedSidebar;

    @FXML private ImageView restaurantLogoImage, expandedLogoImage;
    @FXML private Label expandedRestaurantNameLabel, expandedPortalLabel;

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private StackPane contentArea;

    @FXML private Button navDashboardBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navOrderManagementBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private HBox topbar;

    @FXML private Button expNavDashboardBtn;
    @FXML private Button expNavMenuBtn;
    @FXML private Button expNavSalesOrdersBtn;
    @FXML private Button expNavOrderManagementBtn;
    @FXML private Button expNavAccountManagementBtn;

    @FXML private SVGPath dashboardIcon;
    @FXML private SVGPath menuIcon;
    @FXML private SVGPath salesOrdersIcon;
    @FXML private SVGPath orderManagementIcon;
    @FXML private SVGPath accountManagementIcon;
    @FXML private SVGPath logoutIcon;
    @FXML private SVGPath calendarIcon;
    @FXML private SVGPath expDashboardIcon;
    @FXML private SVGPath expMenuIcon;
    @FXML private SVGPath expSalesOrdersIcon;
    @FXML private SVGPath expOrderManagementIcon;
    @FXML private SVGPath expAccountManagementIcon;
    @FXML private SVGPath expLogoutIcon;

    private static final String NAV_ACTIVE = "nav-item-active";
    private static final String EXP_NAV_ACTIVE = "exp-nav-item-active";

    private static final int NAV_DASHBOARD = 0;
    private static final int NAV_MENU = 1;
    private static final int NAV_SALES = 2;
    private static final int NAV_ACCOUNTS = 3;
    private static final int NAV_ORDER_MANAGEMENT = 4;

    private static final double COMPACT_WIDTH = 93.0;
    private static final double EXPANDED_WIDTH = 304.0;
    private static final Duration ANIM_DURATION = Duration.millis(260);

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Object activeSubController;

    private Button[] compactNav;
    private Button[] expandedNav;

    private boolean sidebarExpanded = false;

    private final Rectangle sidebarClip =
            new Rectangle(COMPACT_WIDTH, 0);

    private Timeline sidebarAnimation;

    private ConnectionOverlay connectionOverlay;

    @FXML private Button navSwitchBtn, expNavSwitchBtn;
    @FXML private SVGPath switchIcon, expSwitchIcon;

    @FXML
    public void initialize() {

        // Store controller reference on appRoot so child controllers can find us
        if (appRoot != null) {
            appRoot.getProperties().put("controller", this);
        }

        EntranceAnimation.prepare(sidebarRoot, topbar, contentArea);

        if (currentDateLabel != null) {
            currentDateLabel.setText(
                    LocalDate.now().format(
                            DateTimeFormatter.ofPattern(
                                    "EEEE, MMMM d, yyyy",
                                    Locale.ENGLISH
                            )
                    )
            );
        }

        restaurantLogoImage.setClip(
                new Circle(34, 34, 34)
        );

        expandedLogoImage.setClip(
                new Circle(34, 34, 34)
        );

        compactNav = new Button[]{
                navDashboardBtn,
                navMenuBtn,
                navSalesOrdersBtn,
                navAccountManagementBtn,
                navOrderManagementBtn
        };

        expandedNav = new Button[]{
                expNavDashboardBtn,
                expNavMenuBtn,
                expNavSalesOrdersBtn,
                expNavAccountManagementBtn,
                expNavOrderManagementBtn
        };

        initializeIcons();

        sidebarClip.heightProperty()
                .bind(expandedSidebar.heightProperty());

        expandedSidebar.setClip(sidebarClip);
        expandedSidebar.setOpacity(0);
        sidebarScrim.setOpacity(0);

        onNavDashboard();

        Platform.runLater(() -> {

            EntranceAnimation.play(sidebarRoot, topbar, contentArea);

            if (appRoot.getScene() == null) {
                return;
            }

            appRoot.getScene().addEventFilter(
                    KeyEvent.KEY_PRESSED,
                    e -> {
                        if (e.getCode() == KeyCode.ESCAPE
                                && sidebarExpanded) {

                            collapseSidebar();
                            e.consume();
                        }
                    }
            );

            Stage stage =
                    (Stage) appRoot.getScene().getWindow();

            stage.setOnCloseRequest(
                    (WindowEvent closeEvent) -> {

                        closeEvent.consume();
                        RealTimePoller.stopPolling();
                        shutdownConnectionWatch();
                        SessionManager.clearSession();

                        NavigationUtil.switchScene(
                                closeEvent,
                                "/com/kainanresto/views/id/LoginView.fxml",
                                "Kainan Ni Juan POS - Login"
                        );
                    }
            );
        });

        RealTimePoller.startPolling(this::refreshCurrentAdminView, 1.0);

        User curUser = SessionManager.getCurrentUser();
        AccountRole role = curUser != null ? curUser.getRole() : null;
        boolean isSupervisor = (role == AccountRole.SUPERVISOR);
        boolean isManager = (role == AccountRole.MANAGER);

        // Only Supervisor has Order Management inside Admin View
        boolean showOrderManagement = isSupervisor;
        for (Button b : new Button[]{navOrderManagementBtn, expNavOrderManagementBtn}) {
            if (b == null) continue;
            b.setVisible(showOrderManagement);
            b.setManaged(showOrderManagement);
        }

        boolean canSwitchToPos = isManager;
        for (Button b : new Button[]{navSwitchBtn, expNavSwitchBtn}) {
            if (b == null) continue;
            b.setVisible(canSwitchToPos);
            b.setManaged(canSwitchToPos);
        }

        ConnectionMonitor.start();
        connectionOverlay = ConnectionOverlay.attach(appRoot, this::refreshCurrentAdminView);
    }

    private void shutdownConnectionWatch() {
        ConnectionMonitor.stop();
        if (connectionOverlay != null) connectionOverlay.dispose();
    }

    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();

    private final java.util.concurrent.atomic.AtomicBoolean isAdminRefreshing = new java.util.concurrent.atomic.AtomicBoolean(false);

    private void refreshCurrentAdminView() {
        if (!ConnectionMonitor.isConnected()) return;
        if (appRoot.getScene() == null || appRoot.getScene().getWindow() == null) return;
        if (activeSubController == null) return;
        if (!isAdminRefreshing.compareAndSet(false, true)) return;

        Thread t = new Thread(() -> {
            try {
                List<OrderReceipt> receipts = orderDAO.loadReceipts(LocalDate.now().atStartOfDay());
                List<Dishes> dishes = productDAO.getAllDishes();
                List<com.kainanresto.model.account.AccountRow> users = userDAO.getAllAccounts();

                Platform.runLater(() -> {
                    try {
                        if (activeSubController instanceof com.kainanresto.controllers.main.admin.sales.SalesController) {
                            ((com.kainanresto.controllers.main.admin.sales.SalesController) activeSubController).refreshData();
                        } else if (activeSubController instanceof com.kainanresto.controllers.main.admin.menu.MenuController) {
                            ((com.kainanresto.controllers.main.admin.menu.MenuController) activeSubController).refreshData(dishes);
                        } else if (activeSubController instanceof com.kainanresto.controllers.main.admin.accounts.AccountController) {
                            ((com.kainanresto.controllers.main.admin.accounts.AccountController) activeSubController).refreshData(users);
                        } else if (activeSubController instanceof com.kainanresto.controllers.main.admin.dashboard.DashboardController) {
                            ((com.kainanresto.controllers.main.admin.dashboard.DashboardController) activeSubController).refreshData();
                        } else if (activeSubController instanceof ClientOrderManagementController omc) {
                            omc.refreshData(receipts);
                        }
                    } catch (Exception e) {
                        try {
                            java.lang.reflect.Method fallback = activeSubController.getClass().getMethod("refreshData");
                            fallback.invoke(activeSubController);
                        } catch (Exception ignored) {}
                    } finally {
                        isAdminRefreshing.set(false);
                    }
                });
            } catch (Exception e) {
                isAdminRefreshing.set(false);
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void initializeIcons() {

        setIconAndScale(
                dashboardIcon,
                Icons.NAV_DASHBOARD,
                34.0
        );

        setIconAndScale(
                menuIcon,
                Icons.NAV_MENU,
                34.0
        );

        setIconAndScale(
                salesOrdersIcon,
                Icons.NAV_SALES_ORDERS,
                34.0
        );

        setIconAndScale(
                orderManagementIcon,
                Icons.CLOCK,
                34.0
        );

        setIconAndScale(
                accountManagementIcon,
                Icons.NAV_ACCOUNTS,
                34.0
        );

        setIconAndScale(
                expSwitchIcon,
                Icons.RECEIPT_TEXT,
                30.0);

        setIconAndScale(
                logoutIcon,
                Icons.NAV_LOGOUT,
                30.0
        );

        setIconAndScale(
                calendarIcon,
                Icons.CALENDAR,
                16.0
        );

        setIconAndScale(
                expDashboardIcon,
                Icons.NAV_DASHBOARD,
                30.0
        );

        setIconAndScale(
                expMenuIcon,
                Icons.NAV_MENU,
                30.0
        );

        setIconAndScale(
                expSalesOrdersIcon,
                Icons.NAV_SALES_ORDERS,
                30.0
        );

        setIconAndScale(
                expOrderManagementIcon,
                Icons.CLOCK,
                30.0
        );

        setIconAndScale(
                expAccountManagementIcon,
                Icons.NAV_ACCOUNTS,
                30.0
        );

        setIconAndScale(
                switchIcon,
                Icons.RECEIPT_TEXT,
                34.0);

        setIconAndScale(
                expLogoutIcon,
                Icons.NAV_LOGOUT,
                30.0
        );
    }

    private void setIconAndScale(
            SVGPath icon,
            String content,
            double targetSize
    ) {

        if (icon != null && content != null) {

            icon.setContent(content);

            double scale = targetSize / 24.0;

            icon.setScaleX(scale);
            icon.setScaleY(scale);

            icon.setStyle(
                    "-fx-stroke-width: "
                            + (2.0 / scale)
                            + ";"
            );
        }
    }

    @FXML
    private void onSidebarClicked(MouseEvent event) {

        if (isInsideButton(
                event.getTarget(),
                sidebarRoot
        )) {
            return;
        }

        expandSidebar();
    }

    @FXML private void onExpandedSidebarClicked(MouseEvent event) {

        if (isInsideButton(
                event.getTarget(),
                expandedSidebar
        )) {
            return;
        }

        collapseSidebar();
    }

    @FXML private void onScrimClicked(MouseEvent event) {
        collapseSidebar();
    }

    private boolean isInsideButton(
            Object target,
            Node boundary
    ) {

        Node node =
                (target instanceof Node)
                        ? (Node) target
                        : null;

        while (node != null && node != boundary) {

            if (node instanceof ButtonBase) {
                return true;
            }

            node = node.getParent();
        }

        return false;
    }

    private void expandSidebar() {

        if (sidebarExpanded) {
            return;
        }

        sidebarExpanded = true;

        animateSidebar(true);
    }

    private void collapseSidebar() {

        if (!sidebarExpanded) {
            return;
        }

        sidebarExpanded = false;

        animateSidebar(false);
    }

    private void animateSidebar(boolean show) {

        if (sidebarAnimation != null) {
            sidebarAnimation.stop();
        }

        if (show) {
            sidebarScrim.setVisible(true);
            expandedSidebar.setVisible(true);
        }

        Interpolator ease =
                Interpolator.EASE_BOTH;

        KeyFrame end =
                new KeyFrame(
                        ANIM_DURATION,
                        new KeyValue(
                                sidebarClip.widthProperty(),
                                show
                                        ? EXPANDED_WIDTH
                                        : COMPACT_WIDTH,
                                ease
                        ),
                        new KeyValue(
                                sidebarScrim.opacityProperty(),
                                show ? 1.0 : 0.0,
                                ease
                        )
                );

        KeyFrame fade =
                show
                        ? new KeyFrame(
                        ANIM_DURATION.multiply(0.4),
                        new KeyValue(
                                expandedSidebar.opacityProperty(),
                                1.0
                        )
                )
                        : new KeyFrame(
                        ANIM_DURATION.multiply(0.6),
                        new KeyValue(
                                expandedSidebar.opacityProperty(),
                                1.0
                        )
                );

        KeyFrame fadeEnd =
                show
                        ? null
                        : new KeyFrame(
                        ANIM_DURATION,
                        new KeyValue(
                                expandedSidebar.opacityProperty(),
                                0.0
                        )
                );

        sidebarAnimation =
                (fadeEnd == null)
                        ? new Timeline(fade, end)
                        : new Timeline(
                        fade,
                        end,
                        fadeEnd
                );

        if (!show) {

            sidebarAnimation.setOnFinished(e -> {

                sidebarScrim.setVisible(false);
                expandedSidebar.setVisible(false);
            });
        }

        sidebarAnimation.play();
    }

    private void switchView(
            String fxmlPath,
            String title,
            String subtitle,
            int navIndex
    ) {

        try {
            Node view = viewCache.get(fxmlPath);
            Object controller = controllerCache.get(fxmlPath);

            if (view == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                view = loader.load();
                controller = loader.getController();

                viewCache.put(fxmlPath, view);
                controllerCache.put(fxmlPath, controller);
            }

            contentArea.getChildren().setAll(view);
            activeSubController = controller;

            pageTitleLabel.setText(title);
            pageSubtitleLabel.setText(subtitle);

            setActiveNav(navIndex);

            collapseSidebar();

        } catch (IOException e) {

            e.printStackTrace();

            com.kainanresto.util.AlertUtil.showError(
                    "Navigation Error",
                    "Could not load module: " + fxmlPath
            );
        }
    }

    private void setActiveNav(int index) {

        for (int i = 0; i < compactNav.length; i++) {

            compactNav[i]
                    .getStyleClass()
                    .remove(NAV_ACTIVE);

            expandedNav[i]
                    .getStyleClass()
                    .remove(EXP_NAV_ACTIVE);
        }

        compactNav[index]
                .getStyleClass()
                .add(NAV_ACTIVE);

        expandedNav[index]
                .getStyleClass()
                .add(EXP_NAV_ACTIVE);
    }

    @FXML
    private void onNavDashboard() {

        switchView(
                "/com/kainanresto/views/main/admin/dashboard/DashboardView.fxml",
                "Dashboard",
                "Overview of restaurant health and inventory diagnostics",
                NAV_DASHBOARD
        );
    }

    @FXML
    private void onNavMenu() {

        switchView(
                "/com/kainanresto/views/main/admin/menu/MenuView.fxml",
                "Menu Management",
                "Configure your dishes, pricing tiers, and categorizations",
                NAV_MENU
        );
    }

    @FXML
    private void onNavSalesOrders() {

        switchView(
                "/com/kainanresto/views/main/admin/sales/SalesView.fxml",
                "Sales & Orders",
                "Manage customer transactions, kitchen states, and order processing logs",
                NAV_SALES
        );
    }

    /**
     * Public entry point for child controllers (e.g. Dashboard) to navigate to Sales & Orders.
     */
    public void navigateToSalesOrders() {
        onNavSalesOrders();
    }

    @FXML
    private void onNavAccountManagement() {

        switchView(
                "/com/kainanresto/views/main/admin/accounts/AccountView.fxml",
                "Account Management",
                "Configure system access, roles, and security permissions",
                NAV_ACCOUNTS
        );
    }

    @FXML
    private void onNavOrderManagement() {
        User curUser = SessionManager.getCurrentUser();
        if (curUser == null || curUser.getRole() != AccountRole.SUPERVISOR) {
            return;
        }

        switchView(
                "/com/kainanresto/views/main/client/om/ClientOrderManagementView.fxml",
                "Order Management",
                "Monitor and manage current restaurant orders",
                NAV_ORDER_MANAGEMENT
        );

        if (activeSubController instanceof ClientOrderManagementController omc) {
            omc.setHeaderVisible(false);
            wireOrderManagement(omc);
            loadTodaysOrders(omc);
        }
    }

    public void navigateToOrderManagement() {
        User curUser = SessionManager.getCurrentUser();
        if (curUser != null && curUser.getRole() == AccountRole.SUPERVISOR) {
            onNavOrderManagement();
        }
    }

    private void wireOrderManagement(ClientOrderManagementController omc) {
        omc.setOnCompleteOrder(order -> {
            if (!orderDAO.updateStatus(order.orderNumber(), OrderStatus.COMPLETED)) {
                AlertUtil.showError("Error", "Could not save the status change.");
            } else {
                AlertUtil.showInfo("Order Completed", "Order #" + order.orderNumber() + " has been completed.");
            }
        });
        omc.setOnCancelOrder(order -> {
            if (!orderDAO.updateStatus(order.orderNumber(), OrderStatus.CANCELLED)) {
                AlertUtil.showError("Error", "Could not cancel the order in the database.");
            } else {
                AlertUtil.showInfo("Order Cancelled", "Order #" + order.orderNumber() + " has been cancelled and refunded.");
            }
        });
    }

    private void loadTodaysOrders(ClientOrderManagementController omc) {
        Thread t = new Thread(() -> {
            List<OrderReceipt> receipts = orderDAO.loadReceipts(LocalDate.now().atStartOfDay());
            Platform.runLater(() -> omc.setReceipts(receipts));
        });
        t.setDaemon(true);
        t.start();
    }



    @FXML private void onSwitchToPos(ActionEvent event) {
        User u = SessionManager.getCurrentUser();
        if (!RoleAccess.canUseBoth(u)) return;
        RealTimePoller.stopPolling();
        shutdownConnectionWatch();
        NavigationUtil.switchScene(event, "/com/kainanresto/views/main/client/ClientView.fxml",
                "Kainan Ni Juan POS - " + u.getFullName() + " (" + u.getRole() + ")");
    }

    @FXML private void onLogout(ActionEvent event) {
        boolean confirmed = AlertUtil.showYesNoConfirmation(
                "",
                "Log Out",
                "Are you sure you want to log out?"
        );

        if (!confirmed) {
            return;
        }

        RealTimePoller.stopPolling();
        shutdownConnectionWatch();

        User currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) {
            new com.kainanresto.dao.UserDAO()
                    .setAccountInactive(currentUser.getUserId());
            if (currentUser.getRole() == com.kainanresto.model.account.AccountRole.ADMIN) {
                com.kainanresto.controllers.id.LoginController.clearRememberedUser();
            }
        }

        SessionManager.clearSession();

        NavigationUtil.switchScene(
                event,
                "/com/kainanresto/views/id/LoginView.fxml",
                "Kainan Ni Juan - Login"
        );
    }
}
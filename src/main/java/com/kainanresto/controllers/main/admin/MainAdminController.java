package com.kainanresto.controllers.main.admin;

import com.kainanresto.model.account.User;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.EntranceAnimation;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.RoleAccess;
import com.kainanresto.util.SessionManager;
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
import java.util.Locale;
import java.util.Map;

public class MainAdminController {

    /* ROOT / LAYERS */
    @FXML private StackPane appRoot;
    @FXML private BorderPane mainLayout;
    @FXML private VBox sidebarRoot;
    @FXML private Region sidebarScrim;
    @FXML private VBox expandedSidebar;

    /* BRANDING */
    @FXML private ImageView restaurantLogoImage, expandedLogoImage;
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
    @FXML private HBox topbar;

    /* EXPANDED NAV BUTTONS */
    @FXML private Button expNavDashboardBtn;
    @FXML private Button expNavMenuBtn;
    @FXML private Button expNavSalesOrdersBtn;
    @FXML private Button expNavAccountManagementBtn;
    @FXML private Button expNavSettingsBtn;

    /* ICONS - COMPACT */
    @FXML private SVGPath dashboardIcon;
    @FXML private SVGPath menuIcon;
    @FXML private SVGPath salesOrdersIcon;
    @FXML private SVGPath accountManagementIcon;
    @FXML private SVGPath settingsIcon;
    @FXML private SVGPath logoutIcon;
    @FXML private SVGPath calendarIcon;

    /* ICONS - EXPANDED */
    @FXML private SVGPath expDashboardIcon;
    @FXML private SVGPath expMenuIcon;
    @FXML private SVGPath expSalesOrdersIcon;
    @FXML private SVGPath expAccountManagementIcon;
    @FXML private SVGPath expSettingsIcon;
    @FXML private SVGPath expLogoutIcon;

    private static final String NAV_ACTIVE = "nav-item-active";
    private static final String EXP_NAV_ACTIVE = "exp-nav-item-active";

    private static final int NAV_DASHBOARD = 0;
    private static final int NAV_MENU = 1;
    private static final int NAV_SALES = 2;
    private static final int NAV_ACCOUNTS = 3;
    private static final int NAV_SETTINGS = 4;

    private static final double COMPACT_WIDTH = 93.0;
    private static final double EXPANDED_WIDTH = 304.0;
    private static final Duration ANIM_DURATION = Duration.millis(260);

    /* VIEW CACHE */
    private final Map<String, Node> viewCache = new HashMap<>();

    private Button[] compactNav;
    private Button[] expandedNav;

    private boolean sidebarExpanded = false;

    private final Rectangle sidebarClip =
            new Rectangle(COMPACT_WIDTH, 0);

    private Timeline sidebarAnimation;

    //SWITCHING SA ADMIN OR CLIENT IF SUPERVISOR OR MANAGER

    @FXML private Button navSwitchBtn, expNavSwitchBtn;
    @FXML private SVGPath switchIcon, expSwitchIcon;

    @FXML
    public void initialize() {

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

        /* KEEP LOGO CIRCLE CLIPS */
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
                navSettingsBtn
        };

        expandedNav = new Button[]{
                expNavDashboardBtn,
                expNavMenuBtn,
                expNavSalesOrdersBtn,
                expNavAccountManagementBtn,
                expNavSettingsBtn
        };

        initializeIcons();

        /* START COLLAPSED */
        sidebarClip.heightProperty()
                .bind(expandedSidebar.heightProperty());

        expandedSidebar.setClip(sidebarClip);
        expandedSidebar.setOpacity(0);
        sidebarScrim.setOpacity(0);

        /* DEFAULT PAGE */
        onNavDashboard();

        Platform.runLater(() -> {

            EntranceAnimation.play(sidebarRoot, topbar, contentArea);

            if (appRoot.getScene() == null) {
                return;
            }

            /* ESC closes expanded sidebar */
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

                        SessionManager.clearSession();

                        NavigationUtil.switchScene(
                                closeEvent,
                                "/com/kainanresto/views/id/LoginView.fxml",
                                "Kainan Ni Juan POS - Login"
                        );
                    }
            );
        });

        //SA ROLE TO
        boolean both = RoleAccess.canUseBoth(SessionManager.getCurrentUser());
        for (Button b : new Button[]{navSwitchBtn, expNavSwitchBtn}) {
            if (b == null) continue;
            b.setVisible(both);
            b.setManaged(both);
        }

    }

    /* ================= ICONS ================= */

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
                accountManagementIcon,
                Icons.NAV_ACCOUNTS,
                34.0
        );

        setIconAndScale(
                settingsIcon,
                Icons.NAV_SETTINGS,
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
                expAccountManagementIcon,
                Icons.NAV_ACCOUNTS,
                30.0
        );

        setIconAndScale(
                expSettingsIcon,
                Icons.NAV_SETTINGS,
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

    /* ================= SIDEBAR EXPAND / COLLAPSE ================= */

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

    @FXML
    private void onExpandedSidebarClicked(MouseEvent event) {

        if (isInsideButton(
                event.getTarget(),
                expandedSidebar
        )) {
            return;
        }

        collapseSidebar();
    }

    @FXML
    private void onScrimClicked(MouseEvent event) {
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

    /* ================= ROUTING LOGIC ================= */

    private void switchView(
            String fxmlPath,
            String title,
            String subtitle,
            int navIndex
    ) {

        try {

            if (!viewCache.containsKey(fxmlPath)) {

                Node view =
                        FXMLLoader.load(
                                getClass().getResource(fxmlPath)
                        );

                viewCache.put(fxmlPath, view);
            }

            contentArea.getChildren().setAll(
                    viewCache.get(fxmlPath)
            );

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

    /* ================= NAVIGATION STATE ================= */

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

    /* ================= NAVIGATION ================= */

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
    private void onNavSettings() {

        switchView(
                "/com/kainanresto/views/main/admin/settings/SettingsView.fxml",
                "Settings",
                "Configure your restaurant preferences, system defaults, and security configurations",
                NAV_SETTINGS
        );
    }

    @FXML private void onSwitchToPos(ActionEvent event) {

        User u = SessionManager.getCurrentUser();
            if (!RoleAccess.canUseBoth(u)) return;
            NavigationUtil.switchScene(event, "/com/kainanresto/views/main/client/ClientView.fxml",
                "Kainan Ni Juan POS - " + u.getFullName() + " (" + u.getRole() + ")");
    }

    /* ================= LOGOUT ================= */

    @FXML
    private void onLogout(ActionEvent event) {

        User currentUser =
                SessionManager.getCurrentUser();

        if (currentUser != null) {

            new com.kainanresto.dao.UserDAO()
                    .setAccountInactive(
                            currentUser.getUserId()
                    );
        }

        SessionManager.clearSession();

        NavigationUtil.switchScene(
                event,
                "/com/kainanresto/views/id/LoginView.fxml",
                "Kainan Ni Juan - Login"
        );
    }
}


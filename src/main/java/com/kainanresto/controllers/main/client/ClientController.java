package com.kainanresto.controllers.main.client;

import com.kainanresto.controllers.main.client.pos.AdminAuthDialog;
import com.kainanresto.dao.OrderDAO;
import com.kainanresto.dao.ProductDAO;
import com.kainanresto.model.account.User;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.*;

import com.kainanresto.controllers.main.client.pos.ClientPosController;
import com.kainanresto.controllers.main.client.pos.PaymentDialog;
import com.kainanresto.controllers.main.client.pos.ReceiptDialog;
import com.kainanresto.controllers.main.client.om.ClientOrderManagementController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ClientController {

    @FXML private StackPane appRoot;
    @FXML private ImageView restaurantLogoImage;
    @FXML private Button navOrderBtn;
    @FXML private Button navHistoryBtn;
    @FXML private Button logoutBtn;
    @FXML private SVGPath orderNavIcon;
    @FXML private SVGPath historyNavIcon;
    @FXML private SVGPath logoutIcon;

    @FXML private HBox posView;
    @FXML private StackPane viewStack;
    @FXML private ClientPosController posViewController;

    @FXML private HBox omView;
    @FXML private ClientOrderManagementController omViewController;

    @FXML private VBox sidebarRoot;
    @FXML private Region sidebarScrim;
    @FXML private VBox expandedSidebar;
    @FXML private ImageView expandedLogoImage;
    @FXML private Button expNavOrderBtn, expNavHistoryBtn, expNavSwitchBtn;
    @FXML private SVGPath expOrderIcon, expHistoryIcon, expSwitchIcon, expLogoutIcon;

    private static final String EXP_NAV_ACTIVE = "exp-nav-item-active";
    private static final double COMPACT_WIDTH = 93.0;
    private static final double EXPANDED_WIDTH = 304.0;
    private static final Duration ANIM_DURATION = Duration.millis(260);

    private boolean sidebarExpanded = false;
    private final Rectangle sidebarClip = new Rectangle(COMPACT_WIDTH, 0);
    private Timeline sidebarAnimation;

    private static final String NAV_ACTIVE = "nav-item-active";

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private final ObservableList<ReceiptLine> currentCart = FXCollections.observableArrayList();
    private BigDecimal currentDiscountRate = BigDecimal.ZERO;
    private int discountCards = 1;
    private String currentDiscountName = "None";
    private ReceiptTotals lastTotals;   // totals currently shown on the receipt panel

    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");            // 12% VAT
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05"); // 5% service charge

    private static final boolean VAT_EXEMPT_ON_DISCOUNTED = true;

    @FXML private Button navSwitchBtn;
    @FXML private SVGPath switchIcon;

    @FXML
    public void initialize() {
        EntranceAnimation.prepare(sidebarRoot, null, viewStack);
        restaurantLogoImage.setClip(new Circle(34, 34, 34));

        initializeIcons();
        normalizeIcons();

        setupCartHandlers();
        wireOrderPersistence();
        showOrder();
        loadTodaysOrders();

        expandedLogoImage.setClip(new Circle(34, 34, 34));

        sidebarClip.heightProperty().bind(expandedSidebar.heightProperty());
        expandedSidebar.setClip(sidebarClip);
        expandedSidebar.setOpacity(0);
        sidebarScrim.setOpacity(0);

        boolean both = RoleAccess.canUseBoth(SessionManager.getCurrentUser());
        if (navSwitchBtn != null) {
            navSwitchBtn.setVisible(both);
            navSwitchBtn.setManaged(both);
            expNavSwitchBtn.setVisible(both);
            expNavSwitchBtn.setManaged(both);
        }

        Platform.runLater(() -> {
            EntranceAnimation.play(sidebarRoot, null, viewStack);

            if (appRoot.getScene() == null) return;
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

    private void setupCartHandlers() {
        posViewController.setOnAddToOrder(dish -> {
            Optional<ReceiptLine> existingLine = currentCart.stream()
                    .filter(line -> line.dishId() == dish.id())
                    .findFirst();

            if (existingLine.isPresent()) {
                ReceiptLine line = existingLine.get();
                updateCartItem(line, line.quantity() + 1);
            } else {
                currentCart.add(new ReceiptLine(dish.id(), dish.name(), dish.imageUrl(), dish.price(), 1));
            }
            refreshReceiptUI();
        });

        posViewController.setOnReceiptQuantityChange((line, newQty) -> {

            //VALIDATION
            if (newQty < line.quantity()) {

                // CHECK IF ADMIN OVERRIDE IS REQUIRED IN SETTINGS
                if (isPinRequiredForVoid()) {
                    boolean isAuthorized = AdminAuthDialog.show(
                            appRoot.getScene().getWindow(),
                            "Authorize Item Void / Reduction"
                    );

                    if (!isAuthorized) {
                        return;
                    }
                }
            }

            if (newQty <= 0) {
                currentCart.remove(line);
            } else {
                updateCartItem(line, newQty);
            }
            refreshReceiptUI();
        });



        //DISCOUNT TYPE
        posViewController.setOnReceiptDiscountChange(discountName -> {
            currentDiscountName = discountName;
            currentDiscountRate = discountName.contains("20%") ? new BigDecimal("0.20") : BigDecimal.ZERO;
            discountCards = 1;
            refreshReceiptUI();
        });

        //NUMBER OF DISCOUNT
        posViewController.setOnReceiptDiscountCardsChange(delta -> {
            discountCards += delta;
            refreshReceiptUI();
        });

        //PAYMENT
        posViewController.setOnProceedToPayment(this::onProceedToPayment);
    }

    private void updateCartItem(ReceiptLine oldLine, int newQty) {
        int index = currentCart.indexOf(oldLine);
        if (index >= 0) {
            currentCart.set(index, new ReceiptLine(
                    oldLine.dishId(),
                    oldLine.name(),
                    oldLine.imageUrl(),
                    oldLine.unitPrice(),
                    newQty
            ));
        }
    }

    private void refreshReceiptUI() {
        if (currentCart.isEmpty()) {
            lastTotals = null;
            posViewController.setReceipt(null, null);
            posViewController.setDiscountSummary(false, 1, null);
            return;
        }

        List<BigDecimal> unitPrices = new ArrayList<>();
        for (ReceiptLine line : currentCart) {
            for (int i = 0; i < line.quantity(); i++) unitPrices.add(line.unitPrice());
        }
        unitPrices.sort(Comparator.reverseOrder());

        boolean discountActive = currentDiscountRate.signum() > 0;
        discountCards = Math.max(1, Math.min(discountCards, unitPrices.size()));

        BigDecimal subtotal = unitPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountedBase = BigDecimal.ZERO;
        if (discountActive) {
            for (int i = 0; i < discountCards; i++) discountedBase = discountedBase.add(unitPrices.get(i));
        }
        BigDecimal discountAmount = discountedBase.multiply(currentDiscountRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSubtotal = subtotal.subtract(discountAmount);
        BigDecimal vatBase = (discountActive && VAT_EXEMPT_ON_DISCOUNTED)
                ? subtotal.subtract(discountedBase)
                : netSubtotal;

        BigDecimal vatAmount = vatBase.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceCharge = netSubtotal.multiply(SERVICE_CHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = netSubtotal.add(vatAmount).add(serviceCharge);

        ReceiptTotals totals = new ReceiptTotals(
                subtotal,
                SERVICE_CHARGE_RATE.multiply(new BigDecimal("100")),
                serviceCharge,
                VAT_RATE.multiply(new BigDecimal("100")),
                vatAmount,
                total
        );

        Map<String, Integer> discountedUnits = new HashMap<>();
        if (discountActive) {
            List<ReceiptLine> units = new ArrayList<>();
            for (ReceiptLine line : currentCart) {
                for (int i = 0; i < line.quantity(); i++) units.add(line);
            }
            units.sort((a, b) -> b.unitPrice().compareTo(a.unitPrice())); // stable sort
            for (int i = 0; i < discountCards && i < units.size(); i++) {
                discountedUnits.merge(units.get(i).name(), 1, Integer::sum);
            }
        }

        lastTotals = totals;
        posViewController.setReceipt(new ArrayList<>(currentCart), totals, discountedUnits, currentDiscountRate);
        posViewController.setDiscountSummary(discountActive, discountCards, discountAmount);
    }

    // ================================= PAYMENT =================================

    private void onProceedToPayment() {
        if (currentCart.isEmpty() || lastTotals == null) return;

        Stage owner = (Stage) appRoot.getScene().getWindow();
        String orderNumber = nextOrderNumber();
        String orderType = "Dine in";


        Optional<PaymentResult> payment = PaymentDialog.show(owner, orderNumber, orderType, lastTotals.total());
        if (payment.isEmpty()) return;

        boolean hasDiscount = currentDiscountRate.signum() > 0;
        OrderReceipt receipt = new OrderReceipt(
                orderNumber,
                orderType,
                null,
                OrderStatus.PREPARING,
                com.kainanresto.controllers.util.SystemTimeManager.getCurrentLocalDateTime(),
                currentCashierName(),
                List.copyOf(currentCart),
                hasDiscount ? currentDiscountName : null,
                lastTotals);

        //SAVE
        User user = SessionManager.getCurrentUser();
            Integer cashierId = user == null ? null : user.getUserId();
        if (!orderDAO.saveOrder(receipt, payment.get(), cashierId, currentCashierRole())) {
            AlertUtil.showError("Order not saved",
                    "Could not save the order. A dish may have run out of stock. The payment was NOT recorded.");
            loadPosData();   // refresh stock so the cashier sees what's left
            return;
        }

        //PDF RECEIPT
        ReceiptDialog.show(owner, receipt, payment.get());

        //TO ORDER MANAGEMENT
        omViewController.addOrder(receipt);
        resetOrder();
        showOrderManagement();
    }

    private void resetOrder() {
        currentCart.clear();
        currentDiscountRate = BigDecimal.ZERO;
        currentDiscountName = "None";
        discountCards = 1;
        refreshReceiptUI();
        loadPosData();
    }

    private String nextOrderNumber() {
        return orderDAO.generateOrderNumber();
    }

    private String currentCashierName() {
        User user = SessionManager.getCurrentUser();
        return user == null ? "\u2014" : user.getFullName();
    }

    private String currentCashierRole() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() == null) return "Cashier";
        String r = String.valueOf(user.getRole()).toLowerCase();
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    // ================================= ORDER PERSISTENCE =================================

    private void loadTodaysOrders() {
        Thread t = new Thread(() -> {
            List<OrderReceipt> receipts = orderDAO.loadReceipts(LocalDate.now().atStartOfDay());
            Platform.runLater(() -> omViewController.setReceipts(receipts));
        });
        t.setDaemon(true);
        t.start();
    }

    private void wireOrderPersistence() {
        omViewController.setOnCompleteOrder(order -> {
            if (!orderDAO.updateStatus(order.orderNumber(), OrderStatus.COMPLETED)) {
                AlertUtil.showError("Error", "Could not save the status change.");
            }
        });
        omViewController.setOnCancelOrder(order -> {
            if (!orderDAO.updateStatus(order.orderNumber(), OrderStatus.CANCELLED)) {
                AlertUtil.showError("Error", "Could not cancel the order in the database.");
            } else {
                loadPosData();
            }
        });
    }

    private void initializeIcons() {
        setIcon(orderNavIcon, Icons.RECEIPT_TEXT);
        setIcon(historyNavIcon, Icons.CLOCK);
        setIcon(logoutIcon, Icons.NAV_LOGOUT);
        setIcon(switchIcon, Icons.NAV_DASHBOARD);
        ClientUIHelper.fitGridIcon(switchIcon, 34.0, 2.0);
        setIcon(expOrderIcon, Icons.RECEIPT_TEXT);
        setIcon(expHistoryIcon, Icons.CLOCK);
        setIcon(expSwitchIcon, Icons.NAV_DASHBOARD);
        setIcon(expLogoutIcon, Icons.NAV_LOGOUT);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) icon.setContent(content);
    }

    private void normalizeIcons() {
        ClientUIHelper.fitGridIcon(orderNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(historyNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(logoutIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expOrderIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expHistoryIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expSwitchIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expLogoutIcon, 30.0, 2.0);
    }

    @FXML private void onNavOrder()   { showOrder();           collapseSidebar(); }
    @FXML private void onNavHistory() { showOrderManagement(); collapseSidebar(); }

    @FXML private void onSwitchToAdmin(ActionEvent event) {
        User u = SessionManager.getCurrentUser();
        if (!RoleAccess.canUseBoth(u)) return;
        NavigationUtil.switchScene(event, "/com/kainanresto/views/main/admin/MainAdminView.fxml",
                "Kainan Ni Juan POS - " + u.getFullName() + " (" + u.getRole() + ")");
    }

    @FXML
    private void onLogout(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            com.kainanresto.dao.UserDAO userDAO = new com.kainanresto.dao.UserDAO();
            userDAO.setAccountInactive(currentUser.getUserId());
        }
        SessionManager.clearSession();
        NavigationUtil.switchScene(event, "/com/kainanresto/views/id/LoginView.fxml", "Kainan Ni Juan - Login");
    }

    private void showView(Node target) {
        for (Node v : new Node[]{posView, omView}) {
            if (v == null) continue;
            boolean on = (v == target);
            v.setVisible(on);
            v.setManaged(on);
        }
    }

    private void showOrder() {
        showView(posView);
        setActiveNav(navOrderBtn);
        loadPosData();
    }

    private void loadPosData() {
        Thread loadDataThread = new Thread(() -> {
            try {
                List<Dishes> dishes = productDAO.getAllDishes();
                List<String> categories = null;
                List<String> discounts = List.of("None", "Senior Citizen (20%)", "PWD (20%)");

                Platform.runLater(() -> {
                    setCategories(categories);
                    setDishes(dishes);
                    setDiscountOptions(discounts);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        loadDataThread.setDaemon(true);
        loadDataThread.start();
    }

    private void showOrderManagement() {
        showView(omView);
        setActiveNav(navHistoryBtn);
    }

    private void setActiveNav(Button active) {
        boolean order = (active == navOrderBtn);

        for (Button b : new Button[]{navOrderBtn, navHistoryBtn}) b.getStyleClass().remove(NAV_ACTIVE);
        for (Button b : new Button[]{expNavOrderBtn, expNavHistoryBtn}) b.getStyleClass().remove(EXP_NAV_ACTIVE);

        (order ? navOrderBtn : navHistoryBtn).getStyleClass().add(NAV_ACTIVE);
        (order ? expNavOrderBtn : expNavHistoryBtn).getStyleClass().add(EXP_NAV_ACTIVE);
    }

    /* ================= SIDEBAR EXPAND / COLLAPSE ================= */

    @FXML private void onSidebarClicked(MouseEvent e) {
        if (isInsideButton(e.getTarget(), sidebarRoot)) return;
        expandSidebar();
    }

    @FXML private void onExpandedSidebarClicked(MouseEvent e) {
        if (isInsideButton(e.getTarget(), expandedSidebar)) return;
        collapseSidebar();
    }

    @FXML private void onScrimClicked(MouseEvent e) { collapseSidebar(); }

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

        KeyFrame end = new KeyFrame(ANIM_DURATION,
                new KeyValue(sidebarClip.widthProperty(), show ? EXPANDED_WIDTH : COMPACT_WIDTH, ease),
                new KeyValue(sidebarScrim.opacityProperty(), show ? 1.0 : 0.0, ease));

        KeyFrame fade = new KeyFrame(ANIM_DURATION.multiply(show ? 0.4 : 0.6),
                new KeyValue(expandedSidebar.opacityProperty(), 1.0));

        sidebarAnimation = show
                ? new Timeline(fade, end)
                : new Timeline(fade, end,
                new KeyFrame(ANIM_DURATION, new KeyValue(expandedSidebar.opacityProperty(), 0.0)));

        if (!show) {
            sidebarAnimation.setOnFinished(e -> {
                sidebarScrim.setVisible(false);
                expandedSidebar.setVisible(false);
            });
        }

        sidebarAnimation.play();
    }

    // ================================= DELEGATED PUBLIC API =================================

    public void setCategories(List<String> categories) { posViewController.setCategories(categories); }
    public void setDishes(List<Dishes> dishes) { posViewController.setDishes(dishes); }
    public void setDiscountOptions(List<String> options) { posViewController.setDiscountOptions(options); }

    private boolean isPinRequiredForVoid() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream input = new java.io.FileInputStream("config.properties")) {
            props.load(input);
            return Boolean.parseBoolean(props.getProperty("requirePinForVoid", "false"));
        } catch (java.io.IOException e) {
            return false;
        }
    }

}
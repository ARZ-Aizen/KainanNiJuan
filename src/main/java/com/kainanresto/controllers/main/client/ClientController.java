package com.kainanresto.controllers.main.client;

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
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;

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
import javafx.scene.control.Label;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.Locale;

public class ClientController {

    /* ROOT / LAYERS */
    @FXML private StackPane appRoot;
    @FXML private VBox sidebarRoot;
    @FXML private Region sidebarScrim;
    @FXML private VBox expandedSidebar;

    /* BRANDING (placeholders until loaded from the database) */
    @FXML private ImageView restaurantLogoImage, expandedLogoImage;
    @FXML private Label logoPlaceholderLabel, expandedLogoPlaceholderLabel;
    @FXML private Label expandedRestaurantNameLabel, expandedPortalLabel;

    /* COMPACT NAV */
    @FXML private Button navOrderBtn;
    @FXML private Button navHistoryBtn;
    @FXML private Button logoutBtn;
    @FXML private SVGPath orderNavIcon;
    @FXML private SVGPath historyNavIcon;
    @FXML private SVGPath logoutIcon;

    /* EXPANDED NAV (separate icon instances: a Node can only have one parent) */
    @FXML private Button expNavOrderBtn;
    @FXML private Button expNavHistoryBtn;
    @FXML private Button expLogoutBtn;
    @FXML private SVGPath expOrderIcon, expHistoryIcon, expLogoutIcon;

    // View includes injected directly
    @FXML private HBox posView;
    @FXML private ClientPosController posViewController;

    @FXML private HBox omView;
    @FXML private ClientOrderManagementController omViewController;

    private static final String NAV_ACTIVE = "nav-item-active";
    private static final String EXP_NAV_ACTIVE = "exp-nav-item-active";

    private static final String PLACEHOLDER_NAME = "Restaurant Name";
    private static final String PORTAL_LABEL = "Cashier Portal";

    private static final double COMPACT_WIDTH = 93.0;
    private static final double EXPANDED_WIDTH = 304.0; // 256 content + 2 x 24 padding
    private static final Duration ANIM_DURATION = Duration.millis(260);

    private final Rectangle sidebarClip = new Rectangle(COMPACT_WIDTH, 0);
    private Timeline sidebarAnimation;
    private boolean sidebarExpanded = false;

    private String brandName = PLACEHOLDER_NAME;
    private boolean hasLogo = false;

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // Active cart state
    private final ObservableList<ReceiptLine> currentCart = FXCollections.observableArrayList();
    private BigDecimal currentDiscountRate = BigDecimal.ZERO;
    private int discountCards = 1;
    private String currentDiscountName = "None";
    private ReceiptTotals lastTotals;   // totals currently shown on the receipt panel

    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");            // 12% VAT
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05"); // 5% service charge

    // Discounted dishes are VAT-exempt (PH senior/PWD rule). Set to false to charge VAT on the full net amount instead.
    private static final boolean VAT_EXEMPT_ON_DISCOUNTED = true;

    @FXML
    public void initialize() {
        restaurantLogoImage.setClip(new Circle(34, 34, 34));
        expandedLogoImage.setClip(new Circle(34, 34, 34));

        initializeIcons();
        normalizeIcons();

        // Start collapsed: the expanded layer is clipped to the compact width and invisible
        sidebarClip.heightProperty().bind(expandedSidebar.heightProperty());
        expandedSidebar.setClip(sidebarClip);
        expandedSidebar.setOpacity(0);
        sidebarScrim.setOpacity(0);

        // Placeholder branding until real data is loaded
        expandedPortalLabel.setText(PORTAL_LABEL);
        updateBrandingLabels();

        setupCartHandlers();
        wireOrderPersistence();
        showOrder();
        loadTodaysOrders();

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

    private void setupCartHandlers() {
        // 1. Add dish to order (match on dish id, not name)
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

        // 2. Change quantity via receipt stepper (+ / -)
        posViewController.setOnReceiptQuantityChange((line, newQty) -> {
            if (newQty <= 0) {
                currentCart.remove(line);
            } else {
                updateCartItem(line, newQty);
            }
            refreshReceiptUI();
        });

        // 3. Discount type (None / Senior / PWD)
        posViewController.setOnReceiptDiscountChange(discountName -> {
            currentDiscountName = discountName;
            currentDiscountRate = discountName.contains("20%") ? new BigDecimal("0.20") : BigDecimal.ZERO;
            discountCards = 1;
            refreshReceiptUI();
        });

        // 4. Number of discount cards (1 card = 1 discounted dish)
        posViewController.setOnReceiptDiscountCardsChange(delta -> {
            discountCards += delta;   // clamped in refreshReceiptUI
            refreshReceiptUI();
        });

        // 5. Proceed to payment
        posViewController.setOnProceedToPayment(this::onProceedToPayment);
    }

    // Helper to update an immutable ReceiptLine record
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

    // Calculates discount, taxes and totals, then pushes to the UI
    private void refreshReceiptUI() {
        if (currentCart.isEmpty()) {
            lastTotals = null;
            posViewController.setReceipt(null, null);
            posViewController.setDiscountSummary(false, 1, null);
            return;
        }

        // Expand the cart into individual units, highest price first
        List<BigDecimal> unitPrices = new ArrayList<>();
        for (ReceiptLine line : currentCart) {
            for (int i = 0; i < line.quantity(); i++) unitPrices.add(line.unitPrice());
        }
        unitPrices.sort(Comparator.reverseOrder());

        boolean discountActive = currentDiscountRate.signum() > 0;
        discountCards = Math.max(1, Math.min(discountCards, unitPrices.size())); // can't exceed dishes ordered

        BigDecimal subtotal = unitPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1 card = 1 discounted dish, always the highest-priced remaining one
        BigDecimal discountedBase = BigDecimal.ZERO;
        if (discountActive) {
            for (int i = 0; i < discountCards; i++) discountedBase = discountedBase.add(unitPrices.get(i));
        }
        BigDecimal discountAmount = discountedBase.multiply(currentDiscountRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSubtotal = subtotal.subtract(discountAmount);
        BigDecimal vatBase = (discountActive && VAT_EXEMPT_ON_DISCOUNTED)
                ? subtotal.subtract(discountedBase)   // discounted dishes carry no VAT
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

        // Work out which dishes actually got the discount (same highest-first rule as above)
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

        // 1. Cash popup: amount received + change
        Optional<PaymentResult> payment = PaymentDialog.show(owner, orderNumber, orderType, lastTotals.total());
        if (payment.isEmpty()) return;   // cancelled: the cart stays untouched

        // 2. Build the order (new orders start as PREPARING in Order Management)
        boolean hasDiscount = currentDiscountRate.signum() > 0;
        OrderReceipt receipt = new OrderReceipt(
                orderNumber,
                orderType,
                null,                                   // orderTypeDetail (table no., etc.)
                OrderStatus.PREPARING,
                LocalDateTime.now(),
                currentCashierName(),
                List.copyOf(currentCart),
                hasDiscount ? currentDiscountName : null,
                lastTotals);

        // 3. Save order + items and deduct stock (one DB transaction)
        User user = SessionManager.getCurrentUser();
            Integer cashierId = user == null ? null : user.getUserId();
        if (!orderDAO.saveOrder(receipt, payment.get(), cashierId, currentCashierRole())) {
            AlertUtil.showError("Order not saved",
                    "Could not save the order. A dish may have run out of stock. The payment was NOT recorded.");
            loadPosData();   // refresh stock so the cashier sees what's left
            return;
        }

        // 4. Virtual receipt
        ReceiptDialog.show(owner, receipt, payment.get());

        // 5. Hand the order to Order Management, clear the cart and switch tabs
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
        loadPosData();   // refreshes stock and puts the discount box back on "None"
    }

    private String nextOrderNumber() {
        return orderDAO.generateOrderNumber();
    }

    private String currentCashierName() {
        User user = SessionManager.getCurrentUser();
        return user == null ? "\u2014" : user.getFullName();   // adjust to your getter
    }

    private String currentCashierRole() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() == null) return "Cashier";   // adjust to your getter
        String r = String.valueOf(user.getRole()).toLowerCase();        // "CASHIER" -> "Cashier"
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    // ================================= ORDER PERSISTENCE =================================

    /** Loads today's saved orders into Order Management (so they survive a restart). */
    private void loadTodaysOrders() {
        Thread t = new Thread(() -> {
            List<OrderReceipt> receipts = orderDAO.loadReceipts(LocalDate.now().atStartOfDay());
            Platform.runLater(() -> omViewController.setReceipts(receipts));
        });
        t.setDaemon(true);
        t.start();
    }

    /** Saves status changes made in Order Management. */
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
                loadPosData();   // stock was restored
            }
        });
    }

    private void initializeIcons() {
        setIcon(orderNavIcon, Icons.RECEIPT_TEXT);
        setIcon(historyNavIcon, Icons.CLOCK);
        setIcon(logoutIcon, Icons.NAV_LOGOUT);

        setIcon(expOrderIcon, Icons.RECEIPT_TEXT);
        setIcon(expHistoryIcon, Icons.CLOCK);
        setIcon(expLogoutIcon, Icons.NAV_LOGOUT);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) icon.setContent(content);
    }

    private void normalizeIcons() {
        ClientUIHelper.fitGridIcon(orderNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(historyNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(logoutIcon, 30.0, 2.0);

        // Expanded sidebar uses a 30px icon host per the design
        ClientUIHelper.fitGridIcon(expOrderIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expHistoryIcon, 30.0, 2.0);
        ClientUIHelper.fitGridIcon(expLogoutIcon, 30.0, 2.0);
    }

    @FXML
    private void onNavOrder() { showOrder(); }

    @FXML
    private void onNavHistory() { showOrderManagement(); }

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
        collapseSidebar(); // no-op if already collapsed
    }

    /** Fetches dishes (fresh stock) and the discount options in the background. */
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
        collapseSidebar();
    }


    private void setActiveNav(Button active) {
        Button[] compact  = {navOrderBtn, navHistoryBtn};
        Button[] expanded = {expNavOrderBtn, expNavHistoryBtn};
        for (int i = 0; i < compact.length; i++) {
            compact[i].getStyleClass().remove(NAV_ACTIVE);
            expanded[i].getStyleClass().remove(EXP_NAV_ACTIVE);
            if (compact[i] == active) {
                compact[i].getStyleClass().add(NAV_ACTIVE);
                expanded[i].getStyleClass().add(EXP_NAV_ACTIVE);
            }
        }
    }

    // ================================= DELEGATED PUBLIC API =================================

    public void setRestaurantLogo(String uri) {
        if (uri == null || uri.isBlank()) {
            applyLogo(null);
            return;
        }
        try {
            applyLogo(new Image(uri, 136.0, 136.0, true, true, true));
        } catch (IllegalArgumentException ex) {
            applyLogo(null);
        }
    }

    public void setRestaurantName(String name) {
        brandName = (name == null || name.isBlank()) ? PLACEHOLDER_NAME : name.trim();
        updateBrandingLabels();
    }

    /** Convenience: set both at once once the DB data arrives. */
    public void applyBranding(String name, String logoUri) {
        setRestaurantName(name);
        setRestaurantLogo(logoUri);
    }

    private void applyLogo(Image image) {
        hasLogo = image != null;
        restaurantLogoImage.setImage(image);
        expandedLogoImage.setImage(image);
        if (image != null) {
            ClientUIHelper.applyCoverCrop(restaurantLogoImage, image);
            ClientUIHelper.applyCoverCrop(expandedLogoImage, image);
        }
        updateBrandingLabels();
    }

    /** Name label + the letter shown in the logo circle when there is no logo. */
    private void updateBrandingLabels() {
        expandedRestaurantNameLabel.setText(brandName);
        String initial = brandName.substring(0, 1).toUpperCase(Locale.ENGLISH);
        logoPlaceholderLabel.setText(initial);
        expandedLogoPlaceholderLabel.setText(initial);
        logoPlaceholderLabel.setVisible(!hasLogo);
        expandedLogoPlaceholderLabel.setVisible(!hasLogo);
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

    public void setCategories(List<String> categories) { posViewController.setCategories(categories); }
    public void setDishes(List<Dishes> dishes) { posViewController.setDishes(dishes); }
    public void setDiscountOptions(List<String> options) { posViewController.setDiscountOptions(options); }
}
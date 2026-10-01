package com.kainanresto.controllers.main.client;

import com.kainanresto.dao.ProductDAO;
// import com.kainanresto.dao.OrderDAO; // Uncomment when hooking up Order Management
import com.kainanresto.model.account.User;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.order.OrderCard;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;

// Added missing imports for the sub-controllers
import com.kainanresto.controllers.main.client.pos.ClientPosController;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientController {

    @FXML private BorderPane appRoot;
    @FXML private ImageView restaurantLogoImage;
    @FXML private Button navOrderBtn;
    @FXML private Button navHistoryBtn;
    @FXML private Button logoutBtn;
    @FXML private SVGPath orderNavIcon;
    @FXML private SVGPath historyNavIcon;
    @FXML private SVGPath logoutIcon;

    // View includes injected directly
    @FXML private HBox posView;
    @FXML private ClientPosController posViewController;

    @FXML private HBox omView;
    @FXML private ClientOrderManagementController omViewController;

    private static final String NAV_ACTIVE = "nav-item-active";

    // Instantiate your DAOs here
    private final ProductDAO productDAO = new ProductDAO();
    // private final OrderDAO orderDAO = new OrderDAO();

    // Active Cart State
    private final ObservableList<ReceiptLine> currentCart = FXCollections.observableArrayList();
    private BigDecimal currentDiscountRate = BigDecimal.ZERO;
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12"); // 12% VAT
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05"); // 5% Service Charge

    @FXML
    public void initialize() {
        restaurantLogoImage.setClip(new Circle(34, 34, 34));

        initializeIcons();
        normalizeIcons();

        setupCartHandlers();

        // This will now trigger the database load when the application starts
        showOrder();

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

    private void setupCartHandlers() {
        // 1. Add Dish to Order
        posViewController.setOnAddToOrder(dish -> {
            Optional<ReceiptLine> existingLine = currentCart.stream()
                    .filter(line -> line.name().equals(dish.name()))
                    .findFirst();

            if (existingLine.isPresent()) {
                // Increment existing item
                ReceiptLine line = existingLine.get();
                updateCartItem(line, line.quantity() + 1);
            } else {
                // Add new item (name, imageUrl, unitPrice, quantity)
                ReceiptLine newLine = new ReceiptLine(
                        dish.name(),
                        dish.imageUrl(),
                        dish.price(),
                        1
                );
                currentCart.add(newLine);
            }
            refreshReceiptUI();
        });

        // 2. Change Quantity via Receipt Stepper (+ / -)
        posViewController.setOnReceiptQuantityChange((line, newQty) -> {
            if (newQty <= 0) {
                currentCart.remove(line);
            } else {
                updateCartItem(line, newQty);
            }
            refreshReceiptUI();
        });

        posViewController.setOnReceiptDiscountChange(discountName -> {
            if (discountName.contains("20%")) {
                currentDiscountRate = new BigDecimal("0.20");
            } else {
                currentDiscountRate = BigDecimal.ZERO;
            }
            refreshReceiptUI();
        });

        posViewController.setOnReceiptOrderTypeChange(type -> {
            System.out.println("Order Type changed to: " + type);
        });
    }

    // Helper to update an immutable ReceiptLine record
    private void updateCartItem(ReceiptLine oldLine, int newQty) {
        int index = currentCart.indexOf(oldLine);
        if (index >= 0) {
            // Rebuild with the correct parameter order
            ReceiptLine updatedLine = new ReceiptLine(
                    oldLine.name(),
                    oldLine.imageUrl(),
                    oldLine.unitPrice(),
                    newQty
            );
            currentCart.set(index, updatedLine);
        }
    }

    // Calculates taxes and totals, then pushes to the UI
    private void refreshReceiptUI() {
        if (currentCart.isEmpty()) {
            posViewController.setReceipt(null, null);
            return;
        }

        BigDecimal subtotal = currentCart.stream()
                .map(ReceiptLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = subtotal.multiply(currentDiscountRate);
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);

        BigDecimal vatAmount = discountedSubtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceCharge = discountedSubtotal.multiply(SERVICE_CHARGE_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = discountedSubtotal.add(vatAmount).add(serviceCharge);

        ReceiptTotals totals = new ReceiptTotals(
                subtotal,
                SERVICE_CHARGE_RATE.multiply(new BigDecimal("100")),
                serviceCharge,
                VAT_RATE.multiply(new BigDecimal("100")),
                vatAmount,
                total
        );

        posViewController.setReceipt(new ArrayList<>(currentCart), totals);
    }

    private void initializeIcons() {
        setIcon(orderNavIcon, Icons.RECEIPT_TEXT);
        setIcon(historyNavIcon, Icons.CLOCK);
        setIcon(logoutIcon, Icons.NAV_LOGOUT);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) icon.setContent(content);
    }

    private void normalizeIcons() {
        ClientUIHelper.fitGridIcon(orderNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(historyNavIcon, 34.0, 2.0);
        ClientUIHelper.fitGridIcon(logoutIcon, 30.0, 2.0);
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

        // Fetch Data Asynchronously from ProductDAO
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
        for (Button btn : new Button[]{navOrderBtn, navHistoryBtn}) btn.getStyleClass().remove(NAV_ACTIVE);
        if (!active.getStyleClass().contains(NAV_ACTIVE)) active.getStyleClass().add(NAV_ACTIVE);
    }

    // ================================= DELEGATED PUBLIC API =================================

    public void setRestaurantLogo(String uri) {
        if (uri == null || uri.isBlank()) {
            restaurantLogoImage.setImage(null);
            return;
        }
        try {
            Image image = new Image(uri, 136.0, 136.0, true, true, true);
            restaurantLogoImage.setImage(image);
            ClientUIHelper.applyCoverCrop(restaurantLogoImage, image);
        } catch (IllegalArgumentException ex) {
            restaurantLogoImage.setImage(null);
        }
    }

    public void setCategories(List<String> categories) { posViewController.setCategories(categories); }
    public void setDishes(List<Dishes> dishes) { posViewController.setDishes(dishes); }
    public void setDiscountOptions(List<String> options) { posViewController.setDiscountOptions(options); }
}
package com.kainanresto.controllers.main;

import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.util.Icons;
import com.kainanresto.model.order.OrderCard;
import com.kainanresto.model.order.OrderLine;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientController {

    /* ============================== SIDEBAR ============================== */
    @FXML private BorderPane appRoot;
    @FXML private VBox sidebarRoot;
    @FXML private ImageView restaurantLogoImage;

    @FXML private Button navOrderBtn;
    @FXML private Button navHistoryBtn;      // opens Order Management
    @FXML private Button logoutBtn;

    @FXML private SVGPath orderNavIcon;
    @FXML private SVGPath historyNavIcon;
    @FXML private SVGPath logoutIcon;

    private static final String NAV_ACTIVE = "nav-item-active";

    /* ============================== VIEW SWITCHING ============================== */
    @FXML private StackPane viewStack;
    @FXML private HBox posView;              // left column (dishes) + receipt panel
    @FXML private HBox omView;               // left column (orders) + receipt panel

    /* ============================== ORDER POS ============================== */
    // The "KAINAN NI JUAN" title is static text in the FXML (no fx:id, no setter).
    @FXML private Label currentDateLabel;

    @FXML private HBox posSearchBox;
    @FXML private SVGPath posSearchIcon;
    @FXML private TextField posSearchField;

    @FXML private FlowPane posCategoryChips;
    @FXML private ScrollPane posScroll;
    @FXML private Label posGridPlaceholder;
    @FXML private FlowPane posDishGrid;

    private static final double ICON_GRID = 24.0;       // Lucide grid
    private static final double ICON_STROKE_PX = 2.0;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");

    // Card geometry taken from the mockup
    private static final double DISH_IMAGE_SIZE = 144.66;
    private static final double CARD_WIDTH = 184.47;
    private static final double CARD_HEIGHT = 190.34;
    private static final double IMAGE_OVERHANG = 60.69;   // image sticks out above the card
    private static final double NAME_HEIGHT = 54.0;       // two lines of 27px

    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private List<String> suppliedCategories = new ArrayList<>();   // from setCategories; empty = derive from dishes
    private String selectedCategory = null;   // null = "All"
    private boolean rebuildingChips = false;
    private Consumer<Dishes> onAddToOrder;

    /* ============================== RECEIPT / CURRENT ORDER (receipt*) ============================== */
    @FXML private VBox receiptPanel;
    @FXML private ToggleGroup receiptTypeGroup;
    @FXML private Label receiptSubtitleLabel;
    @FXML private VBox receiptLines;
    @FXML private Label receiptSubtotalLabel;
    @FXML private ComboBox<String> receiptDiscountBox;
    @FXML private Label receiptServiceLabel;
    @FXML private Label receiptServiceValueLabel;
    @FXML private Label receiptVatLabel;
    @FXML private Label receiptVatValueLabel;
    @FXML private Label receiptTotalLabel;
    @FXML private Button receiptPayBtn;

    private static final double RECEIPT_THUMB = 54.0;

    private String receiptTypeDetail = null;
    private boolean rebuildingDiscount = false;
    private BiConsumer<ReceiptLine, Integer> onReceiptQuantityChange;
    private Consumer<String> onReceiptOrderTypeChange;
    private Consumer<String> onReceiptDiscountChange;
    private Runnable proceedToPaymentHandler;

    /* ============================== ORDER MANAGEMENT (om*) ============================== */
    @FXML private Label omSubtitleLabel;
    @FXML private HBox omSearchBox;
    @FXML private SVGPath   omSearchIcon;
    @FXML private TextField omSearchField;
    @FXML private FlowPane omStatusChips;
    @FXML private ScrollPane omScroll;
    @FXML private Label omGridPlaceholder;
    @FXML private GridPane omGrid;

    /** Static page chrome: this page's subtitle (each page sets its own in its showX()). */
    private static final String OM_SUBTITLE = "Monitor and manage current restaurant orders.";

    private static final int OM_COLUMNS = 3;
    private static final int OM_COLUMNS_WITH_PANEL = 2;
    private static final PseudoClass OM_SELECTED = PseudoClass.getPseudoClass("om-selected");
    private static final DateTimeFormatter OM_DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final ToggleGroup omStatusGroup = new ToggleGroup();
    private final ObservableList<OrderCard> omOrders = FXCollections.observableArrayList();
    private OrderStatus omSelectedStatus = null;    // null = "All"
    private String omSelectedOrderNo = null;
    private Consumer<OrderCard> onCompleteOrder;
    private Consumer<OrderCard> onCancelOrder;
    private Consumer<OrderCard> onOrderDetails;
    private Consumer<OrderCard> onOrderSelected;

    /* ---- Order Management receipt (omr*) ---- */
    @FXML private VBox omReceiptPanel;
    @FXML private Label omrOrderTitleLabel;
    @FXML private Label omrStatusLabel;
    @FXML private Label omrTypeLabel;
    @FXML private Label omrTimeLabel;
    @FXML private Label omrCashierLabel;
    @FXML private VBox omrLines;
    @FXML private Label omrSubtotalLabel;
    @FXML private Label omrDiscountLabel;
    @FXML private Label omrServiceLabel;
    @FXML private Label omrServiceValueLabel;
    @FXML private Label omrVatLabel;
    @FXML private Label omrVatValueLabel;
    @FXML private Label omrTotalLabel;
    @FXML private Button omrCompleteBtn;
    @FXML private Button omrCancelBtn;

    private OrderReceipt omReceiptDetails = null;   // detail for the selected order, once the backend supplies it

    /* ============================== LIFECYCLE ============================== */

    @FXML
    public void initialize() {
        currentDateLabel.setText(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));

        // Circular logo (JavaFX CSS has no overflow:hidden)
        restaurantLogoImage.setClip(new Circle(34, 34, 34));

        setupPosPage();
        setupReceiptPanel();
        setupOrderManagementPage();
        initializeIcons();
        normalizeIcons();
        showOrder();

        Platform.runLater(() -> {
            if (appRoot.getScene() == null) {
                return;
            }
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

    /* ============================== ICONS ============================== */

    private void initializeIcons() {
        setIcon(orderNavIcon, Icons.RECEIPT_TEXT);
        setIcon(historyNavIcon, Icons.CLOCK);
        setIcon(logoutIcon, Icons.NAV_LOGOUT);
        setIcon(posSearchIcon, Icons.SEARCH);
        setIcon(omSearchIcon, Icons.SEARCH);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) {
            icon.setContent(content);
        }
    }

    private void normalizeIcons() {
        fitGridIcon(orderNavIcon, 34.0);
        fitGridIcon(historyNavIcon, 34.0);
        fitGridIcon(logoutIcon, 30.0);
        fitGridIcon(posSearchIcon, 20.0);
        fitGridIcon(omSearchIcon, 20.0);
    }

    private static void fitGridIcon(SVGPath icon, double targetSize) {
        fitGridIcon(icon, targetSize, ICON_STROKE_PX);
    }

    /** Scales a 24-grid stroke icon to the host size and keeps the on-screen stroke at strokePx. */
    private static void fitGridIcon(SVGPath icon, double targetSize, double strokePx) {
        if (icon == null) {
            return;
        }
        double scale = targetSize / ICON_GRID;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (strokePx / scale) + ";");
    }

    /* ============================== DATA ENTRY POINTS ============================== */
    // All of these must be called on the JavaFX Application Thread.

    /** Sidebar logo from any URI Image can load (file:, http:, ...). Null clears it. */
    public void setRestaurantLogo(String uri) {
        if (uri == null || uri.isBlank()) {
            restaurantLogoImage.setImage(null);
            return;
        }
        try {
            Image image = new Image(uri, 136.0, 136.0, true, true, true);
            restaurantLogoImage.setImage(image);
            applyCoverCrop(restaurantLogoImage, image);
        } catch (IllegalArgumentException ex) {
            restaurantLogoImage.setImage(null);
        }
    }

    /**
     * POS: category names from the server/database. "All" is added automatically.
     * If the list is null/empty, the chips are derived from the loaded dishes' categories instead.
     */
    public void setCategories(List<String> categories) {
        suppliedCategories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        rebuildCategoryChips();
    }

    /** POS: dish cards from the server/database. */
    public void setDishes(List<Dishes> dishes) {
        dishItems.setAll(dishes == null ? List.<Dishes>of() : dishes);
        if (suppliedCategories.isEmpty()) {
            rebuildCategoryChips();   // categories come from the dishes
        } else {
            applyPosFilter();
        }
    }

    /** POS: called when the user presses the "+" button on an available dish. */
    public void setOnAddToOrder(Consumer<Dishes> handler) {
        this.onAddToOrder = handler;
    }

    /**
     * Receipt: the current order. A non-empty list shows the panel (dish grid reflows);
     * null or empty hides it. Null totals show dashes.
     */
    public void setReceipt(List<ReceiptLine> lines, ReceiptTotals totals) {
        List<Node> rows = new ArrayList<>();
        if (lines != null) {
            for (ReceiptLine line : lines) {
                if (line != null) {
                    rows.add(createReceiptRow(line));
                }
            }
        }
        receiptLines.getChildren().setAll(rows);

        if (totals == null) {
            receiptSubtotalLabel.setText("\u2014");
            receiptServiceLabel.setText("Service charge");
            receiptServiceValueLabel.setText("\u2014");
            receiptVatLabel.setText("VAT");
            receiptVatValueLabel.setText("\u2014");
            receiptTotalLabel.setText("\u2014");
        } else {
            receiptSubtotalLabel.setText(formatPeso(totals.subtotal()));
            receiptServiceLabel.setText("Service charge" + rateSuffix(totals.serviceChargeRate()));
            receiptServiceValueLabel.setText(formatPeso(totals.serviceCharge()));
            receiptVatLabel.setText("VAT" + rateSuffix(totals.vatRate()));
            receiptVatValueLabel.setText(formatPeso(totals.vat()));
            receiptTotalLabel.setText(formatPeso(totals.total()));
        }

        boolean show = !rows.isEmpty();
        receiptPanel.setVisible(show);
        receiptPanel.setManaged(show);
    }

    /** Receipt: the part after the bullet in the subtitle ("Counter" in "Dine in \u2022 Counter"). Null removes it. */
    public void setReceiptOrderTypeDetail(String detail) {
        this.receiptTypeDetail = detail;
        updateReceiptSubtitle();
    }

    /** Receipt: discount names from the server/database. The first one is selected by default. */
    public void setDiscountOptions(List<String> options) {
        rebuildingDiscount = true;
        receiptDiscountBox.getItems().setAll(options == null ? List.<String>of() : options);
        if (receiptDiscountBox.getItems().isEmpty()) {
            receiptDiscountBox.getSelectionModel().clearSelection();
        } else {
            receiptDiscountBox.getSelectionModel().selectFirst();
        }
        rebuildingDiscount = false;
    }

    /** Receipt: called with the requested new quantity when the user presses \u2212 or +. The handler decides what to do. */
    public void setOnReceiptQuantityChange(BiConsumer<ReceiptLine, Integer> handler) {
        this.onReceiptQuantityChange = handler;
    }

    /** Receipt: called with "Dine in", "To Go" or "Delivery" when the user picks an order type. */
    public void setOnReceiptOrderTypeChange(Consumer<String> handler) {
        this.onReceiptOrderTypeChange = handler;
    }

    /** Receipt: called with the chosen discount name. */
    public void setOnReceiptDiscountChange(Consumer<String> handler) {
        this.onReceiptDiscountChange = handler;
    }

    /** Receipt: called when the user presses "PROCEED TO PAYMENT". */
    public void setOnProceedToPayment(Runnable handler) {
        this.proceedToPaymentHandler = handler;
    }

    /** Order Management: order cards from the server/database. Chip counts and the open receipt update automatically. */
    public void setOrders(List<OrderCard> orders) {
        omOrders.setAll(orders == null ? List.<OrderCard>of() : orders);
        updateOmChipCounts();
        updateOmReceiptPanel();   // closes the panel if the selected order is gone
        applyOmFilter();
    }

    /**
     * Order Management receipt: full detail for an order. Ignored unless it is the order currently
     * selected, so a late response for a previously selected card can't overwrite the panel.
     */
    public void setOrderReceipt(OrderReceipt receipt) {
        if (receipt == null || !Objects.equals(receipt.orderNumber(), omSelectedOrderNo)) {
            return;
        }
        omReceiptDetails = receipt;
        updateOmReceiptPanel();
    }

    /** Order Management: called when the user selects a card (load its detail, then call setOrderReceipt). */
    public void setOnOrderSelected(Consumer<OrderCard> handler) {
        this.onOrderSelected = handler;
    }

    /** Order Management: called when the user presses "Complete Order" (Preparing orders only), card or receipt. */
    public void setOnCompleteOrder(Consumer<OrderCard> handler) {
        this.onCompleteOrder = handler;
    }

    /** Order Management: called when the user presses "CANCEL ORDER" in the receipt (Preparing orders only). */
    public void setOnCancelOrder(Consumer<OrderCard> handler) {
        this.onCancelOrder = handler;
    }

    /** Order Management: called when the user presses "Details". */
    public void setOnOrderDetails(Consumer<OrderCard> handler) {
        this.onOrderDetails = handler;
    }

    /* ============================== NAVIGATION ============================== */

    @FXML
    private void onNavOrder() {
        showOrder();
    }

    @FXML
    private void onNavHistory() {
        showOrderManagement();
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

    /** Shows exactly one page in the view stack. Add every new page to this array. */
    private void showView(Node target) {
        for (Node v : new Node[]{posView, omView}) {
            if (v == null) {
                continue;
            }
            boolean on = (v == target);
            v.setVisible(on);
            v.setManaged(on);
        }
    }

    private void showOrder() {
        showView(posView);
        setActiveNav(navOrderBtn);
        // POS subtitle is the current-date label (set in initialize()).
        // TODO: request restaurant logo, categories, dishes and discount options from the
        //       server/database, then call setRestaurantLogo / setCategories /
        //       setDishes / setDiscountOptions.
    }

    private void showOrderManagement() {
        showView(omView);
        setActiveNav(navHistoryBtn);
        omSubtitleLabel.setText(OM_SUBTITLE);
        // TODO: request current orders from the server/database, then call setOrders(...).
    }

    private void setActiveNav(Button active) {
        for (Button btn : new Button[]{navOrderBtn, navHistoryBtn}) {
            btn.getStyleClass().remove(NAV_ACTIVE);
        }
        if (!active.getStyleClass().contains(NAV_ACTIVE)) {
            active.getStyleClass().add(NAV_ACTIVE);
        }
    }

    /* ============================== ORDER POS ============================== */

    private void setupPosPage() {
        // :focus-within isn't supported in JavaFX CSS, so mirror focus onto the wrapper box.
        posSearchField.focusedProperty().addListener((obs, was, is) ->
                posSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        posSearchField.textProperty().addListener((obs, oldText, newText) -> applyPosFilter());

        // Chips behave like radio buttons: one is always selected.
        categoryToggleGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (rebuildingChips) {
                return;
            }
            if (now == null && old != null) {
                categoryToggleGroup.selectToggle(old);
                return;
            }
            selectedCategory = (now == null) ? null : (String) now.getUserData();
            applyPosFilter();
        });

        setCategories(null);   // shows only "All" and the empty placeholder until data arrives
    }

    /**
     * Rebuilds the chips: "All" + the supplied categories, or, when none were supplied,
     * the distinct categories of the loaded dishes. Keeps the current selection if it still exists.
     */
    private void rebuildCategoryChips() {
        List<String> names = new ArrayList<>();
        if (!suppliedCategories.isEmpty()) {
            names.addAll(suppliedCategories);
        } else {
            LinkedHashSet<String> derived = new LinkedHashSet<>();
            for (Dishes dish : dishItems) {
                String c = dish.category();
                if (c != null && !c.isBlank()) {
                    derived.add(c);
                }
            }
            names.addAll(derived);
        }

        String keep = selectedCategory;
        rebuildingChips = true;
        categoryToggleGroup.getToggles().clear();
        posCategoryChips.getChildren().clear();

        ToggleButton all = createCategoryChip("All", null);
        posCategoryChips.getChildren().add(all);
        ToggleButton toSelect = all;
        for (String c : names) {
            if (c == null) {
                continue;
            }
            ToggleButton chip = createCategoryChip(c, c);
            posCategoryChips.getChildren().add(chip);
            if (keep != null && keep.equalsIgnoreCase(c)) {
                toSelect = chip;
            }
        }
        rebuildingChips = false;

        selectedCategory = (String) toSelect.getUserData();
        toSelect.setSelected(true);
        applyPosFilter();
    }

    private ToggleButton createCategoryChip(String label, String category) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(category);
        chip.setToggleGroup(categoryToggleGroup);
        chip.getStyleClass().add("pos-category-chip");
        return chip;
    }

    private void applyPosFilter() {
        String query = posSearchField.getText() == null ? ""
                : posSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        List<Node> cards = new ArrayList<>();
        for (Dishes dish : dishItems) {
            boolean categoryOk = selectedCategory == null
                    || selectedCategory.equalsIgnoreCase(dish.category());
            boolean textOk = query.isEmpty()
                    || (dish.name() != null && dish.name().toLowerCase(Locale.ENGLISH).contains(query));
            if (categoryOk && textOk) {
                cards.add(createDishCard(dish));
            }
        }
        posDishGrid.getChildren().setAll(cards);

        boolean empty = cards.isEmpty();
        posGridPlaceholder.setText(dishItems.isEmpty()
                ? "No dishes loaded."
                : "No dishes match your filters.");
        posGridPlaceholder.setVisible(empty);
        posGridPlaceholder.setManaged(empty);
    }

    private Node createDishCard(Dishes dish) {
        // Image shell carries the shadow; the image itself is a plain square, as in the mockup.
        StackPane imageShell = new StackPane();
        imageShell.getStyleClass().add("pos-dish-image-shell");
        fixSize(imageShell, DISH_IMAGE_SIZE, DISH_IMAGE_SIZE);
        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            try {
                Image image = new Image(dish.imageUrl(),
                        DISH_IMAGE_SIZE * 2, DISH_IMAGE_SIZE * 2, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(DISH_IMAGE_SIZE);
                imageView.setFitHeight(DISH_IMAGE_SIZE);
                imageView.setPreserveRatio(true);
                applyCoverCrop(imageView, image);
                imageShell.getChildren().add(imageView);
            } catch (IllegalArgumentException ex) {
                // bad URL: leave the shell empty
            }
        }

        Label name = new Label(dish.name() == null ? "\u2014" : dish.name());
        name.getStyleClass().add("pos-dish-name");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setAlignment(Pos.TOP_CENTER);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setMinHeight(NAME_HEIGHT);
        name.setPrefHeight(NAME_HEIGHT);
        name.setMaxHeight(NAME_HEIGHT);

        Label price = new Label(formatPeso(dish.price()));
        price.getStyleClass().add("pos-dish-price");

        Button addBtn = createAddButton(dish);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox priceRow = new HBox(price, spacer, addBtn);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(name, priceRow);
        card.getStyleClass().add("pos-dish-card");
        fixSize(card, CARD_WIDTH, CARD_HEIGHT);

        // The image overhangs the top of the card.
        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(IMAGE_OVERHANG, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        fixSize(wrapper, CARD_WIDTH, CARD_HEIGHT + IMAGE_OVERHANG);
        return wrapper;
    }

    private Button createAddButton(Dishes dish) {
        SVGPath plus = new SVGPath();
        plus.setContent(Icons.PLUS);
        plus.getStyleClass().add("phosphor-icon");
        fitGridIcon(plus, 8.0, 1.5);

        StackPane host = new StackPane(plus);
        host.getStyleClass().add("pos-host-8");

        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("pos-add-btn");
        b.setTooltip(new Tooltip("Add to order"));
        b.setDisable(!dish.available());
        b.setOnAction(e -> {
            if (onAddToOrder != null) {
                onAddToOrder.accept(dish);
            }
        });
        return b;
    }

    /* ============================== RECEIPT / CURRENT ORDER ============================== */

    private void setupReceiptPanel() {
        // Order-type chips behave like radio buttons: one is always selected.
        receiptTypeGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                receiptTypeGroup.selectToggle(old);
                return;
            }
            updateReceiptSubtitle();
            if (now != null && onReceiptOrderTypeChange != null) {
                onReceiptOrderTypeChange.accept(((ToggleButton) now).getText());
            }
        });

        receiptDiscountBox.valueProperty().addListener((obs, old, now) -> {
            if (!rebuildingDiscount && now != null && onReceiptDiscountChange != null) {
                onReceiptDiscountChange.accept(now);
            }
        });

        updateReceiptSubtitle();
        setReceipt(null, null);   // panel stays hidden until an order has items
    }

    private void updateReceiptSubtitle() {
        Toggle selected = receiptTypeGroup.getSelectedToggle();
        String type = selected == null ? "\u2014" : ((ToggleButton) selected).getText();
        boolean hasDetail = receiptTypeDetail != null && !receiptTypeDetail.isBlank();
        receiptSubtitleLabel.setText(hasDetail ? type + " \u2022 " + receiptTypeDetail.trim() : type);
    }

    private Node createReceiptRow(ReceiptLine line) {
        // circular thumbnail
        StackPane thumb = new StackPane();
        fixSize(thumb, RECEIPT_THUMB, RECEIPT_THUMB);
        thumb.setClip(new Circle(RECEIPT_THUMB / 2.0, RECEIPT_THUMB / 2.0, RECEIPT_THUMB / 2.0));
        if (line.imageUrl() != null && !line.imageUrl().isBlank()) {
            try {
                Image image = new Image(line.imageUrl(),
                        RECEIPT_THUMB * 2, RECEIPT_THUMB * 2, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(RECEIPT_THUMB);
                imageView.setFitHeight(RECEIPT_THUMB);
                imageView.setPreserveRatio(true);
                applyCoverCrop(imageView, image);
                thumb.getChildren().add(imageView);
            } catch (IllegalArgumentException ex) {
                // bad URL: leave the thumbnail empty
            }
        }

        // name + unit price
        Label name = new Label(valueOrDash(line.name()));
        name.getStyleClass().add("receipt-line-name");
        name.setWrapText(true);
        Label unit = new Label(formatPeso(line.unitPrice()));
        unit.getStyleClass().add("receipt-line-unit");
        VBox info = new VBox(2.0, name, unit);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        // quantity stepper
        Button minus = createStepButton(Icons.MINUS, "Decrease quantity",
                () -> requestQuantity(line, line.quantity() - 1));
        Label qty = new Label(String.valueOf(line.quantity()));
        qty.getStyleClass().add("receipt-qty");
        Button plus = createStepButton(Icons.PLUS, "Increase quantity",
                () -> requestQuantity(line, line.quantity() + 1));
        HBox stepper = new HBox(6.0, minus, qty, plus);
        stepper.setAlignment(Pos.CENTER);
        stepper.getStyleClass().add("receipt-col-qty");

        // line price (computed)
        Label price = new Label(formatPeso(line.lineTotal()));
        price.getStyleClass().addAll("receipt-line-price", "receipt-col-price");

        HBox row = new HBox(10.0, thumb, info, stepper, price);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button createStepButton(String iconContent, String tooltip, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconContent);
        icon.getStyleClass().add("phosphor-icon");
        fitGridIcon(icon, 8.0, 1.5);

        StackPane host = new StackPane(icon);
        host.getStyleClass().add("pos-host-8");

        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("receipt-step-btn");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private void requestQuantity(ReceiptLine line, int newQuantity) {
        if (onReceiptQuantityChange != null) {
            onReceiptQuantityChange.accept(line, newQuantity);
        }
    }

    @FXML
    private void onProceedToPayment() {
        if (proceedToPaymentHandler != null) {
            proceedToPaymentHandler.run();
        }
    }

    /** " (5%)" from a percentage rate; empty when the rate is unknown. */
    private static String rateSuffix(BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }

    /* ============================== ORDER MANAGEMENT ============================== */

    private void setupOrderManagementPage() {
        omSubtitleLabel.setText(OM_SUBTITLE);

        // :focus-within isn't supported in JavaFX CSS, so mirror focus onto the wrapper box.
        omSearchField.focusedProperty().addListener((obs, was, is) ->
                omSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        omSearchField.textProperty().addListener((obs, oldText, newText) -> applyOmFilter());

        // Status chips: "All" plus one per OrderStatus; they behave like radio buttons.
        ToggleButton all = createOmChip("All", null);
        omStatusChips.getChildren().add(all);
        for (OrderStatus s : OrderStatus.values()) {
            omStatusChips.getChildren().add(createOmChip(s.getDisplayName(), s));
        }
        all.setSelected(true);
        omStatusGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                omStatusGroup.selectToggle(old);
                return;
            }
            omSelectedStatus = (now == null) ? null : (OrderStatus) now.getUserData();
            applyOmFilter();
        });

        // "10 mins ago" labels go stale, so rebuild the cards and the open receipt once a minute.
        Timeline refresh = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            updateOmReceiptPanel();
            applyOmFilter();
        }));
        refresh.setCycleCount(Animation.INDEFINITE);
        refresh.play();
        appRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                refresh.stop();
            }
        });

        updateOmChipCounts();
        updateOmReceiptPanel();   // hidden until a card is selected
        applyOmFilter();          // shows the empty placeholder until data arrives
    }

    private ToggleButton createOmChip(String label, OrderStatus status) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(status);
        chip.getProperties().put("label", label);
        chip.setToggleGroup(omStatusGroup);
        chip.getStyleClass().add("om-status-chip");
        return chip;
    }

    /** Chip text becomes "Label (count)", counted from the loaded orders. */
    private void updateOmChipCounts() {
        for (Node n : omStatusChips.getChildren()) {
            if (!(n instanceof ToggleButton chip)) {
                continue;
            }
            OrderStatus status = (OrderStatus) chip.getUserData();
            long count = status == null
                    ? omOrders.size()
                    : omOrders.stream().filter(o -> o.status() == status).count();
            chip.setText(chip.getProperties().get("label") + " (" + count + ")");
        }
    }

    /** 3 columns normally, 2 while the receipt panel is open. */
    private int currentOmColumns() {
        return omReceiptPanel.isVisible() ? OM_COLUMNS_WITH_PANEL : OM_COLUMNS;
    }

    private void applyOmColumns(int columns) {
        omGrid.getColumnConstraints().clear();
        double percent = 100.0 / columns;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percent);
            cc.setHgrow(Priority.ALWAYS);
            omGrid.getColumnConstraints().add(cc);
        }
    }

    private void applyOmFilter() {
        String query = omSearchField.getText() == null ? ""
                : omSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        int columns = currentOmColumns();
        applyOmColumns(columns);

        omGrid.getChildren().clear();
        int index = 0;
        for (OrderCard order : omOrders) {
            boolean statusOk = omSelectedStatus == null || order.status() == omSelectedStatus;
            if (!statusOk || !matchesOmText(order, query)) {
                continue;
            }
            Node card = createOrderCard(order);
            GridPane.setValignment(card, VPos.TOP);
            omGrid.add(card, index % columns, index / columns);
            index++;
        }

        boolean empty = index == 0;
        omGridPlaceholder.setText(omOrders.isEmpty()
                ? "No orders loaded."
                : "No orders match your filters.");
        omGridPlaceholder.setVisible(empty);
        omGridPlaceholder.setManaged(empty);
    }

    private static boolean matchesOmText(OrderCard order, String query) {
        if (query.isEmpty()) {
            return true;
        }
        if (containsLower(order.orderNumber(), query)
                || containsLower(orderTypeLine(order), query)) {
            return true;
        }
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) {
                if (line != null && containsLower(line.name(), query)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsLower(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(lowerQuery);
    }

    private Node createOrderCard(OrderCard order) {
        // header: title + type line, status badge on the right
        Label title = new Label("Order #" + valueOrDash(order.orderNumber()));
        title.getStyleClass().add("om-order-title");
        Label type = new Label(orderTypeLine(order));
        type.getStyleClass().add("om-order-type");
        VBox titleBox = new VBox(2.0, title, type);

        Label badge = new Label(order.status() == null ? "\u2014" : order.status().getDisplayName());
        badge.getStyleClass().addAll("om-badge", badgeClass(order.status()));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(titleBox, headerSpacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        // meta: relative time, item count
        Label time = new Label(formatRelativeTime(order.createdAt()));
        time.getStyleClass().add("om-order-time");
        int items = countItems(order);
        Label count = new Label(items + (items == 1 ? " Item" : " Items"));
        count.getStyleClass().add("om-order-count");
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        HBox meta = new HBox(time, metaSpacer, count);
        meta.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("om-divider");

        // item lines
        VBox lines = new VBox(6.0);
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) {
                if (line == null) {
                    continue;
                }
                Label name = new Label(valueOrDash(line.name()));
                name.getStyleClass().add("om-line-name");
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, Priority.ALWAYS);
                Label qty = new Label("\u00D7" + line.quantity());
                qty.getStyleClass().add("om-line-qty");
                HBox row = new HBox(name, qty);
                row.setAlignment(Pos.TOP_LEFT);
                lines.getChildren().add(row);
            }
        }

        // total
        Label totalLabel = new Label("Total Amount");
        totalLabel.getStyleClass().add("om-total-label");
        Label totalValue = new Label(formatPeso(order.total()));
        totalValue.getStyleClass().add("om-total-value");
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        HBox total = new HBox(totalLabel, totalSpacer, totalValue);
        total.setAlignment(Pos.CENTER_LEFT);

        // actions
        HBox actions = new HBox(8.0);
        if (order.status() == OrderStatus.PREPARING) {
            Button complete = createOmButton("Complete Order", "om-btn-primary", () -> {
                if (onCompleteOrder != null) {
                    onCompleteOrder.accept(order);
                }
            });
            complete.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(complete, Priority.ALWAYS);
            actions.getChildren().add(complete);
        }
        actions.getChildren().add(createOmButton("Details", "om-btn-outline", () -> {
            if (onOrderDetails != null) {
                onOrderDetails.accept(order);
            }
        }));

        VBox card = new VBox(header, meta, divider, lines, total, actions);
        card.getStyleClass().add("om-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setUserData(order.orderNumber());
        card.pseudoClassStateChanged(OM_SELECTED, Objects.equals(omSelectedOrderNo, order.orderNumber()));
        card.setOnMouseClicked(e -> selectOrder(order.orderNumber()));
        return card;
    }

    private Button createOmButton(String text, String variantClass, Runnable action) {
        Button b = new Button(text);
        b.setMnemonicParsing(false);
        b.getStyleClass().addAll("om-btn", variantClass);
        b.setOnAction(e -> action.run());
        // keep a click on a button from also selecting the card underneath
        b.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        return b;
    }

    /**
     * Clicking a card selects it and opens the receipt panel (cards reflow to 2 columns);
     * clicking the selected card again clears the selection and closes the panel.
     */
    private void selectOrder(String orderNumber) {
        boolean wasOpen = omReceiptPanel.isVisible();
        String previous = omSelectedOrderNo;
        omSelectedOrderNo = Objects.equals(omSelectedOrderNo, orderNumber) ? null : orderNumber;
        if (!Objects.equals(previous, omSelectedOrderNo)) {
            omReceiptDetails = null;   // detail belongs to the previous order
        }
        updateOmReceiptPanel();

        if (wasOpen != omReceiptPanel.isVisible()) {
            applyOmFilter();           // column count changed: re-layout the cards
        } else {
            for (Node n : omGrid.getChildren()) {
                n.pseudoClassStateChanged(OM_SELECTED, Objects.equals(omSelectedOrderNo, n.getUserData()));
            }
        }

        if (omSelectedOrderNo != null && onOrderSelected != null) {
            OrderCard order = findOmOrder(omSelectedOrderNo);
            if (order != null) {
                onOrderSelected.accept(order);
            }
        }
    }

    private OrderCard findOmOrder(String orderNumber) {
        if (orderNumber == null) {
            return null;
        }
        for (OrderCard o : omOrders) {
            if (Objects.equals(o.orderNumber(), orderNumber)) {
                return o;
            }
        }
        return null;
    }

    /* ============================== ORDER MANAGEMENT RECEIPT ============================== */

    /** Fills (or hides) the receipt panel from the selected card plus any detail the backend supplied. */
    private void updateOmReceiptPanel() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order == null) {
            omSelectedOrderNo = null;
            omReceiptDetails = null;
            omReceiptPanel.setVisible(false);
            omReceiptPanel.setManaged(false);
            return;
        }
        omReceiptPanel.setVisible(true);
        omReceiptPanel.setManaged(true);

        OrderReceipt detail = (omReceiptDetails != null
                && Objects.equals(omReceiptDetails.orderNumber(), order.orderNumber()))
                ? omReceiptDetails : null;

        // header
        omrOrderTitleLabel.setText("Order #" + valueOrDash(order.orderNumber()));
        omrStatusLabel.setText(order.status() == null ? "\u2014" : order.status().getDisplayName());
        omrStatusLabel.getStyleClass().removeIf(c -> c.startsWith("omr-badge-"));
        omrStatusLabel.getStyleClass().add(receiptBadgeClass(order.status()));
        omrTypeLabel.setText(orderTypeLine(order));

        // info card
        omrTimeLabel.setText(formatRelativeTime(order.createdAt()));
        omrCashierLabel.setText(detail == null ? "\u2014" : valueOrDash(detail.cashierName()));

        // lines
        List<Node> rows = new ArrayList<>();
        if (detail != null && detail.lines() != null) {
            for (ReceiptLine line : detail.lines()) {
                if (line != null) {
                    rows.add(createOmReceiptRow(line));
                }
            }
        }
        omrLines.getChildren().setAll(rows);

        // totals: before the detail arrives only the card's total is known
        ReceiptTotals totals = detail == null ? null : detail.totals();
        if (totals == null) {
            omrSubtotalLabel.setText("\u2014");
            omrServiceLabel.setText("Service charge");
            omrServiceValueLabel.setText("\u2014");
            omrVatLabel.setText("VAT");
            omrVatValueLabel.setText("\u2014");
            omrTotalLabel.setText(formatPeso(order.total()));
        } else {
            omrSubtotalLabel.setText(formatPeso(totals.subtotal()));
            omrServiceLabel.setText("Service charge" + rateSuffix(totals.serviceChargeRate()));
            omrServiceValueLabel.setText(formatPeso(totals.serviceCharge()));
            omrVatLabel.setText("VAT" + rateSuffix(totals.vatRate()));
            omrVatValueLabel.setText(formatPeso(totals.vat()));
            omrTotalLabel.setText(formatPeso(totals.total()));
        }
        if (detail == null) {
            omrDiscountLabel.setText("\u2014");
        } else {
            String d = detail.discountName();
            omrDiscountLabel.setText((d == null || d.isBlank()) ? "None" : d.trim());
        }

        // actions only make sense while the order is being prepared
        boolean preparing = order.status() == OrderStatus.PREPARING;
        omrCompleteBtn.setVisible(preparing);
        omrCompleteBtn.setManaged(preparing);
        omrCancelBtn.setVisible(preparing);
        omrCancelBtn.setManaged(preparing);
    }

    private Node createOmReceiptRow(ReceiptLine line) {
        Label name = new Label(valueOrDash(line.name()));
        name.getStyleClass().add("omr-line-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label qty = new Label(String.valueOf(line.quantity()));
        qty.getStyleClass().addAll("omr-line-qty", "omr-col-qty");

        Label price = new Label(formatPeso(line.lineTotal()));
        price.getStyleClass().addAll("omr-line-price", "omr-col-price");

        HBox row = new HBox(name, qty, price);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String receiptBadgeClass(OrderStatus status) {
        if (status == null) {
            return "omr-badge-neutral";
        }
        return switch (status) {
            case PREPARING -> "omr-badge-preparing";
            case COMPLETED -> "omr-badge-completed";
            case CANCELLED -> "omr-badge-cancelled";
        };
    }

    @FXML
    private void onOmReceiptComplete() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order != null && onCompleteOrder != null) {
            onCompleteOrder.accept(order);
        }
    }

    @FXML
    private void onOmReceiptCancel() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order != null && onCancelOrder != null) {
            onCancelOrder.accept(order);
        }
    }

    private static String badgeClass(OrderStatus status) {
        if (status == null) {
            return "om-badge-neutral";
        }
        return switch (status) {
            case PREPARING -> "om-badge-preparing";
            case COMPLETED -> "om-badge-completed";
            case CANCELLED -> "om-badge-cancelled";
        };
    }

    /** "Dine-in · Table 12", or just the type when there is no detail. */
    private static String orderTypeLine(OrderCard order) {
        String type = valueOrDash(order.orderType());
        String detail = order.orderTypeDetail();
        return (detail == null || detail.isBlank()) ? type : type + " \u00B7 " + detail.trim();
    }

    private static int countItems(OrderCard order) {
        int sum = 0;
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) {
                if (line != null) {
                    sum += line.quantity();
                }
            }
        }
        return sum;
    }

    private static String formatRelativeTime(LocalDateTime created) {
        if (created == null) {
            return "\u2014";
        }
        long mins = ChronoUnit.MINUTES.between(created, LocalDateTime.now());
        if (mins < 1) {
            return "Just now";
        }
        if (mins < 60) {
            return mins + (mins == 1 ? " min ago" : " mins ago");
        }
        long hours = mins / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hr ago" : " hrs ago");
        }
        long days = hours / 24;
        if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        }
        return created.toLocalDate().format(OM_DATE_FMT);
    }

    /* ============================== SHARED HELPERS ============================== */

    private static void fixSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    /** Center-crops a non-square photo to a square viewport (CSS object-fit: cover). */
    private static void applyCoverCrop(ImageView view, Image image) {
        Runnable crop = () -> {
            double w = image.getWidth();
            double h = image.getHeight();
            if (w <= 0.0 || h <= 0.0) {
                return;
            }
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        };
        if (image.getProgress() >= 1.0) {
            crop.run();
        } else {
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    crop.run();
                }
            });
        }
    }

    private static String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "\u2014" : value;
    }

    private static String formatPeso(BigDecimal amount) {
        return amount == null ? "\u2014" : String.format(Locale.ENGLISH, "\u20B1 %,.2f", amount);
    }
}
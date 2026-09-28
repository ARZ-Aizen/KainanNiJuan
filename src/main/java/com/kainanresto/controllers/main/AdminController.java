package com.kainanresto.controllers.main;

import com.kainanresto.model.Dishes;
import com.kainanresto.model.Icons;
import com.kainanresto.model.OrderStats;
import com.kainanresto.model.Transaction;
import com.kainanresto.model.Product;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class AdminController {

    /* ============================== SIDEBAR ============================== */
    @FXML private VBox sidebarRoot;
    @FXML private ImageView restaurantLogoImage;

    @FXML private Button navDashboardBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button logoutBtn;

    /* ============================== ICONS ============================== */
    @FXML private SVGPath dashboardIcon;
    @FXML private SVGPath inventoryIcon;
    @FXML private SVGPath menuIcon;
    @FXML private SVGPath salesOrdersIcon;
    @FXML private SVGPath accountManagementIcon;
    @FXML private SVGPath settingsIcon;
    @FXML private SVGPath logoutIcon;
    @FXML private SVGPath calendarIcon;
    @FXML private SVGPath salesStatIcon;
    @FXML private SVGPath transactionsStatIcon;
    @FXML private SVGPath completedOrdersStatIcon;
    @FXML private SVGPath menuStatIcon;
    @FXML private SVGPath menuSearchIcon;
    @FXML private SVGPath addDishIcon;
    @FXML private SVGPath ordersSearchIcon;
    @FXML private SVGPath ordersRevenueIcon;
    @FXML private SVGPath ordersTotalIcon;
    @FXML private SVGPath ordersVoidsIcon;
    @FXML private SVGPath ordersAvgSpendIcon;

    private static final double ICON_GRID = 24.0;       // Lucide grid
    private static final double ICON_STROKE_PX = 2.0;   // on-screen stroke per the design

    /* ============================== SHARED TOPBAR ============================== */
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;

    /* ============================== VIEW SWITCHING ============================== */
    @FXML private BorderPane appRoot;
    @FXML private StackPane viewStack;
    @FXML private ScrollPane dashboardView;
    @FXML private VBox inventoryView;
    @FXML private VBox menuView;
    @FXML private VBox salesOrdersView;
    private static final String NAV_ACTIVE = "nav-item-active";

    /* ============================== DASHBOARD: STAT CARDS ============================== */
    @FXML private HBox statCardsRow;
    @FXML private Label todaysSalesValueLabel;
    @FXML private Label todaysSalesDeltaLabel;
    @FXML private Label transactionsValueLabel;
    @FXML private Label transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel;
    @FXML private Label completedOrdersSubLabel;
    @FXML private Label totalMenuItemsValueLabel;
    @FXML private Label totalMenuItemsSubLabel;

    /* ============================== DASHBOARD: ANALYTICS ============================== */
    @FXML private Button rangeTodayBtn;
    @FXML private Button rangeWeekBtn;
    @FXML private Button rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel;
    @FXML private Label weeklyOrdersCountLabel;
    @FXML private Label avgOrderValueLabel;

    /* ============================== DASHBOARD: BEST SELLERS / ALERTS ============================== */
    @FXML private VBox bestSellingItemsContainer;
    @FXML private VBox lowStockAlertsContainer;
    @FXML private VBox outOfStockAlertsContainer;

    /* ============================== INVENTORY ============================== */
    @FXML private VBox inventoryCard;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private TextField itemNameSearchField;
    @FXML private Button deleteBtn;

    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> itemNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> uomColumn;
    @FXML private TableColumn<Product, String> qtyOnHandColumn;
    @FXML private TableColumn<Product, String> parLevelColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    private final ObservableList<Product> inventoryItems = FXCollections.observableArrayList();

    /* ============================== MENU MANAGEMENT ============================== */
    @FXML private HBox menuSearchBox;
    @FXML private TextField menuSearchField;
    @FXML private Button addDishBtn;
    @FXML private FlowPane menuCategoryChips;
    @FXML private FlowPane dishGrid;
    @FXML private Label dishGridPlaceholder;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final double DISH_IMAGE_SIZE = 144.66;

    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private String selectedCategory = null;   // null = "All"
    private boolean rebuildingChips = false;

    /* ============================== SALES & ORDERS ============================== */
    @FXML private Label ordersRevenueValueLabel;
    @FXML private Label ordersRevenueNoteLabel;
    @FXML private Label ordersTotalValueLabel;
    @FXML private Label ordersTotalNoteLabel;
    @FXML private Label ordersVoidsValueLabel;
    @FXML private Label ordersVoidsNoteLabel;
    @FXML private Label ordersAvgSpendValueLabel;
    @FXML private Label ordersAvgSpendNoteLabel;

    @FXML private HBox ordersSearchBox;
    @FXML private TextField ordersSearchField;
    @FXML private HBox orderDatePill;
    @FXML private HBox orderTypePill;
    @FXML private HBox orderStatusPill;
    @FXML private HBox orderPaymentPill;
    @FXML private Label orderDateValueLabel;
    @FXML private Label orderTypeValueLabel;
    @FXML private Label orderStatusValueLabel;
    @FXML private Label orderPaymentValueLabel;
    @FXML private Button clearOrderFiltersBtn;

    @FXML private TableView<Transaction> ordersTable;
    @FXML private Label ordersTablePlaceholder;
    @FXML private TableColumn<Transaction, String> orderIdColumn;
    @FXML private TableColumn<Transaction, String> orderTypeColumn;
    @FXML private TableColumn<Transaction, Transaction> orderStaffColumn;
    @FXML private TableColumn<Transaction, String> orderCreatedColumn;
    @FXML private TableColumn<Transaction, String> orderTotalColumn;
    @FXML private TableColumn<Transaction, String> orderStatusColumn;
    @FXML private TableColumn<Transaction, String> orderPaymentColumn;
    @FXML private TableColumn<Transaction, Transaction> orderActionsColumn;

    private static final DateTimeFormatter ORDER_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private final ObservableList<Transaction> orderItems = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredOrders;
    private FilterPill orderDateFilter;
    private FilterPill orderTypeFilter;
    private FilterPill orderStatusFilter;
    private FilterPill orderPaymentFilter;

    /* ============================== LIFECYCLE ============================== */

    @FXML
    public void initialize() {
        if (currentDateLabel != null) {
            currentDateLabel.setText(
                    LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        }

        // Circular logo (JavaFX CSS has no overflow:hidden)
        if (restaurantLogoImage != null) {
            restaurantLogoImage.setClip(new Circle(34, 34, 34));
        }

        setupInventoryTable();
        setupDashboardChart();
        setupMenuPage();
        setupOrdersPage();
        initializeIcons();
        normalizePhosphorIcons();
        initializeOrderFilterChevrons();
        showDashboard();

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
        // Sidebar
        setIcon(dashboardIcon, Icons.NAV_DASHBOARD);
        setIcon(inventoryIcon, Icons.NAV_INVENTORY);
        setIcon(menuIcon, Icons.NAV_MENU);
        setIcon(salesOrdersIcon, Icons.NAV_SALES_ORDERS);
        setIcon(accountManagementIcon, Icons.NAV_ACCOUNTS);
        setIcon(settingsIcon, Icons.NAV_SETTINGS);
        setIcon(logoutIcon, Icons.NAV_LOGOUT);

        // Topbar
        setIcon(calendarIcon, Icons.CALENDAR);

        // Dashboard stat cards (PROVISIONAL: no dashboard design supplied yet)
        setIcon(salesStatIcon, Icons.DOLLAR_SIGN);
        setIcon(transactionsStatIcon, Icons.NAV_SALES_ORDERS);
        setIcon(completedOrdersStatIcon, Icons.CIRCLE_CHECK);
        setIcon(menuStatIcon, Icons.NAV_MENU);

        // Menu Management
        setIcon(menuSearchIcon, Icons.SEARCH);
        setIcon(addDishIcon, Icons.PLUS);

        // Sales & Orders (glyphs inferred from the flattened design)
        setIcon(ordersSearchIcon, Icons.SEARCH);
        setIcon(ordersRevenueIcon, Icons.DOLLAR_SIGN);
        setIcon(ordersTotalIcon, Icons.SHOPPING_BAG);
        setIcon(ordersVoidsIcon, Icons.ROTATE_CCW);
        setIcon(ordersAvgSpendIcon, Icons.TRENDING_UP);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) {
            icon.setContent(content);
        }
    }

    /** Sizes each icon to the host it sits in, exactly as in the designs. */
    private void normalizePhosphorIcons() {
        fitGridIcon(dashboardIcon, 34.0);
        fitGridIcon(inventoryIcon, 34.0);
        fitGridIcon(menuIcon, 34.0);
        fitGridIcon(salesOrdersIcon, 34.0);
        fitGridIcon(accountManagementIcon, 34.0);
        fitGridIcon(settingsIcon, 34.0);
        fitGridIcon(logoutIcon, 30.0);

        fitGridIcon(calendarIcon, 16.0);

        fitGridIcon(salesStatIcon, 18.0);
        fitGridIcon(transactionsStatIcon, 18.0);
        fitGridIcon(completedOrdersStatIcon, 18.0);
        fitGridIcon(menuStatIcon, 18.0);

        fitGridIcon(menuSearchIcon, 14.0);
        fitGridIcon(addDishIcon, 16.0);

        fitGridIcon(ordersSearchIcon, 14.0);
        fitGridIcon(ordersRevenueIcon, 18.0);
        fitGridIcon(ordersTotalIcon, 18.0);
        fitGridIcon(ordersVoidsIcon, 18.0);
        fitGridIcon(ordersAvgSpendIcon, 18.0);
    }

    /** The four filter-pill chevrons have no fx:id; find them by style class. */
    private void initializeOrderFilterChevrons() {
        if (salesOrdersView == null) {
            return;
        }
        for (Node node : salesOrdersView.lookupAll(".filter-chevron")) {
            if (node instanceof SVGPath chevron) {
                chevron.setContent(Icons.CHEVRON_DOWN);
                fitGridIcon(chevron, 14.0);
            }
        }
    }

    private static void fitGridIcon(SVGPath icon, double targetSize) {
        fitGridIcon(icon, targetSize, ICON_STROKE_PX);
    }

    /**
     * Stroke icon on a 24-unit grid: scale to the host size and counter-scale the
     * stroke so it renders at exactly strokePx on screen, like the mockup.
     */
    private static void fitGridIcon(SVGPath icon, double targetSize, double strokePx) {
        if (icon == null) {
            return;
        }
        double scale = targetSize / ICON_GRID;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (strokePx / scale) + ";");
    }

    /* ============================== DASHBOARD CHART ============================== */

    private void setupDashboardChart() {
        if (weeklySalesChart == null) {
            return;
        }

        // No sample values. The chart is populated only by server/database data.
        weeklySalesXAxis.setAutoRanging(true);
        weeklySalesXAxis.setTickLabelsVisible(true);
        weeklySalesYAxis.setAutoRanging(true);
        weeklySalesYAxis.setForceZeroInRange(true);
        weeklySalesChart.getData().clear();
    }

    /** Backend integration point for weekly sales data. */
    public void setWeeklySalesData(ObservableList<XYChart.Series<String, Number>> series) {
        weeklySalesChart.getData().setAll(series);
    }

    /* ============================== INVENTORY TABLE SETUP ============================== */

    private void setupInventoryTable() {
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        uomColumn.setCellValueFactory(new PropertyValueFactory<>("uom"));
        qtyOnHandColumn.setCellValueFactory(new PropertyValueFactory<>("qtyOnHand"));
        parLevelColumn.setCellValueFactory(new PropertyValueFactory<>("parLevel"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        applyTextStyle(skuColumn, "cell-medium");
        applyTextStyle(itemNameColumn, "cell-strong");
        applyTextStyle(qtyOnHandColumn, "cell-strong");
        statusColumn.setCellFactory(column -> new StatusChipCell());

        inventoryTable.setItems(inventoryItems);
    }

    /* ============================== DATA ENTRY POINTS ============================== */
    // All of these must be called on the JavaFX Application Thread.

    /** Replace the visible inventory rows with server/database supplied data. */
    public void setProducts(ObservableList<Product> items) {
        inventoryItems.setAll(items);
    }

    /** Populate the inventory category filter with server/database supplied values. */
    public void setCategoryOptions(ObservableList<String> categories) {
        categoryFilterCombo.setItems(categories);
    }

    /** Menu page: category names from the server/database. "All" is added automatically. */
    public void setMenuCategories(List<String> categories) {
        rebuildingChips = true;
        categoryToggleGroup.getToggles().clear();
        menuCategoryChips.getChildren().clear();

        ToggleButton all = createCategoryChip("All", null);
        menuCategoryChips.getChildren().add(all);
        if (categories != null) {
            for (String c : categories) {
                menuCategoryChips.getChildren().add(createCategoryChip(c, c));
            }
        }
        rebuildingChips = false;

        selectedCategory = null;
        all.setSelected(true);
        applyMenuFilter();
    }

    /** Menu page: dish rows from the server/database. */
    public void setDishes(List<Dishes> dishes) {
        List<Dishes> safe = dishes == null ? List.of() : dishes;
        dishItems.setAll(safe);
        applyMenuFilter();
    }

    /** Sales & Orders: stat-card values. Pass null to reset the cards to placeholders. */
    public void setOrderStats(OrderStats stats) {
        if (stats == null) {
            for (Label l : new Label[]{
                    ordersRevenueValueLabel, ordersRevenueNoteLabel,
                    ordersTotalValueLabel, ordersTotalNoteLabel,
                    ordersVoidsValueLabel, ordersVoidsNoteLabel,
                    ordersAvgSpendValueLabel, ordersAvgSpendNoteLabel}) {
                l.setText("\u2014");
            }
            return;
        }
        ordersRevenueValueLabel.setText(formatPesoCompact(stats.todaysRevenue()));
        ordersRevenueNoteLabel.setText(valueOrDash(stats.todaysRevenueNote()));
        ordersTotalValueLabel.setText(String.format(Locale.ENGLISH, "%,d", stats.totalOrders()));
        ordersTotalNoteLabel.setText(valueOrDash(stats.totalOrdersNote()));
        ordersVoidsValueLabel.setText(String.format(Locale.ENGLISH, "%,d", stats.voidsAndRefunds()));
        ordersVoidsNoteLabel.setText(valueOrDash(stats.voidsAndRefundsNote()));
        ordersAvgSpendValueLabel.setText(formatPesoCompact(stats.averageSpend()));
        ordersAvgSpendNoteLabel.setText(valueOrDash(stats.averageSpendNote()));
    }

    /** Sales & Orders: table rows from the server/database. */
    public void setOrders(List<Transaction> orders) {
        List<Transaction> safe = orders == null ? List.of() : orders;
        orderItems.setAll(safe);
        applyOrderFilters();
    }

    /** Filter option lists from the server. The FIRST entry is the default ("no filter") value. */
    public void setOrderDateOptions(List<String> options)    { orderDateFilter.setOptions(options); }
    public void setOrderTypeOptions(List<String> options)    { orderTypeFilter.setOptions(options); applyOrderFilters(); }
    public void setOrderStatusOptions(List<String> options)  { orderStatusFilter.setOptions(options); applyOrderFilters(); }
    public void setOrderPaymentOptions(List<String> options) { orderPaymentFilter.setOptions(options); applyOrderFilters(); }

    /** The date option the user picked, so the backend can build its query. */
    public String getSelectedOrderDate() {
        return orderDateFilter.getSelected();
    }

    /* ============================== NAVIGATION ============================== */

    @FXML
    private void onNavDashboard() {
        showDashboard();
    }

    @FXML
    private void onNavInventory() {
        showInventory();
    }

    @FXML
    private void onNavMenu() {
        showMenu();
    }

    @FXML
    private void onNavSalesOrders() {
        showSalesOrders();
    }

    @FXML
    private void onNavAccountManagement() {
        // TODO: navigate to Account Management view in the same FXML when the page is supplied.
    }

    @FXML
    private void onNavSettings() {
        // TODO: navigate to Settings view in the same FXML when the page is supplied.
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
        for (Node v : new Node[]{dashboardView, inventoryView, menuView, salesOrdersView}) {
            boolean on = (v == target);
            v.setVisible(on);
            v.setManaged(on);
        }
    }

    private void showDashboard() {
        showView(dashboardView);
        pageTitleLabel.setText("Dashboard");
        pageSubtitleLabel.setText("Overview of restaurant health and inventory diagnostics");
        setActiveNav(navDashboardBtn);
    }

    private void showInventory() {
        showView(inventoryView);
        pageTitleLabel.setText("Inventory");
        pageSubtitleLabel.setText("Monitor stock levels and manage your ingredients");
        setActiveNav(navInventoryBtn);
    }

    private void showMenu() {
        showView(menuView);
        pageTitleLabel.setText("Menu Management");
        pageSubtitleLabel.setText("Configure your dishes, pricing tiers, and categorizations");
        setActiveNav(navMenuBtn);
        // TODO: request dishes + categories from the server/database, then call
        //       setMenuCategories(...) and setDishes(...).
    }

    private void showSalesOrders() {
        showView(salesOrdersView);
        pageTitleLabel.setText("Sales & Orders");
        pageSubtitleLabel.setText("Manage customer transactions, kitchen states, and order processing logs");
        setActiveNav(navSalesOrdersBtn);
        // TODO: request stats, filter options and orders from the server/database, then call
        //       setOrderDateOptions / setOrderTypeOptions / setOrderStatusOptions /
        //       setOrderPaymentOptions / setOrderStats / setOrders.
    }

    /** Clears nav-item-active from every sidebar button and applies it to the given one. */
    private void setActiveNav(Button active) {
        for (Button btn : new Button[]{
                navDashboardBtn, navInventoryBtn, navMenuBtn,
                navSalesOrdersBtn, navAccountManagementBtn, navSettingsBtn
        }) {
            btn.getStyleClass().remove(NAV_ACTIVE);
        }
        if (!active.getStyleClass().contains(NAV_ACTIVE)) {
            active.getStyleClass().add(NAV_ACTIVE);
        }
    }

    /* ============================== DASHBOARD: RANGE TOGGLE ============================== */

    @FXML
    private void onRangeToday() {
        setActiveRange(rangeTodayBtn);
        // TODO: reload dashboard chart + summary data for "Today" from backend.
    }

    @FXML
    private void onRangeWeek() {
        setActiveRange(rangeWeekBtn);
        // TODO: reload dashboard chart + summary data for "This Week" from backend.
    }

    @FXML
    private void onRangeMonth() {
        setActiveRange(rangeMonthBtn);
        // TODO: reload dashboard chart + summary data for "This Month" from backend.
    }

    private void setActiveRange(Button active) {
        for (Button btn : new Button[]{rangeTodayBtn, rangeWeekBtn, rangeMonthBtn}) {
            btn.getStyleClass().remove("range-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("range-btn-active")) {
            active.getStyleClass().add("range-btn-active");
        }
    }

    /* ============================== INVENTORY: ACTIONS ============================== */

    @FXML
    private void onDelete() {
        // TODO: delete selected inventory record.
    }

    /* ============================== MENU MANAGEMENT ============================== */

    private void setupMenuPage() {
        // :focus-within isn't supported in JavaFX CSS, so mirror focus onto the wrapper box.
        menuSearchField.focusedProperty().addListener((obs, was, is) ->
                menuSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        menuSearchField.textProperty().addListener((obs, oldText, newText) -> applyMenuFilter());

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
            applyMenuFilter();
        });

        applyMenuFilter();   // shows the empty placeholder until data arrives
    }

    private ToggleButton createCategoryChip(String label, String category) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(category);
        chip.setToggleGroup(categoryToggleGroup);
        chip.getStyleClass().add("category-chip");
        return chip;
    }

    private void applyMenuFilter() {
        String query = menuSearchField.getText() == null ? ""
                : menuSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

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
        dishGrid.getChildren().setAll(cards);

        boolean empty = cards.isEmpty();
        dishGridPlaceholder.setText(dishItems.isEmpty()
                ? "No dishes loaded."
                : "No dishes match your filters.");
        dishGridPlaceholder.setVisible(empty);
        dishGridPlaceholder.setManaged(empty);
    }

    private Node createDishCard(Dishes dish) {
        // Shell carries the shadow; the ImageView is circle-clipped inside it.
        StackPane imageShell = new StackPane();
        imageShell.getStyleClass().add("dish-image-shell");
        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            Image image = new Image(dish.imageUrl(),
                    DISH_IMAGE_SIZE * 2, DISH_IMAGE_SIZE * 2, true, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(DISH_IMAGE_SIZE);
            imageView.setFitHeight(DISH_IMAGE_SIZE);
            imageView.setPreserveRatio(true);
            applyCoverCrop(imageView, image);
            double r = DISH_IMAGE_SIZE / 2.0;
            imageView.setClip(new Circle(r, r, r));
            imageShell.getChildren().add(imageView);
        }

        Label name = new Label(dish.name());
        name.getStyleClass().add("dish-name");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);

        Label price = new Label(formatPeso(dish.price()));
        price.getStyleClass().add("dish-price");

        Label badge = new Label(dish.available() ? "Available" : "Unavailable");
        badge.getStyleClass().addAll("dish-badge",
                dish.available() ? "dish-badge-available" : "dish-badge-unavailable");

        VBox card = new VBox(name, price, badge);
        card.getStyleClass().add("dish-card");

        // The image overhangs the card by half its height.
        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(DISH_IMAGE_SIZE / 2.0, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        wrapper.getStyleClass().add("dish-card-wrapper");
        wrapper.setOnMouseClicked(e -> onDishClicked(dish));
        return wrapper;
    }

    /** Center-crops a non-square photo to a square viewport (CSS object-fit: cover). */
    private void applyCoverCrop(ImageView view, Image image) {
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

    @FXML
    private void onAddDish() {
        // TODO: open the Add Dish form/dialog (design not supplied yet).
    }

    private void onDishClicked(Dishes dish) {
        // TODO: open the edit form for this dish (design not supplied yet).
    }

    /* ============================== SALES & ORDERS ============================== */

    private void setupOrdersPage() {
        orderDateFilter    = new FilterPill(orderDatePill, orderDateValueLabel, this::onOrderDateChanged);
        orderTypeFilter    = new FilterPill(orderTypePill, orderTypeValueLabel, this::applyOrderFilters);
        orderStatusFilter  = new FilterPill(orderStatusPill, orderStatusValueLabel, this::applyOrderFilters);
        orderPaymentFilter = new FilterPill(orderPaymentPill, orderPaymentValueLabel, this::applyOrderFilters);

        setupOrdersTable();

        // :focus-within isn't supported in JavaFX CSS, so mirror focus onto the wrapper box.
        ordersSearchField.focusedProperty().addListener((obs, was, is) ->
                ordersSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        ordersSearchField.textProperty().addListener((obs, oldText, newText) -> applyOrderFilters());

        applyOrderFilters();   // sets the empty-table placeholder text
    }

    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderId()));
        orderTypeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderType()));
        orderStaffColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        orderCreatedColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(formatOrderTimestamp(cd.getValue().lastActivityAt())));
        orderTotalColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(formatPesoCompact(cd.getValue().total())));
        orderStatusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().status()));
        orderPaymentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().paymentStatus()));
        orderActionsColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        orderStaffColumn.setCellFactory(col -> new StaffCell());
        orderStatusColumn.setCellFactory(col -> new ChipCell(true, AdminController::statusChipClass));
        orderPaymentColumn.setCellFactory(col -> new ChipCell(false, AdminController::paymentChipClass));
        orderActionsColumn.setCellFactory(col -> new ActionsCell());

        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredOrders = new FilteredList<>(orderItems, o -> true);
        ordersTable.setItems(filteredOrders);
    }

    private void applyOrderFilters() {
        if (filteredOrders == null) {
            return;
        }
        String query = ordersSearchField.getText() == null ? ""
                : ordersSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        filteredOrders.setPredicate(order ->
                matchesOrderText(order, query)
                        && orderTypeFilter.accepts(order.orderType())
                        && orderStatusFilter.accepts(order.status())
                        && orderPaymentFilter.accepts(order.paymentStatus()));

        ordersTablePlaceholder.setText(orderItems.isEmpty()
                ? "No orders loaded."
                : "No orders match your filters.");
    }

    private static boolean matchesOrderText(Transaction order, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return (order.orderId() != null && order.orderId().toLowerCase(Locale.ENGLISH).contains(query))
                || (order.customerName() != null && order.customerName().toLowerCase(Locale.ENGLISH).contains(query));
    }

    private void onOrderDateChanged() {
        // TODO: reload stats + orders for the selected date range from the server
        //       (use getSelectedOrderDate()), then call setOrderStats(...) and setOrders(...).
        applyOrderFilters();
    }

    @FXML
    private void onClearOrderFilters() {
        boolean dateWasChanged = !orderDateFilter.isDefault();

        ordersSearchField.clear();
        orderDateFilter.reset();
        orderTypeFilter.reset();
        orderStatusFilter.reset();
        orderPaymentFilter.reset();

        applyOrderFilters();
        if (dateWasChanged) {
            onOrderDateChanged();
        }
    }

    private void onOrderActions(Transaction order, Node anchor) {
        // TODO: open the row-actions menu / order details (design not supplied yet).
    }

    private static String formatOrderTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "\u2014";
        }
        LocalDate day = timestamp.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "Today, " + timestamp.format(ORDER_TIME_FORMAT);
        }
        if (day.equals(today.minusDays(1))) {
            return "Yesterday, " + timestamp.format(ORDER_TIME_FORMAT);
        }
        return timestamp.format(ORDER_DATE_TIME_FORMAT);
    }

    private static String statusChipClass(String status) {
        String s = status.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("complete")) {
            return "chip-success";
        }
        if (s.startsWith("prepar")) {
            return "chip-warning";
        }
        if (s.startsWith("cancel") || s.startsWith("void")) {
            return "chip-danger";
        }
        return "chip-pending";
    }

    private static String paymentChipClass(String payment) {
        String s = payment.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("paid")) {
            return "chip-success";
        }
        if (s.startsWith("unpaid")) {
            return "chip-danger";
        }
        return "chip-neutral";   // Refunded and anything unknown
    }

    /* ============================== FORMATTING HELPERS ============================== */

    private String formatPeso(BigDecimal amount) {
        return amount == null ? "\u2014" : String.format(Locale.ENGLISH, "\u20B1 %,.2f", amount);
    }

    /** Sales & Orders style: no space after the peso sign. */
    private static String formatPesoCompact(BigDecimal amount) {
        return amount == null ? "\u2014" : String.format(Locale.ENGLISH, "\u20B1%,.2f", amount);
    }

    private static String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "\u2014" : value;
    }

    /* ============================== CELL RENDERING HELPERS ============================== */

    /** Applies a text style class to every cell of a plain text column. */
    private void applyTextStyle(TableColumn<Product, String> column, String styleClass) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().remove(styleClass);
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        });
    }

    /** Renders the inventory status column as a coloured dot plus label. */
    private static final class StatusChipCell extends TableCell<Product, String> {

        private final Label dot = new Label("\u25CF");
        private final Label text = new Label();
        private final HBox chip = new HBox(6.0, dot, text);

        private StatusChipCell() {
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("status-chip");
            dot.getStyleClass().add("status-dot");
            text.getStyleClass().add("status-text");
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setText(null);
            if (empty || value == null || value.isBlank()) {
                setGraphic(null);
                return;
            }
            text.setText(value);
            chip.getStyleClass().removeAll(
                    "status-in-stock",
                    "status-low-stock",
                    "status-out-of-stock"
            );
            chip.getStyleClass().add(styleClassFor(value));
            setGraphic(chip);
        }

        private String styleClassFor(String value) {
            String normalized = value.trim().toLowerCase(Locale.ENGLISH);
            if (normalized.startsWith("out")) {
                return "status-out-of-stock";
            }
            if (normalized.startsWith("low")) {
                return "status-low-stock";
            }
            return "status-in-stock";
        }
    }

    /** Orders table: two-line staff cell (name over role). */
    private static final class StaffCell extends TableCell<Transaction, Transaction> {

        private final Label name = new Label();
        private final Label role = new Label();
        private final VBox box = new VBox(2.0, name, role);

        private StaffCell() {
            box.setAlignment(Pos.CENTER_LEFT);
            // Keep long names inside the column so the labels truncate with an ellipsis.
            box.maxWidthProperty().bind(widthProperty().subtract(12.0));
            name.getStyleClass().add("staff-name");
            role.getStyleClass().add("staff-role");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Transaction order, boolean empty) {
            super.updateItem(order, empty);
            setText(null);
            if (empty || order == null) {
                setGraphic(null);
                return;
            }
            name.setText(order.staffName());
            boolean hasRole = order.staffRole() != null && !order.staffRole().isBlank();
            role.setText(hasRole ? order.staffRole() : "");
            role.setVisible(hasRole);
            role.setManaged(hasRole);
            setGraphic(box);
        }
    }

    /** Orders table: coloured pill for status (with dot) or payment (text only). */
    private static final class ChipCell extends TableCell<Transaction, String> {

        private final Region dot = new Region();
        private final Label text = new Label();
        private final HBox chip = new HBox(6.0);
        private final Function<String, String> styleResolver;
        private String appliedStyle;

        private ChipCell(boolean withDot, Function<String, String> styleResolver) {
            this.styleResolver = styleResolver;
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("chip");
            dot.getStyleClass().add("chip-dot");
            text.getStyleClass().add("chip-text");
            if (withDot) {
                chip.getChildren().add(dot);
            }
            chip.getChildren().add(text);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setText(null);
            if (empty || value == null || value.isBlank()) {
                setGraphic(null);
                return;
            }
            text.setText(value.trim());
            if (appliedStyle != null) {
                chip.getStyleClass().remove(appliedStyle);
            }
            appliedStyle = styleResolver.apply(value);
            chip.getStyleClass().add(appliedStyle);
            setGraphic(chip);
        }
    }

    /** Orders table: the 28x28 row action button. Not static: it calls onOrderActions. */
    private final class ActionsCell extends TableCell<Transaction, Transaction> {

        private final Button button = new Button();

        private ActionsCell() {
            SVGPath icon = new SVGPath();
            icon.setContent(Icons.ELLIPSIS_VERTICAL);
            icon.getStyleClass().addAll("phosphor-icon", "icon-brand");
            fitGridIcon(icon, 14.0, 1.5);   // the design uses a 1.5px stroke here

            StackPane host = new StackPane(icon);
            host.getStyleClass().add("icon-host-14");

            button.setGraphic(host);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMnemonicParsing(false);
            button.getStyleClass().add("row-action-btn");
            button.setOnAction(e -> {
                Transaction order = getItem();
                if (order != null) {
                    onOrderActions(order, button);
                }
            });
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Transaction order, boolean empty) {
            super.updateItem(order, empty);
            setText(null);
            setGraphic(empty || order == null ? null : button);
        }
    }

    /**
     * A "Label: Value v" pill that opens a dropdown of backend-supplied options.
     * The first option is the default ("no filter") value.
     */
    private static final class FilterPill {

        private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");

        private final Label valueLabel;
        private final Runnable onChange;
        private final ContextMenu menu = new ContextMenu();
        private List<String> options = List.of();
        private String selected;

        private FilterPill(HBox pill, Label valueLabel, Runnable onChange) {
            this.valueLabel = valueLabel;
            this.onChange = onChange;

            menu.getStyleClass().add("filter-menu");
            menu.showingProperty().addListener((obs, was, showing) ->
                    pill.pseudoClassStateChanged(OPEN, showing));
            pill.setOnMouseClicked(e -> {
                if (options.isEmpty()) {
                    return;
                }
                if (menu.isShowing()) {
                    menu.hide();
                } else {
                    menu.show(pill, Side.BOTTOM, 0.0, 4.0);
                }
            });
        }

        private void setOptions(List<String> newOptions) {
            options = newOptions == null ? List.of() : List.copyOf(newOptions);
            select(options.isEmpty() ? null : options.get(0), false);
        }

        private boolean isDefault() {
            return options.isEmpty() || Objects.equals(selected, options.get(0));
        }

        private boolean accepts(String value) {
            return isDefault() || (value != null && value.equalsIgnoreCase(selected));
        }

        private String getSelected() {
            return selected;
        }

        private void reset() {
            if (!options.isEmpty()) {
                select(options.get(0), false);
            }
        }

        private void select(String value, boolean notify) {
            selected = value;
            valueLabel.setText(value == null ? "\u2014" : value);
            rebuildMenu();
            if (notify) {
                onChange.run();
            }
        }

        private void rebuildMenu() {
            List<MenuItem> items = new ArrayList<>();
            for (String option : options) {
                MenuItem item = new MenuItem(option);
                item.setMnemonicParsing(false);
                if (option.equals(selected)) {
                    item.getStyleClass().add("filter-option-selected");
                }
                item.setOnAction(e -> {
                    if (!option.equals(selected)) {
                        select(option, true);
                    }
                });
                items.add(item);
            }
            menu.getItems().setAll(items);
        }
    }
}
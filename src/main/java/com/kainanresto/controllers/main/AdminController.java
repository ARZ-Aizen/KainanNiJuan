package com.kainanresto.controllers.main;

import com.kainanresto.model.*;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Priority;


public class AdminController {

    /* ============================== SIDEBAR ============================== */
    @FXML private VBox sidebarRoot;
    @FXML private ImageView restaurantLogoImage;

    @FXML private Button navDashboardBtn;
    @FXML private Button navMenuBtn;
    @FXML private Button navSalesOrdersBtn;
    @FXML private Button navAccountManagementBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button logoutBtn;

    /* ============================== ICONS ============================== */
    @FXML private SVGPath dashboardIcon;
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
    @FXML private VBox menuView;
    @FXML private VBox salesOrdersView;
    @FXML private VBox accountsView;
    @FXML private HBox settingsView;
    private static final String NAV_ACTIVE = "nav-item-active";
    @FXML private ScrollPane editAccountView;

    /* ============================== EDIT ACCOUNT (editAccount*) ============================== */
    private static final DateTimeFormatter EDIT_ACCOUNT_DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    @FXML private TextField editAccountNameField;
    @FXML private TextField editAccountUsernameField;
    @FXML private PasswordField editAccountPasswordField;
    @FXML private ComboBox<AccountRole> editAccountRoleCombo;
    @FXML private ComboBox<AccountStatus> editAccountStatusCombo;
    @FXML private Label editAccountCreatedLabel;
    @FXML private Label editAccountLastLoginLabel;
    @FXML private Label editAccountErrorLabel;
    private AccountRow editingAccount;
    @FXML private TextField editAccountVisiblePasswordField;
    @FXML private Button editAccountPasswordToggleBtn;

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

    /* ============================== ADD MENU ITEM ============================== */
    public record NewDishForm(String name, String category, BigDecimal price, String description,
                              boolean available, String imageUri, int quantity) {}

    @FXML private ScrollPane addDishView;
    @FXML private TextField addDishNameField;
    @FXML private ComboBox<String> addDishCategoryCombo;
    @FXML private TextField addDishPriceField;
    @FXML private TextField addDishQuantityField;
    @FXML private TextArea addDishDescriptionField;
    @FXML private ToggleGroup addDishStatusGroup;
    @FXML private ToggleButton addDishAvailableBtn;
    @FXML private ToggleButton addDishUnavailableBtn;
    @FXML private ImageView addDishPreviewImage;
    @FXML private Label addDishErrorLabel;
    @FXML private SVGPath addDishUploadIcon;

    @FXML private TextField addCategoryNameField;
    @FXML private Label addCategoryErrorLabel;
    @FXML private Label addCategoryCountLabel;
    @FXML private Label addCategoryEmptyLabel;
    @FXML private VBox addCategoryList;
    @FXML private SVGPath addCategoryAddIcon;

    private static final int MAX_QUANTITY = 99999;
    private static final int MAX_CATEGORY_LENGTH = 40;

    private String addDishImageUri;
    private final List<String> categoryNames = new ArrayList<>();
    private Consumer<NewDishForm> onSaveDish;
    private Consumer<String> onCreateCategory;
    private Consumer<String> onDeleteCategory;


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

    /* ============================== ACCOUNT MANAGEMENT (accounts* / account*) ============================== */
    private static final String ACCOUNTS_ALL_ROLES    = "All Roles";
    private static final String ACCOUNTS_ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ACCOUNTS_TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ACCOUNTS_DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

    private enum AccountsLastLoginFilter {
        ANYTIME("Anytime"), TODAY("Today"), LAST_7_DAYS("Last 7 days"),
        LAST_30_DAYS("Last 30 days"), NEVER("Never");

        private final String label;
        AccountsLastLoginFilter(String label) { this.label = label; }
        String label() { return label; }
    }

    @FXML private HBox accountsSearchBox;
    @FXML private SVGPath accountsSearchIcon;
    @FXML private TextField accountsSearchField;
    @FXML private HBox accountsRolePill;
    @FXML private HBox accountsStatusPill;
    @FXML private HBox accountsLastLoginPill;
    @FXML private Label accountsRoleValueLabel;
    @FXML private Label accountsStatusValueLabel;
    @FXML private Label accountsLastLoginValueLabel;
    @FXML private TableView<AccountRow> accountsTable;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColName;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColUsername;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColRole;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColStatus;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColLastLogin;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColCreated;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColActions;

    private final Label accountsEmptyLabel = new Label("No accounts loaded.");
    private final ObservableList<AccountRow> accountsMaster = FXCollections.observableArrayList();
    private FilteredList<AccountRow> accountsFiltered;
    private AccountRole accountsRoleFilter = null;                 // null = all roles
    private AccountStatus accountsStatusFilter = null;             // null = all statuses
    private AccountsLastLoginFilter accountsLastLoginFilter = AccountsLastLoginFilter.ANYTIME;
    private Consumer<AccountRow> onEditAccount;
    private Consumer<AccountRow> onDeleteAccount;

    /* ============================== SETTINGS (settings*) ============================== */
    private static final String SETTINGS_TAB_ACTIVE = "settings-tab-active";
    private static final DateTimeFormatter SETTINGS_DATE_FMT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("MMMM d, yyyy").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter SETTINGS_TIME_FMT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

    @FXML private ScrollPane settingsScroll;
    @FXML private VBox settingsContent;
    @FXML private VBox settingsGeneralCard;
    @FXML private VBox settingsPreferencesCard;
    @FXML private VBox settingsBackupCard;
    @FXML private VBox settingsSecurityCard;

    @FXML private Button settingsTabGeneralBtn;
    @FXML private Button settingsTabPreferencesBtn;
    @FXML private Button settingsTabBackupBtn;
    @FXML private Button settingsTabSecurityBtn;

    @FXML private SVGPath settingsGeneralIcon;
    @FXML private SVGPath settingsPreferencesIcon;
    @FXML private SVGPath settingsBackupTabIcon;
    @FXML private SVGPath settingsSecurityIcon;
    @FXML private SVGPath settingsLogoPlaceholderIcon;
    @FXML private SVGPath settingsLanguageChevron;
    @FXML private SVGPath settingsBackupBtnIcon;
    @FXML private SVGPath settingsExportBtnIcon;
    @FXML private SVGPath settingsRestoreBtnIcon;

    @FXML private ImageView settingsLogoImage;
    @FXML private TextField settingsNameField;
    @FXML private TextField settingsEmailField;
    @FXML private TextField settingsDateField;
    @FXML private TextField settingsTimeField;
    @FXML private ToggleGroup settingsSyncGroup;
    @FXML private ToggleButton settingsSyncManualBtn;
    @FXML private ToggleButton settingsSyncAutoBtn;
    @FXML private HBox settingsLanguagePill;
    @FXML private Label settingsLanguageValueLabel;
    @FXML private ToggleButton settingsAutoBackupToggle;
    @FXML private ToggleButton settingsRequirePinToggle;
    @FXML private Label settingsErrorLabel;

    private FilterPill settingsLanguageFilter;
    private String settingsPendingLanguage;      // language received before the options list
    private String settingsLogoUri;              // currently previewed logo (maybe a newly chosen file)
    private AppSettings settingsLoaded;          // last values from the server (Cancel reverts to these)
    private Consumer<AppSettings> onSaveSettings;
    private Runnable onBackupDatabase;
    private Runnable onExportData;
    private Runnable onRestorePoint;

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

        setupDashboardChart();

        setupMenuPage();

        setupOrdersPage();

        setupAccountsPage();
        loadAccountManagementData();
        setupEditAccountPage();

        setupSettingsPage();

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
        setIcon(addDishUploadIcon, Icons.IMAGE);
        setIcon(addCategoryAddIcon, Icons.PLUS);

        // Sales & Orders (glyphs inferred from the flattened design)
        setIcon(ordersSearchIcon, Icons.SEARCH);
        setIcon(ordersRevenueIcon, Icons.DOLLAR_SIGN);
        setIcon(ordersTotalIcon, Icons.SHOPPING_BAG);
        setIcon(ordersVoidsIcon, Icons.ROTATE_CCW);
        setIcon(ordersAvgSpendIcon, Icons.TRENDING_UP);

        // Account Management: search + pill chevrons are set in setupAccountsPage()

        // Settings (glyphs inferred from the flattened design)
        setIcon(settingsGeneralIcon, Icons.HOUSE);
        setIcon(settingsPreferencesIcon, Icons.GLOBE);
        setIcon(settingsBackupTabIcon, Icons.DATABASE);
        setIcon(settingsSecurityIcon, Icons.SHIELD);
        setIcon(settingsLogoPlaceholderIcon, Icons.IMAGE);
        setIcon(settingsLanguageChevron, Icons.CHEVRON_DOWN);
        setIcon(settingsBackupBtnIcon, Icons.DATABASE);
        setIcon(settingsExportBtnIcon, Icons.DOWNLOAD);
        setIcon(settingsRestoreBtnIcon, Icons.ROTATE_CCW);
    }

    private void setIcon(SVGPath icon, String content) {
        if (icon != null && content != null) {
            icon.setContent(content);
        }
    }

    /** Sizes each icon to the host it sits in, exactly as in the designs. */
    private void normalizePhosphorIcons() {
        fitGridIcon(dashboardIcon, 34.0);
        fitGridIcon(menuIcon, 34.0);
        fitGridIcon(salesOrdersIcon, 34.0);
        fitGridIcon(accountManagementIcon, 34.0);
        fitGridIcon(settingsIcon, 34.0);
        fitGridIcon(logoutIcon, 30.0);

        fitGridIcon(calendarIcon, 16.0);

        fitGridIcon(salesStatIcon, 22.0);
        fitGridIcon(transactionsStatIcon, 22.0);
        fitGridIcon(completedOrdersStatIcon, 22.0);
        fitGridIcon(menuStatIcon, 22.0);

        fitGridIcon(menuSearchIcon, 14.0);
        fitGridIcon(addDishIcon, 16.0);
        fitGridIcon(addDishUploadIcon, 24.0);
        fitGridIcon(addCategoryAddIcon, 14.0);

        fitGridIcon(ordersSearchIcon, 14.0);
        fitGridIcon(ordersRevenueIcon, 18.0);
        fitGridIcon(ordersTotalIcon, 18.0);
        fitGridIcon(ordersVoidsIcon, 18.0);
        fitGridIcon(ordersAvgSpendIcon, 18.0);

        fitGridIcon(settingsGeneralIcon, 18.0);
        fitGridIcon(settingsPreferencesIcon, 18.0);
        fitGridIcon(settingsBackupTabIcon, 18.0);
        fitGridIcon(settingsSecurityIcon, 18.0);
        fitGridIcon(settingsLogoPlaceholderIcon, 32.0);
        fitGridIcon(settingsLanguageChevron, 16.0);
        fitGridIcon(settingsBackupBtnIcon, 16.0);
        fitGridIcon(settingsExportBtnIcon, 16.0);
        fitGridIcon(settingsRestoreBtnIcon, 16.0);
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

    /** Account Management: table rows from the server/database. */
    public void setAccounts(List<AccountRow> rows) {
        accountsMaster.setAll(rows == null ? List.of() : rows);
        applyAccountsFilter();
    }

    public void setOnEditAccount(Consumer<AccountRow> handler)   { this.onEditAccount = handler; }
    public void setOnDeleteAccount(Consumer<AccountRow> handler) { this.onDeleteAccount = handler; }

    /** Settings: fill the form from the server/database. Pass null to reset the form to placeholders. */
    public void setSettings(AppSettings settings) {
        settingsLoaded = settings;
        applySettingsToForm(settings);
    }

    /** Settings: language names for the dropdown. The first entry is shown until settings arrive. */
    public void setSettingsLanguageOptions(List<String> options) {
        settingsLanguageFilter.setOptions(options);
        if (settingsPendingLanguage != null) {
            settingsLanguageFilter.setSelected(settingsPendingLanguage);
        }
    }

    /** Settings: called with the validated form values when the user presses Save Changes. */
    public void setOnSaveSettings(Consumer<AppSettings> handler) { this.onSaveSettings = handler; }
    public void setOnBackupDatabase(Runnable handler)            { this.onBackupDatabase = handler; }
    public void setOnExportData(Runnable handler)                { this.onExportData = handler; }
    public void setOnRestorePoint(Runnable handler)              { this.onRestorePoint = handler; }

    /** Settings: show a message next to the Save button (e.g. a server-side failure). Null/blank clears it. */
    public void setSettingsError(String message) {
        boolean has = message != null && !message.isBlank();
        settingsErrorLabel.setText(has ? message : "");
        settingsErrorLabel.setVisible(has);
        settingsErrorLabel.setManaged(has);
    }

    /* ============================== NAVIGATION ============================== */

    @FXML
    private void onNavDashboard() {
        showDashboard();
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
        showAccounts();
    }

    @FXML
    private void onNavSettings() {
        showSettings();
    }

    @FXML
    private void onLogout(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) {
            com.kainanresto.dao.UserDAO userDAO = new com.kainanresto.dao.UserDAO();
            userDAO.setAccountInactive(currentUser.getUserId());
        }

        SessionManager.clearSession();
        NavigationUtil.switchScene(
                event,
                "/com/kainanresto/views/id/LoginView.fxml",
                "Kainan Ni Juan - Login"
        );
    }

    /** Shows exactly one page in the view stack. Add every new page to this array. */
    private void showView(Node target) {
        for (Node v : new Node[]{dashboardView, menuView, salesOrdersView,
                accountsView, settingsView, editAccountView, addDishView}) {
            if (v == null) {
                continue;
            }
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

    private void showAccounts() {
        showView(accountsView);
        pageTitleLabel.setText("Account Management");
        pageSubtitleLabel.setText("Configure system access, roles, and security permissions");
        setActiveNav(navAccountManagementBtn);
        // TODO: request accounts from the server/database, then call setAccounts(...).
    }

    private void showSettings() {
        showView(settingsView);
        pageTitleLabel.setText("Settings");
        pageSubtitleLabel.setText("Configure your restaurant preferences, system defaults, and security configurations");
        setActiveNav(navSettingsBtn);
        // TODO: request settings + language options from the server/database, then call
        //       setSettingsLanguageOptions(...) and setSettings(...).
    }

    /** Clears nav-item-active from every sidebar button and applies it to the given one. */
    private void setActiveNav(Button active) {
        for (Button btn : new Button[]{
                navDashboardBtn, navMenuBtn,
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
        showAddDish();
    }

    private void onDishClicked(Dishes dish) {
        // TODO: open the edit form for this dish (design not supplied yet).
    }

    /* ============================== ADD MENU ITEM ============================== */
    /** Backend hook: called with the validated form when Save Menu Item is pressed. */
    public void setOnSaveDish(Consumer<NewDishForm> handler) { this.onSaveDish = handler; }

    /** Backend hook: called after a category is created in the UI (insert it into the database). */
    public void setOnCreateCategory(Consumer<String> handler) { this.onCreateCategory = handler; }

    /** Backend hook: called after a category is deleted in the UI (remove it from the database). */
    public void setOnDeleteCategory(Consumer<String> handler) { this.onDeleteCategory = handler; }

    private void setupAddDishPage() {
        addDishPreviewImage.setClip(new Circle(38, 38, 38));
        addDishPriceField.setTextFormatter(decimalFormatter(2));
        addDishQuantityField.setTextFormatter(integerFormatter(5));

        // Status behaves like a radio pair: one is always selected.
        addDishStatusGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                addDishStatusGroup.selectToggle(old);
            }
        });

        // Enter key adds the category
        addCategoryNameField.setOnAction(e -> onAddCategory());

        setAddDishError(null);
        setAddCategoryError(null);
        rebuildCategoryList();
    }

    private void showAddDish() {
        resetAddDishForm();
        showView(addDishView);
        pageTitleLabel.setText("Add Menu Item");
        pageSubtitleLabel.setText("Configure details, pricing, and stock quantity for a new culinary creation");
        setActiveNav(navMenuBtn);   // keep the sidebar highlight on Menu
    }

    private void resetAddDishForm() {
        addDishNameField.clear();
        addDishCategoryCombo.getSelectionModel().clearSelection();
        addDishCategoryCombo.setValue(null);
        addDishPriceField.clear();
        addDishQuantityField.setText("0");
        addDishDescriptionField.clear();
        addDishAvailableBtn.setSelected(true);
        addDishImageUri = null;
        addDishPreviewImage.setImage(null);
        addCategoryNameField.clear();
        setAddDishError(null);
        setAddCategoryError(null);
    }

    @FXML
    private void onAddDishChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose menu item photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(appRoot.getScene().getWindow());
        if (file != null) {
            addDishImageUri = file.toURI().toString();
            Image image = new Image(addDishImageUri, 152.0, 152.0, true, true, true);
            addDishPreviewImage.setImage(image);
            applyCoverCrop(addDishPreviewImage, image);
        }
    }

    /* ---- quantity stepper ---- */

    private int currentQuantity() {
        String t = addDishQuantityField.getText();
        return (t == null || t.isBlank()) ? 0 : Integer.parseInt(t);   // formatter guarantees digits only
    }

    @FXML
    private void onAddDishQtyMinus() {
        addDishQuantityField.setText(String.valueOf(Math.max(0, currentQuantity() - 1)));
    }

    @FXML
    private void onAddDishQtyPlus() {
        addDishQuantityField.setText(String.valueOf(Math.min(MAX_QUANTITY, currentQuantity() + 1)));
    }

    /* ---- category creator ---- */

    @FXML
    private void onAddCategory() {
        setAddCategoryError(null);
        String name = addCategoryNameField.getText() == null ? "" : addCategoryNameField.getText().trim();

        if (name.isEmpty()) {
            setAddCategoryError("Enter a category name.");
            return;
        }
        if (name.length() > MAX_CATEGORY_LENGTH) {
            setAddCategoryError("Category name is too long (max " + MAX_CATEGORY_LENGTH + " characters).");
            return;
        }
        for (String existing : categoryNames) {
            if (existing.equalsIgnoreCase(name)) {
                setAddCategoryError("That category already exists.");
                return;
            }
        }

        List<String> updated = new ArrayList<>(categoryNames);
        updated.add(name);
        setMenuCategories(updated);              // refreshes chips, dropdown and this list
        addDishCategoryCombo.setValue(name);     // preselect it for the dish being created
        addCategoryNameField.clear();

        if (onCreateCategory != null) {
            onCreateCategory.accept(name);
        }
    }

    private void deleteCategory(String name) {
        boolean inUse = dishItems.stream().anyMatch(d -> name.equalsIgnoreCase(d.category()));
        if (inUse) {
            com.kainanresto.util.AlertUtil.showError("Category In Use",
                    "\"" + name + "\" still has dishes assigned to it. Move or remove those dishes first.");
            return;
        }
        boolean confirmed = com.kainanresto.util.AlertUtil.showYesNoConfirmation(
                "Delete Category",
                "Delete \"" + name + "\"?",
                "This category will be removed from the menu.");
        if (!confirmed) {
            return;
        }

        List<String> updated = new ArrayList<>(categoryNames);
        updated.remove(name);
        setMenuCategories(updated);

        if (onDeleteCategory != null) {
            onDeleteCategory.accept(name);
        }
    }

    private void rebuildCategoryList() {
        if (addCategoryList == null) {
            return;
        }
        List<Node> rows = new ArrayList<>();
        for (String name : categoryNames) {
            Region dot = new Region();
            dot.getStyleClass().add("add-category-dot");

            Label label = new Label(name);
            label.getStyleClass().add("add-category-name");
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);

            SVGPath trash = new SVGPath();
            trash.setContent(Icons.TRASH_2);
            trash.getStyleClass().addAll("phosphor-icon", "icon-danger");
            fitGridIcon(trash, 14.0, 1.5);
            StackPane host = new StackPane(trash);
            host.setMinSize(14.0, 14.0);
            host.setPrefSize(14.0, 14.0);
            host.setMaxSize(14.0, 14.0);

            Button delete = new Button();
            delete.setGraphic(host);
            delete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            delete.setMnemonicParsing(false);
            delete.getStyleClass().add("add-category-delete");
            delete.setTooltip(new Tooltip("Delete category"));
            delete.setOnAction(e -> deleteCategory(name));

            HBox row = new HBox(10.0, dot, label, delete);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("add-category-row");
            rows.add(row);
        }
        addCategoryList.getChildren().setAll(rows);

        int n = categoryNames.size();
        addCategoryCountLabel.setText(n + (n == 1 ? " category" : " categories"));
        addCategoryEmptyLabel.setVisible(n == 0);
        addCategoryEmptyLabel.setManaged(n == 0);
    }

    private void setAddCategoryError(String message) {
        boolean has = message != null && !message.isBlank();
        addCategoryErrorLabel.setText(has ? message : "");
        addCategoryErrorLabel.setVisible(has);
        addCategoryErrorLabel.setManaged(has);
    }

    /* ---- save / cancel ---- */

    @FXML
    private void onAddDishCancel() {
        showMenu();
    }

    @FXML
    private void onAddDishSave() {
        setAddDishError(null);

        String name = addDishNameField.getText() == null ? "" : addDishNameField.getText().trim();
        String category = addDishCategoryCombo.getValue();
        String priceText = addDishPriceField.getText() == null ? "" : addDishPriceField.getText().trim();

        if (name.isEmpty() || category == null || priceText.isEmpty()) {
            setAddDishError("Please fill in all required fields.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
        } catch (NumberFormatException ex) {
            setAddDishError("Enter a valid selling price.");
            return;
        }
        if (price.signum() <= 0) {
            setAddDishError("Selling price must be greater than zero.");
            return;
        }

        String desc = addDishDescriptionField.getText() == null ? "" : addDishDescriptionField.getText().trim();
        NewDishForm form = new NewDishForm(name, category, price, desc.isEmpty() ? null : desc,
                addDishAvailableBtn.isSelected(), addDishImageUri, currentQuantity());

        if (onSaveDish != null) {
            onSaveDish.accept(form);
        }
        // TODO: save to the database via your DAO, then call showMenu() and reload the dishes.
    }

    private void setAddDishError(String message) {
        boolean has = message != null && !message.isBlank();
        addDishErrorLabel.setText(has ? message : "");
        addDishErrorLabel.setVisible(has);
        addDishErrorLabel.setManaged(has);
    }

    /* ---- input filters ---- */

    /** Digits with an optional decimal point, limited to maxDecimals places. */
    private static TextFormatter<String> decimalFormatter(int maxDecimals) {
        String regex = "\\d*(\\.\\d{0," + maxDecimals + "})?";
        return new TextFormatter<>(c -> c.getControlNewText().matches(regex) ? c : null);
    }

    /** Digits only, up to maxDigits long. */
    private static TextFormatter<String> integerFormatter(int maxDigits) {
        String regex = "\\d{0," + maxDigits + "}";
        return new TextFormatter<>(c -> c.getControlNewText().matches(regex) ? c : null);
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

    /* ============================== ACCOUNT MANAGEMENT ============================== */

    private void loadAccountManagementData() {
                com.kainanresto.dao.UserDAO userDAO = new com.kainanresto.dao.UserDAO();

                setAccounts(userDAO.getAllAccounts());

                setOnEditAccount(selectedAccount -> {
                    if (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username())) {
                        com.kainanresto.util.AlertUtil.showError("Action Denied", "The primary administrator account cannot be edited.");
                        return;
                    }
                    showEditAccount(selectedAccount);
                });

                //DELETE TO
                setOnDeleteAccount(selectedAccount -> {
                    if (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username())) {
                        com.kainanresto.util.AlertUtil.showError("Action Denied", "The primary administrator account cannot be deleted.");
                        return;
                    }

                    boolean confirmed = com.kainanresto.util.AlertUtil.showYesNoConfirmation(
                            "Delete Account",
                            "Delete account: " + selectedAccount.username() + "?",
                            "Are you sure you want to completely remove this user from the database? This action cannot be undone."
                    );

                    if (confirmed) {
                        com.kainanresto.dao.UserDAO.OperationResult result = userDAO.permanentDeleteUser((int) selectedAccount.id());

                        if (result == com.kainanresto.dao.UserDAO.OperationResult.SUCCESS) {
                            com.kainanresto.util.AlertUtil.showInfo("Success", "Account has been permanently deleted.");
                            loadAccountManagementData();
                        } else {
                            com.kainanresto.util.AlertUtil.showError("Database Error", "Failed to delete the account from the database. Please try again.");
                        }
                    }
        });

    }

    private void setupAccountsPage() {
        // icons
        if (accountsSearchIcon != null) {
            accountsSearchIcon.setContent(Icons.SEARCH);
            fitGridIcon(accountsSearchIcon, 14.0);
        }
        if (accountsView != null) {
            for (Node n : accountsView.lookupAll(".accounts-pill-chevron")) {
                if (n instanceof SVGPath p) {
                    p.setContent(Icons.CHEVRON_DOWN);
                    fitGridIcon(p, 12.0);
                }
            }
        }

        // search box: mirror focus onto the wrapper, filter as the user types
        accountsSearchField.focusedProperty().addListener((obs, was, is) ->
                accountsSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        accountsSearchField.textProperty().addListener((obs, oldText, newText) -> applyAccountsFilter());

        // pills (first option = default / "no filter")
        LinkedHashMap<String, AccountRole> roleOptions = new LinkedHashMap<>();
        roleOptions.put(ACCOUNTS_ALL_ROLES, null);
        for (AccountRole r : AccountRole.values()) {
            roleOptions.put(r.getDisplayName(), r);
        }
        wireAccountsPill(accountsRolePill, accountsRoleValueLabel, roleOptions, v -> {
            accountsRoleFilter = v;
            applyAccountsFilter();
        });

        LinkedHashMap<String, AccountStatus> statusOptions = new LinkedHashMap<>();
        statusOptions.put(ACCOUNTS_ALL_STATUSES, null);
        for (AccountStatus s : AccountStatus.values()) {
            statusOptions.put(s.getDisplayName(), s);
        }
        wireAccountsPill(accountsStatusPill, accountsStatusValueLabel, statusOptions, v -> {
            accountsStatusFilter = v;
            applyAccountsFilter();
        });

        LinkedHashMap<String, AccountsLastLoginFilter> loginOptions = new LinkedHashMap<>();
        for (AccountsLastLoginFilter f : AccountsLastLoginFilter.values()) {
            loginOptions.put(f.label(), f);
        }
        wireAccountsPill(accountsLastLoginPill, accountsLastLoginValueLabel, loginOptions, v -> {
            accountsLastLoginFilter = v;
            applyAccountsFilter();
            reloadAccounts();   // date/range filter -> backend reload
        });

        // table
        accountsEmptyLabel.getStyleClass().add("accounts-empty-label");
        accountsTable.setPlaceholder(accountsEmptyLabel);
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        accountsTable.setFixedCellSize(57.0);
        accountsFiltered = new FilteredList<>(accountsMaster, a -> true);
        accountsTable.setItems(accountsFiltered);

        bindAccountColumn(accountsColName,      () -> accountTextCell(a -> valueOrDash(a.fullName()), "accounts-name"));
        bindAccountColumn(accountsColUsername,  () -> accountTextCell(
                a -> (a.username() == null || a.username().isBlank()) ? "\u2014" : "@" + a.username(),
                "accounts-username"));
        bindAccountColumn(accountsColRole,      this::accountRoleCell);
        bindAccountColumn(accountsColStatus,    this::accountStatusCell);
        bindAccountColumn(accountsColLastLogin, () -> accountTextCell(
                a -> formatAccountLastLogin(a.lastLogin()), "accounts-meta"));
        bindAccountColumn(accountsColCreated,   () -> accountTextCell(
                a -> a.createdDate() == null ? "\u2014" : a.createdDate().format(ACCOUNTS_DATE_FMT),
                "accounts-meta"));
        bindAccountColumn(accountsColActions,   this::accountActionsCell);

        updateAccountsEmptyState();
    }

    private <T> void wireAccountsPill(HBox pill, Label valueLabel,
                                      LinkedHashMap<String, T> options, Consumer<T> onPick) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-menu");
        options.forEach((label, value) -> {
            MenuItem item = new MenuItem(label);
            item.setMnemonicParsing(false);
            item.setOnAction(e -> {
                valueLabel.setText(label);
                onPick.accept(value);
            });
            menu.getItems().add(item);
        });
        pill.setOnMouseClicked(e -> {
            if (menu.isShowing()) {
                menu.hide();
            } else {
                menu.show(pill, Side.BOTTOM, 0.0, 4.0);
            }
        });
    }

    @FXML
    private void onAccountsClearFilters() {
        boolean hadLoginFilter = accountsLastLoginFilter != AccountsLastLoginFilter.ANYTIME;

        accountsRoleFilter = null;
        accountsStatusFilter = null;
        accountsLastLoginFilter = AccountsLastLoginFilter.ANYTIME;
        accountsRoleValueLabel.setText(ACCOUNTS_ALL_ROLES);
        accountsStatusValueLabel.setText(ACCOUNTS_ALL_STATUSES);
        accountsLastLoginValueLabel.setText(AccountsLastLoginFilter.ANYTIME.label());
        accountsSearchField.clear();

        applyAccountsFilter();
        if (hadLoginFilter) {
            reloadAccounts();
        }
    }

    private void applyAccountsFilter() {
        if (accountsFiltered == null) {
            return;
        }
        String q = accountsSearchField.getText() == null ? ""
                : accountsSearchField.getText().trim().toLowerCase(Locale.ROOT);

        accountsFiltered.setPredicate(a ->
                (accountsRoleFilter == null || a.role() == accountsRoleFilter)
                        && (accountsStatusFilter == null || a.status() == accountsStatusFilter)
                        && matchesAccountsLastLogin(a)
                        && (q.isEmpty()
                        || containsIgnoreCase(a.fullName(), q)
                        || containsIgnoreCase(a.username(), q)));
        updateAccountsEmptyState();
    }

    private boolean matchesAccountsLastLogin(AccountRow a) {
        LocalDateTime ll = a.lastLogin();
        LocalDate today = LocalDate.now();
        return switch (accountsLastLoginFilter) {
            case ANYTIME      -> true;
            case NEVER        -> ll == null;
            case TODAY        -> ll != null && ll.toLocalDate().equals(today);
            case LAST_7_DAYS  -> ll != null && !ll.isBefore(today.minusDays(6).atStartOfDay());
            case LAST_30_DAYS -> ll != null && !ll.isBefore(today.minusDays(29).atStartOfDay());
        };
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    private void updateAccountsEmptyState() {
        accountsEmptyLabel.setText(accountsMaster.isEmpty()
                ? "No accounts loaded."
                : "No accounts match your filters.");
    }

    private void reloadAccounts() {
        // TODO: ask the backend for accounts matching accountsLastLoginFilter,
        //       then call setAccounts(...).
    }

    private void bindAccountColumn(TableColumn<AccountRow, AccountRow> col,
                                   Supplier<TableCell<AccountRow, AccountRow>> cellSupplier) {
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(tc -> cellSupplier.get());
    }

    /** Base cell for the accounts table: clears itself when empty, otherwise calls render(). */
    private abstract static class AccountCell extends TableCell<AccountRow, AccountRow> {
        @Override
        protected void updateItem(AccountRow item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                render(item);
            }
        }

        protected abstract void render(AccountRow row);
    }

    private TableCell<AccountRow, AccountRow> accountTextCell(Function<AccountRow, String> text, String styleClass) {
        return new AccountCell() {
            private final Label label = new Label();
            { label.getStyleClass().add(styleClass); }

            @Override
            protected void render(AccountRow r) {
                label.setText(text.apply(r));
                setGraphic(label);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountRoleCell() {
        return new AccountCell() {
            private final Label chip = new Label();

            @Override
            protected void render(AccountRow r) {
                if (r.role() == null) {
                    chip.setText("\u2014");
                    chip.getStyleClass().setAll("accounts-meta");
                } else {
                    chip.setText(r.role().getDisplayName());
                    chip.getStyleClass().setAll("accounts-role-chip",
                            "accounts-role-" + r.role().name().toLowerCase(Locale.ROOT));
                }
                setGraphic(chip);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountStatusCell() {
        return new AccountCell() {
            private final HBox chip = new HBox(6.0);
            private final Region dot = new Region();
            private final Label text = new Label();
            {
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                dot.getStyleClass().add("chip-dot");
                text.getStyleClass().add("chip-text");
                chip.getChildren().addAll(dot, text);
            }

            @Override
            protected void render(AccountRow r) {
                AccountStatus s = r.status();
                String cls = s == null ? "chip-neutral" : switch (s) {
                    case ACTIVE    -> "chip-success";
                    case INACTIVE  -> "chip-warning";
                };
                chip.getStyleClass().setAll("chip", cls);
                text.setText(s == null ? "\u2014" : s.getDisplayName());
                setGraphic(chip);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountActionsCell() {
        return new AccountCell() {
            private AccountRow current;
            private final Button editBtn   = accountsIconButton(Icons.SQUARE_PEN, "icon-brand",  "Edit account");
            private final Button deleteBtn = accountsIconButton(Icons.TRASH_2,    "icon-danger", "Delete account");
            private final HBox box = new HBox(8.0, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e -> {
                    if (onEditAccount != null && current != null) {
                        onEditAccount.accept(current);
                    }
                });
                deleteBtn.setOnAction(e -> {
                    if (onDeleteAccount != null && current != null) {
                        onDeleteAccount.accept(current);
                    }
                });
            }

            @Override
            protected void render(AccountRow r) {
                current = r;
                setGraphic(box);
            }
        };
    }

    private Button accountsIconButton(String pathData, String colorClass, String tooltip) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().addAll("phosphor-icon", colorClass);
        fitGridIcon(icon, 14.0, 1.5);   // the design uses a 1.5px stroke here

        StackPane host = new StackPane(icon);
        host.setMinSize(14.0, 14.0);
        host.setPrefSize(14.0, 14.0);
        host.setMaxSize(14.0, 14.0);

        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("accounts-icon-btn");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private String formatAccountLastLogin(LocalDateTime ll) {
        if (ll == null) {
            return "Never";
        }
        long days = ChronoUnit.DAYS.between(ll.toLocalDate(), LocalDate.now());
        if (days <= 0) {
            return "Today, " + ll.format(ACCOUNTS_TIME_FMT);
        }
        if (days == 1) {
            return "Yesterday, " + ll.format(ACCOUNTS_TIME_FMT);
        }
        if (days < 7) {
            return days + " days ago";
        }
        long weeks = days / 7;
        if (weeks < 5) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }
        return ll.toLocalDate().format(ACCOUNTS_DATE_FMT);
    }

    /* ============================== EDIT ACCOUNT ============================== */

    private void setupEditAccountPage() {
        List<AccountRole> allowableRoles = new ArrayList<>();
        for (AccountRole r : AccountRole.values()) {
            if (r != AccountRole.ADMIN) {
                allowableRoles.add(r);
            }
        }
        editAccountRoleCombo.getItems().setAll(allowableRoles);
        editAccountRoleCombo.setConverter(new StringConverter<>() {
            @Override public String toString(AccountRole r) { return r == null ? "" : r.getDisplayName(); }
            @Override public AccountRole fromString(String s) { return null; }
        });

        editAccountStatusCombo.getItems().setAll(AccountStatus.values());
        editAccountStatusCombo.setConverter(new StringConverter<>() {
            @Override public String toString(AccountStatus s) { return s == null ? "" : s.getDisplayName(); }
            @Override public AccountStatus fromString(String s) { return null; }
        });

        if (editAccountVisiblePasswordField != null && editAccountPasswordField != null) {
            editAccountVisiblePasswordField.managedProperty().bind(editAccountVisiblePasswordField.visibleProperty());
            editAccountPasswordField.managedProperty().bind(editAccountPasswordField.visibleProperty());
        }

        setEditAccountError(null);

        //SHORTCUT ENTER KEY
        editAccountNameField.setOnAction(e -> editAccountUsernameField.requestFocus());

        editAccountUsernameField.setOnAction(e -> {
            if (editAccountVisiblePasswordField.isVisible()) {
                editAccountVisiblePasswordField.requestFocus();
            } else {
                editAccountPasswordField.requestFocus();
            }
        });

        editAccountPasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
        editAccountVisiblePasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
    }

    /** Opens the Edit Account layer for the given row (called by the pen button). */
    private void showEditAccount(AccountRow account) {
        editingAccount = account;

        editAccountNameField.setText(account.fullName() == null ? "" : account.fullName());
        editAccountUsernameField.setText(account.username() == null ? "" :  "" + account.username());

        editAccountPasswordField.setText("");
        editAccountVisiblePasswordField.setText("");

        editAccountPasswordField.setVisible(true);
        editAccountVisiblePasswordField.setVisible(false);

        editAccountRoleCombo.setValue(account.role());
        editAccountStatusCombo.setValue(account.status());
        editAccountCreatedLabel.setText(account.createdDate() == null
                ? "\u2014" : account.createdDate().format(EDIT_ACCOUNT_DATE_FMT));
        editAccountLastLoginLabel.setText(formatAccountLastLogin(account.lastLogin()));
        setEditAccountError(null);

        showView(editAccountView);
        pageTitleLabel.setText("Edit Account");
        pageSubtitleLabel.setText("Update credentials, configure roles, access rights, and security statuses");
        setActiveNav(navAccountManagementBtn);   // keep the sidebar highlight on Accounts
    }

    @FXML
    private void onEditAccountChangePassword() {
        // TODO: open a change-password dialog for editingAccount.
    }
    @FXML
    private void onEditAccountCancel() {
        boolean confirmed = com.kainanresto.util.AlertUtil.showYesNoConfirmation(
                "Cancel Edit",
                "Discard changes?",
                "Are you sure you want to cancel? Any unsaved changes will be lost."
        );

        if (confirmed) {
            editingAccount = null;
            showAccounts();
        }
    }

    @FXML
    private void onEditAccountSave() {
        setEditAccountError(null);
        if (editingAccount == null) {
            return;
        }

        String name = editAccountNameField.getText() == null ? "" : editAccountNameField.getText().trim();
        String username = editAccountUsernameField.getText() == null ? "" : editAccountUsernameField.getText().trim();

        if (username.startsWith("@")) {
            username = username.substring(1);
        }

        // Grab the password from whichever field is currently active
        String newPassword = editAccountVisiblePasswordField.isVisible()
                ? editAccountVisiblePasswordField.getText()
                : editAccountPasswordField.getText();
        newPassword = newPassword == null ? "" : newPassword; // Don't trim passwords, spaces might be intentional

        AccountRole role = editAccountRoleCombo.getValue();
        AccountStatus status = editAccountStatusCombo.getValue();

        if (name.isEmpty() || username.isEmpty() || role == null || status == null) {
            setEditAccountError("Please fill in all required fields.");
            return;
        }

        // Full Name Validation
        if (!name.matches("^[a-zA-Z\\s\\.]+$")) {
            com.kainanresto.util.AlertUtil.showWarning("Invalid Full Name", "Full name can only contain letters, spaces, and periods.");
            editAccountNameField.requestFocus();
            return;
        }

        // Username Validation
        if (!username.matches("^[a-zA-Z0-9_-]{3,}$")) {
            com.kainanresto.util.AlertUtil.showWarning("Invalid Username", "Username must be at least 3 characters long and contain only letters, numbers, underscores, or hyphens.");
            editAccountUsernameField.requestFocus();
            return;
        }

        // Password Validation (Only validate if they are actually typing a NEW password)
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 8 || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                com.kainanresto.util.AlertUtil.showWarning("Weak Password", "Password must be at least 8 characters long and contain both letters and numbers.");

                // Request focus on whichever password field is currently visible
                if (editAccountVisiblePasswordField.isVisible()) {
                    editAccountVisiblePasswordField.requestFocus();
                } else {
                    editAccountPasswordField.requestFocus();
                }
                return;
            }
        }

        // --- CONFIRMATION BLOCK ---
        boolean confirmed = com.kainanresto.util.AlertUtil.showYesNoConfirmation(
                "Save Changes",
                "Update account details?",
                "Are you sure you want to save these changes to the database?"
        );
        if (!confirmed) {
            return;
        }

        String finalPassword = newPassword.isEmpty() ? null : newPassword;
        com.kainanresto.dao.UserDAO userDAO = new com.kainanresto.dao.UserDAO();

        boolean success = userDAO.updateAccount(
                (int) editingAccount.id(),
                name,
                username,
                finalPassword,
                role,
                status
        );

        if (success) {
            editingAccount = null;
            com.kainanresto.util.AlertUtil.showInfo("Account Updated", "The account details were successfully saved.");
            loadAccountManagementData();
            showAccounts();
        } else {
            setEditAccountError("Failed to update account in the database. Username might already exist.");
        }
    }

    private void setEditAccountError(String message) {
        boolean has = message != null && !message.isBlank();
        editAccountErrorLabel.setText(has ? message : "");
        editAccountErrorLabel.setVisible(has);
        editAccountErrorLabel.setManaged(has);
    }

    @FXML
    private void onToggleEditAccountPasswordVisibility() {
        if (editAccountPasswordField == null || editAccountVisiblePasswordField == null) {
            return;
        }

        boolean isVisible = editAccountVisiblePasswordField.isVisible();
        if (isVisible) {
            editAccountPasswordField.setText(editAccountVisiblePasswordField.getText());
            editAccountPasswordField.setVisible(true);
            editAccountVisiblePasswordField.setVisible(false);
        } else {
            // Showing password: sync text from the hidden field to the visible field
            editAccountVisiblePasswordField.setText(editAccountPasswordField.getText());
            editAccountVisiblePasswordField.setVisible(true);
            editAccountPasswordField.setVisible(false);
        }
    }


    /* ============================== SETTINGS ============================== */

    private void setupSettingsPage() {
        // Language dropdown: options are supplied later by setSettingsLanguageOptions(...)
        settingsLanguageFilter = new FilterPill(settingsLanguagePill, settingsLanguageValueLabel, () -> { });

        // Logo preview: rounded-square clip inside the 1px border (80 - 2 = 78)
        Rectangle clip = new Rectangle(78.0, 78.0);
        clip.setArcWidth(18.0);
        clip.setArcHeight(18.0);
        settingsLogoImage.setFitWidth(78.0);
        settingsLogoImage.setFitHeight(78.0);
        settingsLogoImage.setClip(clip);

        // Sync mode behaves like a radio pair: one is always selected.
        settingsSyncManualBtn.setSelected(true);
        settingsSyncGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                settingsSyncGroup.selectToggle(old);
                return;
            }
            updateSettingsTimeFieldsEnabled();
        });
        updateSettingsTimeFieldsEnabled();

        showSettingsLogo(null);
        setSettingsError(null);
    }

    private void updateSettingsTimeFieldsEnabled() {
        boolean manual = settingsSyncManualBtn.isSelected();
        settingsDateField.setDisable(!manual);
        settingsTimeField.setDisable(!manual);
    }

    /** Puts server values into the form. Null resets everything to its empty state. */
    private void applySettingsToForm(AppSettings s) {
        setSettingsError(null);
        if (s == null) {
            settingsNameField.clear();
            settingsEmailField.clear();
            settingsDateField.clear();
            settingsTimeField.clear();
            settingsSyncManualBtn.setSelected(true);
            settingsAutoBackupToggle.setSelected(false);
            settingsRequirePinToggle.setSelected(false);
            settingsPendingLanguage = null;
            settingsLogoUri = null;
            showSettingsLogo(null);
            return;
        }
        settingsNameField.setText(s.restaurantName() == null ? "" : s.restaurantName());
        settingsEmailField.setText(s.contactEmail() == null ? "" : s.contactEmail());

        settingsLogoUri = s.logoUri();
        showSettingsLogo(settingsLogoUri);

        if (s.timeSyncMode() == TimeSyncMode.AUTO) {
            settingsSyncAutoBtn.setSelected(true);
        } else {
            settingsSyncManualBtn.setSelected(true);
        }
        settingsDateField.setText(s.systemDate() == null ? "" : s.systemDate().format(SETTINGS_DATE_FMT));
        settingsTimeField.setText(s.systemTime() == null ? "" : s.systemTime().format(SETTINGS_TIME_FMT));

        settingsPendingLanguage = s.language();
        if (settingsPendingLanguage != null) {
            settingsLanguageFilter.setSelected(settingsPendingLanguage);
        }

        settingsAutoBackupToggle.setSelected(s.autoBackupEnabled());
        settingsRequirePinToggle.setSelected(s.requirePinForSensitiveActions());
    }

    /** Shows the logo at the given URI, or the placeholder icon when null/blank/unloadable. */
    private void showSettingsLogo(String uri) {
        boolean shown = false;
        if (uri != null && !uri.isBlank()) {
            try {
                Image image = new Image(uri, 156.0, 156.0, true, true, true);
                settingsLogoImage.setImage(image);
                applyCoverCrop(settingsLogoImage, image);
                shown = true;
            } catch (IllegalArgumentException ex) {
                settingsLogoImage.setImage(null);
            }
        } else {
            settingsLogoImage.setImage(null);
        }
        settingsLogoImage.setVisible(shown);
        settingsLogoImage.setManaged(shown);
        settingsLogoPlaceholderIcon.setVisible(!shown);
    }

    /** Validates the form. Returns null (and shows a message) when something is invalid. */
    private AppSettings readSettingsForm() {
        String name = settingsNameField.getText() == null ? "" : settingsNameField.getText().trim();
        if (name.isEmpty()) {
            setSettingsError("Restaurant name is required.");
            return null;
        }
        String email = settingsEmailField.getText() == null ? "" : settingsEmailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            setSettingsError("Enter a valid contact email.");
            return null;
        }

        TimeSyncMode mode = settingsSyncAutoBtn.isSelected() ? TimeSyncMode.AUTO : TimeSyncMode.MANUAL;
        LocalDate date = null;
        LocalTime time = null;
        if (mode == TimeSyncMode.MANUAL) {
            try {
                date = LocalDate.parse(normalizeSpaces(settingsDateField.getText()), SETTINGS_DATE_FMT);
            } catch (DateTimeParseException ex) {
                setSettingsError("System date must be in the form Month D, YYYY.");
                return null;
            }
            try {
                time = LocalTime.parse(normalizeSpaces(settingsTimeField.getText()), SETTINGS_TIME_FMT);
            } catch (DateTimeParseException ex) {
                setSettingsError("System time must be in the form H:MM AM/PM.");
                return null;
            }
        }

        return new AppSettings(
                name,
                email.isEmpty() ? null : email,
                settingsLogoUri,
                mode,
                date,
                time,
                settingsLanguageFilter.getSelected(),
                settingsAutoBackupToggle.isSelected(),
                settingsRequirePinToggle.isSelected());
    }

    /** Trims and turns the narrow no-break space that newer JDKs put before AM/PM into a normal one. */
    private static String normalizeSpaces(String s) {
        return s == null ? "" : s.replace('\u202F', ' ').replace('\u00A0', ' ').trim();
    }

    @FXML
    private void onSettingsSave() {
        setSettingsError(null);
        AppSettings values = readSettingsForm();
        if (values == null) {
            return;
        }
        if (onSaveSettings != null) {
            onSaveSettings.accept(values);
        }
    }

    @FXML
    private void onSettingsCancel() {
        applySettingsToForm(settingsLoaded);
    }

    @FXML
    private void onSettingsChangeLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose restaurant logo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(appRoot.getScene().getWindow());
        if (file != null) {
            settingsLogoUri = file.toURI().toString();
            showSettingsLogo(settingsLogoUri);
        }
    }

    @FXML
    private void onSettingsBackupDatabase() {
        if (onBackupDatabase != null) {
            onBackupDatabase.run();
        }
        // TODO: backend backup call.
    }

    @FXML
    private void onSettingsExportData() {
        if (onExportData != null) {
            onExportData.run();
        }
        // TODO: backend CSV export (choose a target file with FileChooser).
    }

    @FXML
    private void onSettingsRestorePoint() {
        if (onRestorePoint != null) {
            onRestorePoint.run();
        }
        // TODO: restore-point picker + confirmation dialog (design not supplied yet).
    }

    @FXML
    private void onSettingsTabGeneral() {
        selectSettingsTab(settingsTabGeneralBtn, settingsGeneralCard);
    }

    @FXML
    private void onSettingsTabPreferences() {
        selectSettingsTab(settingsTabPreferencesBtn, settingsPreferencesCard);
    }

    @FXML
    private void onSettingsTabBackup() {
        selectSettingsTab(settingsTabBackupBtn, settingsBackupCard);
    }

    @FXML
    private void onSettingsTabSecurity() {
        selectSettingsTab(settingsTabSecurityBtn, settingsSecurityCard);
    }

    /** Marks the tab active and scrolls the card column so the matching card is at the top. */
    private void selectSettingsTab(Button active, Node section) {
        for (Button btn : new Button[]{
                settingsTabGeneralBtn, settingsTabPreferencesBtn,
                settingsTabBackupBtn, settingsTabSecurityBtn}) {
            btn.getStyleClass().remove(SETTINGS_TAB_ACTIVE);
        }
        if (!active.getStyleClass().contains(SETTINGS_TAB_ACTIVE)) {
            active.getStyleClass().add(SETTINGS_TAB_ACTIVE);
        }
        scrollSettingsTo(section);
    }

    private void scrollSettingsTo(Node section) {
        double contentHeight = settingsContent.getBoundsInLocal().getHeight();
        double viewportHeight = settingsScroll.getViewportBounds().getHeight();
        double scrollable = contentHeight - viewportHeight;
        if (scrollable <= 0.0) {
            return;
        }
        double y = section.getBoundsInParent().getMinY();   // section is a direct child of settingsContent
        settingsScroll.setVvalue(Math.max(0.0, Math.min(1.0, y / scrollable)));
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

        /** Selects a value without notifying, if it is one of the current options. */
        private void setSelected(String value) {
            if (value != null && options.contains(value)) {
                select(value, false);
            }
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
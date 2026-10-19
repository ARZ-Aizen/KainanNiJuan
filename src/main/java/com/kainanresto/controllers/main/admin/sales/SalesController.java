package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.dao.OrderDAO;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStats;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.Transaction;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;

public class SalesController {

    @FXML private VBox salesRoot;

    @FXML private Label ordersRevenueValueLabel, ordersRevenueNoteLabel;
    @FXML private Label ordersTotalValueLabel, ordersTotalNoteLabel;
    @FXML private Label ordersVoidsValueLabel, ordersVoidsNoteLabel;
    @FXML private Label ordersAvgSpendValueLabel, ordersAvgSpendNoteLabel;

    @FXML private HBox ordersSearchBox, orderDatePill, orderStatusPill, orderPaymentPill, orderSortPill;
    @FXML private TextField ordersSearchField;
    @FXML private Label orderDateValueLabel, orderStatusValueLabel, orderPaymentValueLabel, orderSortValueLabel;

    @FXML private SVGPath ordersSearchIcon, ordersRevenueIcon, ordersTotalIcon, ordersVoidsIcon, ordersAvgSpendIcon;

    @FXML private TableView<Transaction> ordersTable;
    @FXML private Label ordersTablePlaceholder;
    @FXML private TableColumn<Transaction, String> orderIdColumn, orderTypeColumn, orderCreatedColumn, orderTotalColumn, orderStatusColumn, orderPaymentColumn;
    @FXML private TableColumn<Transaction, Transaction> orderStaffColumn, orderActionsColumn;

    @FXML private HBox paginationBar;
    @FXML private Label paginationInfoLabel;
    @FXML private Button prevPageBtn, nextPageBtn;
    @FXML private HBox pageNumbersBox;
    @FXML private Button exportSalesBtn;
    @FXML private SVGPath exportSalesIcon;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Transaction> orderItems = FXCollections.observableArrayList();
    private FilterPill orderDateFilter, orderStatusFilter, orderPaymentFilter, orderSortFilter;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<Transaction> filteredAndSortedList = new ArrayList<>();
    private final ObservableList<Transaction> displayedPageItems = FXCollections.observableArrayList();

    private ContextMenu activeActionsMenu;
    private boolean refreshPending;

    @FXML
    public void initialize() {
        setupIcons();
        setupOrdersPage();
        loadSalesData();
    }

    private void setupIcons() {
        setIconAndScale(ordersSearchIcon, Icons.SEARCH, 14.0);
        setIconAndScale(ordersRevenueIcon, Icons.DOLLAR_SIGN, 18.0);
        setIconAndScale(ordersTotalIcon, Icons.SHOPPING_BAG, 18.0);
        setIconAndScale(ordersVoidsIcon, Icons.ROTATE_CCW, 18.0);
        setIconAndScale(ordersAvgSpendIcon, Icons.TRENDING_UP, 18.0);
        setIconAndScale(exportSalesIcon, Icons.DOWNLOAD, 16.0);

        for (Node node : salesRoot.lookupAll(".filter-chevron")) {
            if (node instanceof SVGPath chevron) {
                setIconAndScale(chevron, Icons.CHEVRON_DOWN, 14.0);
            }
        }
    }

    private void setIconAndScale(SVGPath icon, String content, double targetSize) {
        if (icon != null && content != null) {
            icon.setContent(content);
            double scale = targetSize / 24.0;
            icon.setScaleX(scale);
            icon.setScaleY(scale);
            icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
        }
    }

    public void refreshData() {
        loadSalesData();
    }

    public void refresh() {
        loadSalesData();
    }

    private void loadSalesData() {
        if (activeActionsMenu != null && activeActionsMenu.isShowing()) {
            refreshPending = true;
            return;
        }

        LocalDateTime[] range = dateRange(orderDateFilter == null ? null : orderDateFilter.selected);
        Thread t = new Thread(() -> {
            List<Transaction> tx = orderDAO.getTransactions(range[0], range[1]);
            OrderStats stats = orderDAO.getOrderStats();
            Platform.runLater(() -> {
                setOrderStats(stats);
                setOrders(tx);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private static LocalDateTime[] dateRange(String option) {
        LocalDate today = LocalDate.now();
        LocalDate start = today, end = today.plusDays(1);
        if (option != null) {
            switch (option) {
                case "Yesterday"   -> { start = today.minusDays(1); end = today; }
                case "Last 7 Days" -> start = today.minusDays(6);
                case "This Month"  -> start = today.withDayOfMonth(1);
                case "All Time"    -> start = LocalDate.of(2000, 1, 1);
                default -> { }
            }
        }
        return new LocalDateTime[]{start.atStartOfDay(), end.atStartOfDay()};
    }

    private void setupOrdersPage() {
        orderDateFilter    = new FilterPill(orderDatePill, orderDateValueLabel, this::onOrderDateChanged);
        orderStatusFilter  = new FilterPill(orderStatusPill, orderStatusValueLabel, this::onFilterChanged);
        orderPaymentFilter = new FilterPill(orderPaymentPill, orderPaymentValueLabel, this::onFilterChanged);
        orderSortFilter    = new FilterPill(orderSortPill, orderSortValueLabel, this::onFilterChanged);

        orderDateFilter.setOptions(List.of("Today", "Yesterday", "Last 7 Days", "This Month", "All Time"));
        orderStatusFilter.setOptions(List.of("All States", "Preparing", "Completed", "Cancelled"));
        orderPaymentFilter.setOptions(List.of("All", "Paid", "Refunded"));
        orderSortFilter.setOptions(List.of("Newest First", "Oldest First"));

        setupOrdersTable();

        ordersSearchField.focusedProperty().addListener((obs, was, is) -> ordersSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        ordersSearchField.textProperty().addListener((obs, oldText, newText) -> onFilterChanged());

        applyOrderFilters(true);
    }

    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderId()));
        orderTypeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderType()));
        orderStaffColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        orderCreatedColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatOrderTimestamp(cd.getValue().lastActivityAt())));
        orderTotalColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatPesoCompact(cd.getValue().total())));
        orderStatusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().status()));
        orderPaymentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().paymentStatus()));
        orderActionsColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        orderStaffColumn.setCellFactory(col -> new StaffCell());
        orderStatusColumn.setCellFactory(col -> new ChipCell(true, SalesController::statusChipClass));
        orderPaymentColumn.setCellFactory(col -> new ChipCell(false, SalesController::paymentChipClass));
        orderActionsColumn.setCellFactory(col -> new ActionsCell());

        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        ordersTable.setItems(displayedPageItems);

        ordersTable.skinProperty().addListener((obs, o, n) -> disableTableScrollbars());
    }

    private void disableTableScrollbars() {
        ScrollBar vBar = (ScrollBar) ordersTable.lookup(".scroll-bar:vertical");
        if (vBar != null) {
            vBar.setDisable(true);
            vBar.setVisible(false);
            vBar.setManaged(false);
            vBar.setPrefWidth(0);
            vBar.setMaxWidth(0);
        }
        ScrollBar hBar = (ScrollBar) ordersTable.lookup(".scroll-bar:horizontal");
        if (hBar != null) {
            hBar.setDisable(true);
            hBar.setVisible(false);
            hBar.setManaged(false);
            hBar.setPrefHeight(0);
            hBar.setMaxHeight(0);
        }
    }

    private void onFilterChanged() {
        currentPage = 1;
        applyOrderFilters(true);
    }

    private void applyOrderFilters(boolean resetPage) {
        String query = ordersSearchField.getText() == null ? "" : ordersSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction order : orderItems) {
            if (matchesOrderText(order, query)
                    && (orderStatusFilter == null || orderStatusFilter.accepts(order.status()))
                    && (orderPaymentFilter == null || orderPaymentFilter.accepts(order.paymentStatus()))) {
                filtered.add(order);
            }
        }

        boolean oldestFirst = orderSortFilter != null && "Oldest First".equalsIgnoreCase(orderSortFilter.selected);
        filtered.sort((a, b) -> {
            LocalDateTime timeA = a.lastActivityAt() != null ? a.lastActivityAt() : (a.createdAt() != null ? a.createdAt() : LocalDateTime.MIN);
            LocalDateTime timeB = b.lastActivityAt() != null ? b.lastActivityAt() : (b.createdAt() != null ? b.createdAt() : LocalDateTime.MIN);
            int cmp = timeA.compareTo(timeB);
            if (cmp != 0) {
                return oldestFirst ? cmp : -cmp;
            }
            String idA = a.orderId() == null ? "" : a.orderId();
            String idB = b.orderId() == null ? "" : b.orderId();
            return oldestFirst ? idA.compareTo(idB) : idB.compareTo(idA);
        });
        filteredAndSortedList = filtered;

        ordersTablePlaceholder.setText(orderItems.isEmpty() ? "No orders found." : "No orders match your filters.");

        if (resetPage) {
            currentPage = 1;
        }
        updatePageDisplay();
    }

    private void updatePageDisplay() {
        int totalItems = filteredAndSortedList.size();
        this.totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = totalItems == 0 ? 0 : (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<Transaction> slice = totalItems == 0 ? List.of() : filteredAndSortedList.subList(fromIndex, toIndex);
        displayedPageItems.setAll(slice);

        if (totalItems == 0) {
            paginationInfoLabel.setText("Showing 0 of 0 orders");
        } else {
            paginationInfoLabel.setText(String.format(Locale.ENGLISH, "Showing %,d–%,d of %,d orders", (fromIndex + 1), toIndex, totalItems));
        }

        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        renderPaginationControls(currentPage, totalPages);
        Platform.runLater(this::disableTableScrollbars);
    }

    private void renderPaginationControls(int curPage, int numPages) {
        pageNumbersBox.getChildren().clear();
        if (numPages <= 1) {
            Button btn = createPageButton(1, true);
            pageNumbersBox.getChildren().add(btn);
            return;
        }

        if (numPages <= 7) {
            for (int p = 1; p <= numPages; p++) {
                pageNumbersBox.getChildren().add(createPageButton(p, p == curPage));
            }
            return;
        }

        // When numPages > 7: balanced 7-element window with '...' ellipsis
        if (curPage <= 4) {
            for (int p = 1; p <= 5; p++) {
                pageNumbersBox.getChildren().add(createPageButton(p, p == curPage));
            }
            pageNumbersBox.getChildren().add(createEllipsisButton(Math.min(numPages, curPage + 5)));
            pageNumbersBox.getChildren().add(createPageButton(numPages, false));
        } else if (curPage >= numPages - 3) {
            pageNumbersBox.getChildren().add(createPageButton(1, false));
            pageNumbersBox.getChildren().add(createEllipsisButton(Math.max(1, curPage - 5)));
            for (int p = numPages - 4; p <= numPages; p++) {
                pageNumbersBox.getChildren().add(createPageButton(p, p == curPage));
            }
        } else {
            pageNumbersBox.getChildren().add(createPageButton(1, false));
            pageNumbersBox.getChildren().add(createEllipsisButton(Math.max(1, curPage - 5)));
            pageNumbersBox.getChildren().add(createPageButton(curPage - 1, false));
            pageNumbersBox.getChildren().add(createPageButton(curPage, true));
            pageNumbersBox.getChildren().add(createPageButton(curPage + 1, false));
            pageNumbersBox.getChildren().add(createEllipsisButton(Math.min(numPages, curPage + 5)));
            pageNumbersBox.getChildren().add(createPageButton(numPages, false));
        }
    }

    private Button createEllipsisButton(int jumpTarget) {
        Button btn = new Button("...");
        btn.getStyleClass().addAll("pagination-page-btn", "pagination-ellipsis-btn");
        btn.setMnemonicParsing(false);
        btn.setTooltip(new Tooltip("Jump to page " + jumpTarget));
        btn.setOnAction(e -> goToPage(jumpTarget));
        return btn;
    }

    private Button createPageButton(int page, boolean active) {
        Button btn = new Button(String.valueOf(page));
        btn.getStyleClass().add("pagination-page-btn");
        btn.setMnemonicParsing(false);
        if (active) {
            btn.getStyleClass().add("pagination-page-active");
        } else {
            btn.setOnAction(e -> goToPage(page));
        }
        return btn;
    }

    private void goToPage(int page) {
        this.currentPage = page;
        updatePageDisplay();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            goToPage(currentPage - 1);
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            goToPage(currentPage + 1);
        }
    }

    private static boolean matchesOrderText(Transaction order, String query) {
        if (query.isEmpty()) return true;
        return (order.orderId() != null && order.orderId().toLowerCase(Locale.ENGLISH).contains(query))
                || (order.customerName() != null && order.customerName().toLowerCase(Locale.ENGLISH).contains(query))
                || (order.staffName() != null && order.staffName().toLowerCase(Locale.ENGLISH).contains(query));
    }

    private void onOrderDateChanged() {
        currentPage = 1;
        loadSalesData();
    }

    @FXML
    private void onClearOrderFilters() {
        boolean dateWasChanged = !orderDateFilter.isDefault();
        ordersSearchField.clear();
        orderDateFilter.reset();
        orderStatusFilter.reset();
        orderPaymentFilter.reset();
        if (orderSortFilter != null) orderSortFilter.reset();
        currentPage = 1;
        applyOrderFilters(true);
        if (dateWasChanged) onOrderDateChanged();
    }

    private void onOrderActions(Transaction order, Node anchor) {
        if (activeActionsMenu != null) activeActionsMenu.hide();

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-menu");

        MenuItem viewReceipt = new MenuItem("View Receipt");
        viewReceipt.setMnemonicParsing(false);
        viewReceipt.setOnAction(e -> viewReceipt(order));
        menu.getItems().add(viewReceipt);

        menu.setOnHidden(e -> {
            if (activeActionsMenu == menu) activeActionsMenu = null;
            if (refreshPending) {
                refreshPending = false;
                loadSalesData();
            }
        });

        activeActionsMenu = menu;
        menu.show(anchor, Side.BOTTOM, 0.0, 4.0);
    }

    private void viewReceipt(Transaction order) {
        Thread worker = new Thread(() -> {
            try {
                OrderReceipt receipt = orderDAO.findReceipt(order.orderId());
                if (receipt == null) {
                    Platform.runLater(() -> AlertUtil.showError("View Receipt",
                            "Could not find the details for order " + order.orderId() + "."));
                    return;
                }
                PaymentResult pay = orderDAO.findPayment(order.orderId());
                Platform.runLater(() -> showReceiptModal(order, receipt, pay));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtil.showError("View Receipt",
                        "The receipt could not be loaded."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showReceiptModal(Transaction order, OrderReceipt receipt, PaymentResult pay) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/kainanresto/views/main/admin/sales/ReceiptModalView.fxml"));
            VBox root = loader.load();

            ReceiptModalController controller = loader.getController();
            controller.setReceiptData(order, receipt, pay);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            if (salesRoot.getScene() != null) {
                stage.initOwner(salesRoot.getScene().getWindow());
                root.getStylesheets().addAll(salesRoot.getScene().getStylesheets());
            }

            root.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/kainanresto/styles/util/ReceiptModalView.css"),
                    "ReceiptModalView.css not found").toExternalForm());

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Could not load the receipt view.");
        }
    }

    public void setOrderStats(OrderStats stats) {
        if (stats == null) {
            ordersRevenueValueLabel.setText("—"); ordersRevenueNoteLabel.setText("—");
            ordersTotalValueLabel.setText("—"); ordersTotalNoteLabel.setText("—");
            ordersVoidsValueLabel.setText("—"); ordersVoidsNoteLabel.setText("—");
            ordersAvgSpendValueLabel.setText("—"); ordersAvgSpendNoteLabel.setText("—");
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

    public void setOrders(List<Transaction> orders) {
        List<Transaction> incoming = orders == null ? List.of() : orders;
        if (!orderItems.equals(incoming)) {
            orderItems.setAll(incoming);
            applyOrderFilters(false);
        }
    }

    private static String formatPesoCompact(BigDecimal amount) {
        return amount == null ? "—" : String.format(Locale.ENGLISH, "₱%,.2f", amount);
    }

    private static String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    private static String formatOrderTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "—";
        LocalDate day = timestamp.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) return "Today, " + timestamp.format(ORDER_TIME_FORMAT);
        if (day.equals(today.minusDays(1))) return "Yesterday, " + timestamp.format(ORDER_TIME_FORMAT);
        return timestamp.format(ORDER_DATE_TIME_FORMAT);
    }

    private static String statusChipClass(String status) {
        String s = status.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("complete")) return "chip-success";
        if (s.startsWith("prepar")) return "chip-warning";
        if (s.startsWith("cancel") || s.startsWith("void")) return "chip-danger";
        return "chip-pending";
    }

    private static String paymentChipClass(String payment) {
        String s = payment.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("paid")) return "chip-success";
        if (s.startsWith("unpaid")) return "chip-danger";
        return "chip-neutral";
    }

    private static final class StaffCell extends TableCell<Transaction, Transaction> {
        private final Label name = new Label();
        private final Label role = new Label();
        private final VBox box = new VBox(2.0, name, role);

        private StaffCell() {
            box.setAlignment(Pos.CENTER_LEFT);
            box.maxWidthProperty().bind(widthProperty().subtract(12.0));
            name.getStyleClass().add("staff-name");
            role.getStyleClass().add("staff-role");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override protected void updateItem(Transaction order, boolean empty) {
            super.updateItem(order, empty);
            setText(null);
            if (empty || order == null) { setGraphic(null); return; }
            name.setText(order.staffName());
            boolean hasRole = order.staffRole() != null && !order.staffRole().isBlank();
            role.setText(hasRole ? order.staffRole() : "");
            role.setVisible(hasRole); role.setManaged(hasRole);
            setGraphic(box);
        }
    }

    private static final class ChipCell extends TableCell<Transaction, String> {
        private final Region dot = new Region();
        private final Label text = new Label();
        private final HBox chip = new HBox(6.0);
        private final HBox holder = new HBox(chip);
        private final Function<String, String> styleResolver;
        private String appliedStyle;

        private ChipCell(boolean withDot, Function<String, String> styleResolver) {
            this.styleResolver = styleResolver;

            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("chip");
            chip.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            dot.getStyleClass().add("chip-dot");
            text.getStyleClass().add("chip-text");
            if (withDot) chip.getChildren().add(dot);
            chip.getChildren().add(text);

            holder.setAlignment(Pos.CENTER_LEFT);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setText(null);
            if (empty || value == null || value.isBlank()) { setGraphic(null); return; }
            text.setText(value.trim());
            if (appliedStyle != null) chip.getStyleClass().remove(appliedStyle);
            appliedStyle = styleResolver.apply(value);
            chip.getStyleClass().add(appliedStyle);
            setGraphic(holder);
        }
    }

    private final class ActionsCell extends TableCell<Transaction, Transaction> {
        private final Button button = new Button();

        private ActionsCell() {
            SVGPath icon = new SVGPath();
            icon.setContent(Icons.ELLIPSIS_VERTICAL);
            icon.getStyleClass().addAll("phosphor-icon", "icon-brand");
            double scale = 14.0 / 24.0;
            icon.setScaleX(scale); icon.setScaleY(scale); icon.setStyle("-fx-stroke-width: " + (1.5 / scale) + ";");

            StackPane host = new StackPane(icon);
            host.getStyleClass().add("icon-host-14");

            button.setGraphic(host);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMnemonicParsing(false);
            button.getStyleClass().add("row-action-btn");
            button.setOnAction(e -> {
                Transaction order = getItem();
                if (order != null) onOrderActions(order, button);
            });
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override protected void updateItem(Transaction order, boolean empty) {
            super.updateItem(order, empty);
            setText(null);
            setGraphic(empty || order == null ? null : button);
        }
    }

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
            menu.showingProperty().addListener((obs, was, showing) -> pill.pseudoClassStateChanged(OPEN, showing));
            pill.setOnMouseClicked(e -> {
                if (options.isEmpty()) return;
                if (menu.isShowing()) menu.hide(); else menu.show(pill, Side.BOTTOM, 0.0, 4.0);
            });
        }

        private void setOptions(List<String> newOptions) {
            options = newOptions == null ? List.of() : List.copyOf(newOptions);
            select(options.isEmpty() ? null : options.get(0), false);
        }
        private boolean isDefault() { return options.isEmpty() || Objects.equals(selected, options.get(0)); }
        private boolean accepts(String value) { return isDefault() || (value != null && value.equalsIgnoreCase(selected)); }
        private void reset() { if (!options.isEmpty()) select(options.get(0), false); }

        private void select(String value, boolean notify) {
            selected = value;
            valueLabel.setText(value == null ? "—" : value);
            List<MenuItem> items = new ArrayList<>();
            for (String option : options) {
                MenuItem item = new MenuItem(option);
                item.setMnemonicParsing(false);
                if (option.equals(selected)) item.getStyleClass().add("filter-option-selected");
                item.setOnAction(e -> { if (!option.equals(selected)) select(option, true); });
                items.add(item);
            }
            menu.getItems().setAll(items);
            if (notify) onChange.run();
        }
    }

    @FXML
    private void onExportSales() {
        Optional<YearMonth> picked = MonthFilterController.show(
                salesRoot.getScene().getWindow(), "Export Sales History");
        if (picked.isEmpty()) return;
        YearMonth ym = picked.get();

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Sales History");
        chooser.setInitialFileName("Sales_" + ym + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel/CSV File", "*.csv"));

        File file = chooser.showSaveDialog(salesRoot.getScene().getWindow());
        if (file == null) return;

        Thread t = new Thread(() -> {
            try {
                List<Transaction> sales = orderDAO.getTransactions(start, endExclusive);

                if (sales.isEmpty()) {
                    Platform.runLater(() -> AlertUtil.showInfo("No Data",
                            "There are no transactions for "
                                    + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                    + " " + ym.getYear() + "."));
                    return;
                }

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a", Locale.ENGLISH);
                try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    writer.print('\uFEFF');
                    writer.println("Order ID,Created At,Last Activity,Staff,Total Amount,Order Status,Payment Status");
                    for (Transaction tx : sales) {
                        writer.println(String.join(",",
                                csv(tx.orderId()),
                                csv(tx.createdAt() != null ? tx.createdAt().format(fmt) : ""),
                                csv(tx.lastActivityAt() != null ? tx.lastActivityAt().format(fmt) : ""),
                                csv(tx.staffName()),
                                tx.total() == null ? "0.00" : tx.total().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                                csv(tx.status()),
                                csv(tx.paymentStatus())));
                    }
                }

                Platform.runLater(() -> AlertUtil.showInfo("Export Successful",
                        sales.size() + " transactions exported to:\n" + file.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Export Error",
                        "Could not create the file:\n" + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
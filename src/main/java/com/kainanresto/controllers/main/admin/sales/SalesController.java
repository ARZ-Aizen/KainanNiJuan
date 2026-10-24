package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.dao.OrderDAO;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.SalesAnalyticsData;
import com.kainanresto.model.order.SalesPeriod;
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
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import com.kainanresto.controllers.main.admin.accounts.AdminAuthDialog;
import javafx.beans.binding.Bindings;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

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

    @FXML private ScrollPane salesRootScroll;
    @FXML private VBox salesRoot;

    // Active vs Archived Views
    @FXML private VBox activeOrdersContainer, archivedOrdersContainer;
    @FXML private Button viewArchiveBtn;

    // Orders History Elements
    @FXML private HBox ordersSearchBox, orderDatePill, orderStatusPill, orderPaymentPill, orderSortPill;
    @FXML private TextField ordersSearchField;
    @FXML private Label orderDateValueLabel, orderStatusValueLabel, orderPaymentValueLabel, orderSortValueLabel;
    @FXML private SVGPath ordersSearchIcon, exportSalesIcon;
    @FXML private Button clearOrderFiltersBtn, exportSalesBtn;

    @FXML private TableView<Transaction> ordersTable;
    @FXML private Label ordersTablePlaceholder;
    @FXML private TableColumn<Transaction, String> orderIdColumn, orderTypeColumn, orderCreatedColumn, orderTotalColumn, orderStatusColumn, orderPaymentColumn;
    @FXML private TableColumn<Transaction, Transaction> orderStaffColumn, orderActionsColumn;

    // Archived Orders Elements
    @FXML private TableView<Transaction> archivedOrdersTable;
    @FXML private Label archivedOrdersPlaceholder;
    @FXML private TableColumn<Transaction, String> archivedOrderIdCol, archivedOrderTypeCol, archivedOrderCreatedCol, archivedOrderTotalCol, archivedOrderStatusCol, archivedOrderPaymentCol;
    @FXML private TableColumn<Transaction, Transaction> archivedOrderStaffCol, archivedOrderActionsCol;
    private final ObservableList<Transaction> archivedOrderItems = FXCollections.observableArrayList();

    @FXML private HBox paginationBar;
    @FXML private Label paginationInfoLabel;
    @FXML private Button prevPageBtn, nextPageBtn;
    @FXML private HBox pageNumbersBox;

    // Constants & Formatters
    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Transaction> orderItems = FXCollections.observableArrayList();
    private FilterPill orderDateFilter, orderStatusFilter, orderPaymentFilter, orderSortFilter;

    // 10 items per page
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<Transaction> filteredAndSortedList = new ArrayList<>();
    private final ObservableList<Transaction> displayedPageItems = FXCollections.observableArrayList();

    private ContextMenu activeActionsMenu;
    private boolean refreshPending;

    @FXML
    public void initialize() {
        if (salesRoot != null && salesRootScroll != null) {
            salesRoot.minHeightProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(0.0, salesRootScroll.getViewportBounds().getHeight()),
                    salesRootScroll.viewportBoundsProperty()
            ));
        }
        setupIcons();
        setupOrdersPage();
        setupArchivedOrdersTable();
        loadSalesData();
    }

    private void setupIcons() {
        setIconAndScale(ordersSearchIcon, Icons.SEARCH, 14.0);
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

    // =========================================================================
    // ORDERS HISTORY LOGIC & RENDERING
    // =========================================================================

    public void refreshData() {
        loadSalesData();
    }

    public void refresh() {
        refreshData();
    }

    private void loadSalesData() {
        if (activeActionsMenu != null && activeActionsMenu.isShowing()) {
            refreshPending = true;
            return;
        }

        LocalDateTime[] range = dateRange(orderDateFilter == null ? null : orderDateFilter.selected);
        Thread t = new Thread(() -> {
            List<Transaction> tx = orderDAO.getTransactions(range[0], range[1]);
            Platform.runLater(() -> setOrders(tx));
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

        // Dynamic height that expands with available space (58px row height)
        ordersTable.setFixedCellSize(58.0);
        ordersTable.setMinHeight(300.0);
        ordersTable.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        // Forward scroll events to outer ScrollPane so scrolling over the table smoothly scrolls the page
        ordersTable.addEventFilter(ScrollEvent.ANY, event -> {
            if (salesRootScroll != null) {
                salesRootScroll.fireEvent(event.copyFor(salesRootScroll, salesRootScroll));
                event.consume();
            }
        });

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

    private void setupArchivedOrdersTable() {
        archivedOrderIdCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderId()));
        archivedOrderTypeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderType()));
        archivedOrderStaffCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        archivedOrderCreatedCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatOrderTimestamp(cd.getValue().lastActivityAt())));
        archivedOrderTotalCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatPesoCompact(cd.getValue().total())));
        archivedOrderStatusCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().status()));
        archivedOrderPaymentCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().paymentStatus()));
        archivedOrderActionsCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        archivedOrderStaffCol.setCellFactory(col -> new StaffCell());
        archivedOrderStatusCol.setCellFactory(col -> new ChipCell(true, SalesController::statusChipClass));
        archivedOrderPaymentCol.setCellFactory(col -> new ChipCell(false, SalesController::paymentChipClass));
        archivedOrderActionsCol.setCellFactory(col -> new ArchivedActionsCell());

        archivedOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        archivedOrdersTable.setItems(archivedOrderItems);
        archivedOrdersTable.setFixedCellSize(58.0);
        archivedOrdersTable.setMinHeight(300.0);
        archivedOrdersTable.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(archivedOrdersTable, Priority.ALWAYS);
    }

    @FXML
    private void onShowArchive() {
        activeOrdersContainer.setVisible(false);
        activeOrdersContainer.setManaged(false);
        archivedOrdersContainer.setVisible(true);
        archivedOrdersContainer.setManaged(true);
        loadArchivedOrders();
    }

    @FXML
    private void onShowActiveOrders() {
        archivedOrdersContainer.setVisible(false);
        archivedOrdersContainer.setManaged(false);
        activeOrdersContainer.setVisible(true);
        activeOrdersContainer.setManaged(true);
        loadSalesData();
    }

    private void loadArchivedOrders() {
        Thread t = new Thread(() -> {
            List<Transaction> list = orderDAO.getArchivedTransactions();
            Platform.runLater(() -> archivedOrderItems.setAll(list));
        });
        t.setDaemon(true);
        t.start();
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

        MenuItem editTransaction = new MenuItem("Edit Transaction");
        editTransaction.setMnemonicParsing(false);
        editTransaction.setOnAction(e -> editTransaction(order));

        MenuItem archiveTransaction = new MenuItem("Archive Transaction");
        archiveTransaction.setMnemonicParsing(false);
        archiveTransaction.setOnAction(e -> archiveTransaction(order));

        menu.getItems().addAll(viewReceipt, editTransaction, archiveTransaction);

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

    private void editTransaction(Transaction order) {
        Thread worker = new Thread(() -> {
            try {
                OrderReceipt receipt = orderDAO.findReceipt(order.orderId());
                if (receipt == null) {
                    Platform.runLater(() -> AlertUtil.showError("Edit Transaction",
                            "Could not find the details for order " + order.orderId() + "."));
                    return;
                }
                PaymentResult pay = orderDAO.findPayment(order.orderId());
                Platform.runLater(() -> showEditTransactionModal(order, receipt, pay));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtil.showError("Edit Transaction",
                        "The transaction details could not be loaded."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showEditTransactionModal(Transaction order, OrderReceipt receipt, PaymentResult pay) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/kainanresto/views/main/admin/sales/EditTransactionModalView.fxml"));
            VBox root = loader.load();

            EditTransactionModalController controller = loader.getController();
            controller.setOrderData(order, receipt, pay, this::loadSalesData);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            if (salesRoot.getScene() != null) {
                stage.initOwner(salesRoot.getScene().getWindow());
            }

            var adminCss = getClass().getResource("/com/kainanresto/styles/main/AdminView.css");
            if (adminCss != null) {
                root.getStylesheets().add(adminCss.toExternalForm());
            }

            var modalCss = getClass().getResource("/com/kainanresto/styles/util/EditTransactionModalView.css");
            if (modalCss != null) {
                root.getStylesheets().add(modalCss.toExternalForm());
            }

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Could not load the edit transaction modal: " + e.getMessage());
        }
    }

    private void archiveTransaction(Transaction order) {
        if (!AlertUtil.showYesNoConfirmation("Archive Transaction",
                "Archive transaction #" + order.orderId() + "?",
                "This transaction will be moved to the archive.")) {
            return;
        }

        Window owner = salesRoot != null && salesRoot.getScene() != null ? salesRoot.getScene().getWindow() : null;
        if (!AdminAuthDialog.show(owner, "Authorize archiving Order #" + order.orderId())) {
            return;
        }

        Thread t = new Thread(() -> {
            boolean ok = orderDAO.archiveTransaction(order.orderId());
            Platform.runLater(() -> {
                if (ok) {
                    AlertUtil.showInfo("Archived", "Transaction #" + order.orderId() + " has been archived.");
                    loadSalesData();
                } else {
                    AlertUtil.showError("Error", "Could not archive transaction.");
                }
            });
        });
        t.setDaemon(true);
        t.start();
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
        if (s.startsWith("refund")) return "chip-danger";
        return "chip-neutral";
    }

    // =========================================================================
    // EXPORT SALES LOGIC
    // =========================================================================

    @FXML
    private void onExportSales() {
        List<YearMonth> picked = MonthFilterController.show(
                salesRoot.getScene().getWindow(), "Export Sales History");
        if (picked == null || picked.isEmpty()) return;

        String initialFileName = generateExportFileName(picked);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Sales History");
        chooser.setInitialFileName(initialFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel/CSV File", "*.csv"));

        File file = chooser.showSaveDialog(salesRoot.getScene().getWindow());
        if (file == null) return;

        Thread t = new Thread(() -> {
            try {
                List<Transaction> sales = orderDAO.getTransactionsForMonths(picked);

                if (sales.isEmpty()) {
                    String periodDesc;
                    if (picked.size() == 1) {
                        YearMonth ym = picked.get(0);
                        periodDesc = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                + " " + ym.getYear();
                    } else {
                        periodDesc = "the selected " + picked.size() + " months ("
                                + picked.get(0) + " to " + picked.get(picked.size() - 1) + ")";
                    }
                    Platform.runLater(() -> AlertUtil.showInfo("No Data",
                            "There are no transactions for " + periodDesc + "."));
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

    private static String generateExportFileName(List<YearMonth> months) {
        if (months == null || months.isEmpty()) return "Sales_Export.csv";
        if (months.size() == 1) return "Sales_" + months.get(0) + ".csv";
        YearMonth first = months.get(0);
        YearMonth last = months.get(months.size() - 1);
        return "Sales_" + first + "_to_" + last + ".csv";
    }

    private static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // =========================================================================
    // INNER CLASSES & CELL FACTORIES
    // =========================================================================

    private static final class StaffCell extends TableCell<Transaction, Transaction> {
        private final Label nameLabel = new Label();
        private final Label roleLabel = new Label();
        private final VBox box = new VBox(1.0, nameLabel, roleLabel);

        StaffCell() {
            nameLabel.getStyleClass().add("staff-name");
            roleLabel.getStyleClass().add("staff-role");
            box.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Transaction item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.staffName());
                roleLabel.setText(item.staffRole());
                setGraphic(box);
            }
        }
    }

    private static final class ChipCell extends TableCell<Transaction, String> {
        private final boolean showDot;
        private final Function<String, String> styleResolver;
        private final Region dot = new Region();
        private final Label text = new Label();
        private final HBox chip = new HBox(6.0);

        ChipCell(boolean showDot, Function<String, String> styleResolver) {
            this.showDot = showDot;
            this.styleResolver = styleResolver;
            dot.getStyleClass().add("chip-dot");
            text.getStyleClass().add("chip-text");
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("chip");
            chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            chip.setMinHeight(24.0);
            chip.setPrefHeight(24.0);
            chip.setMaxHeight(24.0);
            if (showDot) {
                chip.getChildren().addAll(dot, text);
            } else {
                chip.getChildren().add(text);
            }
            setAlignment(Pos.CENTER_LEFT);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String val, boolean empty) {
            super.updateItem(val, empty);
            setText(null);
            if (empty || val == null || val.isBlank()) {
                setGraphic(null);
            } else {
                text.setText(val.trim());
                chip.getStyleClass().removeIf(c -> c.startsWith("chip-") && !c.equals("chip"));
                chip.getStyleClass().add(styleResolver.apply(val));
                setGraphic(chip);
            }
        }
    }

    private final class ActionsCell extends TableCell<Transaction, Transaction> {
        private final Button btn = new Button();
        private final StackPane iconHost = new StackPane();
        private final SVGPath icon = new SVGPath();

        ActionsCell() {
            btn.getStyleClass().add("row-action-btn");
            btn.setFocusTraversable(false);
            btn.setMnemonicParsing(false);
            icon.getStyleClass().addAll("phosphor-icon", "icon-muted");
            setIconAndScale(icon, Icons.ELLIPSIS_VERTICAL, 14.0);
            iconHost.getChildren().add(icon);
            btn.setGraphic(iconHost);
            btn.setOnAction(e -> {
                Transaction tx = getItem();
                if (tx != null) {
                    onOrderActions(tx, btn);
                }
            });
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Transaction item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic((empty || item == null) ? null : btn);
        }
    }

    private final class ArchivedActionsCell extends TableCell<Transaction, Transaction> {
        private final Button restoreBtn = new Button();
        private final Button deleteBtn = new Button();
        private final Button receiptBtn = new Button();
        private final HBox box = new HBox(6.0, restoreBtn, deleteBtn, receiptBtn);

        ArchivedActionsCell() {
            box.setAlignment(Pos.CENTER);

            // Restore button
            restoreBtn.getStyleClass().add("row-action-btn");
            restoreBtn.setFocusTraversable(false);
            restoreBtn.setTooltip(new Tooltip("Restore transaction"));
            SVGPath restoreIcon = new SVGPath();
            restoreIcon.setContent(Icons.ARROWS_CLOCKWISE);
            restoreIcon.getStyleClass().addAll("phosphor-icon");
            restoreIcon.setStyle("-fx-stroke: #8C6343; -fx-stroke-width: 2.0;");
            double scale = 14.0 / 24.0;
            restoreIcon.setScaleX(scale);
            restoreIcon.setScaleY(scale);
            restoreBtn.setGraphic(restoreIcon);
            restoreBtn.setOnAction(e -> {
                Transaction tx = getItem();
                if (tx != null) restoreArchivedTransaction(tx);
            });

            // Delete button
            deleteBtn.getStyleClass().add("row-action-btn");
            deleteBtn.setFocusTraversable(false);
            deleteBtn.setTooltip(new Tooltip("Permanently delete transaction"));
            SVGPath trashIcon = new SVGPath();
            trashIcon.setContent(Icons.TRASH_2);
            trashIcon.getStyleClass().addAll("phosphor-icon");
            trashIcon.setStyle("-fx-stroke: #E74C3C; -fx-stroke-width: 2.0;");
            trashIcon.setScaleX(scale);
            trashIcon.setScaleY(scale);
            deleteBtn.setGraphic(trashIcon);
            deleteBtn.setOnAction(e -> {
                Transaction tx = getItem();
                if (tx != null) permanentlyDeleteArchivedTransaction(tx);
            });

            // Receipt button
            receiptBtn.getStyleClass().add("row-action-btn");
            receiptBtn.setFocusTraversable(false);
            receiptBtn.setTooltip(new Tooltip("View receipt"));
            SVGPath receiptIcon = new SVGPath();
            receiptIcon.setContent(Icons.RECEIPT_TEXT);
            receiptIcon.getStyleClass().addAll("phosphor-icon");
            receiptIcon.setStyle("-fx-stroke: #6E625C; -fx-stroke-width: 2.0;");
            receiptIcon.setScaleX(scale);
            receiptIcon.setScaleY(scale);
            receiptBtn.setGraphic(receiptIcon);
            receiptBtn.setOnAction(e -> {
                Transaction tx = getItem();
                if (tx != null) viewReceipt(tx);
            });
        }

        @Override
        protected void updateItem(Transaction item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic((empty || item == null) ? null : box);
        }
    }

    private void restoreArchivedTransaction(Transaction order) {
        if (!AlertUtil.showYesNoConfirmation("Restore Transaction",
                "Restore transaction #" + order.orderId() + "?",
                "This transaction will be restored to active transactions.")) {
            return;
        }

        Window owner = salesRoot != null && salesRoot.getScene() != null ? salesRoot.getScene().getWindow() : null;
        if (!AdminAuthDialog.show(owner, "Authorize restoring Order #" + order.orderId())) {
            return;
        }

        Thread t = new Thread(() -> {
            boolean ok = orderDAO.restoreTransaction(order.orderId());
            Platform.runLater(() -> {
                if (ok) {
                    AlertUtil.showInfo("Restored", "Transaction #" + order.orderId() + " has been restored.");
                    loadArchivedOrders();
                    loadSalesData();
                } else {
                    AlertUtil.showError("Error", "Could not restore transaction.");
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void permanentlyDeleteArchivedTransaction(Transaction order) {
        if (!AlertUtil.showYesNoConfirmation("Permanently Delete",
                "Permanently delete transaction #" + order.orderId() + "?",
                "This action cannot be undone. All order items and history for this transaction will be permanently removed.")) {
            return;
        }

        Window owner = salesRoot != null && salesRoot.getScene() != null ? salesRoot.getScene().getWindow() : null;
        if (!AdminAuthDialog.show(owner, "Authorize permanently deleting Order #" + order.orderId())) {
            return;
        }

        Thread t = new Thread(() -> {
            boolean ok = orderDAO.deleteTransactionPermanently(order.orderId());
            Platform.runLater(() -> {
                if (ok) {
                    AlertUtil.showInfo("Deleted", "Transaction #" + order.orderId() + " has been permanently deleted.");
                    loadArchivedOrders();
                } else {
                    AlertUtil.showError("Error", "Could not delete transaction.");
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private static final class FilterPill {
        private final HBox node;
        private final Label valueLabel;
        private final Runnable onChange;
        private String selected;
        private String defaultOption;
        private final List<String> options = new ArrayList<>();

        FilterPill(HBox node, Label valueLabel, Runnable onChange) {
            this.node = node;
            this.valueLabel = valueLabel;
            this.onChange = onChange;
            this.node.setOnMouseClicked(e -> showMenu());
        }

        void setOptions(List<String> list) {
            options.clear();
            options.addAll(list);
            if (!options.isEmpty()) {
                defaultOption = options.get(0);
                setSelected(defaultOption, false);
            }
        }

        void setSelected(String val, boolean notify) {
            selected = val;
            valueLabel.setText(val);
            if (!val.equals(defaultOption)) {
                if (!node.getStyleClass().contains("filter-pill-active")) {
                    node.getStyleClass().add("filter-pill-active");
                }
            } else {
                node.getStyleClass().remove("filter-pill-active");
            }
            if (notify && onChange != null) {
                onChange.run();
            }
        }

        boolean isDefault() {
            return Objects.equals(selected, defaultOption);
        }

        void reset() {
            if (defaultOption != null) {
                setSelected(defaultOption, false);
            }
        }

        boolean accepts(String target) {
            if (selected == null || selected.equals(defaultOption)) return true;
            if (target == null) return false;
            return target.equalsIgnoreCase(selected);
        }

        private void showMenu() {
            ContextMenu menu = new ContextMenu();
            menu.getStyleClass().add("filter-menu");
            for (String opt : options) {
                MenuItem item = new MenuItem(opt);
                item.setMnemonicParsing(false);
                if (opt.equals(selected)) {
                    item.getStyleClass().add("filter-option-selected");
                }
                item.setOnAction(e -> setSelected(opt, true));
                menu.getItems().add(item);
            }
            menu.show(node, Side.BOTTOM, 0.0, 4.0);
        }
    }
}
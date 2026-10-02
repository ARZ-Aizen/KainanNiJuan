package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.dao.OrderDAO;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStats;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.Transaction;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.ReceiptPdfGenerator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import com.kainanresto.util.AlertUtil;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class SalesController {

    @FXML private VBox salesRoot;

    @FXML private Label ordersRevenueValueLabel, ordersRevenueNoteLabel;
    @FXML private Label ordersTotalValueLabel, ordersTotalNoteLabel;
    @FXML private Label ordersVoidsValueLabel, ordersVoidsNoteLabel;
    @FXML private Label ordersAvgSpendValueLabel, ordersAvgSpendNoteLabel;

    @FXML private HBox ordersSearchBox, orderDatePill, orderStatusPill, orderPaymentPill;
    @FXML private TextField ordersSearchField;
    @FXML private Label orderDateValueLabel, orderStatusValueLabel, orderPaymentValueLabel;

    @FXML private SVGPath ordersSearchIcon, ordersRevenueIcon, ordersTotalIcon, ordersVoidsIcon, ordersAvgSpendIcon;

    @FXML private TableView<Transaction> ordersTable;
    @FXML private Label ordersTablePlaceholder;
    @FXML private TableColumn<Transaction, String> orderIdColumn, orderTypeColumn, orderCreatedColumn, orderTotalColumn, orderStatusColumn, orderPaymentColumn;
    @FXML private TableColumn<Transaction, Transaction> orderStaffColumn, orderActionsColumn;
    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Transaction> orderItems = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredOrders;
    private FilterPill orderDateFilter, orderStatusFilter, orderPaymentFilter;

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

    /* ============================== DATA LOADING ============================== */

    private void loadSalesData() {
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

    /** Call this when the admin opens the Sales tab so it shows fresh data. */
    public void refresh() { loadSalesData(); }

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

    /* ============================== SETUP & FILTERS ============================== */

    private void setupOrdersPage() {
        orderDateFilter    = new FilterPill(orderDatePill, orderDateValueLabel, this::onOrderDateChanged);
        orderStatusFilter  = new FilterPill(orderStatusPill, orderStatusValueLabel, this::applyOrderFilters);
        orderPaymentFilter = new FilterPill(orderPaymentPill, orderPaymentValueLabel, this::applyOrderFilters);

        // The first option in each list is the "no filter" default
        orderDateFilter.setOptions(List.of("Today", "Yesterday", "Last 7 Days", "This Month", "All Time"));
        orderStatusFilter.setOptions(List.of("All States", "Preparing", "Completed", "Cancelled"));

        // Updated to only show Paid and Refunded
        orderPaymentFilter.setOptions(List.of("All", "Paid", "Refunded"));

        setupOrdersTable();

        ordersSearchField.focusedProperty().addListener((obs, was, is) -> ordersSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        ordersSearchField.textProperty().addListener((obs, oldText, newText) -> applyOrderFilters());

        applyOrderFilters();
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

        filteredOrders = new FilteredList<>(orderItems, o -> true);
        ordersTable.setItems(filteredOrders);
    }

    private void applyOrderFilters() {
        if (filteredOrders == null) return;
        String query = ordersSearchField.getText() == null ? "" : ordersSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        filteredOrders.setPredicate(order ->
                matchesOrderText(order, query)
                        && orderStatusFilter.accepts(order.status())
                        && orderPaymentFilter.accepts(order.paymentStatus()));

        ordersTablePlaceholder.setText(orderItems.isEmpty() ? "No orders found." : "No orders match your filters.");
    }

    private static boolean matchesOrderText(Transaction order, String query) {
        if (query.isEmpty()) return true;
        return (order.orderId() != null && order.orderId().toLowerCase(Locale.ENGLISH).contains(query))
                || (order.customerName() != null && order.customerName().toLowerCase(Locale.ENGLISH).contains(query))
                || (order.staffName() != null && order.staffName().toLowerCase(Locale.ENGLISH).contains(query));
    }

    /** The date range is applied in the database query, so reload. */
    private void onOrderDateChanged() { loadSalesData(); }

    @FXML
    private void onClearOrderFilters() {
        boolean dateWasChanged = !orderDateFilter.isDefault();
        ordersSearchField.clear();
        orderDateFilter.reset();
        orderStatusFilter.reset();
        orderPaymentFilter.reset();
        applyOrderFilters();
        if (dateWasChanged) onOrderDateChanged();
    }

    private void onOrderActions(Transaction order, Node anchor) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-menu");

        MenuItem viewReceipt = new MenuItem("View Receipt");
        viewReceipt.setMnemonicParsing(false);
        viewReceipt.setOnAction(e -> viewReceipt(order));

        menu.getItems().add(viewReceipt);
        menu.show(anchor, Side.BOTTOM, 0.0, 4.0);
    }

    /** Placeholder: will generate the receipt PDF and open it once PDF conversion is built. */
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
                File pdf = ReceiptPdfGenerator.generate(receipt, pay);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(pdf);
                } else {
                    Platform.runLater(() -> AlertUtil.showInfo("View Receipt",
                            "Receipt saved to:\n" + pdf.getAbsolutePath()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtil.showError("View Receipt",
                        "The receipt PDF could not be created."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /* ============================== DATA SETTERS ============================== */

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
        orderItems.setAll(orders == null ? List.of() : orders);
        applyOrderFilters();
    }

    /* ============================== FORMATTERS ============================== */

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

    /* ============================== INNER CLASSES ============================== */

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
}
package com.kainanresto.controllers.main.admin.dashboard;

import com.kainanresto.controllers.main.admin.dashboard.DashboardData.BestSeller;
import com.kainanresto.dao.OrderDAO;
import com.kainanresto.dao.TransactionDAO;
import com.kainanresto.model.order.SalesAnalyticsData;
import com.kainanresto.model.order.SalesPeriod;
import com.kainanresto.model.transac.Transaction;
import com.kainanresto.model.util.Icons;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class DashboardController {

    // Legacy Range enum for compatibility
    public enum Range { TODAY, WEEK, MONTH }

    // Period Selection Toggle Buttons (Daily, Weekly, Monthly, Quarterly, Yearly)
    @FXML private ToggleButton dailyRangeBtn, weeklyRangeBtn, monthlyRangeBtn, quarterlyRangeBtn, yearlyRangeBtn;
    @FXML private ToggleGroup analyticsPeriodGroup;

    // Stat cards: Profit, Loss, Orders Count, Total Menu Items (retained, replacing Average Spend)
    @FXML private Label profitSalesValueLabel, profitSalesNoteLabel;
    @FXML private Label lossSalesValueLabel, lossSalesNoteLabel;
    @FXML private Label orderCountValueLabel, orderCountNoteLabel;
    @FXML private Label totalMenuItemsValueLabel, totalMenuItemsSubLabel;

    @FXML private SVGPath profitSalesIcon, lossSalesIcon, orderCountIcon, menuStatIcon;
    @FXML private SVGPath peakSalesIcon, lowestSalesIcon;

    // Peak & Lowest Sales Highlights
    @FXML private Label maxSalesValueLabel, maxSalesDateLabel;
    @FXML private Label minSalesValueLabel, minSalesDateLabel;

    // BarChart
    @FXML private Label salesChartTitleLabel, salesChartSubtitleLabel;
    @FXML private BarChart<String, Number> salesBarChart;
    @FXML private CategoryAxis salesXAxis;
    @FXML private NumberAxis salesYAxis;

    // Period Overview & Breakdown
    @FXML private Label grossSalesValueLabel, avgPeriodicSalesLabel, totalItemsSoldLabel;
    @FXML private VBox orderTypesBreakdownBox;

    // Lower Section: Best Sellers (Preserved) & Recent Transactions (Replaces Sales Overview)
    @FXML private VBox bestSellingItemsContainer;
    @FXML private Button viewAllOrdersBtn;
    @FXML private TableView<Transaction> recentOrdersTable;
    @FXML private TableColumn<Transaction, String> recentOrderIdColumn, recentOrderTypeColumn, recentOrderCreatedColumn, recentOrderTotalColumn, recentOrderStatusColumn, recentOrderPaymentColumn;
    @FXML private TableColumn<Transaction, Transaction> recentOrderStaffColumn;

    private static final String DASH = "—";
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private SalesPeriod currentPeriod = SalesPeriod.WEEKLY;

    private final OrderDAO orderDAO = new OrderDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        setupIcons();
        setupPeriodToggles();
        setupRecentOrdersTable();
        fetchLiveData();
        startAutoRefresh(30);
    }

    private void setupIcons() {
        setIconAndScale(profitSalesIcon, Icons.DOLLAR_SIGN, 18.0);
        setIconAndScale(lossSalesIcon, Icons.ROTATE_CCW, 18.0);
        setIconAndScale(orderCountIcon, Icons.SHOPPING_BAG, 18.0);
        setIconAndScale(menuStatIcon, Icons.NAV_MENU, 18.0);
        setIconAndScale(peakSalesIcon, Icons.AWARD, 18.0);
        setIconAndScale(lowestSalesIcon, Icons.TRENDING_DOWN, 18.0);
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

    private void setupPeriodToggles() {
        if (analyticsPeriodGroup != null) {
            analyticsPeriodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    analyticsPeriodGroup.selectToggle(oldVal);
                }
            });
        }
    }

    private void setupRecentOrdersTable() {
        if (recentOrdersTable == null) return;

        recentOrderIdColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderId()));
        recentOrderTypeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().orderType()));
        recentOrderStaffColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        recentOrderCreatedColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatOrderTimestamp(cd.getValue().lastActivityAt())));
        recentOrderTotalColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatPeso(cd.getValue().total())));
        recentOrderStatusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().status()));
        recentOrderPaymentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().paymentStatus()));

        recentOrderStaffColumn.setCellFactory(col -> new StaffCell());
        recentOrderStatusColumn.setCellFactory(col -> new ChipCell(true, DashboardController::statusChipClass));
        recentOrderPaymentColumn.setCellFactory(col -> new ChipCell(false, DashboardController::paymentChipClass));

        recentOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        recentOrdersTable.setFixedCellSize(52.0);
    }

    // =========================================================================
    // PERIOD SELECTION (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)
    // =========================================================================

    @FXML
    private void onDailyRangeSelected() {
        updateActivePeriod(SalesPeriod.DAILY, dailyRangeBtn);
    }

    @FXML
    private void onWeeklyRangeSelected() {
        updateActivePeriod(SalesPeriod.WEEKLY, weeklyRangeBtn);
    }

    @FXML
    private void onMonthlyRangeSelected() {
        updateActivePeriod(SalesPeriod.MONTHLY, monthlyRangeBtn);
    }

    @FXML
    private void onQuarterlyRangeSelected() {
        updateActivePeriod(SalesPeriod.QUARTERLY, quarterlyRangeBtn);
    }

    @FXML
    private void onYearlyRangeSelected() {
        updateActivePeriod(SalesPeriod.YEARLY, yearlyRangeBtn);
    }

    private void updateActivePeriod(SalesPeriod period, ToggleButton activeBtn) {
        if (this.currentPeriod == period && activeBtn != null && activeBtn.getStyleClass().contains("range-btn-active")) {
            return;
        }
        this.currentPeriod = period;

        if (dailyRangeBtn != null) dailyRangeBtn.getStyleClass().remove("range-btn-active");
        if (weeklyRangeBtn != null) weeklyRangeBtn.getStyleClass().remove("range-btn-active");
        if (monthlyRangeBtn != null) monthlyRangeBtn.getStyleClass().remove("range-btn-active");
        if (quarterlyRangeBtn != null) quarterlyRangeBtn.getStyleClass().remove("range-btn-active");
        if (yearlyRangeBtn != null) yearlyRangeBtn.getStyleClass().remove("range-btn-active");

        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("range-btn-active")) {
                activeBtn.getStyleClass().add("range-btn-active");
            }
            if (!activeBtn.isSelected()) {
                activeBtn.setSelected(true);
            }
        }
        fetchLiveData();
    }

    // =========================================================================
    // DATA FETCHING & RENDERING
    // =========================================================================

    public void refreshData() {
        fetchLiveData();
    }

    public void fetchLiveData() {
        final SalesPeriod period = this.currentPeriod;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                SalesAnalyticsData analyticsData = orderDAO.getSalesAnalytics(period);
                int totalMenuItems = transactionDAO.getTotalMenuItemsCount();
                int availableMenuItems = transactionDAO.getAvailableMenuItemsCount();
                List<BestSeller> bestSellers = transactionDAO.getTopBestSellers(5);
                List<Transaction> recentOrders = orderDAO.getRecentTransactions(5);

                Platform.runLater(() -> {
                    if (analyticsData != null) {
                        renderSalesAnalytics(analyticsData);
                    }
                    if (totalMenuItemsValueLabel != null) {
                        totalMenuItemsValueLabel.setText(String.valueOf(totalMenuItems));
                    }
                    if (totalMenuItemsSubLabel != null) {
                        totalMenuItemsSubLabel.setText(availableMenuItems + " available");
                    }
                    updateBestSellers(bestSellers);
                    if (recentOrdersTable != null) {
                        recentOrdersTable.getItems().setAll(recentOrders != null ? recentOrders : List.of());
                    }
                });
                return null;
            }
        };

        task.setOnFailed(e -> {
            System.err.println("Dashboard background refresh failed: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void renderSalesAnalytics(SalesAnalyticsData data) {
        // 1. Stat cards: Profit, Loss, Count
        if (profitSalesValueLabel != null) profitSalesValueLabel.setText(formatPeso(data.profit()));
        if (profitSalesNoteLabel != null) profitSalesNoteLabel.setText(String.format(Locale.ENGLISH, "Revenue from %,d completed orders", data.completedOrdersCount()));

        if (lossSalesValueLabel != null) lossSalesValueLabel.setText(formatPeso(data.loss()));
        if (lossSalesNoteLabel != null) lossSalesNoteLabel.setText(String.format(Locale.ENGLISH, "From %,d cancelled/refunded", data.cancelledOrdersCount()));

        if (orderCountValueLabel != null) orderCountValueLabel.setText(String.format(Locale.ENGLISH, "%,d Orders", data.totalOrdersCount()));
        if (orderCountNoteLabel != null) orderCountNoteLabel.setText(String.format(Locale.ENGLISH, "%,d completed · %,d items sold", data.completedOrdersCount(), data.totalItemsSold()));

        // 2. Peak & Lowest Highlights
        if (maxSalesValueLabel != null) maxSalesValueLabel.setText(formatPeso(data.maxSalesAmount()));
        if (maxSalesDateLabel != null) maxSalesDateLabel.setText("Peak: " + (data.maxSalesDate() != null && !data.maxSalesDate().isBlank() ? data.maxSalesDate() : "None"));

        if (minSalesValueLabel != null) minSalesValueLabel.setText(formatPeso(data.minSalesAmount()));
        if (minSalesDateLabel != null) minSalesDateLabel.setText("Lowest: " + (data.minSalesDate() != null && !data.minSalesDate().isBlank() ? data.minSalesDate() : "None"));

        // 3. BarChart
        if (salesChartTitleLabel != null) salesChartTitleLabel.setText(data.period().getDisplayName() + " Sales Analytics Trend");
        if (salesChartSubtitleLabel != null) salesChartSubtitleLabel.setText(data.periodRangeLabel());

        if (salesBarChart != null) {
            salesBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (SalesAnalyticsData.SalesChartPoint pt : data.chartPoints()) {
                XYChart.Data<String, Number> point = new XYChart.Data<>(pt.label(), pt.sales());
                series.getData().add(point);
            }
            salesBarChart.getData().add(series);

            Platform.runLater(() -> {
                Node plotBg = salesBarChart.lookup(".chart-plot-background");
                if (plotBg != null) {
                    plotBg.setStyle("-fx-background-color: transparent;");
                }
                for (XYChart.Data<String, Number> item : series.getData()) {
                    Node node = item.getNode();
                    if (node != null) {
                        node.setStyle("-fx-bar-fill: #8C6343; -fx-background-color: #8C6343; -fx-background-radius: 6px 6px 0 0;");
                        Tooltip.install(node, new Tooltip(item.getXValue() + ": ₱" + String.format(Locale.ENGLISH, "%,.2f", item.getYValue().doubleValue())));
                    }
                }
            });
        }

        // 4. Period Overview & Breakdown
        if (grossSalesValueLabel != null) grossSalesValueLabel.setText(formatPeso(data.grossSales()));
        if (avgPeriodicSalesLabel != null) avgPeriodicSalesLabel.setText(formatPeso(data.averageDailySales()));
        if (totalItemsSoldLabel != null) totalItemsSoldLabel.setText(String.format(Locale.ENGLISH, "%,d items", data.totalItemsSold()));

        // 5. Order Types Breakdown
        if (orderTypesBreakdownBox != null) {
            orderTypesBreakdownBox.getChildren().clear();
            if (data.salesByOrderType().isEmpty()) {
                Label emptyLbl = new Label("No completed orders yet.");
                emptyLbl.setStyle("-fx-text-fill: #6E625C; -fx-font-size: 12px;");
                orderTypesBreakdownBox.getChildren().add(emptyLbl);
            } else {
                BigDecimal totalProfit = data.profit().compareTo(BigDecimal.ZERO) > 0 ? data.profit() : BigDecimal.ONE;
                for (Map.Entry<String, BigDecimal> entry : data.salesByOrderType().entrySet()) {
                    VBox row = createOrderTypeRow(entry.getKey(), entry.getValue(), totalProfit);
                    orderTypesBreakdownBox.getChildren().add(row);
                }
            }
        }
    }

    private VBox createOrderTypeRow(String typeName, BigDecimal amount, BigDecimal totalProfit) {
        VBox box = new VBox(4.0);
        box.getStyleClass().add("order-type-row");

        HBox textRow = new HBox();
        textRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(typeName);
        nameLbl.getStyleClass().add("order-type-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label amtLbl = new Label(formatPeso(amount));
        amtLbl.getStyleClass().add("order-type-amount");
        textRow.getChildren().addAll(nameLbl, spacer, amtLbl);

        StackPane barTrack = new StackPane();
        barTrack.getStyleClass().add("order-type-bar");
        barTrack.setAlignment(Pos.CENTER_LEFT);

        double ratio = amount.doubleValue() / totalProfit.doubleValue();
        if (ratio > 1.0) ratio = 1.0;
        if (ratio < 0.0) ratio = 0.0;

        Region barFill = new Region();
        barFill.getStyleClass().add("order-type-bar-fill");
        final double finalRatio = ratio;
        barTrack.widthProperty().addListener((obs, oldW, newW) -> {
            barFill.setPrefWidth(newW.doubleValue() * finalRatio);
            barFill.setMaxWidth(newW.doubleValue() * finalRatio);
        });

        barTrack.getChildren().add(barFill);
        box.getChildren().addAll(textRow, barTrack);
        return box;
    }

    private void updateBestSellers(List<BestSeller> items) {
        if (bestSellingItemsContainer == null) return;
        bestSellingItemsContainer.getChildren().clear();
        if (items == null || items.isEmpty()) {
            setPlaceholder(bestSellingItemsContainer, 0, "No sales recorded yet.");
            return;
        }
        int rank = 1;
        for (BestSeller b : items) {
            if (b == null) continue;
            Label rankLabel = new Label(String.valueOf(rank++));
            rankLabel.getStyleClass().add("best-seller-rank");

            Label name = new Label(b.name() == null || b.name().isBlank() ? DASH : b.name());
            name.getStyleClass().add("best-seller-name");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label qty = new Label(b.quantitySold() + " sold");
            qty.getStyleClass().add("best-seller-qty");

            HBox row = new HBox(12.0, rankLabel, name, qty);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("best-seller-row");
            bestSellingItemsContainer.getChildren().add(row);
        }
    }

    private void setPlaceholder(VBox container, int keep, String text) {
        if (container.getChildren().size() > keep) {
            container.getChildren().remove(keep, container.getChildren().size());
        }
        if (text != null) {
            Label l = new Label(text);
            l.getStyleClass().add("dashboard-placeholder");
            container.getChildren().add(l);
        }
    }

    public void startAutoRefresh(int intervalSeconds) {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(intervalSeconds), e -> fetchLiveData())
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private static String formatPeso(BigDecimal amount) {
        return amount == null ? DASH : String.format(Locale.ENGLISH, "₱%,.2f", amount);
    }

    private static String formatOrderTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return DASH;
        LocalDate day = timestamp.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) return "Today, " + timestamp.format(ORDER_TIME_FORMAT);
        if (day.equals(today.minusDays(1))) return "Yesterday, " + timestamp.format(ORDER_TIME_FORMAT);
        return timestamp.format(ORDER_DATE_TIME_FORMAT);
    }

    private static String statusChipClass(String status) {
        if (status == null) return "chip-pending";
        String s = status.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("complete")) return "chip-success";
        if (s.startsWith("prepar")) return "chip-warning";
        if (s.startsWith("cancel") || s.startsWith("void")) return "chip-danger";
        return "chip-pending";
    }

    // =========================================================================
    // NAVIGATION: VIEW ALL IN SALES & ORDERS
    // =========================================================================

    @FXML
    private void onViewAllOrders() {
        // Walk up the scene graph to find the MainAdminController and invoke its onNavSalesOrders()
        try {
            Node root = recentOrdersTable != null ? recentOrdersTable.getScene().getRoot() : null;
            if (root == null && viewAllOrdersBtn != null) root = viewAllOrdersBtn.getScene().getRoot();
            if (root != null) {
                // The MainAdminController's appRoot is the StackPane root of the scene
                Object controller = root.getProperties().get("controller");
                if (controller instanceof com.kainanresto.controllers.main.admin.MainAdminController mac) {
                    mac.navigateToSalesOrders();
                } else {
                    // Fallback: use lookup to find nav button and fire it
                    Node navBtn = root.lookup("#navSalesOrdersBtn");
                    if (navBtn instanceof Button btn) {
                        btn.fire();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not navigate to Sales & Orders: " + e.getMessage());
        }
    }

    private static String paymentChipClass(String payment) {
        if (payment == null) return "chip-neutral";
        String s = payment.trim().toLowerCase(Locale.ENGLISH);
        if (s.startsWith("paid")) return "chip-success";
        if (s.startsWith("refund")) return "chip-danger";
        return "chip-neutral";
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
}
package com.kainanresto.controllers.main.admin.dashboard;

import com.kainanresto.controllers.main.admin.dashboard.DashboardData.BestSeller;
import com.kainanresto.controllers.main.admin.dashboard.DashboardData.ChartPoint;
import com.kainanresto.model.util.Icons;
import com.kainanresto.service.DashboardService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardController {

    public enum Range { TODAY, WEEK, MONTH }

    @FXML private SVGPath salesStatIcon, transactionsStatIcon, completedOrdersStatIcon, menuStatIcon;

    @FXML private Label todaysSalesValueLabel, todaysSalesDeltaLabel, transactionsValueLabel, transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel, completedOrdersSubLabel, totalMenuItemsValueLabel, totalMenuItemsSubLabel;

    @FXML private Label chartTitleLabel, chartSubtitleLabel, totalSalesCaptionLabel, ordersCountCaptionLabel;
    @FXML private Button rangeTodayBtn, rangeWeekBtn, rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel, weeklyOrdersCountLabel, avgOrderValueLabel;

    @FXML private VBox bestSellingItemsContainer;

    // Sales Overview summary labels (Bound to updated FXML)
    @FXML private Label weeklySalesSummaryLabel, monthlySalesSummaryLabel, yearlySalesSummaryLabel;

    private static final String DASH = "\u2014";

    private Range range = Range.WEEK;
    private Consumer<Range> onRangeChanged;

    private final DashboardService dashboardService = new DashboardService();
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        setupIcons();
        setupDashboardChart();
        applyRangeCaptions();

        // Re-fetch data whenever a range button (Today / Week / Month) is clicked
        setOnRangeChanged(selectedRange -> fetchLiveData());

        // Initial async data fetch
        fetchLiveData();

        // Auto refresh every 30 seconds
        startAutoRefresh(30);
    }

    /* ============================== ASYNC DATA FETCHING ============================== */

    public void fetchLiveData() {
        Range currentRange = getRange();

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                return dashboardService.fetchDashboardSnapshot(currentRange);
            }
        };

        task.setOnSucceeded(e -> setData(task.getValue()));
        task.setOnFailed(e -> {
            System.err.println("Dashboard background refresh failed: " + task.getException().getMessage());
            clearData();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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

    /* ============================== PUBLIC API ============================== */

    public void setOnRangeChanged(Consumer<Range> handler) { this.onRangeChanged = handler; }

    public Range getRange() { return range; }

    /** Fills the dashboard with new data. Safely handles null checks. */
    public void setData(DashboardData data) {
        if (data == null) {
            clearData();
            return;
        }
        updateStatCards(data);
        updateChart(data);
        updateBestSellers(data.bestSellers());
        updateSalesOverview(data);
    }

    public void clearData() {
        for (Label l : new Label[]{
                todaysSalesValueLabel, todaysSalesDeltaLabel, transactionsValueLabel, transactionsSubLabel,
                completedOrdersValueLabel, completedOrdersSubLabel, totalMenuItemsValueLabel, totalMenuItemsSubLabel,
                totalWeeklySalesLabel, weeklyOrdersCountLabel, avgOrderValueLabel,
                weeklySalesSummaryLabel, monthlySalesSummaryLabel, yearlySalesSummaryLabel}) {
            if (l != null) l.setText(DASH);
        }
        setDelta(null);
        weeklySalesChart.getData().clear();
        setPlaceholder(bestSellingItemsContainer, 0, "No dashboard data loaded.");
    }

    /* ============================== STAT CARDS ============================== */

    private void updateStatCards(DashboardData d) {
        todaysSalesValueLabel.setText(peso(d.todaysSales()));
        setDelta(d.salesDeltaPercent());

        transactionsValueLabel.setText(String.valueOf(d.transactionsToday()));
        transactionsSubLabel.setText(d.preparingOrders() + " preparing");

        completedOrdersValueLabel.setText(String.valueOf(d.completedToday()));
        completedOrdersSubLabel.setText(d.cancelledToday() + " cancelled");

        totalMenuItemsValueLabel.setText(String.valueOf(d.totalMenuItems()));
        totalMenuItemsSubLabel.setText(d.availableMenuItems() + " available");
    }

    private void setDelta(BigDecimal percent) {
        todaysSalesDeltaLabel.getStyleClass().removeAll("delta-up", "delta-down");
        if (percent == null) {
            todaysSalesDeltaLabel.setText(DASH);
            return;
        }
        int sign = percent.signum();
        String text = String.format(Locale.ENGLISH, "%s%.1f%% vs yesterday", sign > 0 ? "+" : "", percent);
        todaysSalesDeltaLabel.setText(text);
        if (sign > 0) todaysSalesDeltaLabel.getStyleClass().add("delta-up");
        else if (sign < 0) todaysSalesDeltaLabel.getStyleClass().add("delta-down");
    }

    /* ============================== CHART ============================== */

    private void updateChart(DashboardData d) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ChartPoint p : d.chartPoints()) {
            if (p == null) continue;
            double value = p.sales() == null ? 0.0 : p.sales().doubleValue();
            series.getData().add(new XYChart.Data<>(p.label(), value));
        }
        weeklySalesChart.getData().setAll(series);

        for (XYChart.Data<String, Number> item : series.getData()) {
            Runnable install = () -> {
                Node bar = item.getNode();
                if (bar != null) Tooltip.install(bar, new Tooltip(item.getXValue() + ": " + peso(BigDecimal.valueOf(item.getYValue().doubleValue()))));
            };
            if (item.getNode() != null) install.run();
            else item.nodeProperty().addListener((obs, old, now) -> install.run());
        }

        BigDecimal total = d.rangeTotalSales();
        int orders = d.rangeOrdersCount();
        totalWeeklySalesLabel.setText(peso(total));
        weeklyOrdersCountLabel.setText(String.valueOf(orders));
        avgOrderValueLabel.setText(total == null || orders <= 0
                ? DASH
                : peso(total.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)));
    }

    /* ============================== BEST SELLERS ============================== */

    private void updateBestSellers(List<BestSeller> items) {
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

    /* ============================== SALES OVERVIEW ============================== */

    private void updateSalesOverview(DashboardData d) {
        if (weeklySalesSummaryLabel != null) weeklySalesSummaryLabel.setText(peso(d.weeklySales()));
        if (monthlySalesSummaryLabel != null) monthlySalesSummaryLabel.setText(peso(d.monthlySales()));
        if (yearlySalesSummaryLabel != null) yearlySalesSummaryLabel.setText(peso(d.yearlySales()));
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

    /* ============================== RANGE ============================== */

    @FXML private void onRangeToday() { selectRange(Range.TODAY); }
    @FXML private void onRangeWeek()  { selectRange(Range.WEEK); }
    @FXML private void onRangeMonth() { selectRange(Range.MONTH); }

    private void selectRange(Range newRange) {
        boolean changed = newRange != range;
        range = newRange;
        setActiveRange(switch (newRange) {
            case TODAY -> rangeTodayBtn;
            case WEEK -> rangeWeekBtn;
            case MONTH -> rangeMonthBtn;
        });
        applyRangeCaptions();
        if (changed && onRangeChanged != null) onRangeChanged.accept(newRange);
    }

    private void applyRangeCaptions() {
        switch (range) {
            case TODAY -> {
                chartTitleLabel.setText("Today's Sales Analytics");
                chartSubtitleLabel.setText("Visualizing net revenue by hour today");
                totalSalesCaptionLabel.setText("Total Sales Today");
                ordersCountCaptionLabel.setText("Orders Count Today");
            }
            case WEEK -> {
                chartTitleLabel.setText("Weekly Sales Analytics");
                chartSubtitleLabel.setText("Visualizing net revenue trends over the past 7 days");
                totalSalesCaptionLabel.setText("Total Weekly Sales");
                ordersCountCaptionLabel.setText("Weekly Orders Count");
            }
            case MONTH -> {
                chartTitleLabel.setText("Monthly Sales Analytics");
                chartSubtitleLabel.setText("Visualizing net revenue trends this month");
                totalSalesCaptionLabel.setText("Total Monthly Sales");
                ordersCountCaptionLabel.setText("Monthly Orders Count");
            }
        }
    }

    private void setActiveRange(Button active) {
        for (Button btn : new Button[]{rangeTodayBtn, rangeWeekBtn, rangeMonthBtn}) {
            btn.getStyleClass().remove("range-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("range-btn-active")) {
            active.getStyleClass().add("range-btn-active");
        }
    }

    /* ============================== SETUP ============================== */

    private void setupIcons() {
        setIconAndScale(salesStatIcon, Icons.DOLLAR_SIGN, 22.0);
        setIconAndScale(transactionsStatIcon, Icons.NAV_SALES_ORDERS, 22.0);
        setIconAndScale(completedOrdersStatIcon, Icons.CIRCLE_CHECK, 22.0);
        setIconAndScale(menuStatIcon, Icons.NAV_MENU, 22.0);
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

    private void setupDashboardChart() {
        if (weeklySalesChart != null) {
            weeklySalesXAxis.setAutoRanging(true);
            weeklySalesXAxis.setTickLabelsVisible(true);
            weeklySalesYAxis.setAutoRanging(true);
            weeklySalesYAxis.setForceZeroInRange(true);
            weeklySalesChart.getData().clear();
        }
    }

    /* ============================== HELPERS ============================== */

    private static String peso(BigDecimal value) {
        return value == null ? DASH : "\u20B1 " + String.format(Locale.ENGLISH, "%,.2f", value);
    }
}
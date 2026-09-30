package com.kainanresto.controllers.main.admin.dashboard;

import com.kainanresto.model.util.Icons;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class DashboardController {

    @FXML private SVGPath salesStatIcon, transactionsStatIcon, completedOrdersStatIcon, menuStatIcon;

    @FXML private Label todaysSalesValueLabel, todaysSalesDeltaLabel, transactionsValueLabel, transactionsSubLabel;
    @FXML private Label completedOrdersValueLabel, completedOrdersSubLabel, totalMenuItemsValueLabel, totalMenuItemsSubLabel;

    @FXML private Button rangeTodayBtn, rangeWeekBtn, rangeMonthBtn;
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis weeklySalesXAxis;
    @FXML private NumberAxis weeklySalesYAxis;
    @FXML private Label totalWeeklySalesLabel, weeklyOrdersCountLabel, avgOrderValueLabel;

    @FXML private VBox bestSellingItemsContainer, lowStockAlertsContainer, outOfStockAlertsContainer;

    @FXML
    public void initialize() {
        setupIcons();
        setupDashboardChart();
    }

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

    @FXML private void onRangeToday() { setActiveRange(rangeTodayBtn); }
    @FXML private void onRangeWeek() { setActiveRange(rangeWeekBtn); }
    @FXML private void onRangeMonth() { setActiveRange(rangeMonthBtn); }

    private void setActiveRange(Button active) {
        for (Button btn : new Button[]{rangeTodayBtn, rangeWeekBtn, rangeMonthBtn}) {
            btn.getStyleClass().remove("range-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("range-btn-active")) {
            active.getStyleClass().add("range-btn-active");
        }
    }
}
package com.kainanresto.controllers.main.admin.sales;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class MonthFilterController {

    private static final String FXML_FILE = "/com/kainanresto/views/util/MonthFilterModalView.fxml";
    private static final String SELECTED_STYLE = "mf-selected";
    private static final int YEARS_BACK = 10;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label yearLabel;
    @FXML private Button prevYearBtn;
    @FXML private Button nextYearBtn;
    @FXML private Button selectYearChip;
    @FXML private Button exportBtn;
    @FXML private GridPane monthGrid;

    private final YearMonth now = YearMonth.now();
    private final Set<YearMonth> selectedMonths = new TreeSet<>();
    private int year = now.getYear();
    private List<YearMonth> result;
    private Stage stage;

    public static List<YearMonth> show(Window owner, String title) {
        URL url = MonthFilterController.class.getResource(FXML_FILE);
        if (url == null) {
            throw new IllegalStateException("Cannot find " + FXML_FILE + " next to "
                    + MonthFilterController.class.getName()
                    + ". Put it in src/main/resources/"
                    + MonthFilterController.class.getPackageName().replace('.', '/') + "/");
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            StackPane root = loader.load();
            MonthFilterController controller = loader.getController();
            return controller.open(owner, root, title);
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static List<YearMonth> show(Window owner) {
        return show(owner, "Export Sales History");
    }

    public static Optional<YearMonth> showSingle(Window owner, String title) {
        List<YearMonth> list = show(owner, title);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @FXML
    private void initialize() {
        selectedMonths.add(now);
        refresh();
    }

    private List<YearMonth> open(Window owner, StackPane root, String title) {
        if (title != null && !title.isBlank()) titleLabel.setText(title);

        URL css = MonthFilterController.class.getResource("/com/kainanresto/styles/util/MonthFilterView.css");
        if (css != null) {
            root.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("MonthFilterView.css not found; modal will be unstyled.");
        }

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        Scene ownerScene = owner == null ? null : owner.getScene();
        double x = owner == null ? 0 : owner.getX();
        double y = owner == null ? 0 : owner.getY();
        double w = owner == null ? 800 : owner.getWidth();
        double h = owner == null ? 600 : owner.getHeight();
        if (ownerScene != null) {
            x += ownerScene.getX();
            y += ownerScene.getY();
            w = ownerScene.getWidth();
            h = ownerScene.getHeight();
        }

        Scene scene = new Scene(root, w, h);
        scene.setFill(Color.TRANSPARENT);
        if (ownerScene != null) scene.getStylesheets().addAll(ownerScene.getStylesheets());

        stage.setScene(scene);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(w);
        stage.setHeight(h);
        stage.showAndWait();

        return result == null ? List.of() : Collections.unmodifiableList(result);
    }

    private void refresh() {
        yearLabel.setText(String.valueOf(year));
        prevYearBtn.setDisable(year <= now.getYear() - YEARS_BACK);
        nextYearBtn.setDisable(year >= now.getYear());

        int maxMonthInYear = (year == now.getYear()) ? now.getMonthValue() : 12;
        boolean allInYearSelected = maxMonthInYear > 0;

        for (Node n : monthGrid.getChildren()) {
            if (!(n instanceof Button b)) continue;
            int m = monthOf(b);
            YearMonth ym = YearMonth.of(year, m);
            boolean isFuture = ym.isAfter(now);
            b.setDisable(isFuture);
            b.getStyleClass().remove(SELECTED_STYLE);

            if (selectedMonths.contains(ym)) {
                b.getStyleClass().add(SELECTED_STYLE);
            } else if (!isFuture) {
                allInYearSelected = false;
            }
        }

        if (selectYearChip != null) {
            selectYearChip.setText(allInYearSelected ? "Deselect " + year : "Select " + year);
        }

        // Subtitle summary
        if (subtitleLabel != null) {
            if (selectedMonths.isEmpty()) {
                subtitleLabel.setText("Select one or more months to export");
            } else if (selectedMonths.size() == 1) {
                YearMonth single = selectedMonths.iterator().next();
                subtitleLabel.setText("1 month selected: " + formatMonthYear(single));
            } else if (selectedMonths.size() <= 3) {
                String summary = selectedMonths.stream()
                        .map(this::formatMonthYearShort)
                        .collect(Collectors.joining(", "));
                subtitleLabel.setText(selectedMonths.size() + " months selected: " + summary);
            } else {
                List<YearMonth> sorted = new ArrayList<>(selectedMonths);
                YearMonth first = sorted.get(0);
                YearMonth last = sorted.get(sorted.size() - 1);
                subtitleLabel.setText(selectedMonths.size() + " months selected ("
                        + formatMonthYearShort(first) + " – " + formatMonthYearShort(last) + ")");
            }
        }

        // Export button text & state
        if (selectedMonths.isEmpty()) {
            exportBtn.setText("Export (0 selected)");
            exportBtn.setDisable(true);
        } else if (selectedMonths.size() == 1) {
            YearMonth single = selectedMonths.iterator().next();
            exportBtn.setText("Export " + single.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + single.getYear());
            exportBtn.setDisable(false);
        } else {
            exportBtn.setText("Export " + selectedMonths.size() + " Months");
            exportBtn.setDisable(false);
        }
    }

    private String formatMonthYear(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();
    }

    private String formatMonthYearShort(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
    }

    private static int monthOf(Button b) {
        return Integer.parseInt(String.valueOf(b.getUserData()));
    }

    @FXML
    private void onMonthClicked(ActionEvent e) {
        Button b = (Button) e.getSource();
        YearMonth ym = YearMonth.of(year, monthOf(b));
        if (ym.isAfter(now)) return;

        if (selectedMonths.contains(ym)) {
            selectedMonths.remove(ym);
        } else {
            selectedMonths.add(ym);
        }
        refresh();
    }

    @FXML
    private void onThisMonth(ActionEvent e) {
        selectedMonths.clear();
        selectedMonths.add(now);
        year = now.getYear();
        refresh();
    }

    @FXML
    private void onLastMonth(ActionEvent e) {
        selectedMonths.clear();
        YearMonth lm = now.minusMonths(1);
        selectedMonths.add(lm);
        year = lm.getYear();
        refresh();
    }

    @FXML
    private void onSelectYear(ActionEvent e) {
        int maxMonth = (year == now.getYear()) ? now.getMonthValue() : 12;
        boolean allInYearSelected = true;
        for (int m = 1; m <= maxMonth; m++) {
            if (!selectedMonths.contains(YearMonth.of(year, m))) {
                allInYearSelected = false;
                break;
            }
        }

        if (allInYearSelected) {
            for (int m = 1; m <= maxMonth; m++) {
                selectedMonths.remove(YearMonth.of(year, m));
            }
        } else {
            for (int m = 1; m <= maxMonth; m++) {
                selectedMonths.add(YearMonth.of(year, m));
            }
        }
        refresh();
    }

    @FXML
    private void onClear(ActionEvent e) {
        selectedMonths.clear();
        refresh();
    }

    @FXML
    private void onPrevYear(ActionEvent e) {
        if (year > now.getYear() - YEARS_BACK) {
            year--;
            refresh();
        }
    }

    @FXML
    private void onNextYear(ActionEvent e) {
        if (year < now.getYear()) {
            year++;
            refresh();
        }
    }

    @FXML
    private void onConfirm(ActionEvent e) {
        if (selectedMonths.isEmpty()) return;
        result = new ArrayList<>(selectedMonths);
        stage.close();
    }

    @FXML
    private void onCancel(ActionEvent e) {
        result = null;
        stage.close();
    }

    @FXML
    private void onBackdropClicked(MouseEvent e) {
        result = null;
        stage.close();
    }

    @FXML
    private void onCardClicked(MouseEvent e) {
        e.consume();
    }
}

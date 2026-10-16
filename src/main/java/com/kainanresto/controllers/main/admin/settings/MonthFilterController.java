package com.kainanresto.controllers.main.admin.settings;

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
import java.util.Locale;
import java.util.Optional;

/**
 * Controller for MonthFilterModal.fxml: a dimmed in-app overlay with a month/year picker.
 *
 * Usage:
 *     Optional<YearMonth> picked = MonthFilterController.show(window, "Export Sales History");
 *
 * Returns empty if the user cancels (Cancel, Esc, or clicking the dim backdrop).
 *
 * MonthFilterModal.fxml and month-filter.css must sit in the same resources folder as this class's package:
 *   src/main/resources/com/kainanresto/controllers/main/admin/settings/
 */
public class MonthFilterController {

    private static final String FXML_FILE = "/com/kainanresto/views/util/MonthFilterModalView.fxml";
    private static final String SELECTED_STYLE = "mf-selected";
    private static final int YEARS_BACK = 10;

    @FXML private Label titleLabel;
    @FXML private Label yearLabel;
    @FXML private Button prevYearBtn;
    @FXML private Button nextYearBtn;
    @FXML private Button exportBtn;
    @FXML private GridPane monthGrid;

    private final YearMonth now = YearMonth.now();
    private YearMonth selected = now;
    private int year = now.getYear();
    private YearMonth result;          // stays null if cancelled
    private Stage stage;

    /* ============================== ENTRY POINT ============================== */

    public static Optional<YearMonth> show(Window owner, String title) {
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
            return Optional.empty();
        }
    }

    public static Optional<YearMonth> show(Window owner) {
        return show(owner, "Export Sales History");
    }

    /* ============================== SETUP ============================== */

    @FXML
    private void initialize() {
        refresh();
    }

    private Optional<YearMonth> open(Window owner, StackPane root, String title) {
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

        // Cover the owner window's content area so it looks like an in-app overlay
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

        return Optional.ofNullable(result);
    }

    /* ============================== UI STATE ============================== */

    private void refresh() {
        yearLabel.setText(String.valueOf(year));
        prevYearBtn.setDisable(year <= now.getYear() - YEARS_BACK);
        nextYearBtn.setDisable(year >= now.getYear());

        for (Node n : monthGrid.getChildren()) {
            if (!(n instanceof Button b)) continue;
            YearMonth ym = YearMonth.of(year, monthOf(b));
            b.setDisable(ym.isAfter(now));                       // no future months
            b.getStyleClass().remove(SELECTED_STYLE);
            if (ym.equals(selected)) b.getStyleClass().add(SELECTED_STYLE);
        }

        exportBtn.setText("Export " + selected.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + selected.getYear());
    }

    private static int monthOf(Button b) {
        return Integer.parseInt(String.valueOf(b.getUserData()));   // userData set in the FXML (1-12)
    }

    private void select(YearMonth ym) {
        selected = ym;
        year = ym.getYear();
        refresh();
    }

    /* ============================== HANDLERS ============================== */

    @FXML private void onMonthClicked(ActionEvent e) {
        select(YearMonth.of(year, monthOf((Button) e.getSource())));
    }

    @FXML private void onThisMonth(ActionEvent e) { select(now); }
    @FXML private void onLastMonth(ActionEvent e) { select(now.minusMonths(1)); }

    @FXML private void onPrevYear(ActionEvent e) { year--; refresh(); }
    @FXML private void onNextYear(ActionEvent e) { year++; refresh(); }

    @FXML private void onConfirm(ActionEvent e) {
        result = selected;
        stage.close();
    }

    @FXML private void onCancel(ActionEvent e) { stage.close(); }

    @FXML private void onBackdropClicked(MouseEvent e) { stage.close(); }

    @FXML private void onCardClicked(MouseEvent e) { e.consume(); }   // don't let card clicks reach the backdrop
}
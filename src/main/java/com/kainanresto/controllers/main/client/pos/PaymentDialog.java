package com.kainanresto.controllers.main.client.pos;

import com.kainanresto.controllers.main.client.ClientUIHelper;
import com.kainanresto.model.transac.PaymentResult;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PaymentDialog {

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");

    private PaymentDialog() {}

    public static Optional<PaymentResult> show(Window owner, String orderNumber, String orderType, BigDecimal total) {
        PaymentResult[] result = new PaymentResult[1];

        //HEADER
        Label title = new Label("Payment");
        title.getStyleClass().add("pay-title");
        Label subtitle = new Label(orderType + " \u2022 " + orderNumber);
        subtitle.getStyleClass().add("pay-subtitle");
        VBox header = new VBox(2, title, subtitle);

        //TOTAL
        Label dueLabel = new Label("Total due");
        dueLabel.getStyleClass().add("pay-label");
        Label dueValue = new Label(ClientUIHelper.formatPeso(total));
        dueValue.getStyleClass().add("pay-due-value");
        VBox dueBox = new VBox(4, dueLabel, dueValue);
        dueBox.getStyleClass().add("pay-due-box");

        //AMOUNT
        Label tenderedLabel = new Label("Amount received (cash)");
        tenderedLabel.getStyleClass().add("pay-label");

        Label prefix = new Label("\u20B1");
        prefix.getStyleClass().add("pay-prefix");
        TextField field = new TextField();
        field.setPromptText("0.00");
        field.getStyleClass().add("pay-input");
        field.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? c : null));
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox inputBox = new HBox(prefix, field);
        inputBox.getStyleClass().add("pay-input-box");
        inputBox.setAlignment(Pos.CENTER_LEFT);
        field.focusedProperty().addListener((o, was, is) -> inputBox.pseudoClassStateChanged(FIELD_FOCUSED, is));

        //QUICK AMOUNT
        FlowPane quick = new FlowPane(8, 8);
        List<BigDecimal> suggestions = suggestions(total);
        for (int i = 0; i < suggestions.size(); i++) {
            BigDecimal amount = suggestions.get(i);
            String text = (i == 0) ? "Exact" : "\u20B1 " + String.format("%,d", amount.intValue());
            Button b = new Button(text);
            b.setMnemonicParsing(false);
            b.setFocusTraversable(false);
            b.getStyleClass().add("pay-quick-btn");
            b.setOnAction(e -> {
                field.setText(amount.toPlainString());
                field.requestFocus();
                field.positionCaret(field.getLength());
            });
            quick.getChildren().add(b);
        }

        //CHANGE
        Label changeLabel = new Label("Change");
        changeLabel.getStyleClass().add("pay-change-label");
        Label changeValue = new Label(ClientUIHelper.formatPeso(BigDecimal.ZERO));
        changeValue.getStyleClass().add("pay-change-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox changeRow = new HBox(changeLabel, spacer, changeValue);
        changeRow.getStyleClass().add("pay-change-row");
        changeRow.setAlignment(Pos.CENTER_LEFT);

        //BUTTONS
        Button cancel = new Button("Cancel");
        cancel.setMnemonicParsing(false);
        cancel.getStyleClass().add("pay-cancel-btn");
        Button confirm = new Button("Confirm Payment");
        confirm.setMnemonicParsing(false);
        confirm.getStyleClass().add("pay-confirm-btn");
        confirm.setDisable(true);
        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(18, header, dueBox,
                new VBox(8, tenderedLabel, inputBox),
                quick, changeRow, buttons);
        card.getStyleClass().add("pay-card");

        Stage stage = DialogSupport.createStage(owner, card);

        //BEHAVIOR
        field.textProperty().addListener((o, old, now) -> {
            BigDecimal tendered = parse(now);
            BigDecimal diff = tendered.subtract(total);
            boolean enough = tendered.signum() > 0 && diff.signum() >= 0;

            changeValue.getStyleClass().removeAll("pay-change-ok", "pay-change-short");
            if (tendered.signum() == 0) {
                changeLabel.setText("Change");
                changeValue.setText(ClientUIHelper.formatPeso(BigDecimal.ZERO));
            } else if (enough) {
                changeLabel.setText("Change");
                changeValue.setText(ClientUIHelper.formatPeso(diff));
                changeValue.getStyleClass().add("pay-change-ok");
            } else {
                changeLabel.setText("Short by");
                changeValue.setText(ClientUIHelper.formatPeso(diff.negate()));
                changeValue.getStyleClass().add("pay-change-short");
            }
            confirm.setDisable(!enough);
        });

        Runnable doConfirm = () -> {
            BigDecimal tendered = parse(field.getText());
            if (tendered.compareTo(total) < 0) return;
            result[0] = new PaymentResult(tendered, tendered.subtract(total));
            stage.close();
        };
        confirm.setOnAction(e -> doConfirm.run());
        field.setOnAction(e -> { if (!confirm.isDisable()) doConfirm.run(); });
        cancel.setOnAction(e -> stage.close());
        stage.setOnShown(e -> field.requestFocus());

        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static List<BigDecimal> suggestions(BigDecimal total) {
        Set<BigDecimal> set = new LinkedHashSet<>();
        set.add(total.setScale(2, RoundingMode.UP));
        for (int step : new int[]{50, 100, 500, 1000}) {
            BigDecimal s = BigDecimal.valueOf(step);
            set.add(total.divide(s, 0, RoundingMode.CEILING).multiply(s).setScale(2, RoundingMode.UNNECESSARY));
        }
        return new ArrayList<>(set);
    }

    private static BigDecimal parse(String text) {
        if (text == null || text.isBlank() || text.equals(".")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
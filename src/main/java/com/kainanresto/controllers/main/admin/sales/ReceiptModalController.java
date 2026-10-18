package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.transac.Transaction;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptModalController {

    @FXML private Label orderNoLabel, dateLabel, cashierLabel;
    @FXML private VBox itemsContainer;

    @FXML private Label subtotalLabel;
    @FXML private HBox discountRow;
    @FXML private Label discountNameLabel, discountValueLabel;
    @FXML private Label serviceChargeNameLabel, serviceChargeValueLabel;
    @FXML private Label vatNameLabel, vatValueLabel;
    @FXML private Label totalLabel;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    public void setReceiptData(Transaction order, OrderReceipt receipt, PaymentResult pay) {

        orderNoLabel.setText(valueOrDash(receipt.orderNumber()));
        dateLabel.setText(receipt.createdAt() == null ? "—" : receipt.createdAt().format(DATE_FORMAT));
        cashierLabel.setText(valueOrDash(receipt.cashierName()));

        itemsContainer.getChildren().clear();
        if (receipt.lines() != null) {
            for (ReceiptLine line : receipt.lines()) {
                itemsContainer.getChildren().add(createItemRow(line));
            }
        }

        ReceiptTotals t = receipt.totals();
        if (t != null) {
            subtotalLabel.setText(formatPeso(t.subtotal()));
            serviceChargeNameLabel.setText("Service Charge" + rateSuffix(t.serviceChargeRate()));
            serviceChargeValueLabel.setText(formatPeso(t.serviceCharge()));
            vatNameLabel.setText("VAT" + rateSuffix(t.vatRate()));
            vatValueLabel.setText(formatPeso(t.vat()));
            totalLabel.setText(formatPeso(t.total()));

            if (t.subtotal() != null && t.serviceCharge() != null && t.vat() != null && t.total() != null) {
                BigDecimal discount = t.subtotal().add(t.serviceCharge()).add(t.vat()).subtract(t.total());
                if (discount.signum() > 0) {
                    discountNameLabel.setText(receipt.discountName() == null ? "Discount" : "Discount - " + receipt.discountName());
                    discountValueLabel.setText("-" + formatPeso(discount));
                    discountRow.setVisible(true);
                    discountRow.setManaged(true);
                }
            }
        }
    }

    private HBox createItemRow(ReceiptLine line) {
        Label name = new Label(valueOrDash(line.name()));
        name.setWrapText(true);
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2E2520;");

        Label unit = new Label(formatPeso(line.unitPrice()));
        unit.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #6E625C;");

        VBox nameBox = new VBox(2.0, name, unit);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label qty = new Label(String.valueOf(line.quantity()));
        qty.setPrefWidth(40.0);
        qty.setAlignment(Pos.CENTER);
        qty.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2E2520;");

        Label amt = new Label(formatPeso(line.lineTotal()));
        amt.setPrefWidth(80.0);
        amt.setAlignment(Pos.CENTER_RIGHT);
        amt.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2E2520;");

        HBox row = new HBox(8.0, nameBox, qty, amt);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    @FXML
    private void onClose() {
        ((Stage) subtotalLabel.getScene().getWindow()).close();
    }

    private static String formatPeso(BigDecimal amount) { return amount == null ? "—" : String.format(Locale.ENGLISH, "₱%,.2f", amount); }
    private static String valueOrDash(String value) { return (value == null || value.isBlank()) ? "—" : value; }
    private static String rateSuffix(BigDecimal rate) { return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)"; }
}
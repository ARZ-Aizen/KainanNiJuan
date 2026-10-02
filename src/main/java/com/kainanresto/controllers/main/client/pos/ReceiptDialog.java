package com.kainanresto.controllers.main.client.pos;

import com.kainanresto.controllers.main.client.ClientUIHelper;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.ReceiptPdfGenerator;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Modal "virtual receipt" shown right after a successful payment. */
public final class ReceiptDialog {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.ENGLISH);

    private ReceiptDialog() {}

    public static void show(Window owner, OrderReceipt r, PaymentResult pay) {
        ReceiptTotals t = r.totals();

        // ---------- brand ----------
        Label brand = new Label("KAINAN NI JUAN");
        brand.getStyleClass().add("rcpt-brand");
        Label sub = new Label("Order Receipt");
        sub.getStyleClass().add("rcpt-sub");
        VBox header = new VBox(2, brand, sub);
        header.setAlignment(Pos.CENTER);

        // ---------- order info ----------
        String type = ClientUIHelper.valueOrDash(r.orderType());
        if (r.orderTypeDetail() != null && !r.orderTypeDetail().isBlank()) {
            type += " \u2022 " + r.orderTypeDetail().trim();
        }
        VBox meta = new VBox(6,
                kv("Order No.", r.orderNumber()),
                kv("Date", r.createdAt() == null ? "\u2014" : r.createdAt().format(DATE_FMT)),
                kv("Cashier", ClientUIHelper.valueOrDash(r.cashierName())),
                kv("Order type", type));

        // ---------- table head ----------
        Label hItem = new Label("Item");
        hItem.getStyleClass().add("rcpt-head");
        hItem.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(hItem, Priority.ALWAYS);
        Label hQty = fixed(new Label("Qty"), 40, Pos.CENTER);
        hQty.getStyleClass().add("rcpt-head");
        Label hAmt = fixed(new Label("Amount"), 90, Pos.CENTER_RIGHT);
        hAmt.getStyleClass().add("rcpt-head");
        HBox head = new HBox(8, hItem, hQty, hAmt);

        // ---------- items ----------
        VBox items = new VBox(10);
        for (ReceiptLine l : r.lines()) {
            Label name = new Label(ClientUIHelper.valueOrDash(l.name()));
            name.getStyleClass().add("rcpt-item-name");
            name.setWrapText(true);
            Label unit = new Label(ClientUIHelper.formatPeso(l.unitPrice()));
            unit.getStyleClass().add("rcpt-item-sub");
            VBox left = new VBox(1, name, unit);
            left.setMinWidth(0);
            HBox.setHgrow(left, Priority.ALWAYS);

            Label qty = fixed(new Label(String.valueOf(l.quantity())), 40, Pos.CENTER);
            qty.getStyleClass().add("rcpt-item-qty");
            Label amt = fixed(new Label(ClientUIHelper.formatPeso(l.lineTotal())), 90, Pos.CENTER_RIGHT);
            amt.getStyleClass().add("rcpt-item-amt");

            HBox row = new HBox(8, left, qty, amt);
            row.setAlignment(Pos.TOP_LEFT);
            items.getChildren().add(row);
        }
        ScrollPane scroll = new ScrollPane(items);
        scroll.getStyleClass().add("rcpt-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // Grows with the number of dishes, then scrolls once it reaches the cap
        double cap = Math.max(160, Screen.getPrimary().getVisualBounds().getHeight() - 56 - 620);
        scroll.prefHeightProperty().bind(items.heightProperty().add(2));
        scroll.setMaxHeight(cap);

        // ---------- summary ----------
        // The discount is already baked into total, so recover it: subtotal + service + vat - total
        BigDecimal discount = t.subtotal().add(t.serviceCharge()).add(t.vat()).subtract(t.total());

        VBox summary = new VBox(8);
        summary.getChildren().add(sum("Subtotal", ClientUIHelper.formatPeso(t.subtotal())));
        if (discount.signum() > 0) {
            String label = r.discountName() == null ? "Discount" : "Discount \u2013 " + r.discountName();
            summary.getChildren().add(sum(label, "-" + ClientUIHelper.formatPeso(discount)));
        }
        summary.getChildren().addAll(
                sum("Service charge" + rateSuffix(t.serviceChargeRate()), ClientUIHelper.formatPeso(t.serviceCharge())),
                sum("VAT" + rateSuffix(t.vatRate()), ClientUIHelper.formatPeso(t.vat())));

        Label totalLabel = new Label("TOTAL");
        totalLabel.getStyleClass().add("rcpt-total-label");
        Label totalValue = new Label(ClientUIHelper.formatPeso(t.total()));
        totalValue.getStyleClass().add("rcpt-total-value");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox totalRow = new HBox(totalLabel, sp, totalValue);
        totalRow.setAlignment(Pos.CENTER_LEFT);

        VBox payment = new VBox(8,
                sum("Cash", ClientUIHelper.formatPeso(pay.cashTendered())),
                sum("Change", ClientUIHelper.formatPeso(pay.change())));

        Label thanks = new Label("Thank you! Please come again.");
        thanks.getStyleClass().add("rcpt-thanks");
        thanks.setMaxWidth(Double.MAX_VALUE);
        thanks.setAlignment(Pos.CENTER);

        Button done = new Button("DONE");
        done.setMnemonicParsing(false);
        done.setMaxWidth(Double.MAX_VALUE);
        done.getStyleClass().add("rcpt-done-btn");

        VBox card = new VBox(12,
                header, dash(), meta, dash(), head, scroll, dash(),
                summary, solid(), totalRow, dash(), payment, thanks, done);
        card.getStyleClass().add("rcpt-card");

        Stage stage = DialogSupport.createStage(owner, card);

        // Re-fit and re-center once the item list has been measured
        stage.setOnShown(e -> Platform.runLater(() -> {
            stage.sizeToScene();
            stage.centerOnScreen();
        }));

        // DONE: close the popup and generate the PDF in the background
        done.setOnAction(e -> {
            stage.close();
            Thread worker = new Thread(() -> {
                try {
                    ReceiptPdfGenerator.generate(r, pay);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> AlertUtil.showError("Receipt PDF",
                            "The order was saved, but the receipt PDF could not be created."));
                }
            });
            worker.setDaemon(true);
            worker.start();
        });
        stage.showAndWait();
    }

    // ---------- small builders ----------

    private static HBox kv(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("rcpt-key");
        Label v = new Label(value);
        v.getStyleClass().add("rcpt-val");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(k, sp, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox sum(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("rcpt-sum-label");
        Label v = new Label(value);
        v.getStyleClass().add("rcpt-sum-value");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(l, sp, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label fixed(Label label, double width, Pos alignment) {
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setAlignment(alignment);
        return label;
    }

    private static Region dash() {
        Region r = new Region();
        r.getStyleClass().add("receipt-dash");   // dashed rule already defined in ClientView.css
        return r;
    }

    private static Region solid() {
        Region r = new Region();
        r.getStyleClass().add("receipt-divider");
        return r;
    }

    private static String rateSuffix(BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }
}
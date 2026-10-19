package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.transac.Transaction;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.ReceiptPdfGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @FXML private Button generatePdfBtn;

    private OrderReceipt currentReceipt;
    private PaymentResult currentPay;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    public void setReceiptData(Transaction order, OrderReceipt receipt, PaymentResult pay) {
        this.currentReceipt = receipt;
        this.currentPay = pay;

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
            boolean hasDiscount = receipt.discountName() != null && !receipt.discountName().isBlank() && !"None".equalsIgnoreCase(receipt.discountName());
            boolean isSeniorOrPwd = hasDiscount && (receipt.discountName().toLowerCase().contains("senior") || receipt.discountName().toLowerCase().contains("pwd"));

            if (isSeniorOrPwd) {
                if (t.vat() == null || t.vat().signum() == 0) {
                    vatNameLabel.setText("VAT (0% - VAT Exempt)");
                    vatValueLabel.setText(formatPeso(BigDecimal.ZERO));
                } else {
                    vatNameLabel.setText("VAT (12% on regular items)");
                    vatValueLabel.setText(formatPeso(t.vat()));
                }
            } else {
                vatNameLabel.setText("VAT" + rateSuffix(t.vatRate()));
                vatValueLabel.setText(formatPeso(t.vat()));
            }

            totalLabel.setText(formatPeso(t.total()));

            if (hasDiscount && t.subtotal() != null && t.serviceCharge() != null && t.total() != null) {
                BigDecimal discount = t.subtotal().add(t.serviceCharge()).subtract(t.total());
                if (discount.signum() > 0) {
                    discountNameLabel.setText("Discount - " + receipt.discountName());
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
    private void onGeneratePdf() {
        if (currentReceipt == null) {
            AlertUtil.showError("Generate PDF", "No receipt data available to export.");
            return;
        }

        Stage owner = (Stage) (generatePdfBtn != null && generatePdfBtn.getScene() != null
                ? generatePdfBtn.getScene().getWindow()
                : subtotalLabel.getScene().getWindow());

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Receipt PDF");

        String rawNo = currentReceipt.orderNumber();
        String safeNo = (rawNo == null || rawNo.isBlank()) ? "order" : rawNo.replaceAll("[^A-Za-z0-9_-]", "_");
        chooser.setInitialFileName("receipt-" + safeNo + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        Path defaultDir = Path.of(System.getProperty("user.home"), "Documents", "KainanNiJuan", "Receipts");
        try {
            Files.createDirectories(defaultDir);
            if (Files.exists(defaultDir)) {
                chooser.setInitialDirectory(defaultDir.toFile());
            }
        } catch (Exception ignored) {}

        File selectedFile = chooser.showSaveDialog(owner);
        if (selectedFile == null) {
            return;
        }

        if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
            selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".pdf");
        }

        final File targetFile = selectedFile;
        if (generatePdfBtn != null) generatePdfBtn.setDisable(true);

        Thread worker = new Thread(() -> {
            try {
                ReceiptPdfGenerator.generate(currentReceipt, currentPay, targetFile);
                Platform.runLater(() -> {
                    if (generatePdfBtn != null) generatePdfBtn.setDisable(false);
                    boolean openNow = AlertUtil.showYesNoConfirmation(
                            "PDF Generated",
                            "Receipt PDF Generated",
                            "Receipt successfully saved to:\n" + targetFile.getAbsolutePath() + "\n\nWould you like to open it now?"
                    );
                    if (openNow && Desktop.isDesktopSupported()) {
                        try {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.OPEN)) {
                                desktop.open(targetFile);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (generatePdfBtn != null) generatePdfBtn.setDisable(false);
                    AlertUtil.showError(
                            "PDF Generation Failed",
                            "Could not generate receipt PDF:\n" + ex.getMessage()
                    );
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onClose() {
        ((Stage) subtotalLabel.getScene().getWindow()).close();
    }

    private static String formatPeso(BigDecimal amount) { return amount == null ? "—" : String.format(Locale.ENGLISH, "₱%,.2f", amount); }
    private static String valueOrDash(String value) { return (value == null || value.isBlank()) ? "—" : value; }
    private static String rateSuffix(BigDecimal rate) { return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)"; }
}
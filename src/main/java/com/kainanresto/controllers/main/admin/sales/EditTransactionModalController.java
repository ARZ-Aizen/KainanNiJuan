package com.kainanresto.controllers.main.admin.sales;

import com.kainanresto.dao.OrderDAO;
import com.kainanresto.dao.ProductDAO;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.transac.Transaction;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import com.kainanresto.controllers.main.admin.accounts.AdminAuthDialog;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EditTransactionModalController {

    @FXML private Label modalSubtitleLabel;
    @FXML private ComboBox<String> orderTypeCombo;
    @FXML private ComboBox<OrderStatus> orderStatusCombo;

    @FXML private Label itemsCountLabel;
    @FXML private VBox itemsContainer;

    @FXML private ComboBox<Dishes> addDishCombo;
    @FXML private TextField addDishQtyField;
    @FXML private Button addDishBtn;

    @FXML private ComboBox<String> discountCombo;
    @FXML private VBox discountCardsBox;
    @FXML private Label discountCardsLabel;

    @FXML private Label subtotalValueLabel;
    @FXML private HBox discountRow;
    @FXML private Label discountLabel;
    @FXML private Label discountValueLabel;
    @FXML private Label serviceChargeLabel;
    @FXML private Label serviceChargeValueLabel;
    @FXML private Label vatLabel;
    @FXML private Label vatValueLabel;
    @FXML private Label totalValueLabel;

    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05");
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.12");

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private Transaction currentOrder;
    private OrderReceipt originalReceipt;
    private PaymentResult currentPay;
    private Runnable onSaveSuccess;

    private final ObservableList<ReceiptLine> workingLines = FXCollections.observableArrayList();
    private final List<Dishes> availableDishes = new ArrayList<>();
    private BigDecimal currentDiscountRate = BigDecimal.ZERO;
    private int discountCards = 1;
    private ReceiptTotals currentCalculatedTotals;
    private BigDecimal currentCalculatedDiscountAmount = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        orderTypeCombo.getItems().setAll("Dine in", "Take out");
        orderStatusCombo.getItems().setAll(OrderStatus.PREPARING, OrderStatus.COMPLETED, OrderStatus.CANCELLED);

        discountCombo.getItems().setAll("None", "Senior Citizen (20%)", "PWD (20%)");
        discountCombo.valueProperty().addListener((obs, oldVal, newVal) -> onDiscountChanged(newVal));

        addDishQtyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                addDishQtyField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        setupDishCombo();
    }

    private void setupDishCombo() {
        addDishCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Dishes d) {
                if (d == null) return "";
                return d.name() + " — " + formatPeso(d.price()) + " (" + d.quantity() + " in stock)";
            }

            @Override
            public Dishes fromString(String string) {
                return null;
            }
        });
    }

    public void setOrderData(Transaction order, OrderReceipt receipt, PaymentResult pay, Runnable onSaveSuccess) {
        this.currentOrder = order;
        this.originalReceipt = receipt;
        this.currentPay = pay;
        this.onSaveSuccess = onSaveSuccess;

        String dateStr = receipt.createdAt() == null ? "—" : receipt.createdAt().format(DATE_FORMAT);
        String staff = receipt.cashierName() == null ? "—" : receipt.cashierName();
        modalSubtitleLabel.setText("Order #" + receipt.orderNumber() + " • " + dateStr + " • Staff: " + staff);

        // Populate order type
        String type = receipt.orderType();
        if (type != null && ("Take out".equalsIgnoreCase(type) || "Takeout".equalsIgnoreCase(type))) {
            orderTypeCombo.setValue("Take out");
        } else {
            orderTypeCombo.setValue("Dine in");
        }

        // Populate status
        orderStatusCombo.setValue(receipt.status() != null ? receipt.status() : OrderStatus.COMPLETED);

        // Populate lines
        workingLines.clear();
        if (receipt.lines() != null) {
            workingLines.addAll(receipt.lines());
        }

        // Load dishes for adding
        loadAvailableDishes();

        // Populate discount
        String disc = receipt.discountName();
        if (disc != null && disc.contains("20%")) {
            if (disc.toLowerCase().contains("senior")) {
                discountCombo.setValue("Senior Citizen (20%)");
            } else if (disc.toLowerCase().contains("pwd")) {
                discountCombo.setValue("PWD (20%)");
            } else {
                discountCombo.setValue("None");
            }
        } else {
            discountCombo.setValue("None");
        }

        refreshItemsUI();
        recalculateTotals();
    }

    private void loadAvailableDishes() {
        Thread t = new Thread(() -> {
            try {
                List<Dishes> dishes = productDAO.getAllDishes();
                Platform.runLater(() -> {
                    availableDishes.clear();
                    availableDishes.addAll(dishes);
                    addDishCombo.getItems().setAll(dishes);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void onDiscountChanged(String discountName) {
        if (discountName == null || "None".equalsIgnoreCase(discountName)) {
            currentDiscountRate = BigDecimal.ZERO;
            discountCardsBox.setVisible(false);
            discountCardsBox.setManaged(false);
        } else {
            currentDiscountRate = new BigDecimal("0.20");
            discountCardsBox.setVisible(true);
            discountCardsBox.setManaged(true);
        }
        recalculateTotals();
    }

    @FXML
    private void onDiscountCardsMinus() {
        if (discountCards > 1) {
            discountCards--;
            discountCardsLabel.setText(String.valueOf(discountCards));
            recalculateTotals();
        }
    }

    @FXML
    private void onDiscountCardsPlus() {
        int maxUnits = totalUnitsCount();
        if (discountCards < maxUnits) {
            discountCards++;
            discountCardsLabel.setText(String.valueOf(discountCards));
            recalculateTotals();
        }
    }

    private int totalUnitsCount() {
        int count = 0;
        for (ReceiptLine l : workingLines) {
            count += l.quantity();
        }
        return Math.max(1, count);
    }

    private void recalculateTotals() {
        if (workingLines.isEmpty()) {
            subtotalValueLabel.setText("₱0.00");
            discountRow.setVisible(false);
            discountRow.setManaged(false);
            serviceChargeValueLabel.setText("₱0.00");
            vatValueLabel.setText("₱0.00");
            totalValueLabel.setText("₱0.00");
            currentCalculatedTotals = new ReceiptTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            currentCalculatedDiscountAmount = BigDecimal.ZERO;
            itemsCountLabel.setText("0 items");
            return;
        }

        itemsCountLabel.setText(workingLines.size() + (workingLines.size() == 1 ? " item" : " items"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<BigDecimal> unitPrices = new ArrayList<>();
        for (ReceiptLine line : workingLines) {
            BigDecimal lineTotal = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
            subtotal = subtotal.add(lineTotal);
            for (int i = 0; i < line.quantity(); i++) {
                unitPrices.add(line.unitPrice());
            }
        }
        unitPrices.sort(Comparator.reverseOrder());

        boolean discountActive = currentDiscountRate.signum() > 0;
        discountCards = Math.max(1, Math.min(discountCards, unitPrices.size()));
        discountCardsLabel.setText(String.valueOf(discountCards));

        BigDecimal discountedBase = BigDecimal.ZERO;
        if (discountActive) {
            for (int i = 0; i < discountCards && i < unitPrices.size(); i++) {
                discountedBase = discountedBase.add(unitPrices.get(i));
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountActive) {
            BigDecimal vatExemptBase = discountedBase.divide(VAT_DIVISOR, 4, RoundingMode.HALF_UP);
            BigDecimal vatDeduction = discountedBase.subtract(vatExemptBase);
            BigDecimal pureDiscount = vatExemptBase.multiply(currentDiscountRate).setScale(2, RoundingMode.HALF_UP);
            discountAmount = vatDeduction.add(pureDiscount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal netSales = subtotal.subtract(discountAmount);
        BigDecimal serviceCharge = netSales.multiply(SERVICE_CHARGE_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal nonDiscountedBase = subtotal.subtract(discountedBase);
        BigDecimal embeddedVat = nonDiscountedBase.subtract(nonDiscountedBase.divide(VAT_DIVISOR, 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = netSales.add(serviceCharge);

        currentCalculatedTotals = new ReceiptTotals(
                subtotal,
                SERVICE_CHARGE_RATE.multiply(new BigDecimal("100")),
                serviceCharge,
                VAT_RATE.multiply(new BigDecimal("100")),
                embeddedVat,
                total
        );
        currentCalculatedDiscountAmount = discountAmount;

        // Update labels
        subtotalValueLabel.setText(formatPeso(subtotal));

        if (discountActive && discountAmount.signum() > 0) {
            discountLabel.setText("Discount (" + discountCombo.getValue() + ")");
            discountValueLabel.setText("-" + formatPeso(discountAmount));
            discountRow.setVisible(true);
            discountRow.setManaged(true);
        } else {
            discountRow.setVisible(false);
            discountRow.setManaged(false);
        }

        serviceChargeValueLabel.setText(formatPeso(serviceCharge));
        vatValueLabel.setText(formatPeso(embeddedVat));
        totalValueLabel.setText(formatPeso(total));
    }

    private void refreshItemsUI() {
        itemsContainer.getChildren().clear();
        for (int i = 0; i < workingLines.size(); i++) {
            ReceiptLine line = workingLines.get(i);
            itemsContainer.getChildren().add(createItemRow(line, i));
        }
    }

    private HBox createItemRow(ReceiptLine line, int index) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("modal-item-row");

        // Dish name
        Label name = new Label(line.name());
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2E2520;");
        name.setPrefWidth(200);
        name.setWrapText(true);
        HBox.setHgrow(name, Priority.ALWAYS);

        // Price
        Label price = new Label(formatPeso(line.unitPrice()));
        price.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #6E625C;");
        price.setPrefWidth(75);
        price.setAlignment(Pos.CENTER_RIGHT);

        // Stepper: [-] [ Qty ] [+]
        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().setAll("modal-stepper-btn");
        minusBtn.setPrefSize(28, 28);
        minusBtn.setFocusTraversable(false);
        minusBtn.setOnAction(e -> updateItemQuantity(line, line.quantity() - 1));

        Label qtyLabel = new Label(String.valueOf(line.quantity()));
        qtyLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E2520;");
        qtyLabel.setPrefWidth(30);
        qtyLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().setAll("modal-stepper-btn");
        plusBtn.setPrefSize(28, 28);
        plusBtn.setFocusTraversable(false);
        plusBtn.setOnAction(e -> updateItemQuantity(line, line.quantity() + 1));

        HBox stepper = new HBox(3, minusBtn, qtyLabel, plusBtn);
        stepper.setAlignment(Pos.CENTER);
        stepper.setPrefWidth(110);

        // Line total
        Label lineTotal = new Label(formatPeso(line.lineTotal()));
        lineTotal.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E2520;");
        lineTotal.setPrefWidth(85);
        lineTotal.setAlignment(Pos.CENTER_RIGHT);

        // Delete button
        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().setAll("modal-delete-btn");
        deleteBtn.setFocusTraversable(false);
        deleteBtn.setPrefSize(28, 28);

        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent(Icons.TRASH_2);
        trashIcon.getStyleClass().addAll("phosphor-icon");
        trashIcon.setStyle("-fx-stroke: #E74C3C; -fx-stroke-width: 2.0;");
        double scale = 14.0 / 24.0;
        trashIcon.setScaleX(scale);
        trashIcon.setScaleY(scale);

        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setTooltip(new Tooltip("Remove from order"));
        deleteBtn.setOnAction(e -> removeItem(line));

        row.getChildren().addAll(name, price, stepper, lineTotal, deleteBtn);
        return row;
    }

    private void updateItemQuantity(ReceiptLine oldLine, int newQty) {
        if (newQty <= 0) {
            removeItem(oldLine);
            return;
        }
        int index = workingLines.indexOf(oldLine);
        if (index >= 0) {
            workingLines.set(index, new ReceiptLine(
                    oldLine.dishId(),
                    oldLine.name(),
                    oldLine.imageUrl(),
                    oldLine.unitPrice(),
                    newQty
            ));
            refreshItemsUI();
            recalculateTotals();
            clearError();
        }
    }

    private void removeItem(ReceiptLine line) {
        if (workingLines.size() <= 1) {
            showError("Order must contain at least one dish. You cannot delete the last item.");
            return;
        }
        workingLines.remove(line);
        refreshItemsUI();
        recalculateTotals();
        clearError();
    }

    @FXML
    private void onAddDish() {
        Dishes selected = addDishCombo.getValue();
        if (selected == null) {
            showError("Please select a dish to add.");
            return;
        }

        int qty = 1;
        try {
            String txt = addDishQtyField.getText().trim();
            if (!txt.isEmpty()) {
                qty = Math.max(1, Integer.parseInt(txt));
            }
        } catch (NumberFormatException ignored) {
            qty = 1;
        }

        // Check if dish already exists in order
        Optional<ReceiptLine> existing = workingLines.stream()
                .filter(l -> l.dishId() == selected.id())
                .findFirst();

        if (existing.isPresent()) {
            ReceiptLine l = existing.get();
            updateItemQuantity(l, l.quantity() + qty);
        } else {
            workingLines.add(new ReceiptLine(
                    selected.id(),
                    selected.name(),
                    selected.imageUrl(),
                    selected.price(),
                    qty
            ));
            refreshItemsUI();
            recalculateTotals();
        }

        addDishCombo.setValue(null);
        addDishQtyField.setText("1");
        clearError();
    }

    @FXML
    private void onSaveChanges() {
        if (workingLines.isEmpty()) {
            showError("The order cannot be empty. Please add at least one dish.");
            return;
        }

        Window window = modalSubtitleLabel.getScene().getWindow();
        if (!AdminAuthDialog.show(window, "Authorize editing Order #" + originalReceipt.orderNumber())) {
            return;
        }

        String orderType = orderTypeCombo.getValue();
        OrderStatus newStatus = orderStatusCombo.getValue();
        String discountName = discountCombo.getValue();

        saveBtn.setDisable(true);
        Thread t = new Thread(() -> {
            try {
                boolean ok = orderDAO.updateTransaction(
                        originalReceipt.orderNumber(),
                        orderType,
                        newStatus,
                        discountName,
                        currentCalculatedTotals,
                        currentCalculatedDiscountAmount,
                        new ArrayList<>(workingLines),
                        originalReceipt.lines(),
                        originalReceipt.status()
                );

                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    if (ok) {
                        AlertUtil.showInfo("Transaction Updated",
                                "Order #" + originalReceipt.orderNumber() + " has been successfully updated.");
                        closeModal();
                        if (onSaveSuccess != null) {
                            onSaveSuccess.run();
                        }
                    } else {
                        showError("Failed to update transaction in database.");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    showError(ex.getMessage() != null ? ex.getMessage() : "Error saving changes.");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) modalSubtitleLabel.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private static String formatPeso(BigDecimal amount) {
        if (amount == null) return "₱0.00";
        return String.format(Locale.ENGLISH, "₱%,.2f", amount);
    }
}

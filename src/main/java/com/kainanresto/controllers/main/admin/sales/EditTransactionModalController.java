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
        orderTypeCombo.getItems().setAll("Dine in");
        orderTypeCombo.setValue("Dine in");
        orderTypeCombo.setDisable(true);
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

        // Populate order type - all orders in Kainan Ni Juan are strictly Dine in
        orderTypeCombo.setValue("Dine in");

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
        name.setMinWidth(120);
        name.setWrapText(true);
        HBox.setHgrow(name, Priority.ALWAYS);

        // Price
        Label price = new Label(formatPeso(line.unitPrice()));
        price.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #6E625C;");
        price.setPrefWidth(70);
        price.setMinWidth(70);
        price.setMaxWidth(70);
        price.setAlignment(Pos.CENTER_RIGHT);

        // Stepper: [-] [ Qty ] [+]
        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().setAll("modal-stepper-btn");
        minusBtn.setPrefSize(26, 26);
        minusBtn.setMinSize(26, 26);
        minusBtn.setMaxSize(26, 26);
        minusBtn.setFocusTraversable(false);
        minusBtn.setOnAction(e -> updateItemQuantity(line, line.quantity() - 1));

        Label qtyLabel = new Label(String.valueOf(line.quantity()));
        qtyLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E2520;");
        qtyLabel.setPrefWidth(32);
        qtyLabel.setMinWidth(32);
        qtyLabel.setMaxWidth(32);
        qtyLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().setAll("modal-stepper-btn");
        plusBtn.setPrefSize(26, 26);
        plusBtn.setMinSize(26, 26);
        plusBtn.setMaxSize(26, 26);
        plusBtn.setFocusTraversable(false);
        plusBtn.setOnAction(e -> updateItemQuantity(line, line.quantity() + 1));

        HBox stepper = new HBox(4, minusBtn, qtyLabel, plusBtn);
        stepper.setAlignment(Pos.CENTER);
        stepper.setPrefWidth(96);
        stepper.setMinWidth(96);
        stepper.setMaxWidth(96);

        // Line total
        Label lineTotal = new Label(formatPeso(line.lineTotal()));
        lineTotal.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E2520;");
        lineTotal.setPrefWidth(80);
        lineTotal.setMinWidth(80);
        lineTotal.setMaxWidth(80);
        lineTotal.setAlignment(Pos.CENTER_RIGHT);

        // Delete button
        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().setAll("modal-delete-btn");
        deleteBtn.setFocusTraversable(false);
        deleteBtn.setPrefSize(28, 28);
        deleteBtn.setMinSize(28, 28);
        deleteBtn.setMaxSize(28, 28);

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

        Optional<Dishes> dishOpt = availableDishes.stream()
                .filter(d -> d.id() == oldLine.dishId())
                .findFirst();

        if (dishOpt.isPresent()) {
            Dishes dish = dishOpt.get();
            int originalQty = (originalReceipt != null && originalReceipt.lines() != null)
                    ? originalReceipt.lines().stream()
                        .filter(l -> l.dishId() == dish.id())
                        .mapToInt(ReceiptLine::quantity)
                        .sum()
                    : 0;
            int netAdditional = newQty - originalQty;
            if (netAdditional > dish.quantity()) {
                int maxAllowed = Math.max(0, dish.quantity() + originalQty);
                AlertUtil.showWarning("Stock Limit", "Cannot set quantity to " + newQty + ". Only " + maxAllowed + " units available based on current inventory.");
                showError("Only " + maxAllowed + " units available for \"" + dish.name() + "\".");
                return;
            }
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
            AlertUtil.showWarning("Cannot Remove Item", "Order must contain at least one dish. You cannot delete the last item.");
            showError("Order must contain at least one dish. You cannot delete the last item.");
            return;
        }

        boolean confirm = AlertUtil.showYesNoConfirmation(
                "Remove Dish",
                "Remove \"" + line.name() + "\"?",
                "Are you sure you want to remove this dish from the order?"
        );
        if (!confirm) {
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
            AlertUtil.showWarning("Dish Selection", "Please select a dish to add.");
            showError("Please select a dish to add.");
            return;
        }

        int qty = 1;
        try {
            String txt = addDishQtyField.getText().trim();
            if (!txt.isEmpty()) {
                qty = Integer.parseInt(txt);
            }
        } catch (NumberFormatException ignored) {
            qty = 1;
        }

        if (qty <= 0) {
            AlertUtil.showWarning("Invalid Quantity", "Quantity must be at least 1.");
            showError("Quantity must be at least 1.");
            return;
        }

        int alreadyInOrder = workingLines.stream()
                .filter(l -> l.dishId() == selected.id())
                .mapToInt(ReceiptLine::quantity)
                .sum();

        int originalQty = (originalReceipt != null && originalReceipt.lines() != null)
                ? originalReceipt.lines().stream()
                    .filter(l -> l.dishId() == selected.id())
                    .mapToInt(ReceiptLine::quantity)
                    .sum()
                : 0;

        int netAdditionalQty = (alreadyInOrder + qty) - originalQty;
        if (netAdditionalQty > selected.quantity()) {
            int maxCanAdd = Math.max(0, selected.quantity() + originalQty - alreadyInOrder);
            String msg = maxCanAdd > 0
                    ? "Cannot add " + qty + " of \"" + selected.name() + "\". Only " + maxCanAdd + " additional units can be added based on current stock (" + selected.quantity() + " available)."
                    : "Cannot add more \"" + selected.name() + "\". Available stock (" + selected.quantity() + ") is exhausted.";
            AlertUtil.showWarning("Stock Limit", msg);
            showError(msg);
            return;
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
            AlertUtil.showWarning("Empty Order", "The order cannot be empty. Please add at least one dish.");
            showError("The order cannot be empty. Please add at least one dish.");
            return;
        }

        boolean confirm = AlertUtil.showYesNoConfirmation(
                "Save Changes",
                "Update Order #" + originalReceipt.orderNumber() + "?",
                "Are you sure you want to save all changes to this transaction?"
        );
        if (!confirm) {
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
                        AlertUtil.showError("Update Failed", "Failed to update transaction in database.");
                        showError("Failed to update transaction in database.");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Error saving changes.";
                    AlertUtil.showError("Save Error", msg);
                    showError(msg);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() {
        if (hasUnsavedChanges()) {
            boolean discard = AlertUtil.showYesNoConfirmation(
                    "Discard Changes",
                    "Discard unsaved changes?",
                    "You have unsaved changes to this transaction. Are you sure you want to close without saving?"
            );
            if (!discard) {
                return;
            }
        }
        closeModal();
    }

    private boolean hasUnsavedChanges() {
        if (originalReceipt == null) return false;

        OrderStatus newStatus = orderStatusCombo.getValue();
        if (newStatus != originalReceipt.status()) return true;

        String origDisc = originalReceipt.discountName() == null ? "None" : originalReceipt.discountName();
        String newDisc = discountCombo.getValue() == null ? "None" : discountCombo.getValue();
        if (!origDisc.equalsIgnoreCase(newDisc)) return true;

        List<ReceiptLine> origLines = originalReceipt.lines() == null ? List.of() : originalReceipt.lines();
        if (workingLines.size() != origLines.size()) return true;

        for (int i = 0; i < workingLines.size(); i++) {
            ReceiptLine w = workingLines.get(i);
            ReceiptLine o = origLines.get(i);
            if (w.dishId() != o.dishId() || w.quantity() != o.quantity()) {
                return true;
            }
        }
        return false;
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

package com.kainanresto.controllers.main.client.pos;

import com.kainanresto.controllers.main.client.ClientUIHelper;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.util.Icons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientPosController {
    @FXML private Label currentDateLabel;
    @FXML private HBox posSearchBox;
    @FXML private SVGPath posSearchIcon;
    @FXML private TextField posSearchField;
    @FXML private FlowPane posCategoryChips;
    @FXML private ScrollPane posScroll;
    @FXML private Label posGridPlaceholder;
    @FXML private FlowPane posDishGrid;

    @FXML private VBox receiptPanel;
    @FXML private VBox receiptLines;
    @FXML private Label receiptSubtotalLabel;
    @FXML private ComboBox<String> receiptDiscountBox;
    @FXML private Label receiptDiscountValueLabel;
    @FXML private HBox discountCardsRow;
    @FXML private Label discountCardsLabel;
    @FXML private Label receiptServiceLabel;
    @FXML private Label receiptServiceValueLabel;
    @FXML private Label receiptVatLabel;
    @FXML private Label receiptVatValueLabel;
    @FXML private Label receiptTotalLabel;
    @FXML private Button receiptPayBtn;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final double DISH_IMAGE_SIZE = 144.66;
    private static final double CARD_WIDTH = 184.47;
    private static final double RECEIPT_THUMB = 54.0;

    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private List<String> suppliedCategories = new ArrayList<>();
    private String selectedCategory = null;
    private boolean rebuildingChips = false;
    private boolean rebuildingDiscount = false;

    private Consumer<Dishes> onAddToOrder;
    private BiConsumer<ReceiptLine, Integer> onReceiptQuantityChange;
    private Consumer<String> onReceiptDiscountChange;
    private Consumer<Integer> onReceiptDiscountCardsChange;
    private Runnable proceedToPaymentHandler;

    private Map<String, Integer> discountedUnits = Map.of();
    private BigDecimal discountRate = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));

        posSearchIcon.setContent(Icons.SEARCH);
        ClientUIHelper.fitGridIcon(posSearchIcon, 20.0, 2.0);

        posSearchField.focusedProperty().addListener((obs, was, is) -> posSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        posSearchField.textProperty().addListener((obs, oldText, newText) -> applyPosFilter());

        categoryToggleGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (rebuildingChips) return;
            if (now == null && old != null) {
                categoryToggleGroup.selectToggle(old);
                return;
            }
            selectedCategory = (now == null) ? null : (String) now.getUserData();
            applyPosFilter();
        });

        receiptDiscountBox.valueProperty().addListener((obs, old, now) -> {
            if (!rebuildingDiscount && now != null && onReceiptDiscountChange != null) {
                onReceiptDiscountChange.accept(now);
            }
        });

        setCategories(null);
        setReceipt(null, null);
        setDiscountSummary(false, 1, null);
    }

    /* ============================== PUBLIC API ============================== */

    public void setCategories(List<String> categories) {
        suppliedCategories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        rebuildCategoryChips();
    }

    public void setDishes(List<Dishes> dishes) {
        dishItems.setAll(dishes == null ? List.<Dishes>of() : dishes);
        if (suppliedCategories.isEmpty()) rebuildCategoryChips();
        else applyPosFilter();
    }

    public void setDiscountOptions(List<String> options) {
        rebuildingDiscount = true;
        receiptDiscountBox.getItems().setAll(options == null ? List.<String>of() : options);
        if (receiptDiscountBox.getItems().isEmpty()) receiptDiscountBox.getSelectionModel().clearSelection();
        else receiptDiscountBox.getSelectionModel().selectFirst();
        rebuildingDiscount = false;
    }

    public void setReceipt(List<ReceiptLine> lines, ReceiptTotals totals) {
        setReceipt(lines, totals, Map.of(), BigDecimal.ZERO);
    }

    public void setReceipt(List<ReceiptLine> lines, ReceiptTotals totals,
                           Map<String, Integer> discountedUnits, BigDecimal discountRate) {
        this.discountedUnits = discountedUnits == null ? Map.of() : discountedUnits;
        this.discountRate = discountRate == null ? BigDecimal.ZERO : discountRate;

        List<Node> rows = new ArrayList<>();
        if (lines != null) {
            for (ReceiptLine line : lines) {
                if (line != null) rows.add(createReceiptRow(line));
            }
        }

        if (rows.isEmpty()) {
            Label empty = new Label("No items in the order yet.\nTap a dish to add it.");
            empty.getStyleClass().add("receipt-empty-label");
            empty.setTextAlignment(TextAlignment.CENTER);
            empty.setWrapText(true);
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            rows.add(empty);
        }
        receiptLines.getChildren().setAll(rows);

        if (totals == null) {
            BigDecimal zero = BigDecimal.ZERO;
            receiptSubtotalLabel.setText(ClientUIHelper.formatPeso(zero));
            receiptServiceLabel.setText("Service charge");
            receiptServiceValueLabel.setText(ClientUIHelper.formatPeso(zero));
            receiptVatLabel.setText("VAT");
            receiptVatValueLabel.setText(ClientUIHelper.formatPeso(zero));
            receiptTotalLabel.setText(ClientUIHelper.formatPeso(zero));
        } else {
            receiptSubtotalLabel.setText(ClientUIHelper.formatPeso(totals.subtotal()));
            receiptServiceLabel.setText("Service charge" + rateSuffix(totals.serviceChargeRate()));
            receiptServiceValueLabel.setText(ClientUIHelper.formatPeso(totals.serviceCharge()));
            receiptVatLabel.setText("VAT" + rateSuffix(totals.vatRate()));
            receiptVatValueLabel.setText(ClientUIHelper.formatPeso(totals.vat()));
            receiptTotalLabel.setText(ClientUIHelper.formatPeso(totals.total()));
        }

        receiptPayBtn.setDisable(lines == null || lines.isEmpty());
    }

    /** Shows/hides the "discount cards" row and the discount amount next to the combo. */
    public void setDiscountSummary(boolean active, int cards, BigDecimal discountAmount) {
        discountCardsRow.setVisible(active);
        discountCardsRow.setManaged(active);
        discountCardsLabel.setText(String.valueOf(cards));
        boolean hasAmount = active && discountAmount != null && discountAmount.signum() > 0;
        receiptDiscountValueLabel.setText(hasAmount ? "-" + ClientUIHelper.formatPeso(discountAmount) : "");
    }

    public void setOnAddToOrder(Consumer<Dishes> handler) { this.onAddToOrder = handler; }
    public void setOnReceiptQuantityChange(BiConsumer<ReceiptLine, Integer> handler) { this.onReceiptQuantityChange = handler; }
    public void setOnReceiptDiscountChange(Consumer<String> handler) { this.onReceiptDiscountChange = handler; }
    public void setOnReceiptDiscountCardsChange(Consumer<Integer> handler) { this.onReceiptDiscountCardsChange = handler; }
    public void setOnProceedToPayment(Runnable handler) { this.proceedToPaymentHandler = handler; }

    /* ============================== CATEGORY / FILTER ============================== */

    private void rebuildCategoryChips() {
        List<String> names = new ArrayList<>();
        if (!suppliedCategories.isEmpty()) {
            names.addAll(suppliedCategories);
        } else {
            LinkedHashSet<String> derived = new LinkedHashSet<>();
            for (Dishes dish : dishItems) {
                if (dish.category() != null && !dish.category().isBlank()) derived.add(dish.category());
            }
            names.addAll(derived);
        }

        String keep = selectedCategory;
        rebuildingChips = true;
        categoryToggleGroup.getToggles().clear();
        posCategoryChips.getChildren().clear();

        ToggleButton all = createCategoryChip("All", null);
        posCategoryChips.getChildren().add(all);
        ToggleButton toSelect = all;

        for (String c : names) {
            if (c == null) continue;
            ToggleButton chip = createCategoryChip(c, c);
            posCategoryChips.getChildren().add(chip);
            if (keep != null && keep.equalsIgnoreCase(c)) toSelect = chip;
        }
        rebuildingChips = false;

        selectedCategory = (String) toSelect.getUserData();
        toSelect.setSelected(true);
        applyPosFilter();
    }

    private ToggleButton createCategoryChip(String label, String category) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(category);
        chip.setToggleGroup(categoryToggleGroup);
        chip.getStyleClass().add("pos-category-chip");
        return chip;
    }

    private void applyPosFilter() {
        String query = posSearchField.getText() == null ? "" : posSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        List<Node> cards = new ArrayList<>();
        for (Dishes dish : dishItems) {
            boolean categoryOk = selectedCategory == null || selectedCategory.equalsIgnoreCase(dish.category());
            boolean textOk = query.isEmpty() || (dish.name() != null && dish.name().toLowerCase(Locale.ENGLISH).contains(query));
            if (categoryOk && textOk) cards.add(createDishCard(dish));
        }
        posDishGrid.getChildren().setAll(cards);

        boolean empty = cards.isEmpty();
        posGridPlaceholder.setText(dishItems.isEmpty() ? "No dishes loaded." : "No dishes match your filters.");
        posGridPlaceholder.setVisible(empty);
        posGridPlaceholder.setManaged(empty);
    }

    /* ============================== DISH CARD (matches admin menu card) ============================== */

    private Node createDishCard(Dishes dish) {
        boolean inStock = dish.quantity() > 0;

        // Image
        StackPane imageShell = new StackPane();
        imageShell.getStyleClass().add("pos-dish-image-shell");
        ClientUIHelper.fixSize(imageShell, DISH_IMAGE_SIZE, DISH_IMAGE_SIZE);

        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            try {
                Image image = new Image(dish.imageUrl(), DISH_IMAGE_SIZE * 2, DISH_IMAGE_SIZE * 2, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(DISH_IMAGE_SIZE);
                imageView.setFitHeight(DISH_IMAGE_SIZE);
                imageView.setPreserveRatio(true);
                ClientUIHelper.applyCoverCrop(imageView, image);
                double r = DISH_IMAGE_SIZE / 2.0;
                imageView.setClip(new Circle(r, r, r));
                imageShell.getChildren().add(imageView);
            } catch (IllegalArgumentException ignored) {}
        }

        // Texts
        Label name = new Label(dish.name() == null ? "\u2014" : dish.name());
        name.getStyleClass().add("pos-dish-name");
        name.setWrapText(false);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setTooltip(new Tooltip(name.getText()));

        Label price = new Label(ClientUIHelper.formatPeso(dish.price()));
        price.getStyleClass().add("pos-dish-price");

        Label qtyLabel = new Label("Qty: " + dish.quantity());
        qtyLabel.getStyleClass().add("pos-dish-qty");

        Label statusBadge = new Label(inStock ? "Available" : "Unavailable");
        statusBadge.getStyleClass().addAll("pos-dish-badge",
                inStock ? "pos-dish-badge-available" : "pos-dish-badge-unavailable");

        VBox badgeBox = new VBox(6, qtyLabel, statusBadge);
        badgeBox.setAlignment(Pos.CENTER);

        // Card
        VBox card = new VBox(name, price, badgeBox);
        card.getStyleClass().add("pos-dish-card");
        card.setMinWidth(CARD_WIDTH);
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);

        double overhang = DISH_IMAGE_SIZE / 2.0;
        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(overhang, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        wrapper.setMinWidth(CARD_WIDTH);
        wrapper.setPrefWidth(CARD_WIDTH);
        wrapper.setMaxWidth(CARD_WIDTH);
        wrapper.setAlignment(Pos.TOP_CENTER);

        // Click to add; out-of-stock dishes are dimmed and ignore clicks
        if (inStock) {
            wrapper.setCursor(Cursor.HAND);
            wrapper.setOnMouseClicked(e -> { if (onAddToOrder != null) onAddToOrder.accept(dish); });
        } else {
            wrapper.setOpacity(0.55);
        }
        return wrapper;
    }

    /* ============================== RECEIPT ROWS ============================== */

    private Node createReceiptRow(ReceiptLine line) {
        StackPane thumb = new StackPane();
        ClientUIHelper.fixSize(thumb, RECEIPT_THUMB, RECEIPT_THUMB);
        thumb.setClip(new Circle(RECEIPT_THUMB / 2.0, RECEIPT_THUMB / 2.0, RECEIPT_THUMB / 2.0));

        if (line.imageUrl() != null && !line.imageUrl().isBlank()) {
            try {
                Image image = new Image(line.imageUrl(), RECEIPT_THUMB * 2, RECEIPT_THUMB * 2, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(RECEIPT_THUMB);
                imageView.setFitHeight(RECEIPT_THUMB);
                imageView.setPreserveRatio(true);
                ClientUIHelper.applyCoverCrop(imageView, image);
                thumb.getChildren().add(imageView);
            } catch (IllegalArgumentException ignored) {}
        }

        // ---- discount info for this line ----
        int discQty = Math.min(discountedUnits.getOrDefault(line.name(), 0), line.quantity());
        boolean discounted = discQty > 0 && discountRate.signum() > 0;
        BigDecimal off = discounted
                ? line.unitPrice().multiply(discountRate).multiply(BigDecimal.valueOf(discQty)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal discountedTotal = line.lineTotal().subtract(off);
        boolean wholeLine = discQty == line.quantity();

        // ---- name + unit price ----
        Label name = new Label(ClientUIHelper.valueOrDash(line.name()));
        name.getStyleClass().add("receipt-line-name");
        name.setWrapText(true);

        VBox info = new VBox(2.0, name);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        if (discounted && wholeLine) {
            // every unit discounted: old unit price struck through, then the new one
            BigDecimal unitOff = line.unitPrice().multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
            Text oldUnit = new Text(ClientUIHelper.formatPeso(line.unitPrice()));
            oldUnit.setStrikethrough(true);
            oldUnit.getStyleClass().add("receipt-old-text");
            Label newUnit = new Label(ClientUIHelper.formatPeso(line.unitPrice().subtract(unitOff)));
            newUnit.getStyleClass().add("receipt-new-unit");
            HBox unitRow = new HBox(6.0, oldUnit, newUnit);
            unitRow.setAlignment(Pos.CENTER_LEFT);
            info.getChildren().add(unitRow);
        } else {
            Label unit = new Label(ClientUIHelper.formatPeso(line.unitPrice()));
            unit.getStyleClass().add("receipt-line-unit");
            info.getChildren().add(unit);
        }

        if (discounted) {
            String pct = discountRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();
            String text = wholeLine
                    ? pct + "% off"
                    : pct + "% off on " + discQty + " of " + line.quantity();
            Label badge = new Label(text);
            badge.getStyleClass().add("receipt-discount-badge");
            info.getChildren().add(badge);
        }

        // ---- qty stepper ----
        Button minus = createStepButton(Icons.MINUS, "Decrease quantity", () -> requestQuantity(line, line.quantity() - 1));
        Label qty = new Label(String.valueOf(line.quantity()));
        qty.getStyleClass().add("receipt-qty");
        Button plus = createStepButton(Icons.PLUS, "Increase quantity", () -> requestQuantity(line, line.quantity() + 1));
        HBox stepper = new HBox(6.0, minus, qty, plus);
        stepper.setAlignment(Pos.CENTER);
        stepper.getStyleClass().add("receipt-col-qty");

        // ---- line price (original struck through above the discounted total) ----
        VBox priceBox = new VBox(2.0);
        priceBox.getStyleClass().add("receipt-col-price");
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        if (discounted) {
            Text oldTotal = new Text(ClientUIHelper.formatPeso(line.lineTotal()));
            oldTotal.setStrikethrough(true);
            oldTotal.getStyleClass().add("receipt-old-text");
            Label newTotal = new Label(ClientUIHelper.formatPeso(discountedTotal));
            newTotal.getStyleClass().add("receipt-new-price");
            priceBox.getChildren().addAll(oldTotal, newTotal);
        } else {
            Label price = new Label(ClientUIHelper.formatPeso(line.lineTotal()));
            price.getStyleClass().add("receipt-line-price");
            priceBox.getChildren().add(price);
        }

        HBox row = new HBox(10.0, thumb, info, stepper, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button createStepButton(String iconContent, String tooltip, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconContent);
        icon.getStyleClass().add("phosphor-icon");
        ClientUIHelper.fitGridIcon(icon, 10.0, 1.5);
        StackPane host = new StackPane(icon);
        host.getStyleClass().add("pos-host-10");

        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("receipt-step-btn");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private void requestQuantity(ReceiptLine line, int newQuantity) {
        if (onReceiptQuantityChange != null) onReceiptQuantityChange.accept(line, newQuantity);
    }

    /* ============================== FXML HANDLERS ============================== */

    @FXML
    private void onProceedToPayment() {
        if (proceedToPaymentHandler != null) proceedToPaymentHandler.run();
    }

    @FXML
    private void onDiscountCardsMinus() {
        if (onReceiptDiscountCardsChange != null) onReceiptDiscountCardsChange.accept(-1);
    }

    @FXML
    private void onDiscountCardsPlus() {
        if (onReceiptDiscountCardsChange != null) onReceiptDiscountCardsChange.accept(1);
    }

    private static String rateSuffix(BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }
}
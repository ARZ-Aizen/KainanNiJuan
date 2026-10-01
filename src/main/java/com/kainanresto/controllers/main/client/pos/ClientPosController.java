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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

import java.math.BigDecimal;
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
    @FXML private ToggleGroup receiptTypeGroup;
    @FXML private Label receiptSubtitleLabel;
    @FXML private VBox receiptLines;
    @FXML private Label receiptSubtotalLabel;
    @FXML private ComboBox<String> receiptDiscountBox;
    @FXML private Label receiptServiceLabel;
    @FXML private Label receiptServiceValueLabel;
    @FXML private Label receiptVatLabel;
    @FXML private Label receiptVatValueLabel;
    @FXML private Label receiptTotalLabel;
    @FXML private Button receiptPayBtn;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final double DISH_IMAGE_SIZE = 144.66;
    private static final double CARD_WIDTH = 184.47;
    private static final double CARD_HEIGHT = 190.34;
    private static final double IMAGE_OVERHANG = 60.69;
    private static final double NAME_HEIGHT = 54.0;
    private static final double RECEIPT_THUMB = 54.0;

    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private List<String> suppliedCategories = new ArrayList<>();
    private String selectedCategory = null;
    private boolean rebuildingChips = false;
    private boolean rebuildingDiscount = false;
    private String receiptTypeDetail = null;

    private Consumer<Dishes> onAddToOrder;
    private BiConsumer<ReceiptLine, Integer> onReceiptQuantityChange;
    private Consumer<String> onReceiptOrderTypeChange;
    private Consumer<String> onReceiptDiscountChange;
    private Runnable proceedToPaymentHandler;

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

        receiptTypeGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                receiptTypeGroup.selectToggle(old);
                return;
            }
            updateReceiptSubtitle();
            if (now != null && onReceiptOrderTypeChange != null) {
                onReceiptOrderTypeChange.accept(((ToggleButton) now).getText());
            }
        });

        receiptDiscountBox.valueProperty().addListener((obs, old, now) -> {
            if (!rebuildingDiscount && now != null && onReceiptDiscountChange != null) {
                onReceiptDiscountChange.accept(now);
            }
        });

        updateReceiptSubtitle();
        setCategories(null);
        setReceipt(null, null);
    }

    public void setCategories(List<String> categories) {
        suppliedCategories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        rebuildCategoryChips();
    }

    public void setDishes(List<Dishes> dishes) {
        dishItems.setAll(dishes == null ? List.<Dishes>of() : dishes);
        if (suppliedCategories.isEmpty()) rebuildCategoryChips();
        else applyPosFilter();
    }

    public void setOnAddToOrder(Consumer<Dishes> handler) { this.onAddToOrder = handler; }

    public void setReceipt(List<ReceiptLine> lines, ReceiptTotals totals) {
        List<Node> rows = new ArrayList<>();
        if (lines != null) {
            for (ReceiptLine line : lines) {
                if (line != null) rows.add(createReceiptRow(line));
            }
        }
        receiptLines.getChildren().setAll(rows);

        if (totals == null) {
            receiptSubtotalLabel.setText("\u2014");
            receiptServiceLabel.setText("Service charge");
            receiptServiceValueLabel.setText("\u2014");
            receiptVatLabel.setText("VAT");
            receiptVatValueLabel.setText("\u2014");
            receiptTotalLabel.setText("\u2014");
        } else {
            receiptSubtotalLabel.setText(ClientUIHelper.formatPeso(totals.subtotal()));
            receiptServiceLabel.setText("Service charge" + rateSuffix(totals.serviceChargeRate()));
            receiptServiceValueLabel.setText(ClientUIHelper.formatPeso(totals.serviceCharge()));
            receiptVatLabel.setText("VAT" + rateSuffix(totals.vatRate()));
            receiptVatValueLabel.setText(ClientUIHelper.formatPeso(totals.vat()));
            receiptTotalLabel.setText(ClientUIHelper.formatPeso(totals.total()));
        }

        boolean show = !rows.isEmpty();
        receiptPanel.setVisible(show);
        receiptPanel.setManaged(show);
    }

    public void setReceiptOrderTypeDetail(String detail) {
        this.receiptTypeDetail = detail;
        updateReceiptSubtitle();
    }

    public void setDiscountOptions(List<String> options) {
        rebuildingDiscount = true;
        receiptDiscountBox.getItems().setAll(options == null ? List.<String>of() : options);
        if (receiptDiscountBox.getItems().isEmpty()) receiptDiscountBox.getSelectionModel().clearSelection();
        else receiptDiscountBox.getSelectionModel().selectFirst();
        rebuildingDiscount = false;
    }

    public void setOnReceiptQuantityChange(BiConsumer<ReceiptLine, Integer> handler) { this.onReceiptQuantityChange = handler; }
    public void setOnReceiptOrderTypeChange(Consumer<String> handler) { this.onReceiptOrderTypeChange = handler; }
    public void setOnReceiptDiscountChange(Consumer<String> handler) { this.onReceiptDiscountChange = handler; }
    public void setOnProceedToPayment(Runnable handler) { this.proceedToPaymentHandler = handler; }

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

    private Node createDishCard(Dishes dish) {
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
                imageShell.getChildren().add(imageView);
            } catch (IllegalArgumentException ignored) {}
        }

        Label name = new Label(dish.name() == null ? "\u2014" : dish.name());
        name.getStyleClass().add("pos-dish-name");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setAlignment(Pos.TOP_CENTER);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setMinHeight(NAME_HEIGHT);
        name.setPrefHeight(NAME_HEIGHT);
        name.setMaxHeight(NAME_HEIGHT);

        Label price = new Label(ClientUIHelper.formatPeso(dish.price()));
        price.getStyleClass().add("pos-dish-price");

        Button addBtn = createAddButton(dish);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox priceRow = new HBox(price, spacer, addBtn);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(name, priceRow);
        card.getStyleClass().add("pos-dish-card");
        ClientUIHelper.fixSize(card, CARD_WIDTH, CARD_HEIGHT);

        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(IMAGE_OVERHANG, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        ClientUIHelper.fixSize(wrapper, CARD_WIDTH, CARD_HEIGHT + IMAGE_OVERHANG);
        return wrapper;
    }

    private Button createAddButton(Dishes dish) {
        SVGPath plus = new SVGPath();
        plus.setContent(Icons.PLUS);
        plus.getStyleClass().add("phosphor-icon");
        ClientUIHelper.fitGridIcon(plus, 8.0, 1.5);

        StackPane host = new StackPane(plus);
        host.getStyleClass().add("pos-host-8");

        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("pos-add-btn");
        b.setTooltip(new Tooltip("Add to order"));
        b.setDisable(!dish.available());
        b.setOnAction(e -> { if (onAddToOrder != null) onAddToOrder.accept(dish); });
        return b;
    }

    private void updateReceiptSubtitle() {
        Toggle selected = receiptTypeGroup.getSelectedToggle();
        String type = selected == null ? "\u2014" : ((ToggleButton) selected).getText();
        boolean hasDetail = receiptTypeDetail != null && !receiptTypeDetail.isBlank();
        receiptSubtitleLabel.setText(hasDetail ? type + " \u2022 " + receiptTypeDetail.trim() : type);
    }

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

        Label name = new Label(ClientUIHelper.valueOrDash(line.name()));
        name.getStyleClass().add("receipt-line-name");
        name.setWrapText(true);
        Label unit = new Label(ClientUIHelper.formatPeso(line.unitPrice()));
        unit.getStyleClass().add("receipt-line-unit");
        VBox info = new VBox(2.0, name, unit);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button minus = createStepButton(Icons.MINUS, "Decrease quantity", () -> requestQuantity(line, line.quantity() - 1));
        Label qty = new Label(String.valueOf(line.quantity()));
        qty.getStyleClass().add("receipt-qty");
        Button plus = createStepButton(Icons.PLUS, "Increase quantity", () -> requestQuantity(line, line.quantity() + 1));
        HBox stepper = new HBox(6.0, minus, qty, plus);
        stepper.setAlignment(Pos.CENTER);
        stepper.getStyleClass().add("receipt-col-qty");

        Label price = new Label(ClientUIHelper.formatPeso(line.lineTotal()));
        price.getStyleClass().addAll("receipt-line-price", "receipt-col-price");

        HBox row = new HBox(10.0, thumb, info, stepper, price);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button createStepButton(String iconContent, String tooltip, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconContent);
        icon.getStyleClass().add("phosphor-icon");
        ClientUIHelper.fitGridIcon(icon, 8.0, 1.5);
        StackPane host = new StackPane(icon);
        host.getStyleClass().add("pos-host-8");

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

    @FXML
    private void onProceedToPayment() {
        if (proceedToPaymentHandler != null) proceedToPaymentHandler.run();
    }

    private static String rateSuffix(BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }
}
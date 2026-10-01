package com.kainanresto.controllers.main.client.om;

import com.kainanresto.controllers.main.client.ClientUIHelper;
import com.kainanresto.model.order.*;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.util.Icons;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class ClientOrderManagementController {

    @FXML private Label omSubtitleLabel;
    @FXML private HBox omSearchBox;
    @FXML private SVGPath omSearchIcon;
    @FXML private TextField omSearchField;
    @FXML private FlowPane omStatusChips;
    @FXML private ScrollPane omScroll;
    @FXML private Label omGridPlaceholder;
    @FXML private GridPane omGrid;

    @FXML private VBox omReceiptPanel;
    @FXML private Label omrOrderTitleLabel;
    @FXML private Label omrStatusLabel;
    @FXML private Label omrTypeLabel;
    @FXML private Label omrTimeLabel;
    @FXML private Label omrCashierLabel;
    @FXML private VBox omrLines;
    @FXML private Label omrSubtotalLabel;
    @FXML private Label omrDiscountLabel;
    @FXML private Label omrServiceLabel;
    @FXML private Label omrServiceValueLabel;
    @FXML private Label omrVatLabel;
    @FXML private Label omrVatValueLabel;
    @FXML private Label omrTotalLabel;
    @FXML private Button omrCompleteBtn;
    @FXML private Button omrCancelBtn;

    public static final String OM_SUBTITLE = "Monitor and manage current restaurant orders.";
    private static final int OM_COLUMNS = 3;
    private static final int OM_COLUMNS_WITH_PANEL = 2;
    private static final PseudoClass OM_SELECTED = PseudoClass.getPseudoClass("om-selected");
    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final DateTimeFormatter OM_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final ToggleGroup omStatusGroup = new ToggleGroup();
    private final ObservableList<OrderCard> omOrders = FXCollections.observableArrayList();
    private OrderStatus omSelectedStatus = null;
    private String omSelectedOrderNo = null;
    private OrderReceipt omReceiptDetails = null;
    private Timeline refreshTimeline;

    private Consumer<OrderCard> onCompleteOrder;
    private Consumer<OrderCard> onCancelOrder;
    private Consumer<OrderCard> onOrderDetails;
    private Consumer<OrderCard> onOrderSelected;

    @FXML
    public void initialize() {
        omSubtitleLabel.setText(OM_SUBTITLE);

        omSearchIcon.setContent(Icons.SEARCH);
        ClientUIHelper.fitGridIcon(omSearchIcon, 20.0, 2.0);

        omSearchField.focusedProperty().addListener((obs, was, is) -> omSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        omSearchField.textProperty().addListener((obs, old, now) -> applyOmFilter());

        ToggleButton all = createOmChip("All", null);
        omStatusChips.getChildren().add(all);
        for (OrderStatus s : OrderStatus.values()) {
            omStatusChips.getChildren().add(createOmChip(s.getDisplayName(), s));
        }
        all.setSelected(true);
        omStatusGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                omStatusGroup.selectToggle(old);
                return;
            }
            omSelectedStatus = (now == null) ? null : (OrderStatus) now.getUserData();
            applyOmFilter();
        });

        refreshTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            updateOmReceiptPanel();
            applyOmFilter();
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();

        // Bind stop listener to grid since root property isn't available
        omGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) refreshTimeline.stop();
        });

        updateOmChipCounts();
        updateOmReceiptPanel();
        applyOmFilter();
    }

    public void setOrders(List<OrderCard> orders) {
        omOrders.setAll(orders == null ? List.<OrderCard>of() : orders);
        updateOmChipCounts();
        updateOmReceiptPanel();
        applyOmFilter();
    }

    public void setOrderReceipt(OrderReceipt receipt) {
        if (receipt == null || !Objects.equals(receipt.orderNumber(), omSelectedOrderNo)) return;
        omReceiptDetails = receipt;
        updateOmReceiptPanel();
    }

    public void setOnOrderSelected(Consumer<OrderCard> handler) { this.onOrderSelected = handler; }
    public void setOnCompleteOrder(Consumer<OrderCard> handler) { this.onCompleteOrder = handler; }
    public void setOnCancelOrder(Consumer<OrderCard> handler) { this.onCancelOrder = handler; }
    public void setOnOrderDetails(Consumer<OrderCard> handler) { this.onOrderDetails = handler; }

    private ToggleButton createOmChip(String label, OrderStatus status) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(status);
        chip.getProperties().put("label", label);
        chip.setToggleGroup(omStatusGroup);
        chip.getStyleClass().add("om-status-chip");
        return chip;
    }

    private void updateOmChipCounts() {
        for (Node n : omStatusChips.getChildren()) {
            if (!(n instanceof ToggleButton chip)) continue;
            OrderStatus status = (OrderStatus) chip.getUserData();
            long count = status == null ? omOrders.size() : omOrders.stream().filter(o -> o.status() == status).count();
            chip.setText(chip.getProperties().get("label") + " (" + count + ")");
        }
    }

    private void applyOmColumns(int columns) {
        omGrid.getColumnConstraints().clear();
        double percent = 100.0 / columns;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percent);
            cc.setHgrow(Priority.ALWAYS);
            omGrid.getColumnConstraints().add(cc);
        }
    }

    private void applyOmFilter() {
        String query = omSearchField.getText() == null ? "" : omSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        int columns = omReceiptPanel.isVisible() ? OM_COLUMNS_WITH_PANEL : OM_COLUMNS;
        applyOmColumns(columns);

        omGrid.getChildren().clear();
        int index = 0;
        for (OrderCard order : omOrders) {
            boolean statusOk = omSelectedStatus == null || order.status() == omSelectedStatus;
            if (!statusOk || !matchesOmText(order, query)) continue;
            Node card = createOrderCard(order);
            GridPane.setValignment(card, VPos.TOP);
            omGrid.add(card, index % columns, index / columns);
            index++;
        }

        boolean empty = index == 0;
        omGridPlaceholder.setText(omOrders.isEmpty() ? "No orders loaded." : "No orders match your filters.");
        omGridPlaceholder.setVisible(empty);
        omGridPlaceholder.setManaged(empty);
    }

    private Node createOrderCard(OrderCard order) {
        Label title = new Label("Order #" + ClientUIHelper.valueOrDash(order.orderNumber()));
        title.getStyleClass().add("om-order-title");
        Label type = new Label(orderTypeLine(order));
        type.getStyleClass().add("om-order-type");
        VBox titleBox = new VBox(2.0, title, type);

        Label badge = new Label(order.status() == null ? "\u2014" : order.status().getDisplayName());
        badge.getStyleClass().addAll("om-badge", badgeClass(order.status()));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(titleBox, headerSpacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label time = new Label(formatRelativeTime(order.createdAt()));
        time.getStyleClass().add("om-order-time");
        int items = countItems(order);
        Label count = new Label(items + (items == 1 ? " Item" : " Items"));
        count.getStyleClass().add("om-order-count");
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        HBox meta = new HBox(time, metaSpacer, count);
        meta.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("om-divider");

        VBox lines = new VBox(6.0);
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) {
                if (line == null) continue;
                Label name = new Label(ClientUIHelper.valueOrDash(line.name()));
                name.getStyleClass().add("om-line-name");
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, Priority.ALWAYS);
                Label qty = new Label("\u00D7" + line.quantity());
                qty.getStyleClass().add("om-line-qty");
                HBox row = new HBox(name, qty);
                row.setAlignment(Pos.TOP_LEFT);
                lines.getChildren().add(row);
            }
        }

        Label totalLabel = new Label("Total Amount");
        totalLabel.getStyleClass().add("om-total-label");
        Label totalValue = new Label(ClientUIHelper.formatPeso(order.total()));
        totalValue.getStyleClass().add("om-total-value");
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        HBox total = new HBox(totalLabel, totalSpacer, totalValue);
        total.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(8.0);
        if (order.status() == OrderStatus.PREPARING) {
            Button complete = createOmButton("Complete Order", "om-btn-primary", () -> {
                if (onCompleteOrder != null) onCompleteOrder.accept(order);
            });
            complete.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(complete, Priority.ALWAYS);
            actions.getChildren().add(complete);
        }
        actions.getChildren().add(createOmButton("Details", "om-btn-outline", () -> {
            if (onOrderDetails != null) onOrderDetails.accept(order);
        }));

        VBox card = new VBox(header, meta, divider, lines, total, actions);
        card.getStyleClass().add("om-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setUserData(order.orderNumber());
        card.pseudoClassStateChanged(OM_SELECTED, Objects.equals(omSelectedOrderNo, order.orderNumber()));
        card.setOnMouseClicked(e -> selectOrder(order.orderNumber()));
        return card;
    }

    private Button createOmButton(String text, String variantClass, Runnable action) {
        Button b = new Button(text);
        b.setMnemonicParsing(false);
        b.getStyleClass().addAll("om-btn", variantClass);
        b.setOnAction(e -> action.run());
        b.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        return b;
    }

    private void selectOrder(String orderNumber) {
        boolean wasOpen = omReceiptPanel.isVisible();
        String previous = omSelectedOrderNo;
        omSelectedOrderNo = Objects.equals(omSelectedOrderNo, orderNumber) ? null : orderNumber;
        if (!Objects.equals(previous, omSelectedOrderNo)) omReceiptDetails = null;
        updateOmReceiptPanel();

        if (wasOpen != omReceiptPanel.isVisible()) applyOmFilter();
        else {
            for (Node n : omGrid.getChildren()) {
                n.pseudoClassStateChanged(OM_SELECTED, Objects.equals(omSelectedOrderNo, n.getUserData()));
            }
        }

        if (omSelectedOrderNo != null && onOrderSelected != null) {
            OrderCard order = findOmOrder(omSelectedOrderNo);
            if (order != null) onOrderSelected.accept(order);
        }
    }

    private OrderCard findOmOrder(String orderNumber) {
        if (orderNumber == null) return null;
        for (OrderCard o : omOrders) if (Objects.equals(o.orderNumber(), orderNumber)) return o;
        return null;
    }

    private void updateOmReceiptPanel() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order == null) {
            omSelectedOrderNo = null;
            omReceiptDetails = null;
            omReceiptPanel.setVisible(false);
            omReceiptPanel.setManaged(false);
            return;
        }
        omReceiptPanel.setVisible(true);
        omReceiptPanel.setManaged(true);

        OrderReceipt detail = (omReceiptDetails != null && Objects.equals(omReceiptDetails.orderNumber(), order.orderNumber())) ? omReceiptDetails : null;

        omrOrderTitleLabel.setText("Order #" + ClientUIHelper.valueOrDash(order.orderNumber()));
        omrStatusLabel.setText(order.status() == null ? "\u2014" : order.status().getDisplayName());
        omrStatusLabel.getStyleClass().removeIf(c -> c.startsWith("omr-badge-"));
        omrStatusLabel.getStyleClass().add(receiptBadgeClass(order.status()));
        omrTypeLabel.setText(orderTypeLine(order));
        omrTimeLabel.setText(formatRelativeTime(order.createdAt()));
        omrCashierLabel.setText(detail == null ? "\u2014" : ClientUIHelper.valueOrDash(detail.cashierName()));

        List<Node> rows = new ArrayList<>();
        if (detail != null && detail.lines() != null) {
            for (ReceiptLine line : detail.lines()) {
                if (line != null) rows.add(createOmReceiptRow(line));
            }
        }
        omrLines.getChildren().setAll(rows);

        ReceiptTotals totals = detail == null ? null : detail.totals();
        if (totals == null) {
            omrSubtotalLabel.setText("\u2014");
            omrServiceLabel.setText("Service charge");
            omrServiceValueLabel.setText("\u2014");
            omrVatLabel.setText("VAT");
            omrVatValueLabel.setText("\u2014");
            omrTotalLabel.setText(ClientUIHelper.formatPeso(order.total()));
        } else {
            omrSubtotalLabel.setText(ClientUIHelper.formatPeso(totals.subtotal()));
            omrServiceLabel.setText("Service charge" + rateSuffix(totals.serviceChargeRate()));
            omrServiceValueLabel.setText(ClientUIHelper.formatPeso(totals.serviceCharge()));
            omrVatLabel.setText("VAT" + rateSuffix(totals.vatRate()));
            omrVatValueLabel.setText(ClientUIHelper.formatPeso(totals.vat()));
            omrTotalLabel.setText(ClientUIHelper.formatPeso(totals.total()));
        }

        omrDiscountLabel.setText((detail == null || detail.discountName() == null || detail.discountName().isBlank()) ? "None" : detail.discountName().trim());

        boolean preparing = order.status() == OrderStatus.PREPARING;
        omrCompleteBtn.setVisible(preparing);
        omrCompleteBtn.setManaged(preparing);
        omrCancelBtn.setVisible(preparing);
        omrCancelBtn.setManaged(preparing);
    }

    private Node createOmReceiptRow(ReceiptLine line) {
        Label name = new Label(ClientUIHelper.valueOrDash(line.name()));
        name.getStyleClass().add("omr-line-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);
        Label qty = new Label(String.valueOf(line.quantity()));
        qty.getStyleClass().addAll("omr-line-qty", "omr-col-qty");
        Label price = new Label(ClientUIHelper.formatPeso(line.lineTotal()));
        price.getStyleClass().addAll("omr-line-price", "omr-col-price");
        HBox row = new HBox(name, qty, price);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @FXML private void onOmReceiptComplete() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order != null && onCompleteOrder != null) onCompleteOrder.accept(order);
    }

    @FXML private void onOmReceiptCancel() {
        OrderCard order = findOmOrder(omSelectedOrderNo);
        if (order != null && onCancelOrder != null) onCancelOrder.accept(order);
    }

    private static String badgeClass(OrderStatus status) {
        if (status == null) return "om-badge-neutral";
        return switch (status) { case PREPARING -> "om-badge-preparing"; case COMPLETED -> "om-badge-completed"; case CANCELLED -> "om-badge-cancelled"; };
    }

    private static String receiptBadgeClass(OrderStatus status) {
        if (status == null) return "omr-badge-neutral";
        return switch (status) { case PREPARING -> "omr-badge-preparing"; case COMPLETED -> "omr-badge-completed"; case CANCELLED -> "omr-badge-cancelled"; };
    }

    private static String orderTypeLine(OrderCard order) {
        String type = ClientUIHelper.valueOrDash(order.orderType());
        String detail = order.orderTypeDetail();
        return (detail == null || detail.isBlank()) ? type : type + " \u00B7 " + detail.trim();
    }

    private static int countItems(OrderCard order) {
        int sum = 0;
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) if (line != null) sum += line.quantity();
        }
        return sum;
    }

    private static String formatRelativeTime(LocalDateTime created) {
        if (created == null) return "\u2014";
        long mins = ChronoUnit.MINUTES.between(created, LocalDateTime.now());
        if (mins < 1) return "Just now";
        if (mins < 60) return mins + (mins == 1 ? " min ago" : " mins ago");
        long hours = mins / 60;
        if (hours < 24) return hours + (hours == 1 ? " hr ago" : " hrs ago");
        long days = hours / 24;
        if (days < 7) return days + (days == 1 ? " day ago" : " days ago");
        return created.toLocalDate().format(OM_DATE_FMT);
    }

    private static boolean matchesOmText(OrderCard order, String query) {
        if (query.isEmpty()) return true;
        if (containsLower(order.orderNumber(), query) || containsLower(orderTypeLine(order), query)) return true;
        if (order.lines() != null) {
            for (OrderLine line : order.lines()) {
                if (line != null && containsLower(line.name(), query)) return true;
            }
        }
        return false;
    }

    private static boolean containsLower(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(lowerQuery);
    }

    private static String rateSuffix(java.math.BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }
}
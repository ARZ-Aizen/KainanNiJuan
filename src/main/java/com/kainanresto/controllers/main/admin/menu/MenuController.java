package com.kainanresto.controllers.main.admin.menu;

import com.kainanresto.dao.ProductDAO;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuController {

    public record NewDishForm(String name, String category, BigDecimal price, String description, boolean available, String imageUri, int quantity) {}

    /* ============================== VIEWS ============================== */
    @FXML private VBox menuGridView;
    @FXML private VBox addDishView;

    /* ============================== MENU GRID ============================== */
    @FXML private HBox menuSearchBox;
    @FXML private TextField menuSearchField;
    @FXML private FlowPane menuCategoryChips;
    @FXML private FlowPane dishGrid;
    @FXML private Label dishGridPlaceholder;
    @FXML private SVGPath menuSearchIcon, addDishIcon;

    /* ============================== ADD DISH FORM ============================== */
    @FXML private Label addDishFormTitle;
    @FXML private Button saveDishBtn;
    @FXML private TextField addDishNameField, addDishPriceField, addDishQuantityField, addCategoryNameField;
    @FXML private TextArea addDishDescriptionField;
    @FXML private ComboBox<String> addDishCategoryCombo;
    @FXML private ToggleGroup addDishStatusGroup;
    @FXML private ToggleButton addDishAvailableBtn, addDishUnavailableBtn;
    @FXML private ImageView addDishPreviewImage;
    @FXML private Label addDishErrorLabel, addCategoryErrorLabel, addCategoryCountLabel, addCategoryEmptyLabel;
    @FXML private SVGPath addDishUploadIcon, addCategoryAddIcon;
    @FXML private VBox addCategoryList;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final double DISH_IMAGE_SIZE = 144.66;
    private static final int MAX_QUANTITY = 99999;
    private static final int MAX_CATEGORY_LENGTH = 40;

    private final ProductDAO productDAO = new ProductDAO();
    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private final List<String> categoryNames = new ArrayList<>();

    private String selectedCategory = null;
    private boolean rebuildingChips = false;
    private String addDishImageUri;

    // Tracks the dish currently being edited. If null, we are adding a new dish.
    private Dishes currentEditingDish = null;

    @FXML
    public void initialize() {
        setupIcons();
        setupMenuPage();
        setupAddDishPage();
        loadMenuManagementData();
        showMenuGrid();
    }

    private void setupIcons() {
        setIconAndScale(menuSearchIcon, Icons.SEARCH, 14.0);
        setIconAndScale(addDishIcon, Icons.PLUS, 16.0);
        setIconAndScale(addDishUploadIcon, Icons.IMAGE, 24.0);
        setIconAndScale(addCategoryAddIcon, Icons.PLUS, 14.0);
    }

    private void setIconAndScale(SVGPath icon, String content, double targetSize) {
        if (icon != null && content != null) {
            icon.setContent(content);
            double scale = targetSize / 24.0;
            icon.setScaleX(scale);
            icon.setScaleY(scale);
            icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
        }
    }

    /* ============================== VIEW TOGGLING ============================== */

    private void showMenuGrid() {
        addDishView.setVisible(false);
        addDishView.setManaged(false);
        menuGridView.setVisible(true);
        menuGridView.setManaged(true);
    }

    @FXML
    private void onAddDish() {
        resetAddDishForm();
        menuGridView.setVisible(false);
        menuGridView.setManaged(false);
        addDishView.setVisible(true);
        addDishView.setManaged(true);
    }

    @FXML
    private void onAddDishCancel() {
        showMenuGrid();
    }

    /* ============================== DATA LOADING ============================== */

    private void loadMenuManagementData() {
        setMenuCategories(productDAO.getAllCategories());
        setDishes(productDAO.getAllDishes());
    }

    /* ============================== MENU GRID LOGIC ============================== */

    private void setupMenuPage() {
        menuSearchField.focusedProperty().addListener((obs, was, is) -> menuSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        menuSearchField.textProperty().addListener((obs, oldText, newText) -> applyMenuFilter());

        categoryToggleGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (rebuildingChips) return;
            if (now == null && old != null) {
                categoryToggleGroup.selectToggle(old);
                return;
            }
            selectedCategory = (now == null) ? null : (String) now.getUserData();
            applyMenuFilter();
        });
    }

    private void setMenuCategories(List<String> categories) {
        List<String> safeCategories = categories == null ? new ArrayList<>() : categories;
        categoryNames.clear();
        categoryNames.addAll(safeCategories);

        rebuildingChips = true;
        categoryToggleGroup.getToggles().clear();
        menuCategoryChips.getChildren().clear();

        ToggleButton all = createCategoryChip("All", null);
        menuCategoryChips.getChildren().add(all);
        for (String c : categoryNames) {
            menuCategoryChips.getChildren().add(createCategoryChip(c, c));
        }
        rebuildingChips = false;

        selectedCategory = null;
        all.setSelected(true);
        applyMenuFilter();

        if (addDishCategoryCombo != null) {
            addDishCategoryCombo.getItems().setAll(categoryNames);
        }
        rebuildCategoryList();
    }

    private void setDishes(List<Dishes> dishes) {
        dishItems.setAll(dishes == null ? List.of() : dishes);
        applyMenuFilter();
    }

    private ToggleButton createCategoryChip(String label, String category) {
        ToggleButton chip = new ToggleButton(label);
        chip.setMnemonicParsing(false);
        chip.setUserData(category);
        chip.setToggleGroup(categoryToggleGroup);
        chip.getStyleClass().add("category-chip");
        return chip;
    }

    private void applyMenuFilter() {
        String query = menuSearchField.getText() == null ? "" : menuSearchField.getText().trim().toLowerCase(Locale.ENGLISH);

        List<Node> cards = new ArrayList<>();
        for (Dishes dish : dishItems) {
            boolean categoryOk = selectedCategory == null || selectedCategory.equalsIgnoreCase(dish.category());
            boolean textOk = query.isEmpty() || (dish.name() != null && dish.name().toLowerCase(Locale.ENGLISH).contains(query));
            if (categoryOk && textOk) {
                cards.add(createDishCard(dish));
            }
        }
        dishGrid.getChildren().setAll(cards);

        boolean empty = cards.isEmpty();
        dishGridPlaceholder.setText(dishItems.isEmpty() ? "No dishes loaded." : "No dishes match your filters.");
        dishGridPlaceholder.setVisible(empty);
        dishGridPlaceholder.setManaged(empty);
    }

    private Node createDishCard(Dishes dish) {
        StackPane imageShell = new StackPane();
        imageShell.getStyleClass().add("dish-image-shell");
        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            try {
                Image image = new Image(dish.imageUrl(), DISH_IMAGE_SIZE * 2, DISH_IMAGE_SIZE * 2, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(DISH_IMAGE_SIZE);
                imageView.setFitHeight(DISH_IMAGE_SIZE);
                imageView.setPreserveRatio(true);
                applyCoverCrop(imageView, image);
                double r = DISH_IMAGE_SIZE / 2.0;
                imageView.setClip(new Circle(r, r, r));
                imageShell.getChildren().add(imageView);
            } catch (Exception ignored) {}
        }

        Label name = new Label(dish.name());
        name.getStyleClass().add("dish-name");
        name.setWrapText(false); // Force truncation (ellipsis)
        name.setTextAlignment(TextAlignment.CENTER);
        name.setTooltip(new Tooltip(dish.name())); // Show full name on hover

        Label price = new Label(dish.price() == null ? "—" : String.format(Locale.ENGLISH, "₱ %,.2f", dish.price()));
        price.getStyleClass().add("dish-price");

        Label badge = new Label(dish.available() ? "Available" : "Unavailable");
        badge.getStyleClass().addAll("dish-badge", dish.available() ? "dish-badge-available" : "dish-badge-unavailable");

        VBox card = new VBox(name, price, badge);
        card.getStyleClass().add("dish-card");

        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(DISH_IMAGE_SIZE / 2.0, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        wrapper.getStyleClass().add("dish-card-wrapper");

        // Trigger Edit Mode when clicking the card
        wrapper.setOnMouseClicked(e -> openEditDishForm(dish));

        return wrapper;
    }

    /* ============================== ADD/EDIT DISH FORM LOGIC ============================== */

    private void setupAddDishPage() {
        addDishPreviewImage.setClip(new Circle(75, 75, 75));
        addDishPriceField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? c : null));
        addDishQuantityField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,5}") ? c : null));

        addDishStatusGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) addDishStatusGroup.selectToggle(old);
        });

        addCategoryNameField.setOnAction(e -> onAddCategory());
        resetAddDishForm();
    }

    private void resetAddDishForm() {
        currentEditingDish = null;
        if (addDishFormTitle != null) addDishFormTitle.setText("Add Menu Item");
        if (saveDishBtn != null) saveDishBtn.setText("Save Menu Item");

        addDishNameField.clear();
        addDishCategoryCombo.getSelectionModel().clearSelection();
        addDishPriceField.clear();
        addDishQuantityField.setText("0");
        addDishDescriptionField.clear();
        addDishAvailableBtn.setSelected(true);
        addDishImageUri = null;
        addDishPreviewImage.setImage(null);
        addCategoryNameField.clear();
        setAddDishError(null);
        setAddCategoryError(null);
    }

    private void openEditDishForm(Dishes dish) {
        resetAddDishForm();
        currentEditingDish = dish;

        if (addDishFormTitle != null) addDishFormTitle.setText("Edit Menu Item");
        if (saveDishBtn != null) saveDishBtn.setText("Update Menu Item");

        // Populate fields with existing data
        addDishNameField.setText(dish.name());
        addDishCategoryCombo.setValue(dish.category());
        if (dish.price() != null) {
            addDishPriceField.setText(dish.price().toPlainString());
        }

        // Failsafe in case your Dishes model doesn't store descriptions or quantities yet
        try {
            addDishDescriptionField.setText(dish.description() != null ? dish.description() : "");
            addDishQuantityField.setText(String.valueOf(dish.quantity()));
        } catch (Exception ignored) {}

        if (dish.available()) {
            addDishStatusGroup.selectToggle(addDishAvailableBtn);
        } else {
            addDishStatusGroup.selectToggle(addDishUnavailableBtn);
        }

        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            addDishImageUri = dish.imageUrl();
            try {
                Image image = new Image(addDishImageUri, 300.0, 300.0, true, true, true);
                addDishPreviewImage.setImage(image);
                applyCoverCrop(addDishPreviewImage, image);
            } catch (Exception ignored) {}
        }

        // Switch to the form view
        menuGridView.setVisible(false);
        menuGridView.setManaged(false);
        addDishView.setVisible(true);
        addDishView.setManaged(true);
    }

    @FXML private void onAddDishChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose menu item photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(menuGridView.getScene().getWindow());
        if (file != null) {
            addDishImageUri = file.toURI().toString();
            Image image = new Image(addDishImageUri, 300.0, 300.0, true, true, true);
            addDishPreviewImage.setImage(image);
            applyCoverCrop(addDishPreviewImage, image);
        }
    }

    @FXML private void onAddDishQtyMinus() { addDishQuantityField.setText(String.valueOf(Math.max(0, currentQuantity() - 1))); }
    @FXML private void onAddDishQtyPlus() { addDishQuantityField.setText(String.valueOf(Math.min(MAX_QUANTITY, currentQuantity() + 1))); }
    private int currentQuantity() { String t = addDishQuantityField.getText(); return (t == null || t.isBlank()) ? 0 : Integer.parseInt(t); }

    @FXML
    private void onAddDishSave() {
        setAddDishError(null);

        String name = addDishNameField.getText() == null ? "" : addDishNameField.getText().trim();
        String category = addDishCategoryCombo.getValue();
        String priceText = addDishPriceField.getText() == null ? "" : addDishPriceField.getText().trim();

        if (name.isEmpty() || category == null || priceText.isEmpty()) {
            setAddDishError("Please fill in all required fields.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
            if (price.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setAddDishError("Enter a valid selling price greater than zero.");
            return;
        }

        String desc = addDishDescriptionField.getText() == null ? "" : addDishDescriptionField.getText().trim();
        NewDishForm form = new NewDishForm(name, category, price, desc.isEmpty() ? null : desc,
                addDishAvailableBtn.isSelected(), addDishImageUri, currentQuantity());

        // BRANCH LOGIC: Check if we are updating or adding
        if (currentEditingDish == null) {
            // DIRECT DATABASE CALL: ADD
            if (productDAO.addDish(form)) {
                AlertUtil.showInfo("Success", form.name() + " was added to the menu.");
                loadMenuManagementData(); // Refresh UI
                showMenuGrid();           // Go back to table
            } else {
                setAddDishError("Failed to save the menu item to the database.");
            }
        } else {

            if (productDAO.updateDish(currentEditingDish, form)) {
                AlertUtil.showInfo("Success", form.name() + " was updated.");
                loadMenuManagementData(); // Refresh UI
                showMenuGrid();           // Go back to table
            } else {
                setAddDishError("Failed to update the menu item in the database.");
            }
        }
    }

    /* ============================== CATEGORY CREATOR ============================== */

    @FXML
    private void onAddCategory() {
        setAddCategoryError(null);
        String name = addCategoryNameField.getText() == null ? "" : addCategoryNameField.getText().trim();

        if (name.isEmpty() || name.length() > MAX_CATEGORY_LENGTH) {
            setAddCategoryError("Invalid category name.");
            return;
        }
        if (categoryNames.stream().anyMatch(e -> e.equalsIgnoreCase(name))) {
            setAddCategoryError("That category already exists.");
            return;
        }

        // DIRECT DATABASE CALL
        if (productDAO.addCategory(name)) {
            addCategoryNameField.clear();
            loadMenuManagementData(); // Refresh chips and dropdown
            addDishCategoryCombo.setValue(name);
        } else {
            setAddCategoryError("Database error creating category.");
        }
    }

    private void deleteCategory(String name) {
        if (dishItems.stream().anyMatch(d -> name.equalsIgnoreCase(d.category()))) {
            AlertUtil.showError("Category In Use", "\"" + name + "\" still has dishes assigned to it.");
            return;
        }
        if (AlertUtil.showYesNoConfirmation("Delete Category", "Delete \"" + name + "\"?", "This cannot be undone.")) {
            // DIRECT DATABASE CALL
            if (productDAO.deleteCategory(name)) {
                loadMenuManagementData();
            } else {
                AlertUtil.showError("Database Error", "Failed to delete category.");
            }
        }
    }

    private void rebuildCategoryList() {
        if (addCategoryList == null) return;
        List<Node> rows = new ArrayList<>();
        for (String name : categoryNames) {
            Region dot = new Region(); dot.getStyleClass().add("add-category-dot");
            Label label = new Label(name); label.getStyleClass().add("add-category-name"); label.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(label, Priority.ALWAYS);

            SVGPath trash = new SVGPath(); trash.setContent(Icons.TRASH_2); trash.getStyleClass().addAll("phosphor-icon", "icon-danger");
            setIconAndScale(trash, Icons.TRASH_2, 14.0);
            StackPane host = new StackPane(trash); host.setPrefSize(14.0, 14.0);

            Button delete = new Button(); delete.setGraphic(host); delete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            delete.setMnemonicParsing(false); delete.getStyleClass().add("add-category-delete");
            delete.setOnAction(e -> deleteCategory(name));

            HBox row = new HBox(10.0, dot, label, delete); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("add-category-row");
            rows.add(row);
        }
        addCategoryList.getChildren().setAll(rows);
        addCategoryCountLabel.setText(categoryNames.size() + (categoryNames.size() == 1 ? " category" : " categories"));
        addCategoryEmptyLabel.setVisible(categoryNames.isEmpty());
        addCategoryEmptyLabel.setManaged(categoryNames.isEmpty());
    }

    /* ============================== UTILS ============================== */

    private void applyCoverCrop(ImageView view, Image image) {
        Runnable crop = () -> {
            double w = image.getWidth(); double h = image.getHeight();
            if (w <= 0.0 || h <= 0.0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        };
        if (image.getProgress() >= 1.0) crop.run();
        else image.progressProperty().addListener((obs, o, n) -> { if (n.doubleValue() >= 1.0) crop.run(); });
    }

    private void setAddDishError(String msg) {
        boolean has = msg != null && !msg.isBlank();
        addDishErrorLabel.setText(has ? msg : "");
        addDishErrorLabel.setVisible(has); addDishErrorLabel.setManaged(has);
    }

    private void setAddCategoryError(String msg) {
        boolean has = msg != null && !msg.isBlank();
        addCategoryErrorLabel.setText(has ? msg : "");
        addCategoryErrorLabel.setVisible(has); addCategoryErrorLabel.setManaged(has);
    }
}
package com.kainanresto.controllers.main.admin.menu;

import com.kainanresto.controllers.main.admin.accounts.AdminAuthDialog;
import com.kainanresto.dao.ProductDAO;
import com.kainanresto.model.dish.Dishes;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.DishImageLoader;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.stage.Window;
import javafx.util.Duration;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class MenuController {

    public record NewDishForm(String name, String category, BigDecimal price, String description, boolean available, String imageUri, int quantity) {}

    @FXML private VBox menuGridView;
    @FXML private VBox addDishView;

    @FXML private Button viewArchiveBtn;
    @FXML private VBox archiveMenuView;
    @FXML private ToggleButton archiveDishesToggle, archiveCategoriesToggle;
    @FXML private ToggleGroup archiveSubNavGroup;
    @FXML private VBox archivedDishesContainer, archivedCategoriesContainer;

    @FXML private TableView<Dishes> archiveDishesTable;
    @FXML private TableColumn<Dishes, Dishes> archiveDishColName, archiveDishColCategory, archiveDishColPrice, archiveDishColQuantity, archiveDishColActions;

    @FXML private TableView<ProductDAO.ArchivedCategory> archiveCategoriesTable;
    @FXML private TableColumn<ProductDAO.ArchivedCategory, ProductDAO.ArchivedCategory> archiveCategoryColName, archiveCategoryColDishCount, archiveCategoryColActions;

    @FXML private HBox menuSearchBox;
    @FXML private TextField menuSearchField;
    @FXML private FlowPane menuCategoryChips;
    @FXML private FlowPane dishGrid;
    @FXML private Label dishGridPlaceholder;
    @FXML private SVGPath menuSearchIcon, addDishIcon;

    @FXML private Label addDishFormTitle;
    @FXML private Button saveDishBtn, deleteDishBtn, qtyMinusBtn, qtyPlusBtn;
    @FXML private TextField addDishNameField, addDishPriceField, addDishQuantityField, addCategoryNameField;
    @FXML private TextArea addDishDescriptionField;
    @FXML private ComboBox<String> addDishCategoryCombo;
    @FXML private ToggleGroup addDishStatusGroup;
    @FXML private ToggleButton addDishAvailableBtn, addDishUnavailableBtn;
    @FXML private ImageView addDishPreviewImage;
    @FXML private Label addDishErrorLabel, addCategoryErrorLabel, addCategoryCountLabel, addCategoryEmptyLabel;
    @FXML private SVGPath addDishUploadIcon, addCategoryAddIcon;
    @FXML private VBox addCategoryList;
    @FXML private ScrollPane addCategoryScroll;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final double DISH_IMAGE_SIZE = 144.66;
    private static final double PREVIEW_IMAGE_SIZE = 150.0;
    private static final int MAX_QUANTITY = 99999;
    private static final int MAX_CATEGORY_LENGTH = 40;

    private final ProductDAO productDAO = new ProductDAO();
    private final ToggleGroup categoryToggleGroup = new ToggleGroup();
    private final ObservableList<Dishes> dishItems = FXCollections.observableArrayList();
    private final List<String> categoryNames = new ArrayList<>();

    private final ObservableList<Dishes> archivedDishesMaster = FXCollections.observableArrayList();
    private final ObservableList<ProductDAO.ArchivedCategory> archivedCategoriesMaster = FXCollections.observableArrayList();
    private final Label archiveDishesEmptyLabel = new Label("No archived dishes.");
    private final Label archiveCategoriesEmptyLabel = new Label("No archived categories.");

    private String selectedCategory = null;
    private boolean rebuildingChips = false;
    private boolean loading = false;
    private String addDishImageUri;
    private File addDishImageFile;

    private Dishes currentEditingDish = null;
    private boolean isViewMode = false;

    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private static final int MAX_NAME_LENGTH = 60;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    private static final String NAME_ALLOWED = "[\\p{L}\\p{N} '&.,()\\-]*";
    private static final String CATEGORY_ALLOWED = "[\\p{L} '&\\-]*";

    @FXML
    public void initialize() {
        setupIcons();
        setupMenuPage();
        setupAddDishPage();
        setupArchivePage();
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

    public void refreshData(List<Dishes> latestDishes) {
        if (addDishView.isVisible()) return;
        List<Dishes> incoming = latestDishes == null ? List.of() : latestDishes;
        if (incoming.equals(dishItems)) return;

        new Thread(() -> {
            List<String> cats = productDAO.getAllCategories();
            Platform.runLater(() -> {
                setMenuCategories(cats);
                setDishes(incoming);
            });
        }).start();
    }

    private void showMenuGrid() {
        if (archiveMenuView != null) {
            archiveMenuView.setVisible(false);
            archiveMenuView.setManaged(false);
        }
        addDishView.setVisible(false);
        addDishView.setManaged(false);
        menuGridView.setVisible(true);
        menuGridView.setManaged(true);
    }

    @FXML
    private void onAddDish() {
        refreshCategories();
        resetAddDishForm();
        setFormEditable(true);
        menuGridView.setVisible(false);
        menuGridView.setManaged(false);
        addDishView.setVisible(true);
        addDishView.setManaged(true);
    }

    @FXML
    private void onAddDishCancel() {
        if (isViewMode) {
            showMenuGrid();
            return;
        }

        if (currentEditingDish == null && !hasUnsavedInput()) {
            showMenuGrid();
            return;
        }

        boolean discard = AlertUtil.showYesNoConfirmation(
                "Discard Changes",
                "Discard changes?",
                currentEditingDish == null
                        ? "The menu item you're adding will not be saved."
                        : "Your edits to \"" + currentEditingDish.name() + "\" will not be saved."
        );

        if (discard) {
            showMenuGrid();
        }
    }

    private boolean hasUnsavedInput() {
        return !normalize(addDishNameField.getText()).isEmpty()
                || addDishCategoryCombo.getValue() != null
                || !(addDishPriceField.getText() == null || addDishPriceField.getText().isBlank())
                || !(addDishDescriptionField.getText() == null || addDishDescriptionField.getText().isBlank())
                || !"0".equals(addDishQuantityField.getText())
                || addDishImageFile != null;
    }

    private void loadMenuManagementData() {
        loadMenuManagementData(null);
    }

    private void loadMenuManagementData(Runnable afterLoad) {
        loading = true;
        dishGrid.getChildren().clear();
        dishGridPlaceholder.setText("Loading dishes...");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        spinner.setMaxSize(48, 48);

        spinner.setStyle("-fx-progress-color: #6E625C;");

        dishGridPlaceholder.setMaxWidth(Double.MAX_VALUE);
        dishGridPlaceholder.setMaxHeight(Double.MAX_VALUE);
        dishGridPlaceholder.setAlignment(Pos.CENTER);

        dishGridPlaceholder.setGraphic(spinner);
        dishGridPlaceholder.setContentDisplay(ContentDisplay.TOP);
        dishGridPlaceholder.setGraphicTextGap(12);
        dishGridPlaceholder.setVisible(true);
        dishGridPlaceholder.setManaged(true);

        Thread t = new Thread(() -> {
            List<String> cats = null;
            List<Dishes> dishes = null;
            try {
                cats = productDAO.getAllCategories();
                dishes = productDAO.getAllDishes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<String> fc = cats;
            List<Dishes> fd = dishes;
            Platform.runLater(() -> {
                loading = false;
                dishGridPlaceholder.setGraphic(null);
                dishGridPlaceholder.setContentDisplay(ContentDisplay.LEFT);
                setMenuCategories(fc);
                setDishes(fd);
                if (afterLoad != null) afterLoad.run();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void refreshCategories() {
        setMenuCategories(productDAO.getAllCategories());
    }

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
        List<String> safeCategories = categories == null ? List.of() : categories;
        if (safeCategories.equals(categoryNames)) {
            return;
        }

        String previousChip = selectedCategory;
        String previousComboValue = addDishCategoryCombo != null ? addDishCategoryCombo.getValue() : null;

        categoryNames.clear();
        categoryNames.addAll(safeCategories);

        rebuildingChips = true;
        categoryToggleGroup.getToggles().clear();
        menuCategoryChips.getChildren().clear();

        ToggleButton all = createCategoryChip("All", null);
        ToggleButton toSelect = all;
        menuCategoryChips.getChildren().add(all);
        for (String c : categoryNames) {
            ToggleButton chip = createCategoryChip(c, c);
            menuCategoryChips.getChildren().add(chip);
            if (previousChip != null && c.equalsIgnoreCase(previousChip)) toSelect = chip;
        }
        rebuildingChips = false;

        selectedCategory = (String) toSelect.getUserData();
        toSelect.setSelected(true);
        applyMenuFilter();

        if (addDishCategoryCombo != null) {
            addDishCategoryCombo.getItems().setAll(categoryNames);
            if (previousComboValue != null && categoryNames.contains(previousComboValue)) {
                addDishCategoryCombo.setValue(previousComboValue);
            }
        }
        rebuildCategoryList();
    }

    private void setDishes(List<Dishes> dishes) {
        List<Dishes> incoming = dishes == null ? List.of() : dishes;
        if (incoming.equals(dishItems)) {
            return;
        }
        dishItems.setAll(incoming);
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
        if (loading) return;
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

        Node imageView = DishImageLoader.circleView(dish.imageUrl(), DISH_IMAGE_SIZE);
        if (imageView != null) imageShell.getChildren().add(imageView);

        Label name = new Label(dish.name());
        name.getStyleClass().add("dish-name");
        name.setWrapText(false);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setTooltip(new Tooltip(dish.name()));

        Label price = new Label(dish.price() == null ? "—" : String.format(Locale.ENGLISH, "₱ %,.2f", dish.price()));
        price.getStyleClass().add("dish-price");

        Label qtyLabel = new Label("Qty: " + dish.quantity());
        qtyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #6E625C;");

        boolean isAvailable = dish.quantity() > 0;
        Label statusBadge = new Label(isAvailable ? "Available" : "Unavailable");
        statusBadge.getStyleClass().addAll("dish-badge", isAvailable ? "dish-badge-available" : "dish-badge-unavailable");

        VBox badgeBox = new VBox(6, qtyLabel, statusBadge);
        badgeBox.setAlignment(Pos.CENTER);

        VBox card = new VBox(name, price, badgeBox);
        card.getStyleClass().add("dish-card");

        StackPane wrapper = new StackPane(card, imageShell);
        StackPane.setMargin(card, new Insets(DISH_IMAGE_SIZE / 2.0, 0, 0, 0));
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(imageShell, Pos.TOP_CENTER);
        wrapper.getStyleClass().add("dish-card-wrapper");

        wrapper.setOnMouseClicked(e -> openViewDishForm(dish));

        return wrapper;
    }

    private void setupAddDishPage() {
        addDishNameField.setTextFormatter(textFilter(NAME_ALLOWED, MAX_NAME_LENGTH));
        addDishDescriptionField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? c : null));
        addCategoryNameField.setTextFormatter(textFilter(CATEGORY_ALLOWED, MAX_CATEGORY_LENGTH));

        addDishPriceField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("(0|[1-9]\\d{0,4})?(\\.\\d{0,2})?") ? c : null));
        addDishQuantityField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,5}") ? c : null));

        addDishPriceField.focusedProperty().addListener((obs, was, is) -> {
            String t = addDishPriceField.getText();
            if (!is && t != null && !t.isEmpty() && !t.equals(".")) {
                addDishPriceField.setText(new BigDecimal(t).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
        });
        addDishQuantityField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && addDishQuantityField.getText().isBlank()) addDishQuantityField.setText("0");
        });

        addDishNameField.textProperty().addListener((o, a, b) -> setInvalid(addDishNameField, false));
        addCategoryNameField.textProperty().addListener((o, a, b) -> setInvalid(addCategoryNameField, false));
        addDishPriceField.textProperty().addListener((o, a, b) -> setInvalid(addDishPriceField.getParent(), false));
        addDishCategoryCombo.valueProperty().addListener((o, a, b) -> setInvalid(addDishCategoryCombo, false));

        Circle previewClip = new Circle(PREVIEW_IMAGE_SIZE / 2.0, PREVIEW_IMAGE_SIZE / 2.0, PREVIEW_IMAGE_SIZE / 2.0);
        addDishPreviewImage.setClip(previewClip);
        addDishPreviewImage.setPreserveRatio(true);
    }

    private void showPreview(Image image) {
        addDishPreviewImage.setViewport(null);
        addDishPreviewImage.setOpacity(0);
        addDishPreviewImage.setImage(image);

        Runnable finish = () -> {
            if (addDishPreviewImage.getImage() != image) return;
            if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                double side = Math.min(image.getWidth(), image.getHeight());
                addDishPreviewImage.setViewport(new Rectangle2D(
                        (image.getWidth() - side) / 2.0,
                        (image.getHeight() - side) / 2.0,
                        side, side));
            }
            FadeTransition fade = new FadeTransition(Duration.millis(180), addDishPreviewImage);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        };

        if (image.getProgress() >= 1.0) {
            if (Platform.isFxApplicationThread()) finish.run();
            else Platform.runLater(finish);
        } else {
            image.progressProperty().addListener((obs, was, now) -> {
                if (now.doubleValue() >= 1.0) {
                    Platform.runLater(finish);
                }
            });
        }
    }

    private void resetAddDishForm() {
        currentEditingDish = null;
        isViewMode = false;

        if (addDishFormTitle != null) addDishFormTitle.setText("Add Menu Item");

        if (saveDishBtn != null) {
            saveDishBtn.setText("Save Menu Item");
            saveDishBtn.getStyleClass().remove("btn-secondary");
            if (!saveDishBtn.getStyleClass().contains("add-save-btn")) {
                saveDishBtn.getStyleClass().add("add-save-btn");
            }
        }

        if (deleteDishBtn != null) {
            deleteDishBtn.setVisible(false);
            deleteDishBtn.setManaged(false);
        }

        addDishNameField.clear();
        addDishCategoryCombo.getSelectionModel().clearSelection();
        addDishPriceField.clear();
        addDishQuantityField.setText("0");
        addDishDescriptionField.clear();
        addDishAvailableBtn.setSelected(true);
        addDishImageUri = null;
        addDishImageFile = null;
        addDishPreviewImage.setViewport(null);
        addDishPreviewImage.setOpacity(1);
        addDishPreviewImage.setImage(null);
        addCategoryNameField.clear();
        setAddDishError(null);
        setAddCategoryError(null);
        if (addCategoryScroll != null) {
            addCategoryScroll.setVvalue(0.0);
        }
    }

    private void setFormEditable(boolean editable) {
        addDishNameField.setDisable(!editable);
        addDishCategoryCombo.setDisable(!editable);
        addDishPriceField.setDisable(!editable);
        addDishQuantityField.setDisable(!editable);
        addDishDescriptionField.setDisable(!editable);
        addDishAvailableBtn.setDisable(!editable);
        addDishUnavailableBtn.setDisable(!editable);

        if (qtyMinusBtn != null) qtyMinusBtn.setDisable(!editable);
        if (qtyPlusBtn != null) qtyPlusBtn.setDisable(!editable);
        if (addDishPreviewImage.getParent() != null) {
            addDishPreviewImage.getParent().setDisable(!editable);
        }
    }

    private void openViewDishForm(Dishes dish) {
        refreshCategories();
        resetAddDishForm();
        currentEditingDish = dish;
        isViewMode = true;

        if (addDishFormTitle != null) addDishFormTitle.setText("View Menu Item");

        if (saveDishBtn != null) {
            saveDishBtn.setText("Edit Menu Item");
            saveDishBtn.getStyleClass().remove("add-save-btn");
            if (!saveDishBtn.getStyleClass().contains("btn-secondary")) {
                saveDishBtn.getStyleClass().add("btn-secondary");
            }
        }

        if (deleteDishBtn != null) {
            deleteDishBtn.setVisible(true);
            deleteDishBtn.setManaged(true);
        }

        setFormEditable(false);

        addDishNameField.setText(dish.name());
        addDishCategoryCombo.setValue(dish.category());
        if (dish.price() != null) {
            addDishPriceField.setText(dish.price().toPlainString());
        }

        try {
            addDishDescriptionField.setText(dish.description() != null ? dish.description() : "");
            addDishQuantityField.setText(String.valueOf(dish.quantity()));
        } catch (Exception ignored) {}

        if (dish.quantity() > 0) {
            addDishStatusGroup.selectToggle(addDishAvailableBtn);
        } else {
            addDishStatusGroup.selectToggle(addDishUnavailableBtn);
        }

        if (dish.imageUrl() != null && !dish.imageUrl().isBlank()) {
            addDishImageUri = dish.imageUrl();
            Image image = DishImageLoader.load(addDishImageUri, PREVIEW_IMAGE_SIZE * 2);
            if (image != null) showPreview(image);
        }

        menuGridView.setVisible(false);
        menuGridView.setManaged(false);
        addDishView.setVisible(true);
        addDishView.setManaged(true);
    }

    @FXML private void onAddDishChoosePhoto() {
        if (isViewMode) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose menu item photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(menuGridView.getScene().getWindow());
        if (file != null) {
            addDishImageUri = file.getName();
            addDishImageFile = file;

            String previewUri = file.toURI().toString();
            Image image = new Image(previewUri, 300.0, 300.0, true, true, true);
            showPreview(image);
        }
    }

    @FXML private void onAddDishQtyMinus() { addDishQuantityField.setText(String.valueOf(Math.max(0, currentQuantity() - 1))); }
    @FXML private void onAddDishQtyPlus() { addDishQuantityField.setText(String.valueOf(Math.min(MAX_QUANTITY, currentQuantity() + 1))); }
    private int currentQuantity() { String t = addDishQuantityField.getText(); return (t == null || t.isBlank()) ? 0 : Integer.parseInt(t); }

    @FXML private void onAddDishSave() {

        if (isViewMode) {
            isViewMode = false;
            setFormEditable(true);
            if (addDishFormTitle != null) addDishFormTitle.setText("Edit Menu Item");
            if (saveDishBtn != null) {
                saveDishBtn.setText("Update Menu Item");
                saveDishBtn.getStyleClass().remove("btn-secondary");
                saveDishBtn.getStyleClass().add("add-save-btn");
            }
            if (deleteDishBtn != null) {
                deleteDishBtn.setVisible(false);
                deleteDishBtn.setManaged(false);
            }
            return;
        }

        setAddDishError(null);
        clearInvalidStates();

        String name = normalize(addDishNameField.getText());
        String category = addDishCategoryCombo.getValue();
        String priceText = addDishPriceField.getText() == null ? "" : addDishPriceField.getText().trim();

        if (name.isEmpty()) {
            failDish(addDishNameField, "Menu item name is required.");
            return;
        }
        if (name.length() < 2 || !hasLetter(name)) {
            failDish(addDishNameField, "Name must be at least 2 characters and contain letters.");
            return;
        }
        boolean duplicate = dishItems.stream().anyMatch(d ->
                d.name() != null && d.name().equalsIgnoreCase(name)
                        && (currentEditingDish == null || !Objects.equals(d.id(), currentEditingDish.id())));
        if (duplicate) {
            failDish(addDishNameField, "A menu item named \"" + name + "\" already exists.");
            return;
        }
        if (category == null) {
            failDish(addDishCategoryCombo, "Please select a category.");
            return;
        }
        if (priceText.isEmpty()) {
            failDish(addDishPriceField, "Selling price is required.");
            setInvalid(addDishPriceField.getParent(), true);
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
            if (price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            failDish(addDishPriceField, "Price must be greater than 0 and not more than ₱99,999.99.");
            setInvalid(addDishPriceField.getParent(), true);
            return;
        }

        boolean isEdit = currentEditingDish != null;
        boolean confirmed = AlertUtil.showYesNoConfirmation(
                isEdit ? "Update Dish" : "Add Dish",
                isEdit ? "Update this menu item?" : "Add this menu item?",
                isEdit
                        ? "Save your changes to \"" + name + "\"?"
                        : "\"" + name + "\" will be added to " + category + "."
        );
        if (!confirmed) {
            return;
        }

        boolean finalAvailable = currentQuantity() > 0;

        String imagePath = addDishImageUri;
        if (addDishImageFile != null) {
            imagePath = categoryFolder(category) + "/" + addDishImageFile.getName();
        } else if (imagePath != null
                && (imagePath.startsWith("http") || imagePath.startsWith("file:") || imagePath.startsWith("jar:"))) {
            String f = imagePath.substring(imagePath.lastIndexOf('/') + 1);
            f = java.net.URLDecoder.decode(f.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8);
            imagePath = categoryFolder(category) + "/" + f;
        }

        String desc = addDishDescriptionField.getText() == null ? "" : addDishDescriptionField.getText().trim();

        if (addDishImageFile != null) {
            ProductDAO.UploadResult result = productDAO.uploadImageResult(addDishImageFile, category);
            if (result == ProductDAO.UploadResult.FAILED) {
                AlertUtil.showError("Upload Failed",
                        "Could not save the image. Check the server connection and try again.");
                return;
            }
            DishImageLoader.clearCache();
            if (result == ProductDAO.UploadResult.LOCAL_ONLY) {
                AlertUtil.showInfo("Saved Locally Only",
                        "The server could not be reached, so the photo was saved on this computer only. "
                                + "Other computers will not see it until it is uploaded to the server.");
            }
        }

        NewDishForm form = new NewDishForm(name, category, price, desc.isEmpty() ? null : desc,
                finalAvailable, imagePath, currentQuantity());

        if (!isEdit) {
            if (productDAO.addDish(form)) {
                AlertUtil.showInfo("Success", form.name() + " was added to the menu.");
                loadMenuManagementData();
                showMenuGrid();
            } else {
                AlertUtil.showError("Save Failed", "Failed to save the menu item to the database.");
            }
        } else {
            if (productDAO.updateDish(currentEditingDish, form)) {
                AlertUtil.showInfo("Success", form.name() + " was updated.");
                loadMenuManagementData();
                showMenuGrid();
            } else {
                AlertUtil.showError("Update Failed", "Failed to update the menu item in the database.");
            }
        }
    }

    @FXML
    private void onDeleteDish() {
        if (currentEditingDish == null) return;

        if (AlertUtil.showYesNoConfirmation("Archive Dish", "Are you sure you want to archive \"" + currentEditingDish.name() + "\"?",
                "This dish will be moved to the archive and will no longer appear on the menu.")) {
            Window window = menuGridView.getScene().getWindow();
            if (!AdminAuthDialog.show(window, "Authorize Dish Archiving")) return;

            if (productDAO.archiveDish(currentEditingDish.id())) {
                AlertUtil.showInfo("Archived", currentEditingDish.name() + " has been moved to the archive.");
                loadMenuManagementData();
                showMenuGrid();
            } else {
                setAddDishError("Failed to archive the menu item.");
            }
        }
    }

    @FXML
    private void onAddCategory() {
        setAddCategoryError(null);
        setInvalid(addCategoryNameField, false);
        String name = normalize(addCategoryNameField.getText());

        String problem = null;
        if (name.isEmpty()) problem = "Enter a category name.";
        else if (name.length() < 2 || !hasLetter(name)) problem = "Category name must be at least 2 letters.";
        else if (categoryNames.stream().anyMatch(e -> e.equalsIgnoreCase(name))) problem = "That category already exists.";

        if (problem != null) {
            setInvalid(addCategoryNameField, true);
            setAddCategoryError(problem);
            return;
        }

        if (productDAO.addCategory(name)) {
            addCategoryNameField.clear();
            loadMenuManagementData(() -> addDishCategoryCombo.setValue(name));
        } else {
            setAddCategoryError("Database error creating category.");
        }
    }

    private void deleteCategory(String name) {
        if (dishItems.stream().anyMatch(d -> name.equalsIgnoreCase(d.category()))) {
            AlertUtil.showError("Category In Use", "\"" + name + "\" still has active dishes assigned to it.");
            return;
        }
        if (AlertUtil.showYesNoConfirmation("Archive Category", "Archive \"" + name + "\"?", "This category will be moved to the archive.")) {
            Window window = menuGridView.getScene().getWindow();
            if (!AdminAuthDialog.show(window, "Authorize Category Archiving")) return;

            if (productDAO.archiveCategory(name)) {
                loadMenuManagementData();
            } else {
                AlertUtil.showError("Database Error", "Failed to archive category.");
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

    private static TextFormatter<String> textFilter(String allowedRegex, int maxLen) {
        return new TextFormatter<>(c -> {
            String t = c.getControlNewText();
            boolean ok = t.length() <= maxLen
                    && t.matches(allowedRegex)
                    && !t.startsWith(" ")
                    && !t.contains("  ");
            return ok ? c : null;
        });
    }

    private static String categoryFolder(String category) {
        return category.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static boolean hasLetter(String s) {
        return s.codePoints().anyMatch(Character::isLetter);
    }

    private void setInvalid(Node node, boolean invalid) {
        if (node != null) node.pseudoClassStateChanged(INVALID, invalid);
    }

    private void clearInvalidStates() {
        setInvalid(addDishNameField, false);
        setInvalid(addDishCategoryCombo, false);
        setInvalid(addDishPriceField.getParent(), false);
        setInvalid(addCategoryNameField, false);
    }

    private void failDish(Node field, String message) {
        setInvalid(field, true);
        setAddDishError(message);
        field.requestFocus();
    }

    private void setupArchivePage() {
        archiveDishesEmptyLabel.getStyleClass().add("accounts-empty-label");
        archiveDishesTable.setPlaceholder(archiveDishesEmptyLabel);
        archiveDishesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        archiveDishesTable.setFixedCellSize(57.0);
        archiveDishesTable.setItems(archivedDishesMaster);
        archiveDishColName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveDishColCategory.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveDishColPrice.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveDishColQuantity.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveDishColActions.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));

        archiveDishColName.setCellFactory(col -> createDishTextCell(d -> d.name() == null ? "—" : d.name(), "accounts-name"));
        archiveDishColCategory.setCellFactory(col -> createDishTextCell(d -> d.category() == null ? "—" : d.category(), "accounts-username"));
        archiveDishColPrice.setCellFactory(col -> createDishTextCell(d -> d.price() == null ? "—" : "₱" + d.price().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), "accounts-meta"));
        archiveDishColQuantity.setCellFactory(col -> createDishTextCell(d -> String.valueOf(d.quantity()), "accounts-meta"));
        archiveDishColActions.setCellFactory(col -> archiveDishActionsCell());

        archiveCategoriesEmptyLabel.getStyleClass().add("accounts-empty-label");
        archiveCategoriesTable.setPlaceholder(archiveCategoriesEmptyLabel);
        archiveCategoriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        archiveCategoriesTable.setFixedCellSize(57.0);
        archiveCategoriesTable.setItems(archivedCategoriesMaster);

        archiveCategoryColName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveCategoryColDishCount.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        archiveCategoryColActions.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));

        archiveCategoryColName.setCellFactory(col -> createCategoryTextCell(c -> c.name() == null ? "—" : c.name(), "accounts-name"));
        archiveCategoryColDishCount.setCellFactory(col -> createCategoryTextCell(c -> String.valueOf(c.dishCount()), "accounts-meta"));
        archiveCategoryColActions.setCellFactory(col -> archiveCategoryActionsCell());
    }

    private TableCell<Dishes, Dishes> createDishTextCell(Function<Dishes, String> textExtractor, String styleClass) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add(styleClass);
            }
            @Override
            protected void updateItem(Dishes item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(textExtractor.apply(item));
                    setGraphic(label);
                }
            }
        };
    }

    private TableCell<ProductDAO.ArchivedCategory, ProductDAO.ArchivedCategory> createCategoryTextCell(Function<ProductDAO.ArchivedCategory, String> textExtractor, String styleClass) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add(styleClass);
            }
            @Override
            protected void updateItem(ProductDAO.ArchivedCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(textExtractor.apply(item));
                    setGraphic(label);
                }
            }
        };
    }

    private TableCell<Dishes, Dishes> archiveDishActionsCell() {
        return new TableCell<>() {
            private Dishes current;
            private final Button restoreBtn = menuIconButton(Icons.ARROWS_CLOCKWISE, "icon-brand", "Restore dish");
            private final Button deleteBtn = menuIconButton(Icons.TRASH_2, "icon-danger", "Permanently delete");
            private final HBox box = new HBox(8.0, restoreBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                restoreBtn.setOnAction(e -> {
                    if (current == null) return;
                    if (AlertUtil.showYesNoConfirmation("Restore Dish", "Restore \"" + current.name() + "\"?",
                            "This dish will be restored and made visible in the active menu.")) {
                        Window window = archiveDishesTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Dish Restoration")) return;
                        if (productDAO.restoreDish(current.id())) {
                            AlertUtil.showInfo("Restored", current.name() + " has been restored to the menu.");
                            loadArchiveData();
                            loadMenuManagementData();
                        } else {
                            AlertUtil.showError("Database Error", "Failed to restore dish.");
                        }
                    }
                });
                deleteBtn.setOnAction(e -> {
                    if (current == null) return;
                    if (AlertUtil.showYesNoConfirmation("Permanently Delete Dish", "Permanently delete \"" + current.name() + "\"?",
                            "This cannot be undone! The dish and its associated image will be permanently removed.")) {
                        Window window = archiveDishesTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Permanent Deletion")) return;
                        if (productDAO.permanentDeleteDish(current.id())) {
                            AlertUtil.showInfo("Permanently Deleted", current.name() + " has been deleted permanently.");
                            loadArchiveData();
                            loadMenuManagementData();
                        } else {
                            AlertUtil.showError("Database Error", "Failed to permanently delete dish.");
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Dishes item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    current = null;
                    setGraphic(null);
                } else {
                    current = item;
                    setGraphic(box);
                }
            }
        };
    }

    private TableCell<ProductDAO.ArchivedCategory, ProductDAO.ArchivedCategory> archiveCategoryActionsCell() {
        return new TableCell<>() {
            private ProductDAO.ArchivedCategory current;
            private final Button restoreBtn = menuIconButton(Icons.ARROWS_CLOCKWISE, "icon-brand", "Restore category");
            private final Button deleteBtn = menuIconButton(Icons.TRASH_2, "icon-danger", "Permanently delete");
            private final HBox box = new HBox(8.0, restoreBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                restoreBtn.setOnAction(e -> {
                    if (current == null) return;
                    if (AlertUtil.showYesNoConfirmation("Restore Category", "Restore \"" + current.name() + "\"?",
                            "This category will be restored to the active menu.")) {
                        Window window = archiveCategoriesTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Category Restoration")) return;
                        if (productDAO.restoreCategory(current.name())) {
                            AlertUtil.showInfo("Restored", current.name() + " category has been restored.");
                            loadArchiveData();
                            loadMenuManagementData();
                        } else {
                            AlertUtil.showError("Database Error", "Failed to restore category.");
                        }
                    }
                });
                deleteBtn.setOnAction(e -> {
                    if (current == null) return;
                    if (current.dishCount() > 0) {
                        AlertUtil.showError("Category In Use", "Cannot permanently delete \"" + current.name() + "\" because it still has " + current.dishCount() + " active dishes assigned to it.");
                        return;
                    }
                    if (AlertUtil.showYesNoConfirmation("Permanently Delete Category", "Permanently delete \"" + current.name() + "\"?",
                            "This cannot be undone! The category folder and all its contents will be permanently deleted.")) {
                        Window window = archiveCategoriesTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Permanent Deletion")) return;
                        if (productDAO.permanentDeleteCategory(current.name())) {
                            AlertUtil.showInfo("Permanently Deleted", current.name() + " has been deleted permanently.");
                            loadArchiveData();
                            loadMenuManagementData();
                        } else {
                            AlertUtil.showError("Database Error", "Failed to permanently delete category.");
                        }
                    }
                });
            }
            @Override
            protected void updateItem(ProductDAO.ArchivedCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    current = null;
                    setGraphic(null);
                } else {
                    current = item;
                    setGraphic(box);
                }
            }
        };
    }

    private Button menuIconButton(String pathData, String colorClass, String tooltip) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().addAll("phosphor-icon", colorClass);
        double scale = 14.0 / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (1.5 / scale) + ";");
        StackPane host = new StackPane(icon);
        host.setPrefSize(14.0, 14.0);
        Button b = new Button();
        b.setGraphic(host);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("accounts-icon-btn");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    @FXML
    public void onShowArchive() {
        menuGridView.setVisible(false);
        menuGridView.setManaged(false);
        addDishView.setVisible(false);
        addDishView.setManaged(false);
        archiveMenuView.setVisible(true);
        archiveMenuView.setManaged(true);
        loadArchiveData();
    }

    @FXML
    public void onBackToMenu() {
        showMenuGrid();
        loadMenuManagementData();
    }

    @FXML
    public void onToggleArchiveTab() {
        boolean showDishes = archiveDishesToggle.isSelected();
        archivedDishesContainer.setVisible(showDishes);
        archivedDishesContainer.setManaged(showDishes);
        archivedCategoriesContainer.setVisible(!showDishes);
        archivedCategoriesContainer.setManaged(!showDishes);
    }

    private void loadArchiveData() {
        new Thread(() -> {
            List<Dishes> archivedDishes = productDAO.getArchivedDishes();
            List<ProductDAO.ArchivedCategory> archivedCategories = productDAO.getArchivedCategories();
            Platform.runLater(() -> {
                archivedDishesMaster.setAll(archivedDishes);
                archivedCategoriesMaster.setAll(archivedCategories);
            });
        }).start();
    }
}
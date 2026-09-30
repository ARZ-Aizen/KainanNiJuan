package com.kainanresto.controllers.main.admin.settings;

import com.kainanresto.model.util.AppSettings;
import com.kainanresto.model.util.Icons;
import com.kainanresto.model.util.TimeSyncMode;
import com.kainanresto.util.AlertUtil;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SettingsController {

    /* ============================== VIEWS ============================== */
    @FXML private HBox settingsRoot;
    @FXML private ScrollPane settingsScroll;
    @FXML private VBox settingsContent;
    @FXML private VBox settingsGeneralCard, settingsPreferencesCard, settingsBackupCard, settingsSecurityCard;

    /* ============================== TAB BUTTONS & ICONS ============================== */
    @FXML private Button settingsTabGeneralBtn, settingsTabPreferencesBtn, settingsTabBackupBtn, settingsTabSecurityBtn;
    @FXML private SVGPath settingsGeneralIcon, settingsPreferencesIcon, settingsBackupTabIcon, settingsSecurityIcon;
    @FXML private SVGPath settingsLogoPlaceholderIcon, settingsLanguageChevron;
    @FXML private SVGPath settingsBackupBtnIcon, settingsExportBtnIcon, settingsRestoreBtnIcon;

    /* ============================== FORM CONTROLS ============================== */
    @FXML private ImageView settingsLogoImage;
    @FXML private TextField settingsNameField, settingsEmailField, settingsDateField, settingsTimeField;
    @FXML private ToggleGroup settingsSyncGroup;
    @FXML private ToggleButton settingsSyncManualBtn, settingsSyncAutoBtn;
    @FXML private HBox settingsLanguagePill;
    @FXML private Label settingsLanguageValueLabel, settingsErrorLabel;
    @FXML private ToggleButton settingsAutoBackupToggle, settingsRequirePinToggle;

    private static final String SETTINGS_TAB_ACTIVE = "settings-tab-active";
    private static final DateTimeFormatter SETTINGS_DATE_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d, yyyy").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter SETTINGS_TIME_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

    private FilterPill settingsLanguageFilter;
    private String settingsPendingLanguage;
    private String settingsLogoUri;
    private AppSettings settingsLoaded;

    @FXML
    public void initialize() {
        setupIcons();
        setupSettingsPage();
        loadSettingsData();
    }

    private void setupIcons() {
        setIconScale(settingsGeneralIcon, Icons.HOUSE, 18.0);
        setIconScale(settingsPreferencesIcon, Icons.GLOBE, 18.0);
        setIconScale(settingsBackupTabIcon, Icons.DATABASE, 18.0);
        setIconScale(settingsSecurityIcon, Icons.SHIELD, 18.0);
        setIconScale(settingsLogoPlaceholderIcon, Icons.IMAGE, 32.0);
        setIconScale(settingsLanguageChevron, Icons.CHEVRON_DOWN, 16.0);
        setIconScale(settingsBackupBtnIcon, Icons.DATABASE, 16.0);
        setIconScale(settingsExportBtnIcon, Icons.DOWNLOAD, 16.0);
        setIconScale(settingsRestoreBtnIcon, Icons.ROTATE_CCW, 16.0);
    }

    private void setIconScale(SVGPath icon, String content, double targetSize) {
        if (icon != null && content != null) {
            icon.setContent(content);
            double scale = targetSize / 24.0;
            icon.setScaleX(scale); icon.setScaleY(scale);
            icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
        }
    }

    private void loadSettingsData() {
        // TODO: Wire up SettingsDAO here to load the actual configuration
        // Example empty load:
        setSettingsLanguageOptions(List.of("English (US)", "Tagalog"));
        setSettings(null);
    }

    private void setupSettingsPage() {
        settingsLanguageFilter = new FilterPill(settingsLanguagePill, settingsLanguageValueLabel, () -> { });

        Rectangle clip = new Rectangle(78.0, 78.0);
        clip.setArcWidth(18.0); clip.setArcHeight(18.0);
        settingsLogoImage.setFitWidth(78.0); settingsLogoImage.setFitHeight(78.0);
        settingsLogoImage.setClip(clip);

        settingsSyncManualBtn.setSelected(true);
        settingsSyncGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) settingsSyncGroup.selectToggle(old);
            else updateSettingsTimeFieldsEnabled();
        });
        updateSettingsTimeFieldsEnabled();
        showSettingsLogo(null);
        setSettingsError(null);
    }

    private void updateSettingsTimeFieldsEnabled() {
        boolean manual = settingsSyncManualBtn.isSelected();
        settingsDateField.setDisable(!manual);
        settingsTimeField.setDisable(!manual);
    }

    /* ============================== LOGIC & VALIDATION ============================== */

    public void setSettingsLanguageOptions(List<String> options) {
        settingsLanguageFilter.setOptions(options);
        if (settingsPendingLanguage != null) {
            settingsLanguageFilter.setSelected(settingsPendingLanguage);
        }
    }

    public void setSettings(AppSettings settings) {
        settingsLoaded = settings;
        applySettingsToForm(settings);
    }

    private void applySettingsToForm(AppSettings s) {
        setSettingsError(null);
        if (s == null) {
            settingsNameField.clear(); settingsEmailField.clear();
            settingsDateField.clear(); settingsTimeField.clear();
            settingsSyncManualBtn.setSelected(true);
            settingsAutoBackupToggle.setSelected(false); settingsRequirePinToggle.setSelected(false);
            settingsPendingLanguage = null; settingsLogoUri = null;
            showSettingsLogo(null);
            return;
        }

        settingsNameField.setText(s.restaurantName() == null ? "" : s.restaurantName());
        settingsEmailField.setText(s.contactEmail() == null ? "" : s.contactEmail());
        settingsLogoUri = s.logoUri();
        showSettingsLogo(settingsLogoUri);

        if (s.timeSyncMode() == TimeSyncMode.AUTO) settingsSyncAutoBtn.setSelected(true);
        else settingsSyncManualBtn.setSelected(true);

        settingsDateField.setText(s.systemDate() == null ? "" : s.systemDate().format(SETTINGS_DATE_FMT));
        settingsTimeField.setText(s.systemTime() == null ? "" : s.systemTime().format(SETTINGS_TIME_FMT));

        settingsPendingLanguage = s.language();
        if (settingsPendingLanguage != null) settingsLanguageFilter.setSelected(settingsPendingLanguage);

        settingsAutoBackupToggle.setSelected(s.autoBackupEnabled());
        settingsRequirePinToggle.setSelected(s.requirePinForSensitiveActions());
    }

    private AppSettings readSettingsForm() {
        String name = settingsNameField.getText() == null ? "" : settingsNameField.getText().trim();
        if (name.isEmpty()) { setSettingsError("Restaurant name is required."); return null; }
        String email = settingsEmailField.getText() == null ? "" : settingsEmailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { setSettingsError("Enter a valid contact email."); return null; }

        TimeSyncMode mode = settingsSyncAutoBtn.isSelected() ? TimeSyncMode.AUTO : TimeSyncMode.MANUAL;
        LocalDate date = null; LocalTime time = null;

        if (mode == TimeSyncMode.MANUAL) {
            try { date = LocalDate.parse(normalizeSpaces(settingsDateField.getText()), SETTINGS_DATE_FMT); }
            catch (DateTimeParseException ex) { setSettingsError("System date must be in the form Month D, YYYY."); return null; }
            try { time = LocalTime.parse(normalizeSpaces(settingsTimeField.getText()), SETTINGS_TIME_FMT); }
            catch (DateTimeParseException ex) { setSettingsError("System time must be in the form H:MM AM/PM."); return null; }
        }

        return new AppSettings(name, email.isEmpty() ? null : email, settingsLogoUri, mode, date, time,
                settingsLanguageFilter.getSelected(), settingsAutoBackupToggle.isSelected(), settingsRequirePinToggle.isSelected());
    }

    private static String normalizeSpaces(String s) {
        return s == null ? "" : s.replace('\u202F', ' ').replace('\u00A0', ' ').trim();
    }

    private void showSettingsLogo(String uri) {
        boolean shown = false;
        if (uri != null && !uri.isBlank()) {
            try {
                Image image = new Image(uri, 156.0, 156.0, true, true, true);
                settingsLogoImage.setImage(image);
                applyCoverCrop(settingsLogoImage, image);
                shown = true;
            } catch (IllegalArgumentException ex) { settingsLogoImage.setImage(null); }
        } else {
            settingsLogoImage.setImage(null);
        }
        settingsLogoImage.setVisible(shown); settingsLogoImage.setManaged(shown);
        settingsLogoPlaceholderIcon.setVisible(!shown);
    }

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

    private void setSettingsError(String message) {
        boolean has = message != null && !message.isBlank();
        settingsErrorLabel.setText(has ? message : "");
        settingsErrorLabel.setVisible(has); settingsErrorLabel.setManaged(has);
    }

    /* ============================== ACTIONS ============================== */

    @FXML private void onSettingsSave() {
        setSettingsError(null);
        AppSettings values = readSettingsForm();
        if (values != null) {
            // TODO: Wire up DAO to save configuration here
            AlertUtil.showInfo("Settings Saved", "System configuration updated successfully.");
        }
    }

    @FXML private void onSettingsCancel() { applySettingsToForm(settingsLoaded); }

    @FXML private void onSettingsChangeLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose restaurant logo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(settingsRoot.getScene().getWindow());
        if (file != null) {
            settingsLogoUri = file.toURI().toString();
            showSettingsLogo(settingsLogoUri);
        }
    }

    @FXML private void onSettingsBackupDatabase() { AlertUtil.showInfo("Backup", "Database backup requested."); }
    @FXML private void onSettingsExportData() { AlertUtil.showInfo("Export", "CSV export requested."); }
    @FXML private void onSettingsRestorePoint() { AlertUtil.showInfo("Restore", "Restore point dialog requested."); }

    /* ============================== TAB NAVIGATION ============================== */

    @FXML private void onSettingsTabGeneral() { selectSettingsTab(settingsTabGeneralBtn, settingsGeneralCard); }
    @FXML private void onSettingsTabPreferences() { selectSettingsTab(settingsTabPreferencesBtn, settingsPreferencesCard); }
    @FXML private void onSettingsTabBackup() { selectSettingsTab(settingsTabBackupBtn, settingsBackupCard); }
    @FXML private void onSettingsTabSecurity() { selectSettingsTab(settingsTabSecurityBtn, settingsSecurityCard); }

    private void selectSettingsTab(Button active, Node section) {
        for (Button btn : new Button[]{settingsTabGeneralBtn, settingsTabPreferencesBtn, settingsTabBackupBtn, settingsTabSecurityBtn}) {
            btn.getStyleClass().remove(SETTINGS_TAB_ACTIVE);
        }
        if (!active.getStyleClass().contains(SETTINGS_TAB_ACTIVE)) active.getStyleClass().add(SETTINGS_TAB_ACTIVE);
        scrollSettingsTo(section);
    }

    private void scrollSettingsTo(Node section) {
        double contentHeight = settingsContent.getBoundsInLocal().getHeight();
        double viewportHeight = settingsScroll.getViewportBounds().getHeight();
        double scrollable = contentHeight - viewportHeight;
        if (scrollable <= 0.0) return;
        double y = section.getBoundsInParent().getMinY();
        settingsScroll.setVvalue(Math.max(0.0, Math.min(1.0, y / scrollable)));
    }

    /* ============================== INNER CLASSES ============================== */

    private static final class FilterPill {
        private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
        private final Label valueLabel;
        private final Runnable onChange;
        private final ContextMenu menu = new ContextMenu();
        private List<String> options = List.of();
        private String selected;

        private FilterPill(HBox pill, Label valueLabel, Runnable onChange) {
            this.valueLabel = valueLabel;
            this.onChange = onChange;
            menu.getStyleClass().add("filter-menu");
            menu.showingProperty().addListener((obs, was, showing) -> pill.pseudoClassStateChanged(OPEN, showing));
            pill.setOnMouseClicked(e -> {
                if (options.isEmpty()) return;
                if (menu.isShowing()) menu.hide(); else menu.show(pill, Side.BOTTOM, 0.0, 4.0);
            });
        }

        private void setOptions(List<String> newOptions) {
            options = newOptions == null ? List.of() : List.copyOf(newOptions);
            select(options.isEmpty() ? null : options.get(0), false);
        }

        private void setSelected(String value) {
            if (value != null && options.contains(value)) select(value, false);
        }

        private String getSelected() { return selected; }

        private void select(String value, boolean notify) {
            selected = value;
            valueLabel.setText(value == null ? "—" : value);
            List<MenuItem> items = new ArrayList<>();
            for (String option : options) {
                MenuItem item = new MenuItem(option);
                item.setMnemonicParsing(false);
                if (option.equals(selected)) item.getStyleClass().add("filter-option-selected");
                item.setOnAction(e -> { if (!option.equals(selected)) select(option, true); });
                items.add(item);
            }
            menu.getItems().setAll(items);
            if (notify) onChange.run();
        }
    }
}
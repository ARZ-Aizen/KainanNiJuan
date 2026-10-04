package com.kainanresto.controllers.main.admin.settings;

import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Properties;
import com.kainanresto.dao.OrderDAO;
import com.kainanresto.model.transac.Transaction;
import java.time.LocalDateTime;
import com.kainanresto.dao.ProductDAO;
import com.kainanresto.model.dish.Dishes;


public class SettingsController {

    /* ============================== VIEWS ============================== */
    @FXML private HBox settingsRoot;
    @FXML private ScrollPane settingsScroll;
    @FXML private VBox settingsContent;
    @FXML private VBox settingsTimeCard, settingsBackupCard, settingsSecurityCard;

    /* ============================== TAB BUTTONS & ICONS ============================== */
    @FXML private Button settingsTabTimeBtn, settingsTabBackupBtn, settingsTabSecurityBtn;
    @FXML private SVGPath settingsGeneralIcon, settingsBackupTabIcon, settingsSecurityIcon;
    @FXML private SVGPath settingsBackupBtnIcon;

    /* ============================== FORM CONTROLS ============================== */
    @FXML private TextField settingsDateField, settingsTimeField;
    @FXML private ToggleGroup settingsSyncGroup;
    @FXML private ToggleButton settingsSyncManualBtn, settingsSyncAutoBtn;
    @FXML private ToggleButton settingsRequirePinToggle;
    @FXML private Label settingsErrorLabel;

    private static final String SETTINGS_TAB_ACTIVE = "settings-tab-active";
    private static final DateTimeFormatter SETTINGS_DATE_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d, yyyy").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter SETTINGS_TIME_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

    // File to store app settings
    private static final String CONFIG_FILE = "config.properties";
    private Properties appProperties = new Properties();

    @FXML private SVGPath settingsExportInvBtnIcon;

    @FXML
    public void initialize() {
        setupIcons();
        setupSettingsPage();
        loadSettingsData();
    }

    private void setupIcons() {
        setIconScale(settingsGeneralIcon, Icons.CLOCK, 18.0);
        setIconScale(settingsBackupTabIcon, Icons.DATABASE, 18.0);
        setIconScale(settingsSecurityIcon, Icons.SHIELD, 18.0);
        setIconScale(settingsBackupBtnIcon, Icons.DOWNLOAD, 16.0);
        setIconScale(settingsExportInvBtnIcon, Icons.CLIPBOARD_TEXT, 16.0);
    }

    private void setIconScale(SVGPath icon, String content, double targetSize) {
        if (icon != null && content != null) {
            icon.setContent(content);
            double scale = targetSize / 24.0;
            icon.setScaleX(scale); icon.setScaleY(scale);
            icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
        }
    }

    private void setupSettingsPage() {
        settingsSyncManualBtn.setSelected(true);
        settingsSyncGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) settingsSyncGroup.selectToggle(old);
            else updateSettingsTimeFieldsEnabled();
        });
        updateSettingsTimeFieldsEnabled();
        setSettingsError(null);
    }

    private void updateSettingsTimeFieldsEnabled() {
        boolean auto = settingsSyncAutoBtn.isSelected();
        settingsDateField.setDisable(auto);
        settingsTimeField.setDisable(auto);

        if (auto) {
            settingsDateField.setText(LocalDate.now().format(SETTINGS_DATE_FMT));
            settingsTimeField.setText(LocalTime.now().format(SETTINGS_TIME_FMT));
        }
    }

    /* ============================== LOGIC & VALIDATION ============================== */

    private void loadSettingsData() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            appProperties.load(input);
        } catch (IOException ex) {
            // File might not exist yet, ignore
        }

        // Apply Time Settings
        boolean isAuto = Boolean.parseBoolean(appProperties.getProperty("timeSyncAuto", "true"));
        if (isAuto) {
            settingsSyncAutoBtn.setSelected(true);
        } else {
            settingsSyncManualBtn.setSelected(true);
            settingsDateField.setText(appProperties.getProperty("manualDate", LocalDate.now().format(SETTINGS_DATE_FMT)));
            settingsTimeField.setText(appProperties.getProperty("manualTime", LocalTime.now().format(SETTINGS_TIME_FMT)));
        }

        // Apply Security Settings
        boolean requirePin = Boolean.parseBoolean(appProperties.getProperty("requirePinForVoid", "false"));
        settingsRequirePinToggle.setSelected(requirePin);
    }

    private void setSettingsError(String message) {
        boolean has = message != null && !message.isBlank();
        settingsErrorLabel.setText(has ? message : "");
        settingsErrorLabel.setVisible(has); settingsErrorLabel.setManaged(has);
    }

    /* ============================== ACTIONS ============================== */

    @FXML
    private void onSettingsSave() {
        setSettingsError(null);

        boolean isAuto = settingsSyncAutoBtn.isSelected();

        if (!isAuto) {
            try {
                LocalDate.parse(settingsDateField.getText().trim(), SETTINGS_DATE_FMT);
            } catch (DateTimeParseException ex) {
                setSettingsError("System date must be in the form Month D, YYYY."); return;
            }
            try {
                LocalTime.parse(settingsTimeField.getText().trim(), SETTINGS_TIME_FMT);
            } catch (DateTimeParseException ex) {
                setSettingsError("System time must be in the form H:MM AM/PM."); return;
            }
        }

        // Save to properties
        appProperties.setProperty("timeSyncAuto", String.valueOf(isAuto));
        appProperties.setProperty("manualDate", settingsDateField.getText().trim());
        appProperties.setProperty("manualTime", settingsTimeField.getText().trim());
        appProperties.setProperty("requirePinForVoid", String.valueOf(settingsRequirePinToggle.isSelected()));

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            appProperties.store(output, "Kainan Ni Juan System Settings");
            AlertUtil.showInfo("Settings Saved", "System configuration updated successfully.");
        } catch (IOException io) {
            setSettingsError("Error saving settings file.");
            io.printStackTrace();
        }
    }

    @FXML
    private void onSettingsCancel() {
        loadSettingsData();
    }

    /* ============================== BACKUP & RESTORE ============================== */

    @FXML
    private void onSettingsBackupDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Sales History to Excel");
        chooser.setInitialFileName("Sales_Backup_" + LocalDate.now().toString() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel/CSV File", "*.csv"));

        File file = chooser.showSaveDialog(settingsRoot.getScene().getWindow());
        if (file != null) {
            Thread t = new Thread(() -> {
                try {
                    OrderDAO orderDAO = new OrderDAO();
                    java.util.List<Transaction> sales = orderDAO.getTransactions(
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            LocalDateTime.now().plusDays(1)
                    );

                    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                        writer.println("Order ID,Created At,Staff,Total Amount,Order Status,Payment Status");

                        for (Transaction tx : sales) {
                            String date = tx.lastActivityAt() != null ?
                                    tx.lastActivityAt().format(DateTimeFormatter.ofPattern("MMM d yyyy h:mm a", Locale.ENGLISH)) : "";

                            // Using quotes around strings ensures Excel doesn't break if a name has a comma in it
                            writer.printf("\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\"%n",
                                    tx.orderId(),
                                    date,
                                    tx.staffName(),
                                    tx.total(),
                                    tx.status(),
                                    tx.paymentStatus()
                            );
                        }
                    }

                    Platform.runLater(() -> {
                        AlertUtil.showInfo("Export Successful", "Sales history successfully exported to:\n" + file.getAbsolutePath());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtil.showError("Export Error", "Could not create the Excel file:\n" + e.getMessage()));
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /* ============================== TAB NAVIGATION ============================== */

    @FXML private void onSettingsTabTime() { selectSettingsTab(settingsTabTimeBtn, settingsTimeCard); }
    @FXML private void onSettingsTabBackup() { selectSettingsTab(settingsTabBackupBtn, settingsBackupCard); }
    @FXML private void onSettingsTabSecurity() { selectSettingsTab(settingsTabSecurityBtn, settingsSecurityCard); }

    private void selectSettingsTab(Button active, Node section) {
        for (Button btn : new Button[]{settingsTabTimeBtn, settingsTabBackupBtn, settingsTabSecurityBtn}) {
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
}
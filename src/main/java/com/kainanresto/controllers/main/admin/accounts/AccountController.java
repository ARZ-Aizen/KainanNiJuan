package com.kainanresto.controllers.main.admin.accounts;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.AccountRow;
import com.kainanresto.model.account.AccountStatus;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AccountController {

    /* ============================== VIEWS ============================== */
    @FXML private VBox accountsView;
    @FXML private ScrollPane editAccountView;

    /* ============================== ACCOUNTS LIST ============================== */
    @FXML private HBox accountsSearchBox, accountsRolePill, accountsStatusPill, accountsLastLoginPill;
    @FXML private SVGPath accountsSearchIcon;
    @FXML private TextField accountsSearchField;
    @FXML private Label accountsRoleValueLabel, accountsStatusValueLabel, accountsLastLoginValueLabel;

    @FXML private TableView<AccountRow> accountsTable;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColName, accountsColUsername, accountsColRole;
    @FXML private TableColumn<AccountRow, AccountRow> accountsColStatus, accountsColLastLogin, accountsColCreated, accountsColActions;

    /* ============================== EDIT ACCOUNT FORM ============================== */
    @FXML private TextField editAccountNameField, editAccountUsernameField, editAccountVisiblePasswordField;
    @FXML private PasswordField editAccountPasswordField;
    @FXML private ComboBox<AccountRole> editAccountRoleCombo;
    @FXML private ComboBox<AccountStatus> editAccountStatusCombo;
    @FXML private Label editAccountCreatedLabel, editAccountLastLoginLabel, editAccountErrorLabel;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final String ACCOUNTS_ALL_ROLES = "All Roles";
    private static final String ACCOUNTS_ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ACCOUNTS_TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ACCOUNTS_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter EDIT_ACCOUNT_DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private enum AccountsLastLoginFilter {
        ANYTIME("Anytime"), TODAY("Today"), LAST_7_DAYS("Last 7 days"), LAST_30_DAYS("Last 30 days"), NEVER("Never");
        private final String label;
        AccountsLastLoginFilter(String label) { this.label = label; }
        String label() { return label; }
    }

    private final Label accountsEmptyLabel = new Label("No accounts loaded.");
    private final ObservableList<AccountRow> accountsMaster = FXCollections.observableArrayList();
    private FilteredList<AccountRow> accountsFiltered;

    private AccountRole accountsRoleFilter = null;
    private AccountStatus accountsStatusFilter = null;
    private AccountsLastLoginFilter accountsLastLoginFilter = AccountsLastLoginFilter.ANYTIME;

    private AccountRow editingAccount;
    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        setupIcons();
        setupAccountsPage();
        setupEditAccountPage();
        loadAccountManagementData();
        showAccountsList();
    }

    private void setupIcons() {
        if (accountsSearchIcon != null) {
            accountsSearchIcon.setContent(Icons.SEARCH);
            setIconScale(accountsSearchIcon, 14.0);
        }
        if (accountsView != null) {
            for (Node n : accountsView.lookupAll(".accounts-pill-chevron")) {
                if (n instanceof SVGPath p) {
                    p.setContent(Icons.CHEVRON_DOWN);
                    setIconScale(p, 12.0);
                }
            }
        }
    }

    private void setIconScale(SVGPath icon, double targetSize) {
        double scale = targetSize / 24.0;
        icon.setScaleX(scale); icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
    }

    /* ============================== VIEW TOGGLING ============================== */

    private void showAccountsList() {
        editAccountView.setVisible(false); editAccountView.setManaged(false);
        accountsView.setVisible(true); accountsView.setManaged(true);
    }

    private void showEditAccountForm() {
        accountsView.setVisible(false); accountsView.setManaged(false);
        editAccountView.setVisible(true); editAccountView.setManaged(true);
    }

    /* ============================== DATA LOADING ============================== */

    private void loadAccountManagementData() {
        accountsMaster.setAll(userDAO.getAllAccounts());
        applyAccountsFilter();
    }

    /* ============================== ACCOUNTS LIST LOGIC ============================== */

    private void setupAccountsPage() {
        accountsSearchField.focusedProperty().addListener((obs, was, is) -> accountsSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        accountsSearchField.textProperty().addListener((obs, oldText, newText) -> applyAccountsFilter());

        LinkedHashMap<String, AccountRole> roleOptions = new LinkedHashMap<>();
        roleOptions.put(ACCOUNTS_ALL_ROLES, null);
        for (AccountRole r : AccountRole.values()) roleOptions.put(r.getDisplayName(), r);
        wireAccountsPill(accountsRolePill, accountsRoleValueLabel, roleOptions, v -> { accountsRoleFilter = v; applyAccountsFilter(); });

        LinkedHashMap<String, AccountStatus> statusOptions = new LinkedHashMap<>();
        statusOptions.put(ACCOUNTS_ALL_STATUSES, null);
        for (AccountStatus s : AccountStatus.values()) statusOptions.put(s.getDisplayName(), s);
        wireAccountsPill(accountsStatusPill, accountsStatusValueLabel, statusOptions, v -> { accountsStatusFilter = v; applyAccountsFilter(); });

        LinkedHashMap<String, AccountsLastLoginFilter> loginOptions = new LinkedHashMap<>();
        for (AccountsLastLoginFilter f : AccountsLastLoginFilter.values()) loginOptions.put(f.label(), f);
        wireAccountsPill(accountsLastLoginPill, accountsLastLoginValueLabel, loginOptions, v -> { accountsLastLoginFilter = v; applyAccountsFilter(); });

        accountsEmptyLabel.getStyleClass().add("accounts-empty-label");
        accountsTable.setPlaceholder(accountsEmptyLabel);
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        accountsTable.setFixedCellSize(57.0);
        accountsFiltered = new FilteredList<>(accountsMaster, a -> true);
        accountsTable.setItems(accountsFiltered);

        bindAccountColumn(accountsColName, () -> accountTextCell(a -> a.fullName() == null || a.fullName().isBlank() ? "—" : a.fullName(), "accounts-name"));
        bindAccountColumn(accountsColUsername, () -> accountTextCell(a -> (a.username() == null || a.username().isBlank()) ? "—" : "@" + a.username(), "accounts-username"));
        bindAccountColumn(accountsColRole, this::accountRoleCell);
        bindAccountColumn(accountsColStatus, this::accountStatusCell);
        bindAccountColumn(accountsColLastLogin, () -> accountTextCell(a -> formatAccountLastLogin(a.lastLogin()), "accounts-meta"));
        bindAccountColumn(accountsColCreated, () -> accountTextCell(a -> a.createdDate() == null ? "—" : a.createdDate().format(ACCOUNTS_DATE_FMT), "accounts-meta"));
        bindAccountColumn(accountsColActions, this::accountActionsCell);

        updateAccountsEmptyState();
    }

    private <T> void wireAccountsPill(HBox pill, Label valueLabel, LinkedHashMap<String, T> options, Consumer<T> onPick) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-menu");
        options.forEach((label, value) -> {
            MenuItem item = new MenuItem(label);
            item.setMnemonicParsing(false);
            item.setOnAction(e -> { valueLabel.setText(label); onPick.accept(value); });
            menu.getItems().add(item);
        });
        pill.setOnMouseClicked(e -> { if (menu.isShowing()) menu.hide(); else menu.show(pill, Side.BOTTOM, 0.0, 4.0); });
    }

    @FXML
    private void onAccountsClearFilters() {
        accountsRoleFilter = null; accountsStatusFilter = null; accountsLastLoginFilter = AccountsLastLoginFilter.ANYTIME;
        accountsRoleValueLabel.setText(ACCOUNTS_ALL_ROLES);
        accountsStatusValueLabel.setText(ACCOUNTS_ALL_STATUSES);
        accountsLastLoginValueLabel.setText(AccountsLastLoginFilter.ANYTIME.label());
        accountsSearchField.clear();
        applyAccountsFilter();
    }

    private void applyAccountsFilter() {
        if (accountsFiltered == null) return;
        String q = accountsSearchField.getText() == null ? "" : accountsSearchField.getText().trim().toLowerCase(Locale.ROOT);

        accountsFiltered.setPredicate(a ->
                (accountsRoleFilter == null || a.role() == accountsRoleFilter)
                        && (accountsStatusFilter == null || a.status() == accountsStatusFilter)
                        && matchesAccountsLastLogin(a)
                        && (q.isEmpty() || (a.fullName() != null && a.fullName().toLowerCase(Locale.ROOT).contains(q)) || (a.username() != null && a.username().toLowerCase(Locale.ROOT).contains(q))));
        updateAccountsEmptyState();
    }

    private boolean matchesAccountsLastLogin(AccountRow a) {
        LocalDateTime ll = a.lastLogin(); LocalDate today = LocalDate.now();
        return switch (accountsLastLoginFilter) {
            case ANYTIME      -> true;
            case NEVER        -> ll == null;
            case TODAY        -> ll != null && ll.toLocalDate().equals(today);
            case LAST_7_DAYS  -> ll != null && !ll.isBefore(today.minusDays(6).atStartOfDay());
            case LAST_30_DAYS -> ll != null && !ll.isBefore(today.minusDays(29).atStartOfDay());
        };
    }

    private void updateAccountsEmptyState() {
        accountsEmptyLabel.setText(accountsMaster.isEmpty() ? "No accounts loaded." : "No accounts match your filters.");
    }

    /* ============================== ACTIONS LOGIC ============================== */

    private void editAccount(AccountRow selectedAccount) {
        if (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username())) {
            AlertUtil.showError("Action Denied", "The primary administrator account cannot be edited.");
            return;
        }

        editingAccount = selectedAccount;
        editAccountNameField.setText(selectedAccount.fullName() == null ? "" : selectedAccount.fullName());
        editAccountUsernameField.setText(selectedAccount.username() == null ? "" : selectedAccount.username());
        editAccountPasswordField.setText(""); editAccountVisiblePasswordField.setText("");
        editAccountPasswordField.setVisible(true); editAccountVisiblePasswordField.setVisible(false);
        editAccountRoleCombo.setValue(selectedAccount.role());
        editAccountStatusCombo.setValue(selectedAccount.status());
        editAccountCreatedLabel.setText(selectedAccount.createdDate() == null ? "—" : selectedAccount.createdDate().format(EDIT_ACCOUNT_DATE_FMT));
        editAccountLastLoginLabel.setText(formatAccountLastLogin(selectedAccount.lastLogin()));

        setEditAccountError(null);
        showEditAccountForm();
    }

    private void deleteAccount(AccountRow selectedAccount) {
        if (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username())) {
            AlertUtil.showError("Action Denied", "The primary administrator account cannot be deleted.");
            return;
        }
        if (AlertUtil.showYesNoConfirmation("Delete Account", "Delete account: " + selectedAccount.username() + "?", "This action cannot be undone.")) {
            if (userDAO.permanentDeleteUser((int) selectedAccount.id()) == UserDAO.OperationResult.SUCCESS) {
                AlertUtil.showInfo("Success", "Account has been permanently deleted.");
                loadAccountManagementData();
            } else {
                AlertUtil.showError("Database Error", "Failed to delete the account from the database.");
            }
        }
    }

    /* ============================== EDIT ACCOUNT FORM ============================== */

    private void setupEditAccountPage() {
        List<AccountRole> allowableRoles = new ArrayList<>();
        for (AccountRole r : AccountRole.values()) if (r != AccountRole.ADMIN) allowableRoles.add(r);

        editAccountRoleCombo.getItems().setAll(allowableRoles);
        editAccountRoleCombo.setConverter(new StringConverter<>() {
            @Override public String toString(AccountRole r) { return r == null ? "" : r.getDisplayName(); }
            @Override public AccountRole fromString(String s) { return null; }
        });

        editAccountStatusCombo.getItems().setAll(AccountStatus.values());
        editAccountStatusCombo.setConverter(new StringConverter<>() {
            @Override public String toString(AccountStatus s) { return s == null ? "" : s.getDisplayName(); }
            @Override public AccountStatus fromString(String s) { return null; }
        });

        editAccountVisiblePasswordField.managedProperty().bind(editAccountVisiblePasswordField.visibleProperty());
        editAccountPasswordField.managedProperty().bind(editAccountPasswordField.visibleProperty());

        editAccountNameField.setOnAction(e -> editAccountUsernameField.requestFocus());
        editAccountUsernameField.setOnAction(e -> { if (editAccountVisiblePasswordField.isVisible()) editAccountVisiblePasswordField.requestFocus(); else editAccountPasswordField.requestFocus(); });
        editAccountPasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
        editAccountVisiblePasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
    }

    @FXML
    private void onToggleEditAccountPasswordVisibility() {
        if (editAccountVisiblePasswordField.isVisible()) {
            editAccountPasswordField.setText(editAccountVisiblePasswordField.getText());
            editAccountPasswordField.setVisible(true); editAccountVisiblePasswordField.setVisible(false);
        } else {
            editAccountVisiblePasswordField.setText(editAccountPasswordField.getText());
            editAccountVisiblePasswordField.setVisible(true); editAccountPasswordField.setVisible(false);
        }
    }

    @FXML
    private void onEditAccountCancel() {
        if (AlertUtil.showYesNoConfirmation("Cancel Edit", "Discard changes?", "Any unsaved changes will be lost.")) {
            editingAccount = null;
            showAccountsList();
        }
    }

    @FXML
    private void onEditAccountSave() {
        setEditAccountError(null);
        if (editingAccount == null) return;

        String name = editAccountNameField.getText() == null ? "" : editAccountNameField.getText().trim();
        String username = editAccountUsernameField.getText() == null ? "" : editAccountUsernameField.getText().trim();
        if (username.startsWith("@")) username = username.substring(1);

        String newPassword = editAccountVisiblePasswordField.isVisible() ? editAccountVisiblePasswordField.getText() : editAccountPasswordField.getText();
        newPassword = newPassword == null ? "" : newPassword;

        AccountRole role = editAccountRoleCombo.getValue();
        AccountStatus status = editAccountStatusCombo.getValue();

        if (name.isEmpty() || username.isEmpty() || role == null || status == null) { setEditAccountError("Please fill in all required fields."); return; }
        if (!name.matches("^[a-zA-Z\\s\\.]+$")) { AlertUtil.showWarning("Invalid Full Name", "Full name can only contain letters, spaces, and periods."); editAccountNameField.requestFocus(); return; }
        if (!username.matches("^[a-zA-Z0-9_-]{3,}$")) { AlertUtil.showWarning("Invalid Username", "Username must be at least 3 characters long."); editAccountUsernameField.requestFocus(); return; }

        if (!newPassword.isEmpty() && (newPassword.length() < 8 || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$"))) {
            AlertUtil.showWarning("Weak Password", "Password must be at least 8 characters long and contain both letters and numbers.");
            if (editAccountVisiblePasswordField.isVisible()) editAccountVisiblePasswordField.requestFocus(); else editAccountPasswordField.requestFocus();
            return;
        }

        if (!AlertUtil.showYesNoConfirmation("Save Changes", "Update account details?", "Save these changes to the database?")) return;

        String finalPassword = newPassword.isEmpty() ? null : newPassword;
        if (userDAO.updateAccount((int) editingAccount.id(), name, username, finalPassword, role, status)) {
            editingAccount = null;
            AlertUtil.showInfo("Account Updated", "The account details were successfully saved.");
            loadAccountManagementData();
            showAccountsList();
        } else {
            setEditAccountError("Failed to update account. Username might already exist.");
        }
    }

    private void setEditAccountError(String message) {
        boolean has = message != null && !message.isBlank();
        editAccountErrorLabel.setText(has ? message : "");
        editAccountErrorLabel.setVisible(has); editAccountErrorLabel.setManaged(has);
    }

    /* ============================== CELL FACTORIES ============================== */

    private void bindAccountColumn(TableColumn<AccountRow, AccountRow> col, Supplier<TableCell<AccountRow, AccountRow>> cellSupplier) {
        col.setSortable(false); col.setReorderable(false);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(tc -> cellSupplier.get());
    }

    private abstract static class AccountCell extends TableCell<AccountRow, AccountRow> {
        @Override protected void updateItem(AccountRow item, boolean empty) {
            super.updateItem(item, empty);
            setText(null); setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            if (empty || item == null) setGraphic(null); else render(item);
        }
        protected abstract void render(AccountRow row);
    }

    private TableCell<AccountRow, AccountRow> accountTextCell(Function<AccountRow, String> text, String styleClass) {
        return new AccountCell() {
            private final Label label = new Label();
            { label.getStyleClass().add(styleClass); }
            @Override protected void render(AccountRow r) { label.setText(text.apply(r)); setGraphic(label); }
        };
    }

    private TableCell<AccountRow, AccountRow> accountRoleCell() {
        return new AccountCell() {
            private final Label chip = new Label();
            @Override protected void render(AccountRow r) {
                if (r.role() == null) { chip.setText("—"); chip.getStyleClass().setAll("accounts-meta"); }
                else { chip.setText(r.role().getDisplayName()); chip.getStyleClass().setAll("accounts-role-chip", "accounts-role-" + r.role().name().toLowerCase(Locale.ROOT)); }
                setGraphic(chip);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountStatusCell() {
        return new AccountCell() {
            private final HBox chip = new HBox(6.0);
            private final Region dot = new Region();
            private final Label text = new Label();
            {
                chip.setAlignment(Pos.CENTER_LEFT); chip.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                dot.getStyleClass().add("chip-dot"); text.getStyleClass().add("chip-text");
                chip.getChildren().addAll(dot, text);
            }
            @Override protected void render(AccountRow r) {
                AccountStatus s = r.status();
                String cls = s == null ? "chip-neutral" : switch (s) { case ACTIVE -> "chip-success"; case INACTIVE -> "chip-warning"; };
                chip.getStyleClass().setAll("chip", cls);
                text.setText(s == null ? "—" : s.getDisplayName());
                setGraphic(chip);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountActionsCell() {
        return new AccountCell() {
            private AccountRow current;
            private final Button editBtn = accountsIconButton(Icons.SQUARE_PEN, "icon-brand", "Edit account");
            private final Button deleteBtn = accountsIconButton(Icons.TRASH_2, "icon-danger", "Delete account");
            private final HBox box = new HBox(8.0, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e -> { if (current != null) editAccount(current); });
                deleteBtn.setOnAction(e -> { if (current != null) deleteAccount(current); });
            }
            @Override protected void render(AccountRow r) { current = r; setGraphic(box); }
        };
    }

    private Button accountsIconButton(String pathData, String colorClass, String tooltip) {
        SVGPath icon = new SVGPath(); icon.setContent(pathData); icon.getStyleClass().addAll("phosphor-icon", colorClass);
        double scale = 14.0 / 24.0; icon.setScaleX(scale); icon.setScaleY(scale); icon.setStyle("-fx-stroke-width: " + (1.5 / scale) + ";");
        StackPane host = new StackPane(icon); host.setPrefSize(14.0, 14.0);
        Button b = new Button(); b.setGraphic(host); b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMnemonicParsing(false); b.getStyleClass().add("accounts-icon-btn"); b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private String formatAccountLastLogin(LocalDateTime ll) {
        if (ll == null) return "Never";
        long days = ChronoUnit.DAYS.between(ll.toLocalDate(), LocalDate.now());
        if (days <= 0) return "Today, " + ll.format(ACCOUNTS_TIME_FMT);
        if (days == 1) return "Yesterday, " + ll.format(ACCOUNTS_TIME_FMT);
        if (days < 7) return days + " days ago";
        long weeks = days / 7;
        if (weeks < 5) return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        return ll.toLocalDate().format(ACCOUNTS_DATE_FMT);
    }
}
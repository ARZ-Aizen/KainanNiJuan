package com.kainanresto.controllers.main.admin.accounts;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.AccountRow;
import com.kainanresto.model.account.AccountStatus;
import com.kainanresto.model.account.User;
import com.kainanresto.model.util.Icons;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.PasswordHasher;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
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
import javafx.stage.Window;
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

    @FXML
    private VBox accountsView;
    @FXML
    private VBox archiveAccountsView;
    @FXML
    private ScrollPane editAccountView;
    @FXML
    private ScrollPane addAccountView;

    @FXML
    private HBox accountsSearchBox, accountsRolePill, accountsStatusPill, accountsLastLoginPill;
    @FXML
    private SVGPath accountsSearchIcon, addAccountBtnIcon;
    @FXML
    private TextField accountsSearchField;
    @FXML
    private Label accountsRoleValueLabel, accountsStatusValueLabel, accountsLastLoginValueLabel;

    @FXML
    private TableView<AccountRow> accountsTable;
    @FXML
    private TableColumn<AccountRow, AccountRow> accountsColName, accountsColUsername, accountsColRole;
    @FXML
    private TableColumn<AccountRow, AccountRow> accountsColStatus, accountsColLastLogin, accountsColCreated,
            accountsColActions;

    @FXML
    private TableView<AccountRow> archiveTable;
    @FXML
    private TableColumn<AccountRow, AccountRow> archiveColName, archiveColUsername, archiveColRole, archiveColActions;

    @FXML
    private TextField editAccountNameField, editAccountUsernameField, editAccountVisiblePasswordField;
    @FXML
    private PasswordField editAccountPasswordField;
    @FXML
    private TextField editAccountVisibleConfirmPasswordField;
    @FXML
    private PasswordField editAccountConfirmPasswordField;
    @FXML
    private ComboBox<AccountRole> editAccountRoleCombo;
    @FXML
    private Label editAccountCreatedLabel, editAccountLastLoginLabel, editAccountErrorLabel;

    @FXML
    private SVGPath addAccountEyeIcon;
    @FXML
    private TextField addAccountNameField, addAccountUsernameField, addAccountVisiblePasswordField;
    @FXML
    private PasswordField addAccountPasswordField;
    @FXML
    private TextField addAccountVisibleConfirmPasswordField;
    @FXML
    private PasswordField addAccountConfirmPasswordField;
    @FXML
    private ComboBox<AccountRole> addAccountRoleCombo;
    @FXML
    private Label addAccountErrorLabel;

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final String ACCOUNTS_ALL_ROLES = "All Roles";
    private static final String ACCOUNTS_ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ACCOUNTS_TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ACCOUNTS_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy",
            Locale.ENGLISH);
    private static final DateTimeFormatter EDIT_ACCOUNT_DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy",
            Locale.ENGLISH);

    private static final String ICON_PLUS = "M5 12h14 M12 5v14";
    private static final String ICON_EYE = "M12 5C7.3 5 3.1 8.3 1.2 12c1.9 3.7 6.1 7 10.8 7s8.9-3.3 10.8-7C20.9 8.3 16.7 5 12 5z m0 10c-1.7 0-3-1.3-3-3s1.3-3 3-3 3 1.3 3 3-1.3 3-3 3z";

    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    private static final String PASSWORD_RULE_MSG = "Password must be at least 8 characters long and contain at least 1 capital letter, 1 number, and 1 special character.";

    private enum AccountsLastLoginFilter {
        ANYTIME("Anytime"), TODAY("Today"), LAST_7_DAYS("Last 7 days"), LAST_30_DAYS("Last 30 days"), NEVER("Never");

        private final String label;

        AccountsLastLoginFilter(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private final Label accountsEmptyLabel = new Label("No accounts loaded.");
    private final ObservableList<AccountRow> accountsMaster = FXCollections.observableArrayList();
    private final ObservableList<AccountRow> archiveMaster = FXCollections.observableArrayList();
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
        setupArchivePage();
        setupEditAccountPage();
        setupAddAccountPage();
        loadAccountManagementData();
        showAccountsList();
    }

    private void setupIcons() {
        if (accountsSearchIcon != null) {
            accountsSearchIcon.setContent(Icons.SEARCH);
            setIconScale(accountsSearchIcon, 14.0);
        }
        if (addAccountBtnIcon != null) {
            addAccountBtnIcon.setContent(ICON_PLUS);
            setIconScale(addAccountBtnIcon, 16.0);
        }
        if (addAccountEyeIcon != null) {
            addAccountEyeIcon.setContent(ICON_EYE);
            setIconScale(addAccountEyeIcon, 16.0);
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
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (2.0 / scale) + ";");
    }

    public void refreshData(List<AccountRow> latestUsers) {
        if (addAccountView.isVisible() || editAccountView.isVisible()) {
            return;
        }
        Platform.runLater(() -> {
            accountsMaster.setAll(filterVisibleAccounts(latestUsers));
            if (archiveAccountsView.isVisible()) {
                archiveMaster.setAll(filterVisibleArchiveAccounts(userDAO.getArchivedAccounts()));
            }
        });
    }

    private void showView(Node target) {
        for (Node n : new Node[] { accountsView, editAccountView, addAccountView, archiveAccountsView }) {
            if (n == null)
                continue;
            boolean on = n == target;
            n.setVisible(on);
            n.setManaged(on);
        }
    }

    private void showAccountsList() {
        showView(accountsView);
        if (editAccountRoleCombo != null)
            editAccountRoleCombo.setDisable(false);
        if (editAccountUsernameField != null)
            editAccountUsernameField.setDisable(false);
    }

    private void showEditAccountForm() {
        showView(editAccountView);
    }

    private void showAddAccountForm() {
        showView(addAccountView);
    }

    private void loadAccountManagementData() {
        accountsMaster.setAll(filterVisibleAccounts(userDAO.getAllAccounts()));
        applyAccountsFilter();
    }

    @FXML
    private void onShowArchive() {
        archiveMaster.setAll(filterVisibleArchiveAccounts(userDAO.getArchivedAccounts()));

        if (archiveTable.getItems().isEmpty() && archiveMaster.isEmpty()) {
            Label emptyLabel = new Label("No archived accounts.");
            emptyLabel.getStyleClass().add("accounts-empty-label");
            archiveTable.setPlaceholder(emptyLabel);
            archiveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            archiveTable.setFixedCellSize(57.0);

            bindAccountColumn(archiveColName,
                    () -> accountTextCell(a -> a.fullName() == null || a.fullName().isBlank() ? "—" : a.fullName(),
                            "accounts-name"));
            bindAccountColumn(archiveColUsername,
                    () -> accountTextCell(
                            a -> (a.username() == null || a.username().isBlank()) ? "—" : "@" + a.username(),
                            "accounts-username"));
            bindAccountColumn(archiveColRole, this::accountRoleCell);
            bindAccountColumn(archiveColActions, this::archiveActionsCell);
        }

        archiveTable.setItems(archiveMaster);
        showView(archiveAccountsView);
    }

    @FXML
    private void onShowActiveAccounts() {
        loadAccountManagementData();
        showAccountsList();
    }

    private void setupArchivePage() {
        Label emptyLabel = new Label("No archived accounts.");
        emptyLabel.getStyleClass().add("accounts-empty-label");
        archiveTable.setPlaceholder(emptyLabel);
        archiveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        archiveTable.setFixedCellSize(57.0);

        bindAccountColumn(archiveColName,
                () -> accountTextCell(a -> a.fullName() == null || a.fullName().isBlank() ? "—" : a.fullName(),
                        "accounts-name"));
        bindAccountColumn(archiveColUsername,
                () -> accountTextCell(a -> (a.username() == null || a.username().isBlank()) ? "—" : "@" + a.username(),
                        "accounts-username"));
        bindAccountColumn(archiveColRole, this::accountRoleCell);
        bindAccountColumn(archiveColActions, this::archiveActionsCell);

        archiveMaster.setAll(filterVisibleArchiveAccounts(userDAO.getArchivedAccounts()));
        archiveTable.setItems(archiveMaster);
    }

    private boolean isStrongPassword(String p) {
        return p != null && p.matches(PASSWORD_REGEX);
    }

    private String readField(PasswordField hidden, TextField visible) {
        String p = visible.isVisible() ? visible.getText() : hidden.getText();
        return p == null ? "" : p;
    }

    private void togglePasswordVisibility(PasswordField pw, TextField pwVisible, PasswordField cf,
            TextField cfVisible) {
        boolean showing = pwVisible.isVisible();
        if (showing) {
            pw.setText(pwVisible.getText());
            cf.setText(cfVisible.getText());
        } else {
            pwVisible.setText(pw.getText());
            cfVisible.setText(cf.getText());
        }
        pw.setVisible(showing);
        cf.setVisible(showing);
        pwVisible.setVisible(!showing);
        cfVisible.setVisible(!showing);
    }

    private void resetPasswordFields(PasswordField pw, TextField pwVisible, PasswordField cf, TextField cfVisible) {
        pw.clear();
        pwVisible.clear();
        cf.clear();
        cfVisible.clear();
        pw.setVisible(true);
        cf.setVisible(true);
        pwVisible.setVisible(false);
        cfVisible.setVisible(false);
    }

    private void setupAccountsPage() {
        accountsSearchField.focusedProperty()
                .addListener((obs, was, is) -> accountsSearchBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        accountsSearchField.textProperty().addListener((obs, oldText, newText) -> applyAccountsFilter());

        AccountRole currentRole = getCurrentUserRole();
        LinkedHashMap<String, AccountRole> roleOptions = new LinkedHashMap<>();
        roleOptions.put(ACCOUNTS_ALL_ROLES, null);
        for (AccountRole r : AccountRole.values()) {
            if (canViewRole(currentRole, r)) {
                roleOptions.put(r.getDisplayName(), r);
            }
        }
        wireAccountsPill(accountsRolePill, accountsRoleValueLabel, roleOptions, v -> {
            accountsRoleFilter = v;
            applyAccountsFilter();
        });

        LinkedHashMap<String, AccountStatus> statusOptions = new LinkedHashMap<>();
        statusOptions.put(ACCOUNTS_ALL_STATUSES, null);
        for (AccountStatus s : AccountStatus.values())
            statusOptions.put(s.getDisplayName(), s);
        wireAccountsPill(accountsStatusPill, accountsStatusValueLabel, statusOptions, v -> {
            accountsStatusFilter = v;
            applyAccountsFilter();
        });

        LinkedHashMap<String, AccountsLastLoginFilter> loginOptions = new LinkedHashMap<>();
        for (AccountsLastLoginFilter f : AccountsLastLoginFilter.values())
            loginOptions.put(f.label(), f);
        wireAccountsPill(accountsLastLoginPill, accountsLastLoginValueLabel, loginOptions, v -> {
            accountsLastLoginFilter = v;
            applyAccountsFilter();
        });

        accountsEmptyLabel.getStyleClass().add("accounts-empty-label");
        accountsTable.setPlaceholder(accountsEmptyLabel);
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        accountsTable.setFixedCellSize(57.0);
        accountsFiltered = new FilteredList<>(accountsMaster, a -> true);
        accountsTable.setItems(accountsFiltered);

        bindAccountColumn(accountsColName,
                () -> accountTextCell(a -> a.fullName() == null || a.fullName().isBlank() ? "—" : a.fullName(),
                        "accounts-name"));
        bindAccountColumn(accountsColUsername,
                () -> accountTextCell(a -> (a.username() == null || a.username().isBlank()) ? "—" : "@" + a.username(),
                        "accounts-username"));
        bindAccountColumn(accountsColRole, this::accountRoleCell);
        bindAccountColumn(accountsColStatus, this::accountStatusCell);
        bindAccountColumn(accountsColLastLogin,
                () -> accountTextCell(a -> formatAccountLastLogin(a.lastLogin()), "accounts-meta"));
        bindAccountColumn(accountsColCreated,
                () -> accountTextCell(a -> a.createdDate() == null ? "—" : a.createdDate().format(ACCOUNTS_DATE_FMT),
                        "accounts-meta"));
        bindAccountColumn(accountsColActions, this::accountActionsCell);

        updateAccountsEmptyState();
    }

    private <T> void wireAccountsPill(HBox pill, Label valueLabel, LinkedHashMap<String, T> options,
            Consumer<T> onPick) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-menu");
        options.forEach((label, value) -> {
            MenuItem item = new MenuItem(label);
            item.setMnemonicParsing(false);
            item.setOnAction(e -> {
                valueLabel.setText(label);
                onPick.accept(value);
            });
            menu.getItems().add(item);
        });
        pill.setOnMouseClicked(e -> {
            if (menu.isShowing())
                menu.hide();
            else
                menu.show(pill, Side.BOTTOM, 0.0, 4.0);
        });
    }

    @FXML
    private void onAccountsClearFilters() {
        accountsRoleFilter = null;
        accountsStatusFilter = null;
        accountsLastLoginFilter = AccountsLastLoginFilter.ANYTIME;
        accountsRoleValueLabel.setText(ACCOUNTS_ALL_ROLES);
        accountsStatusValueLabel.setText(ACCOUNTS_ALL_STATUSES);
        accountsLastLoginValueLabel.setText(AccountsLastLoginFilter.ANYTIME.label());
        accountsSearchField.clear();
        applyAccountsFilter();
    }

    private void applyAccountsFilter() {
        if (accountsFiltered == null)
            return;
        String q = accountsSearchField.getText() == null ? ""
                : accountsSearchField.getText().trim().toLowerCase(Locale.ROOT);
        AccountRole currentRole = getCurrentUserRole();

        accountsFiltered.setPredicate(a -> canViewAccount(currentRole, a)
                && (accountsRoleFilter == null || a.role() == accountsRoleFilter)
                && (accountsStatusFilter == null || a.status() == accountsStatusFilter)
                && matchesAccountsLastLogin(a)
                && (q.isEmpty() || (a.fullName() != null && a.fullName().toLowerCase(Locale.ROOT).contains(q))
                        || (a.username() != null && a.username().toLowerCase(Locale.ROOT).contains(q))));
        updateAccountsEmptyState();
    }

    private boolean matchesAccountsLastLogin(AccountRow a) {
        LocalDateTime ll = a.lastLogin();
        LocalDate today = LocalDate.now();
        return switch (accountsLastLoginFilter) {
            case ANYTIME -> true;
            case NEVER -> ll == null;
            case TODAY -> ll != null && ll.toLocalDate().equals(today);
            case LAST_7_DAYS -> ll != null && !ll.isBefore(today.minusDays(6).atStartOfDay());
            case LAST_30_DAYS -> ll != null && !ll.isBefore(today.minusDays(29).atStartOfDay());
        };
    }

    private void updateAccountsEmptyState() {
        accountsEmptyLabel
                .setText(accountsMaster.isEmpty() ? "No accounts loaded." : "No accounts match your filters.");
    }

    private AccountRole getCurrentUserRole() {
        User currentUser = SessionManager.getCurrentUser();
        return currentUser != null ? currentUser.getRole() : null;
    }

    public static boolean canViewRole(AccountRole viewerRole, AccountRole targetRole) {
        if (targetRole == null) {
            return false;
        }
        if (viewerRole == null || viewerRole == AccountRole.ADMIN) {
            return true;
        }
        if (targetRole == AccountRole.ADMIN) {
            return false;
        }
        if (viewerRole == AccountRole.SUPERVISOR) {
            return true; // Supervisor sees Supervisor, Manager, Cashier
        }
        if (viewerRole == AccountRole.MANAGER) {
            return targetRole == AccountRole.MANAGER || targetRole == AccountRole.CASHIER;
        }
        return false;
    }

    public boolean canViewAccount(AccountRole viewerRole, AccountRow target) {
        return canViewAccount(viewerRole, target, isSelf(target));
    }

    public static boolean canViewAccount(AccountRole viewerRole, AccountRow target, boolean isSelf) {
        if (target == null) {
            return false;
        }
        if (viewerRole == null || viewerRole == AccountRole.ADMIN) {
            return true;
        }

        boolean isTargetAdmin = target.role() == AccountRole.ADMIN
                || "admin".equalsIgnoreCase(target.username());

        if (viewerRole == AccountRole.SUPERVISOR) {
            // Supervisor cannot see admin and co-supervisor
            if (isTargetAdmin) {
                return false;
            }
            if (target.role() == AccountRole.SUPERVISOR) {
                return isSelf;
            }
            return true;
        }

        if (viewerRole == AccountRole.MANAGER) {
            // Manager cannot see admin, supervisor, and co manager
            if (isTargetAdmin || target.role() == AccountRole.SUPERVISOR) {
                return false;
            }
            if (target.role() == AccountRole.MANAGER) {
                return isSelf;
            }
            return true;
        }

        return isSelf;
    }

    private List<AccountRow> filterVisibleAccounts(List<AccountRow> list) {
        if (list == null) return List.of();
        AccountRole currentRole = getCurrentUserRole();
        if (currentRole == null || currentRole == AccountRole.ADMIN) {
            return list;
        }
        List<AccountRow> result = new ArrayList<>();
        for (AccountRow a : list) {
            if (canViewAccount(currentRole, a)) {
                result.add(a);
            }
        }
        return result;
    }

    private List<AccountRow> filterVisibleArchiveAccounts(List<AccountRow> list) {
        if (list == null) return List.of();
        AccountRole currentRole = getCurrentUserRole();
        if (currentRole == null || currentRole == AccountRole.ADMIN) {
            return list;
        }
        List<AccountRow> result = new ArrayList<>();
        for (AccountRow a : list) {
            if (canViewAccount(currentRole, a, false)) {
                result.add(a);
            }
        }
        return result;
    }

    public static List<AccountRole> getAssignableRoles(AccountRole actorRole) {
        if (actorRole == null) {
            return List.of();
        }
        return switch (actorRole) {
            case ADMIN -> List.of(AccountRole.CASHIER, AccountRole.MANAGER, AccountRole.SUPERVISOR);
            case SUPERVISOR -> List.of(AccountRole.CASHIER, AccountRole.MANAGER);
            case MANAGER -> List.of(AccountRole.CASHIER);
            case CASHIER -> List.of();
        };
    }

    public static boolean canManageRole(AccountRole actorRole, AccountRole targetRole) {
        if (actorRole == null || targetRole == null) {
            return false;
        }
        if (targetRole == AccountRole.ADMIN) {
            return false;
        }
        return getAssignableRoles(actorRole).contains(targetRole);
    }

    private boolean isSelf(AccountRow account) {
        if (account == null)
            return false;
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null)
            return false;
        return currentUser.getUserId() == account.id()
                || (account.username() != null && account.username().equalsIgnoreCase(currentUser.getUsername()));
    }

    private void editAccount(AccountRow selectedAccount) {
        boolean self = isSelf(selectedAccount);
        if (!self && (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username()))) {
            AlertUtil.showError("Action Denied", "The primary administrator account cannot be edited.");
            return;
        }

        AccountRole currentRole = getCurrentUserRole();
        if (!self && !canManageRole(currentRole, selectedAccount.role())) {
            AlertUtil.showError("Action Denied", "You do not have permission to edit a "
                    + selectedAccount.role().getDisplayName() + " account.");
            return;
        }

        editingAccount = selectedAccount;
        editAccountNameField.setText(selectedAccount.fullName() == null ? "" : selectedAccount.fullName());
        editAccountUsernameField.setText(selectedAccount.username() == null ? "" : selectedAccount.username());

        boolean isRootAdmin = "admin".equalsIgnoreCase(selectedAccount.username())
                && selectedAccount.role() == AccountRole.ADMIN;
        editAccountUsernameField.setDisable(isRootAdmin);

        resetPasswordFields(editAccountPasswordField, editAccountVisiblePasswordField,
                editAccountConfirmPasswordField, editAccountVisibleConfirmPasswordField);

        if (self) {
            editAccountRoleCombo.getItems().setAll(selectedAccount.role());
            editAccountRoleCombo.setValue(selectedAccount.role());
            editAccountRoleCombo.setDisable(true);
        } else {
            List<AccountRole> allowableRoles = getAssignableRoles(currentRole);
            editAccountRoleCombo.getItems().setAll(allowableRoles);
            editAccountRoleCombo.setValue(selectedAccount.role());
            editAccountRoleCombo.setDisable(false);
        }

        editAccountCreatedLabel.setText(selectedAccount.createdDate() == null ? "—"
                : selectedAccount.createdDate().format(EDIT_ACCOUNT_DATE_FMT));
        editAccountLastLoginLabel.setText(formatAccountLastLogin(selectedAccount.lastLogin()));

        setEditAccountError(null);
        showEditAccountForm();
    }

    private void deleteAccount(AccountRow selectedAccount) {
        if (isSelf(selectedAccount)) {
            AlertUtil.showError("Action Denied", "You cannot archive your own active account.");
            return;
        }
        if (selectedAccount.role() == AccountRole.ADMIN || "admin".equalsIgnoreCase(selectedAccount.username())) {
            AlertUtil.showError("Action Denied", "The primary administrator account cannot be archived.");
            return;
        }

        AccountRole currentRole = getCurrentUserRole();
        if (!canManageRole(currentRole, selectedAccount.role())) {
            AlertUtil.showError("Action Denied", "You do not have permission to archive a "
                    + selectedAccount.role().getDisplayName() + " account.");
            return;
        }

        if (AlertUtil.showYesNoConfirmation("Archive Account", "Archive account: " + selectedAccount.username() + "?",
                "This user will be moved to the archive and can no longer log in.")) {

            Window window = accountsView.getScene().getWindow();
            if (!AdminAuthDialog.show(window, "Authorize Account Archiving"))
                return;

            if (userDAO.archiveUser((int) selectedAccount.id()) == UserDAO.OperationResult.SUCCESS) {
                AlertUtil.showInfo("Success", "Account has been archived.");
                loadAccountManagementData();
            } else {
                AlertUtil.showError("Database Error", "Failed to archive the account.");
            }
        }
    }

    private void setupAddAccountPage() {
        addAccountRoleCombo.getItems().setAll(getAssignableRoles(getCurrentUserRole()));
        addAccountRoleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AccountRole r) {
                return r == null ? "" : r.getDisplayName();
            }

            @Override
            public AccountRole fromString(String s) {
                return null;
            }
        });

        addAccountVisiblePasswordField.managedProperty().bind(addAccountVisiblePasswordField.visibleProperty());
        addAccountPasswordField.managedProperty().bind(addAccountPasswordField.visibleProperty());
        addAccountVisibleConfirmPasswordField.managedProperty()
                .bind(addAccountVisibleConfirmPasswordField.visibleProperty());
        addAccountConfirmPasswordField.managedProperty().bind(addAccountConfirmPasswordField.visibleProperty());

        addAccountNameField.setOnAction(e -> addAccountUsernameField.requestFocus());
        addAccountUsernameField.setOnAction(e -> {
            if (addAccountVisiblePasswordField.isVisible())
                addAccountVisiblePasswordField.requestFocus();
            else
                addAccountPasswordField.requestFocus();
        });
        addAccountPasswordField.setOnAction(e -> addAccountConfirmPasswordField.requestFocus());
        addAccountVisiblePasswordField.setOnAction(e -> addAccountVisibleConfirmPasswordField.requestFocus());
        addAccountConfirmPasswordField.setOnAction(e -> addAccountRoleCombo.requestFocus());
        addAccountVisibleConfirmPasswordField.setOnAction(e -> addAccountRoleCombo.requestFocus());
    }

    @FXML
    private void onAddAccount() {
        AccountRole currentRole = getCurrentUserRole();
        List<AccountRole> allowableRoles = getAssignableRoles(currentRole);
        if (allowableRoles.isEmpty()) {
            AlertUtil.showError("Action Denied", "You do not have permission to create accounts.");
            return;
        }

        addAccountNameField.clear();
        addAccountUsernameField.clear();
        resetPasswordFields(addAccountPasswordField, addAccountVisiblePasswordField,
                addAccountConfirmPasswordField, addAccountVisibleConfirmPasswordField);

        addAccountRoleCombo.getItems().setAll(allowableRoles);
        if (allowableRoles.size() == 1) {
            addAccountRoleCombo.setValue(allowableRoles.get(0));
        } else {
            addAccountRoleCombo.setValue(null);
        }

        setAddAccountError(null);
        showAddAccountForm();
        addAccountNameField.requestFocus();
    }

    @FXML
    private void onToggleAddAccountPasswordVisibility() {
        togglePasswordVisibility(addAccountPasswordField, addAccountVisiblePasswordField,
                addAccountConfirmPasswordField, addAccountVisibleConfirmPasswordField);
    }

    @FXML
    private void onAddAccountCancel() {
        boolean dirty = !addAccountNameField.getText().isBlank()
                || !addAccountUsernameField.getText().isBlank()
                || !addAccountPasswordField.getText().isEmpty()
                || !addAccountVisiblePasswordField.getText().isEmpty()
                || !addAccountConfirmPasswordField.getText().isEmpty()
                || !addAccountVisibleConfirmPasswordField.getText().isEmpty();
        if (!dirty || AlertUtil.showYesNoConfirmation("Cancel", "Discard new account?",
                "Anything you entered will be lost.")) {
            showAccountsList();
        }
    }

    @FXML
    private void onAddAccountSave() {
        setAddAccountError(null);

        String name = addAccountNameField.getText() == null ? "" : addAccountNameField.getText().trim();
        String username = addAccountUsernameField.getText() == null ? "" : addAccountUsernameField.getText().trim();
        if (username.startsWith("@"))
            username = username.substring(1);

        String password = readField(addAccountPasswordField, addAccountVisiblePasswordField);
        String confirm = readField(addAccountConfirmPasswordField, addAccountVisibleConfirmPasswordField);
        AccountRole role = addAccountRoleCombo.getValue();

        AccountRole currentRole = getCurrentUserRole();
        if (!getAssignableRoles(currentRole).contains(role)) {
            AlertUtil.showError("Action Denied", "You do not have permission to create a "
                    + (role != null ? role.getDisplayName() : "selected") + " account.");
            return;
        }

        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty() || role == null) {
            setAddAccountError("Please fill in all required fields.");
            return;
        }
        if (!name.matches("^[a-zA-Z\\s\\.]+$")) {
            AlertUtil.showWarning("Invalid Full Name", "Full name can only contain letters, spaces, and periods.");
            addAccountNameField.requestFocus();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_-]{3,}$")) {
            AlertUtil.showWarning("Invalid Username", "Username must be at least 3 characters long.");
            addAccountUsernameField.requestFocus();
            return;
        }
        if (!isStrongPassword(password)) {
            AlertUtil.showWarning("Weak Password", PASSWORD_RULE_MSG);
            if (addAccountVisiblePasswordField.isVisible())
                addAccountVisiblePasswordField.requestFocus();
            else
                addAccountPasswordField.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            AlertUtil.showWarning("Passwords Do Not Match", "The password and confirmation must be the same.");
            if (addAccountVisibleConfirmPasswordField.isVisible())
                addAccountVisibleConfirmPasswordField.requestFocus();
            else
                addAccountConfirmPasswordField.requestFocus();
            return;
        }

        if (!AlertUtil.showYesNoConfirmation("Create Account", "Create this account?",
                "A " + role.getDisplayName() + " account will be created for \"" + name + "\" (@" + username + ")."))
            return;

        Window window = addAccountNameField.getScene().getWindow();
        if (!AdminAuthDialog.show(window, "Authorize New Account Creation"))
            return;

        User newUser = new User(0, username, password, role, name, false);
        UserDAO.OperationResult result = userDAO.register(newUser);

        switch (result) {
            case SUCCESS -> {
                AlertUtil.showInfo("Account Created", "The new account was successfully added.");
                loadAccountManagementData();
                showAccountsList();
            }
            case DUPLICATE_ENTRY -> {
                AlertUtil.showWarning("Username Taken",
                        "The username \"" + username + "\" already exists. Please choose another.");
                addAccountUsernameField.requestFocus();
            }
            default -> AlertUtil.showError("Database Error", "Failed to create the account. Please try again.");
        }
    }

    private void setAddAccountError(String message) {
        boolean has = message != null && !message.isBlank();
        addAccountErrorLabel.setText(has ? message : "");
        addAccountErrorLabel.setVisible(has);
        addAccountErrorLabel.setManaged(has);
    }

    private void setupEditAccountPage() {
        editAccountRoleCombo.getItems().setAll(getAssignableRoles(getCurrentUserRole()));
        editAccountRoleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AccountRole r) {
                return r == null ? "" : r.getDisplayName();
            }

            @Override
            public AccountRole fromString(String s) {
                return null;
            }
        });

        editAccountVisiblePasswordField.managedProperty().bind(editAccountVisiblePasswordField.visibleProperty());
        editAccountPasswordField.managedProperty().bind(editAccountPasswordField.visibleProperty());
        editAccountVisibleConfirmPasswordField.managedProperty()
                .bind(editAccountVisibleConfirmPasswordField.visibleProperty());
        editAccountConfirmPasswordField.managedProperty().bind(editAccountConfirmPasswordField.visibleProperty());

        editAccountNameField.setOnAction(e -> editAccountUsernameField.requestFocus());
        editAccountUsernameField.setOnAction(e -> {
            if (editAccountVisiblePasswordField.isVisible())
                editAccountVisiblePasswordField.requestFocus();
            else
                editAccountPasswordField.requestFocus();
        });
        editAccountPasswordField.setOnAction(e -> editAccountConfirmPasswordField.requestFocus());
        editAccountVisiblePasswordField.setOnAction(e -> editAccountVisibleConfirmPasswordField.requestFocus());
        editAccountConfirmPasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
        editAccountVisibleConfirmPasswordField.setOnAction(e -> editAccountRoleCombo.requestFocus());
    }

    @FXML
    private void onToggleEditAccountPasswordVisibility() {
        togglePasswordVisibility(editAccountPasswordField, editAccountVisiblePasswordField,
                editAccountConfirmPasswordField, editAccountVisibleConfirmPasswordField);
    }

    @FXML
    private void onEditAccountCancel() {
        if (!editFormChanged() || AlertUtil.showYesNoConfirmation("Cancel Edit", "Discard changes?",
                "Any unsaved changes will be lost.")) {
            editingAccount = null;
            showAccountsList();
        }
    }

    private boolean editFormChanged() {
        if (editingAccount == null)
            return false;
        String name = editAccountNameField.getText() == null ? "" : editAccountNameField.getText().trim();
        String username = editAccountUsernameField.getText() == null ? "" : editAccountUsernameField.getText().trim();
        if (username.startsWith("@"))
            username = username.substring(1);

        String origName = editingAccount.fullName() == null ? "" : editingAccount.fullName();
        String origUser = editingAccount.username() == null ? "" : editingAccount.username();

        return !name.equals(origName)
                || !username.equals(origUser)
                || editAccountRoleCombo.getValue() != editingAccount.role()
                || !readField(editAccountPasswordField, editAccountVisiblePasswordField).isEmpty()
                || !readField(editAccountConfirmPasswordField, editAccountVisibleConfirmPasswordField).isEmpty();
    }

    @FXML
    private void onEditAccountSave() {
        setEditAccountError(null);
        if (editingAccount == null)
            return;

        String name = editAccountNameField.getText() == null ? "" : editAccountNameField.getText().trim();
        String username = editAccountUsernameField.getText() == null ? "" : editAccountUsernameField.getText().trim();
        if (username.startsWith("@"))
            username = username.substring(1);

        String newPassword = readField(editAccountPasswordField, editAccountVisiblePasswordField);
        String confirm = readField(editAccountConfirmPasswordField, editAccountVisibleConfirmPasswordField);
        AccountRole role = editAccountRoleCombo.getValue();

        AccountRole currentRole = getCurrentUserRole();
        boolean self = isSelf(editingAccount);

        if (!self && !canManageRole(currentRole, editingAccount.role())) {
            AlertUtil.showError("Action Denied", "You do not have permission to edit a "
                    + editingAccount.role().getDisplayName() + " account.");
            return;
        }
        if (self) {
            if (role != editingAccount.role()) {
                AlertUtil.showError("Action Denied", "You cannot change your own account role.");
                return;
            }
        } else if (!getAssignableRoles(currentRole).contains(role)) {
            AlertUtil.showError("Action Denied", "You do not have permission to assign the role of "
                    + (role != null ? role.getDisplayName() : "selected") + ".");
            return;
        }

        if (name.isEmpty() || username.isEmpty() || role == null) {
            setEditAccountError("Please fill in all required fields.");
            return;
        }
        if (!name.matches("^[a-zA-Z\\s\\.]+$")) {
            AlertUtil.showWarning("Invalid Full Name", "Full name can only contain letters, spaces, and periods.");
            editAccountNameField.requestFocus();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_-]{3,}$")) {
            AlertUtil.showWarning("Invalid Username", "Username must be at least 3 characters long.");
            editAccountUsernameField.requestFocus();
            return;
        }

        if (!newPassword.isEmpty() || !confirm.isEmpty()) {
            if (!isStrongPassword(newPassword)) {
                AlertUtil.showWarning("Weak Password", PASSWORD_RULE_MSG);
                if (editAccountVisiblePasswordField.isVisible())
                    editAccountVisiblePasswordField.requestFocus();
                else
                    editAccountPasswordField.requestFocus();
                return;
            }
            if (!newPassword.equals(confirm)) {
                AlertUtil.showWarning("Passwords Do Not Match", "The password and confirmation must be the same.");
                if (editAccountVisibleConfirmPasswordField.isVisible())
                    editAccountVisibleConfirmPasswordField.requestFocus();
                else
                    editAccountConfirmPasswordField.requestFocus();
                return;
            }
        }

        if (!editFormChanged()) {
            AlertUtil.showInfo("No Changes", "You haven't changed anything.");
            return;
        }
        if (!AlertUtil.showYesNoConfirmation("Save Changes", "Update account details?",
                "Save these changes to the database?"))
            return;

        Window window = editAccountNameField.getScene().getWindow();
        if (!AdminAuthDialog.show(window, "Authorize Account Update"))
            return;

        String finalPassword = newPassword.isEmpty() ? null : newPassword;
        if (userDAO.updateAccount((int) editingAccount.id(), name, username, finalPassword, role,
                editingAccount.status())) {
            if (self) {
                User current = SessionManager.getCurrentUser();
                if (current != null) {
                    current.setFullName(name);
                    current.setUsername(username);
                    if (finalPassword != null) {
                        current.setPassword(PasswordHasher.hash(finalPassword));
                    }
                }
            }
            editingAccount = null;
            AlertUtil.showInfo("Account Updated", "The account details were successfully saved.");
            loadAccountManagementData();
            showAccountsList();
        } else {
            AlertUtil.showError("Update Failed", "Failed to update the account. The username might already exist.");
        }
    }

    private void setEditAccountError(String message) {
        boolean has = message != null && !message.isBlank();
        editAccountErrorLabel.setText(has ? message : "");
        editAccountErrorLabel.setVisible(has);
        editAccountErrorLabel.setManaged(has);
    }

    private void bindAccountColumn(TableColumn<AccountRow, AccountRow> col,
            Supplier<TableCell<AccountRow, AccountRow>> cellSupplier) {
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(tc -> cellSupplier.get());
    }

    private abstract static class AccountCell extends TableCell<AccountRow, AccountRow> {
        @Override
        protected void updateItem(AccountRow item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            if (empty || item == null)
                setGraphic(null);
            else
                render(item);
        }

        protected abstract void render(AccountRow row);
    }

    private TableCell<AccountRow, AccountRow> accountTextCell(Function<AccountRow, String> text, String styleClass) {
        return new AccountCell() {
            private final Label label = new Label();
            {
                label.getStyleClass().add(styleClass);
            }

            @Override
            protected void render(AccountRow r) {
                label.setText(text.apply(r));
                setGraphic(label);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> accountRoleCell() {
        return new AccountCell() {
            private final Label chip = new Label();

            @Override
            protected void render(AccountRow r) {
                if (r.role() == null) {
                    chip.setText("—");
                    chip.getStyleClass().setAll("accounts-meta");
                } else {
                    chip.setText(r.role().getDisplayName());
                    chip.getStyleClass().setAll("accounts-role-chip",
                            "accounts-role-" + r.role().name().toLowerCase(Locale.ROOT));
                }
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
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                dot.getStyleClass().add("chip-dot");
                text.getStyleClass().add("chip-text");
                chip.getChildren().addAll(dot, text);
            }

            @Override
            protected void render(AccountRow r) {
                AccountStatus s = r.status();
                String cls = s == null ? "chip-neutral" : switch (s) {
                    case ACTIVE -> "chip-success";
                    case INACTIVE -> "chip-warning";
                };
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
            private final Button deleteBtn = accountsIconButton(Icons.TRASH_2, "icon-danger", "Archive account");
            private final HBox box = new HBox(8.0, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e -> {
                    if (current != null)
                        editAccount(current);
                });
                deleteBtn.setOnAction(e -> {
                    if (current != null)
                        deleteAccount(current);
                });
            }

            @Override
            protected void render(AccountRow r) {
                current = r;
                AccountRole currentRole = getCurrentUserRole();
                boolean self = isSelf(r);
                boolean canEdit = self || (r != null && canManageRole(currentRole, r.role()));
                boolean canDelete = !self && (r != null && canManageRole(currentRole, r.role()));
                editBtn.setDisable(!canEdit);
                deleteBtn.setDisable(!canDelete);
                setGraphic(box);
            }
        };
    }

    private TableCell<AccountRow, AccountRow> archiveActionsCell() {
        return new AccountCell() {
            private AccountRow current;
            private final Button restoreBtn = accountsIconButton(Icons.ARROWS_CLOCKWISE, "icon-brand",
                    "Restore account");
            private final Button deleteBtn = accountsIconButton(Icons.TRASH_2, "icon-danger", "Permanently delete");
            private final HBox box = new HBox(8.0, restoreBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                restoreBtn.setOnAction(e -> {
                    if (current == null) return;
                    AccountRole currentRole = getCurrentUserRole();
                    if (!canManageRole(currentRole, current.role())) {
                        AlertUtil.showError("Action Denied", "You do not have permission to restore a "
                                + current.role().getDisplayName() + " account.");
                        return;
                    }

                    if (AlertUtil.showYesNoConfirmation("Restore", "Restore account",
                            "Do you want to restore this account?")) {

                        Window window = archiveTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Account Restoration"))
                            return;

                        userDAO.restoreUser((int) current.id());
                        onShowArchive();
                    }
                });
                deleteBtn.setOnAction(e -> {
                    if (current == null) return;
                    AccountRole currentRole = getCurrentUserRole();
                    if (!canManageRole(currentRole, current.role())) {
                        AlertUtil.showError("Action Denied", "You do not have permission to permanently delete a "
                                + current.role().getDisplayName() + " account.");
                        return;
                    }

                    if (AlertUtil.showYesNoConfirmation("Permanent Delete",
                            "Permanently delete this account?", "This cannot be undone!")) {

                        Window window = archiveTable.getScene().getWindow();
                        if (!AdminAuthDialog.show(window, "Authorize Permanent Deletion"))
                            return;

                        userDAO.permanentDeleteUser((int) current.id());
                        onShowArchive();
                    }
                });
            }

            @Override
            protected void render(AccountRow r) {
                current = r;
                AccountRole currentRole = getCurrentUserRole();
                boolean allowed = r != null && canManageRole(currentRole, r.role());
                restoreBtn.setDisable(!allowed);
                deleteBtn.setDisable(!allowed);
                setGraphic(box);
            }
        };
    }

    private Button accountsIconButton(String pathData, String colorClass, String tooltip) {
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

    private String formatAccountLastLogin(LocalDateTime ll) {
        if (ll == null)
            return "Never";
        long days = ChronoUnit.DAYS.between(ll.toLocalDate(), LocalDate.now());
        if (days <= 0)
            return "Today, " + ll.format(ACCOUNTS_TIME_FMT);
        if (days == 1)
            return "Yesterday, " + ll.format(ACCOUNTS_TIME_FMT);
        if (days < 7)
            return days + " days ago";
        long weeks = days / 7;
        if (weeks < 5)
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        return ll.toLocalDate().format(ACCOUNTS_DATE_FMT);
    }
}
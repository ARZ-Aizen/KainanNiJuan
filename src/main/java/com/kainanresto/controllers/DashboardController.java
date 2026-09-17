package com.kainanresto.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DashboardController implements Initializable {

    // ===================== ROOT =====================
    @FXML private BorderPane rootPane;

    // ===================== NAVIGATION =====================
    @FXML private HBox navigationBar;
    @FXML private Label appTitleLabel;
    @FXML private Region navSpacer;
    @FXML private Button moreButton;
    @FXML private Button shareButton;
    @FXML private ImageView avatarImageView;
    @FXML private Button accountMenuButton;

    // ===================== CONTENT / TOOLBAR =====================
    @FXML private VBox contentContainer;
    @FXML private HBox toolbarRow;
    @FXML private HBox segmentedControl;
    @FXML private ToggleGroup segmentToggleGroup;
    @FXML private ToggleButton segmentTabOne;
    @FXML private ToggleButton segmentTabTwo;
    @FXML private ToggleButton segmentTabThree;
    @FXML private Region toolbarSpacer;
    @FXML private TextField searchField;

    // ===================== KPI CARDS =====================
    @FXML private HBox cardsRow;
    @FXML private VBox revenueCard;
    @FXML private Label revenueCardTitle;
    @FXML private Label revenueCardValue;
    @FXML private Label revenueCardDelta;
    @FXML private VBox ordersCard;
    @FXML private Label ordersCardTitle;
    @FXML private Label ordersCardValue;
    @FXML private Label ordersCardDelta;
    @FXML private VBox sessionsCard;
    @FXML private Label sessionsCardTitle;
    @FXML private Label sessionsCardValue;
    @FXML private Label sessionsCardDelta;

    // ===================== MIDDLE ROW =====================
    @FXML private HBox middleRow;
    @FXML private VBox lineChartCard;
    @FXML private Label lineChartTitle;
    @FXML private LineChart<String, Number> revenueLineChart;
    @FXML private CategoryAxis lineChartXAxis;
    @FXML private NumberAxis lineChartYAxis;
    @FXML private VBox peopleCard;
    @FXML private Label peopleListTitle;
    @FXML private ListView<Person> peopleListView;

    // ===================== BOTTOM ROW =====================
    @FXML private HBox bottomRow;
    @FXML private VBox sourcesCard;
    @FXML private Label sourcesTableTitle;
    @FXML private TableView<SourceRow> sourcesTableView;
    @FXML private TableColumn<SourceRow, String> sourceColumn;
    @FXML private TableColumn<SourceRow, Integer> sessionsColumn;
    @FXML private TableColumn<SourceRow, String> changeColumn;
    @FXML private VBox barChartCard;
    @FXML private Label barChartTitle;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private CategoryAxis barChartXAxis;
    @FXML private NumberAxis barChartYAxis;

    // ===================== DATA =====================
    private final ObservableList<SourceRow> sourceData = FXCollections.observableArrayList();
    private final ObservableList<Person> peopleData = FXCollections.observableArrayList();

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureCards();
        configureSourcesTable();
        configurePeopleList();
        configureLineChart();
        configureBarChart();
    }

    private void configureCards() {
        revenueCardTitle.setText("Revenue");
        revenueCardValue.setText("$45,678.90");
        revenueCardDelta.setText("+20% month over month");

        ordersCardTitle.setText("Orders");
        ordersCardValue.setText("2,405");
        ordersCardDelta.setText("+33% month over month");

        sessionsCardTitle.setText("Sessions");
        sessionsCardValue.setText("10,353");
        sessionsCardDelta.setText("-8% month over month");
    }

    private void configureSourcesTable() {
        sourcesTableTitle.setText("Top sources");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        sessionsColumn.setCellValueFactory(new PropertyValueFactory<>("sessions"));
        changeColumn.setCellValueFactory(new PropertyValueFactory<>("change"));

        sourceData.setAll(
                new SourceRow("website.net", 4321, "+84%"),
                new SourceRow("website.net", 4033, "-8%"),
                new SourceRow("website.net", 3128, "+2%"),
                new SourceRow("website.net", 2104, "+33%"),
                new SourceRow("website.net", 2003, "+30%"),
                new SourceRow("website.net", 1894, "+15%"),
                new SourceRow("website.net", 405, "-12%")
        );
        sourcesTableView.setItems(sourceData);
    }

    private void configurePeopleList() {
        peopleListTitle.setText("People");
        peopleData.setAll(
                new Person("Helena", "email@figmasfakedomain.net"),
                new Person("Oscar", "email@figmasfakedomain.net"),
                new Person("Daniel", "email@figmasfakedomain.net"),
                new Person("Daniel Jay Park", "email@figmasfakedomain.net"),
                new Person("Mark Rojas", "email@figmasfakedomain.net")
        );
        peopleListView.setItems(peopleData);
    }

    private void configureLineChart() {
        lineChartTitle.setText("Daily revenue");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        series.getData().add(new XYChart.Data<>("23 Nov", 27000));
        series.getData().add(new XYChart.Data<>("24", 31000));
        series.getData().add(new XYChart.Data<>("25", 29500));
        series.getData().add(new XYChart.Data<>("26", 34000));
        series.getData().add(new XYChart.Data<>("27", 38000));
        series.getData().add(new XYChart.Data<>("28", 36500));
        series.getData().add(new XYChart.Data<>("29", 43000));
        series.getData().add(new XYChart.Data<>("30", 47000));
        revenueLineChart.getData().add(series);
    }

    private void configureBarChart() {
        barChartTitle.setText("Monthly revenue");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int[] values = {54000, 59000, 54000, 52000, 63000, 80000,
                64000, 72000, 60000, 55000, 52000, 34000};
        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], values[i]));
        }
        monthlyBarChart.getData().add(series);
    }

    // ===================== EVENT HANDLERS =====================
    @FXML
    private void handleMoreButton(ActionEvent event) {
        // TODO: show the overflow menu
    }

    @FXML
    private void handleShareButton(ActionEvent event) {
        // TODO: share the dashboard
    }

    @FXML
    private void handleAccountMenuButton(ActionEvent event) {
        // TODO: open the account dropdown
    }

    @FXML
    private void handleSegmentSelection(ActionEvent event) {
        // TODO: reload dashboard data for the selected tab
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // TODO: filter dashboard data using searchField.getText()
    }

    @FXML
    private void handlePersonSelected(MouseEvent event) {
        // TODO: open the selected person's detail view
    }

    // ===================== MODELS =====================
    public static class SourceRow {
        private final StringProperty source = new SimpleStringProperty(this, "source");
        private final IntegerProperty sessions = new SimpleIntegerProperty(this, "sessions");
        private final StringProperty change = new SimpleStringProperty(this, "change");

        public SourceRow(String source, int sessions, String change) {
            this.source.set(source);
            this.sessions.set(sessions);
            this.change.set(change);
        }

        public String getSource() { return source.get(); }
        public void setSource(String value) { source.set(value); }
        public StringProperty sourceProperty() { return source; }

        public int getSessions() { return sessions.get(); }
        public void setSessions(int value) { sessions.set(value); }
        public IntegerProperty sessionsProperty() { return sessions; }

        public String getChange() { return change.get(); }
        public void setChange(String value) { change.set(value); }
        public StringProperty changeProperty() { return change; }
    }

    public static class Person {
        private final StringProperty name = new SimpleStringProperty(this, "name");
        private final StringProperty email = new SimpleStringProperty(this, "email");

        public Person(String name, String email) {
            this.name.set(name);
            this.email.set(email);
        }

        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public StringProperty nameProperty() { return name; }

        public String getEmail() { return email.get(); }
        public void setEmail(String value) { email.set(value); }
        public StringProperty emailProperty() { return email; }

        @Override
        public String toString() { return getName() + "  —  " + getEmail(); }
    }
}
package io.mosip.packet.client.controller;

import io.mosip.packet.client.controller.popup.FilterAddPopupController;
import io.mosip.packet.client.dto.FilterTableViewData;
import io.mosip.packet.client.model.ApplicationResourceContext;
import io.mosip.packet.client.mvp.BaseController;
import io.mosip.packet.core.constant.FieldType;
import io.mosip.packet.core.constant.FilterCondition;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.QueryFilter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TableFilterSelectionController extends BaseController {

    @FXML
    private Label t1DatabaseType;

    @FXML
    private Label t1Databaseurl;

    @FXML
    private Label t1DatabaseName;

    @FXML
    private Label t1DatabasePort;

    @FXML
    private Label t1DatabaseUser;

    @FXML
    private TableView t1TableView2;

    @FXML
    private ComboBox tableComboBox;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private HashMap<String, List<QueryFilter>> filterMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DBImportRequest importRequest = getDbImportRequest();
        t1DatabaseType.setText(importRequest.getDbType().toString());
        t1Databaseurl.setText(importRequest.getUrl());
        t1DatabaseName.setText(importRequest.getDatabaseName());
        t1DatabasePort.setText(importRequest.getPort());
        t1DatabaseUser.setText(importRequest.getUserId());

        addButton.setDisable(true);
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        List<String> tableList = importRequest.getTableDetails().stream().filter(e-> e.getQueryType().equals(QuerySelection.TABLE))
                .map(e-> e.getTableName())
                .collect(Collectors.toList());
        tableComboBox.setItems(FXCollections.observableArrayList(tableList));

        for(String table : tableList)
            filterMap.put(table, new ArrayList<>());

        tableComboBox.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue != null)
                    t1TableView2.getItems().addAll(filterMap.get(newValue.toString()));
                    addButton.setDisable(false);
                    editButton.setDisable(false);
                    deleteButton.setDisable(false);
            }
        });

        designTableView2();
    }

    public void designTableView2() {
        int width = getScreenWidth().intValue();
        int table2Width = Double.valueOf(width * .70).intValue() - 10;

        t1TableView2.setEditable(true);
        t1TableView2.setPrefWidth(table2Width);
        t1TableView2.setMaxWidth(table2Width);

        TableColumn<FilterTableViewData, Boolean> col0 = new TableColumn<FilterTableViewData, Boolean>("Select");
        TableColumn<FilterTableViewData, String> col1 = new TableColumn<FilterTableViewData, String>("Column Name");
        TableColumn<FilterTableViewData, FieldType> col2 = new TableColumn<FilterTableViewData, FieldType>("Column Type");
        TableColumn<FilterTableViewData, FilterCondition> col3 = new TableColumn<FilterTableViewData, FilterCondition>("Condition");
        TableColumn<FilterTableViewData, String> col4 = new TableColumn<FilterTableViewData, String>("From Value");
        TableColumn<FilterTableViewData, String> col5 = new TableColumn<FilterTableViewData, String>("To Value");

        col0.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, Boolean>("selected"));
        col1.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, String>("filterField"));
        col2.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, FieldType>("fieldType"));
        col3.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, FilterCondition>("filterCondition"));
        col4.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, String>("fromValue"));
        col5.setCellValueFactory(new PropertyValueFactory<FilterTableViewData, String>("toValue"));

        col0.setCellFactory(p -> {
            CheckBox checkBox = new CheckBox();
            TableCell<FilterTableViewData, Boolean> cell = new TableCell<FilterTableViewData, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    if (empty) {
                        setGraphic(null);
                    } else {
                        checkBox.setSelected(item);
                        setGraphic(checkBox);
                    }
                }
            };

            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    ((FilterTableViewData)cell.getTableRow().getItem()).setSelect(isSelected));
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);
            return cell ;
        });

        col0.setPrefWidth(Double.valueOf(table2Width * .10)-1);
        col1.setPrefWidth(Double.valueOf(table2Width * .25)-1);
        col2.setPrefWidth(Double.valueOf(table2Width * .25)-1);
        col3.setPrefWidth(Double.valueOf(table2Width * .10)-1);
        col4.setPrefWidth(Double.valueOf(table2Width * .15)-1);
        col5.setPrefWidth(Double.valueOf(table2Width * .15)-1);

        col0.setEditable(true);
        col1.setEditable(false);
        col2.setEditable(false);
        col3.setEditable(false);
        col4.setEditable(false);
        col5.setEditable(false);

        t1TableView2.getColumns().add(col0);
        t1TableView2.getColumns().add(col1);
        t1TableView2.getColumns().add(col2);
        t1TableView2.getColumns().add(col3);
        t1TableView2.getColumns().add(col4);
        t1TableView2.getColumns().add(col5);
    }

    @FXML
    public void addFilter(ActionEvent event) {
        try {
            loadPopup(getClass().getClassLoader().getResource("fxml/popup/FilterAddPopup.fxml"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void editFilter(ActionEvent event) {

    }

    @FXML
    public void deleteFilter(ActionEvent event) {

    }

    public void loadPopup(URL url) throws IOException {
        FXMLLoader loader = new FXMLLoader(url);
        FilterAddPopupController controller = ApplicationResourceContext.getInstance().getApplicationContext().getBean(FilterAddPopupController.class);
        controller.setModel(new FilterTableViewData(), tableComboBox.getValue().toString());
        loader.setController(controller);
        GridPane parent = loader.load();
        parent.setPrefWidth((getScreenWidth()*.75) - 10);
        parent.setPrefHeight((getScreenHeight() *.75) - 10);
        Scene scene = new Scene(parent);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("application.css").toExternalForm());
        Stage popupStage = new Stage();
        popupStage.setScene(scene);
        popupStage.show();
    }
}

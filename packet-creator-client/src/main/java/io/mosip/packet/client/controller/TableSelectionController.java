package io.mosip.packet.client.controller;

import io.mosip.packet.client.dto.TableData;
import io.mosip.packet.client.mvp.BaseController;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.TableRequestDto;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TableSelectionController extends BaseController {

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
    private TableView t1TableView1;

    @FXML
    private TableView t1TableView2;

    private List<ComboBox> table2ComboBoxList = new ArrayList<>();


    ObservableList<Integer> seqItemsList = FXCollections.observableArrayList();

    public ChangeListener changeListener = new ChangeListener() {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            if(newValue != null) {
                if(oldValue != null && !oldValue.equals(0))
                    seqItemsList.add((Integer) oldValue);

                if(seqItemsList.indexOf(newValue) > -1) {
                    seqItemsList.remove(seqItemsList.indexOf(newValue));
                    ((TableRequestDto)((TableCell)((ComboBox)((SimpleObjectProperty) observable).getBean()).getParent()).getTableRow().getItem()).setExecutionOrderSequence((Integer) newValue);
                }
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DBImportRequest importRequest = getDbImportRequest();
        t1DatabaseType.setText(importRequest.getDbType().toString());
        t1Databaseurl.setText(importRequest.getUrl());
        t1DatabaseName.setText(importRequest.getDatabaseName());
        t1DatabasePort.setText(importRequest.getPort());
        t1DatabaseUser.setText(importRequest.getUserId());

        designTableView1();
        designTableView2();

        for(Map<String, Object> map : tableList) {
            Object[] obj = map.values().toArray();
            if(obj.length > 1) {
                t1TableView1.getItems().add(new TableData(obj[0], obj[1], false));
            } else {
                t1TableView1.getItems().add(new TableData(null, obj[0], false));
            }
        }

        seqItemsList.addListener(new ListChangeListener<Integer>() {
            @Override
            public void onChanged(Change<? extends Integer> c) {
                for (ComboBox comboBox : table2ComboBoxList) {
                    c.next();
                    List<Integer> removed = (List<Integer>) c.getRemoved();
                    List<? extends Integer> added = c.getAddedSubList();

                    for(Integer addedItem : added)
                        if(!comboBox.getItems().contains(addedItem))
                            comboBox.getItems().add(addedItem);

                    for(Integer removeItem : removed)
                        if(!removeItem.equals(comboBox.getValue()))
                            comboBox.getItems().remove(removeItem);

                    Collections.sort(comboBox.getItems());
                }
            }
        });
    }

    public void loadScreen(List<Map<String, Object>> tableData) {
        this.tableList = tableData;
        GridPane child = null;
        child = loadScene(getClass().getClassLoader().getResource("fxml/TableSelectionPage.fxml"), null);
        addPane(child);
    }

    @FXML
    public void freezeTableSelection(ActionEvent event) {
        for(Parent object : getObjectList())
            object.setDisable(true);
        t1TableView2.getItems().clear();



        ObservableList<TableData> list =  t1TableView1.getItems();
        List<TableData> data = new ArrayList<>();
        data = list.stream().filter(e-> e.isSelected()).collect(Collectors.toList());

        seqItemsList.clear();
        int i = data.size();
        for(int j = 0; j < i; j++)
            seqItemsList.add(j+1);

        for(TableData tableData : data) {
            TableRequestDto dto = new TableRequestDto();
            dto.setTableName(tableData.getTableName().toString());
            t1TableView2.getItems().add(dto);
        }

    }

    @FXML
    public void clearTableSelection(ActionEvent event) {
        t1TableView2.getItems().clear();
        for(Parent object : getObjectList())
            object.setDisable(false);

       for(ComboBox comboBox : table2ComboBoxList) {
            comboBox.getItems().clear();
        }
    }

    public void designTableView1() {
        int width = getScreenWidth().intValue();
        int table1Width = Double.valueOf(width * .40).intValue() - 10;

        t1TableView1.setEditable(true);
        t1TableView1.setPrefWidth(table1Width);
        t1TableView1.setMaxWidth(table1Width);

        TableColumn<TableData, String> col1 = new TableColumn<TableData, String>("Schema");
        TableColumn<TableData, String> col2 = new TableColumn<TableData, String>("Table Name");
        TableColumn<TableData, Boolean> col3 = new TableColumn<TableData, Boolean>("Select");


        col1.setCellValueFactory(new PropertyValueFactory<>("schema"));
        col2.setCellValueFactory(new PropertyValueFactory<>("tableName"));
        col3.setCellValueFactory(new PropertyValueFactory<>("selected"));

        col3.setCellFactory(p -> {
            CheckBox checkBox = new CheckBox();
            getObjectList().add(checkBox);
            TableCell<TableData, Boolean> cell = new TableCell<TableData, Boolean>() {
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
                    ((TableData)cell.getTableRow().getItem()).setSelected(isSelected));
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);
            return cell ;
        });

        col1.setPrefWidth(Double.valueOf(table1Width * .25)-1);
        col2.setPrefWidth(Double.valueOf(table1Width * .5)-1);
        col3.setPrefWidth(Double.valueOf(table1Width * .25)-1);

        col1.setEditable(false);
        col2.setEditable(false);
        col3.setEditable(true);

        t1TableView1.getColumns().add(col1);
        t1TableView1.getColumns().add(col2);
        t1TableView1.getColumns().add(col3);
    }

    public void designTableView2() {
        int width = getScreenWidth().intValue();
        int table2Width = Double.valueOf(width * .60).intValue() - 10;

        t1TableView2.setEditable(true);
        t1TableView2.setPrefWidth(table2Width);
        t1TableView2.setMaxWidth(table2Width);

        TableColumn<TableRequestDto, String> col1 = new TableColumn<TableRequestDto, String>("Table Name");
        TableColumn<TableRequestDto, QuerySelection> col2 = new TableColumn<TableRequestDto, QuerySelection>("Query Type");
        TableColumn<TableRequestDto, Integer> col3 = new TableColumn<TableRequestDto, Integer>("Execution Sequence");
        TableColumn<TableRequestDto, String> col4 = new TableColumn<TableRequestDto, String>("SQL Query");

        col1.setCellValueFactory(new PropertyValueFactory<TableRequestDto, String>("tableName"));
        col2.setCellValueFactory(new PropertyValueFactory<TableRequestDto, QuerySelection>("queryType"));
        col3.setCellValueFactory(new PropertyValueFactory<TableRequestDto, Integer>("executionOrderSequence"));
        col4.setCellValueFactory(new PropertyValueFactory<TableRequestDto, String>("sqlQuery"));


        col2.setCellFactory(p -> {
            ComboBox comboBox = new ComboBox();
            comboBox.setItems(FXCollections.observableArrayList(QuerySelection.values()));
            TableCell<TableRequestDto, QuerySelection> cell = new TableCell<TableRequestDto, QuerySelection>() {
                @Override
                public void updateItem(QuerySelection item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        comboBox.setValue(item);
                        setGraphic(comboBox);
                    }
                }
            };

            comboBox.valueProperty().addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    ((TableRequestDto)((TableCell)((ComboBox)((SimpleObjectProperty) observable).getBean()).getParent()).getTableRow().getItem()).setQueryType((QuerySelection) newValue);
                }
            });
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);

            return cell;
        });

        col3.setCellFactory(p -> {
            ComboBox comboBox = new ComboBox();
            ObservableList<Integer> valueList = FXCollections.observableArrayList();
            valueList.addAll(seqItemsList);
            comboBox.setItems(valueList);

            TableCell<TableRequestDto, Integer> cell = new TableCell<TableRequestDto, Integer>() {
                @Override
                public void updateItem(Integer item, boolean empty) {
                    if (empty) {
                        setGraphic(null);
                    } else {
                        comboBox.setValue(item);
                        setGraphic(comboBox);
                    }
                }
            };
            comboBox.valueProperty().addListener(changeListener);
            table2ComboBoxList.add(comboBox);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);

            return cell;
        });

        col4.setCellFactory(p->{
            TextArea text = new TextArea();
            text.setPrefHeight(100);

            TableCell<TableRequestDto, String> cell = new TableCell<TableRequestDto, String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if(!isEmpty()) {
                        text.setEditable(false);
                        text.setText(item ==null ? "":item.toString());
                        setGraphic(text);
                    }
                }
            };

            text.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    QuerySelection querySelection = cell.getTableRow().getItem().getQueryType();

                    if(querySelection != null && QuerySelection.SQL_QUERY.equals(querySelection)) {
                        text.setEditable(true);
                    }else {
                        text.setText(null);
                        text.setEditable(false);
                    }
                    cell.getTableRow().getItem().setSqlQuery(text.getText());
                }
            });
            return cell;
        });

        col1.setPrefWidth(Double.valueOf(table2Width * .20)-1);
        col2.setPrefWidth(Double.valueOf(table2Width * .15)-1);
        col3.setPrefWidth(Double.valueOf(table2Width * .15)-1);
        col4.setPrefWidth(Double.valueOf(table2Width * .5)-1);

        col1.setEditable(false);
        col2.setEditable(true);
        col3.setEditable(true);
        col4.setEditable(true);

        t1TableView2.getColumns().add(col1);
        t1TableView2.getColumns().add(col2);
        t1TableView2.getColumns().add(col3);
        t1TableView2.getColumns().add(col4);
    }

    @FXML
    public void next(ActionEvent event) {
        ObservableList<TableRequestDto> list =  t1TableView2.getItems();
        DBImportRequest request = getDbImportRequest();
        request.setTableDetails(new ArrayList<>());

        List<TableRequestDto> data =  list.stream().collect(Collectors.toList());
        request.getTableDetails().addAll(data);

        addPane(loadScene(getClass().getClassLoader().getResource("fxml/TableFilterSelectionPage.fxml"), null));
    }
}

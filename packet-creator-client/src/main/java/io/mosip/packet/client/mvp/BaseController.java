package io.mosip.packet.client.mvp;

import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class BaseController implements Initializable {
    public static List<Map<String, Object>> tableList ;

    private List<Parent> objectList = new ArrayList<>();

    private static VBox bodyPane = null;

    private static DBImportRequest dbImportRequest= null;

    private static Double screenWidth;

    private static Double screenHeight;

    public static void setBodyPane(VBox bodyPane) {
        BaseController.bodyPane = bodyPane;
    }

    public static DBImportRequest getDbImportRequest() {
        return dbImportRequest;
    }

    public static void setDbImportRequest(DBImportRequest dbImportRequest) {
        BaseController.dbImportRequest = dbImportRequest;
    }

    public static Double getScreenWidth() {
        return screenWidth;
    }

    public static void setScreenWidth(Double screenWidth) {
        BaseController.screenWidth = screenWidth;
    }

    public static Double getScreenHeight() {
        return screenHeight;
    }

    public static void setScreenHeight(Double screenHeight) {
        BaseController.screenHeight = screenHeight;
    }

    public List<Parent> getObjectList() {
        return objectList;
    }

    public ObservableList<Integer> seqItemsList = FXCollections.observableArrayList();

    public void setObjectList(List<Parent> objectList) {
        this.objectList = objectList;
    }

    public <T> T loadScene(URL location, ResourceBundle resources) {
        Parent child = null;

        try {
            child = new FXMLLoader(location).load();
            int height = (int) (getScreenHeight() * .85);
            child.prefHeight(height);
            child.prefWidth(getScreenWidth());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (T) child;
    }

    public void addPane(Node pane) {
        bodyPane.getChildren().clear();
        bodyPane.getChildren().add(pane);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}

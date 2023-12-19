package io.mosip.packet.client.mvp;

import io.mosip.packet.client.mvp.BaseController;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class HomePage extends BaseController {

    @FXML
    private VBox bodyPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        GridPane child = null;
        child = loadScene(getClass().getClassLoader().getResource("fxml/DBLoginPage.fxml"), null);
        int height = (int) (getScreenHeight() * .85);
        int size = (int) (height/(child.getRowCount() + 1));
        child.getRowConstraints().stream().forEach(e -> {
            e.setMinHeight(size);
            e.setMaxHeight(size);
        });
        setBodyPane(bodyPane);
        addPane(child);
    }
}

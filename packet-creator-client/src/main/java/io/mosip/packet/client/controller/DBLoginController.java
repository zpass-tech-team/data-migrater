package io.mosip.packet.client.controller;

import io.mosip.packet.client.model.ApplicationResourceContext;
import io.mosip.packet.client.mvp.BaseController;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.dto.RequestWrapper;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class DBLoginController extends BaseController {

    @FXML
    private ComboBox databaseType;

    @FXML
    private TextField databaseUrl;

    @FXML
    private TextField databasePort;

    @FXML
    private TextField databaseName;

    @FXML
    private TextField databaseUser;

    @FXML
    private PasswordField databasePassword;

    @FXML
    private Label dbTypeErrorLabel;

    @FXML
    private Label dbIPErrorLabel;

    @FXML
    private Label dbPortErrorLabel;

    @FXML
    private Label dbNameErrorLabel;

    @FXML
    private Label dbUserErrorLabel;

    @FXML
    private Label dbPasswordErrorLabel;

    private RestTemplate restTemplate = new RestTemplate();

    @FXML
    public void loginDataBase(ActionEvent event) {
        DBImportRequest dbImportRequest = new DBImportRequest();
        dbTypeErrorLabel.setText("");
        dbIPErrorLabel.setText("");
        dbPortErrorLabel.setText("");
        dbNameErrorLabel.setText("");
        dbUserErrorLabel.setText("");
        dbPasswordErrorLabel.setText("");

        Boolean errorFree = true;

        if(databaseType.getValue() == null || databaseType.getValue().toString().isEmpty()) {
            dbTypeErrorLabel.setText("Database Type is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setDbType(Enum.valueOf(DBTypes.class, databaseType.getValue().toString()));

        if(databaseName.getText() == null || databaseName.getText().isEmpty()) {
            dbNameErrorLabel.setText("Database Name is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setDatabaseName(databaseName.getText());

        if(databaseUrl.getText() == null || databaseUrl.getText().isEmpty()) {
            dbIPErrorLabel.setText("Database Url is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setUrl(databaseUrl.getText());

        if(databasePort.getText() == null || databasePort.getText().isEmpty()) {
            dbPortErrorLabel.setText("Database Port is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setPort(databasePort.getText());

        if(databaseUser.getText() == null || databaseUser.getText().isEmpty()) {
            dbUserErrorLabel.setText("Database User is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setUserId(databaseUser.getText());

        if(databasePassword.getText() == null || databasePassword.getText().isEmpty()) {
            dbPasswordErrorLabel.setText("Database Password is Mandatory");
            errorFree = false;
        }
        else
            dbImportRequest.setPassword(databasePassword.getText());

        try {
            if(errorFree) {
                setDbImportRequest(dbImportRequest);
                Environment env = ApplicationResourceContext.getInstance().getApplicationContext().getEnvironment();
                RequestWrapper<DBImportRequest> request = new RequestWrapper<>();
                request.setRequest(dbImportRequest);
                ResponseWrapper response= (ResponseWrapper) restTemplate.postForObject(env.getProperty(ApiName.FETCH_TABLE_LIST.toString()), request, ResponseWrapper.class);
                List<Map<String, Object>> tableListResponse = ( List<Map<String, Object>>) response.getResponse();

                TableSelectionController controller = ApplicationResourceContext.getInstance().getApplicationContext().getBean(TableSelectionController.class);
                controller.loadScreen(tableListResponse);

            }
        } catch (RestClientException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dbTypeErrorLabel.setTextFill(Color.RED);
        dbIPErrorLabel.setTextFill(Color.RED);
        dbPortErrorLabel.setTextFill(Color.RED);
        dbNameErrorLabel.setTextFill(Color.RED);
        dbUserErrorLabel.setTextFill(Color.RED);
        dbPasswordErrorLabel.setTextFill(Color.RED);
        databaseType.setItems(FXCollections.observableArrayList(DBTypes.values()));
    }

    @FXML
    public void clearDatabase(ActionEvent event) {
        dbTypeErrorLabel.setText("");
        dbIPErrorLabel.setText("");
        dbPortErrorLabel.setText("");
        dbNameErrorLabel.setText("");
        dbUserErrorLabel.setText("");
        dbPasswordErrorLabel.setText("");
        databaseName.setText("MOSIP_NEW");
        databaseUrl.setText("localhost\\\\SQLEXPRESS");
        databasePort.setText("55630");
        databaseUser.setText("trialblazerseee");
        databasePassword.setText("ka09071989");
        databaseType.setValue(DBTypes.MSSQL.toString());
    }
}

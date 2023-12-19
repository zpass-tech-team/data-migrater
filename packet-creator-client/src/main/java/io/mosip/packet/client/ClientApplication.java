package io.mosip.packet.client;

import io.mosip.packet.client.config.AppConfig;
import io.mosip.packet.client.mvp.BaseController;
import io.mosip.packet.client.mvp.HomePage;
import io.mosip.packet.client.model.ApplicationResourceContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ClientApplication extends Application {

    private ApplicationContext context;

    @Override
    public void init() throws Exception {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
 //       SpringApplicationBuilder builder = new SpringApplicationBuilder(ClientApplication.class);
 //       context = builder.run(getParameters().getRaw().toArray(new String[0]));
        ApplicationResourceContext.getInstance().setApplicationLanguage(context.getEnvironment().getProperty("mosip.primary-language"));
        ApplicationResourceContext.getInstance().setApplicationSupportedLanguage(context.getEnvironment().getProperty("mosip.supported-languages"));
        ApplicationResourceContext.getInstance().setApplicationContext(context);
    }

    @Override
    public void start(Stage stage) throws Exception {
        viewOnResize(stage);
        HomePage homePage =ApplicationResourceContext.getInstance().getApplicationContext().getBean(HomePage.class);
        Parent loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/mvp/HomePage.fxml")).load();
        loader.minWidth(BaseController.getScreenWidth());
        loader.minHeight(BaseController.getScreenHeight());
        Scene scene = new Scene(loader);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Packet Creator Helper");
        stage.show();
    }

    private void viewOnResize(Stage stage) {
        Rectangle2D bound = Screen.getPrimary().getBounds();
        stage.setHeight(bound.getHeight());
        stage.setWidth(bound.getWidth());
        stage.setMaximized(true);
        stage.setMaxHeight(bound.getMaxY());
        stage.setMaxWidth(bound.getMaxX());
        BaseController.setScreenWidth(bound.getWidth());
        BaseController.setScreenHeight(bound.getHeight());
    }
}

package io.mosip.packet.client;

import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Initialization {

    public static void main(String[] args) {
        LauncherImpl.launchApplication(ClientApplication.class, null, args);
    }
}

package de.exit.jdmx;

import de.exit.jdmx.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * JavaFX Application entry point for jdmx.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1280, 800);

        // Attach KaSo Dark Theme CSS stylesheets
        URL darkTheme = getClass().getResource("/de/exit/jdmx/styles/dark-theme.css");
        if (darkTheme != null) {
            scene.getStylesheets().add(darkTheme.toExternalForm());
        }

        URL components = getClass().getResource("/de/exit/jdmx/styles/components.css");
        if (components != null) {
            scene.getStylesheets().add(components.toExternalForm());
        }

        primaryStage.setTitle("jdmx - Professional Lighting Controller");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

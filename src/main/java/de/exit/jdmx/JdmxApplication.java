package de.exit.jdmx;

import de.exit.jdmx.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

/**
 * JavaFX Anwendungs-Einstiegspunkt für JDMX.
 * <p>
 * Konfiguriert das Primärfenster (Stage), lädt das Studio Dark Theme Stylesheet
 * und instanziiert das {@link MainWindow}.
 * </p>
 */
public class JdmxApplication extends Application {

    public static final String APP_TITLE = "jdmx - Java DMX Controller";
    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 800;
    public static final double MIN_WIDTH = 1024;
    public static final double MIN_HEIGHT = 650;

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Studio Dark Theme laden
        URL cssUrl = getClass().getResource("/de/exit/jdmx/styles/studio-dark.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);

        // App-Icon (Warm Amber Glühbirne im Dark Squircle) laden
        URL iconUrl = getClass().getResource("/icons/jdmx.png");
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        primaryStage.show();
    }
}

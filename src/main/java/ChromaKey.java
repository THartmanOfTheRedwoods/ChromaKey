import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import java.io.IOException;

public class ChromaKey extends Application {

    private final static EventBus eventBus = EventBus.getInstance();
    private boolean allDone = false; // Variable used to control exit of app + all threads.

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Load the main FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainView.fxml")); // Replace "main.fxml" with your actual filename
        Parent root = loader.load();

        // Set the scene on the stage
        Scene scene = new Scene(root); // my need to send width: 800, height: 600 here.
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> {
            // Register to get notified when child controllers/threads exit.
            eventBus.register("CHILD_CLOSE_ACK", (eventData) -> {
                allDone = true; // Once exited, we set this to true to allow the application to exit.
            });
            // Fire close event so child controllers/threads will close down and exit.
            eventBus.fireEvent(event.getEventType().getName(),
                    new EventBusData<Object>(event.getEventType().getName(), null));
            // Let's sleep a bit to allow the child threads a chance to close.
            try {
                // Busy wait until child controller notifies us that its done.
                while(!allDone) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) { }
        });

        eventBus.register("CHILD_CLOSE_REQUEST", (eventData) -> {
            // This event comes from the child, so I forward it on to my setOnCloseRequest handler.
            primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        // Set the title and show the stage
        primaryStage.setTitle("College of the Redwoods Green Screen");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

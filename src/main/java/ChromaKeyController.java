import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChromaKeyController implements Initializable {
    @FXML
    VBox rootPane;

    @FXML
    AnchorPane contentPane;
    @FXML
    MenuItem menuItemImageView;
    @FXML
    MenuItem menuItemPreferences;
    @FXML
    MenuItem menuItemQuit;
    @FXML
    private Stage primaryStage;
    private final EventBus eventBus;
    private final Configuration configuration;
    public ChromaKeyController() {
        configuration = Configuration.getInstance();
        eventBus = EventBus.getInstance();
    }

    private FXMLLoader contentLoader; // Loader for content FXML

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuItemImageView.setDisable(true);  // Start with the imageView active
        menuItemPreferences.setDisable(false);
        contentLoader = new FXMLLoader(getClass().getResource("imageView.fxml"));
        try {
            Parent initialContent = contentLoader.load();
            contentPane.getChildren().add(initialContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void swapContent(String fxmlFileName) throws IOException {
        // Do this first to make sure content panes don't swap before the event is triggered.
        eventBus.fireEvent("SCENE_SWAP_REQUEST",
                new EventBusData<String>("SCENE_SWAP_REQUEST", fxmlFileName));
        // Load the new content FXML
        FXMLLoader newLoader = new FXMLLoader(getClass().getResource(fxmlFileName));
        Parent newContent = newLoader.load();
        // Replace content pane with the new content
        contentPane.getChildren().clear();
        contentPane.getChildren().add(newContent);
    }

    @FXML
    public void onPreferencesSelected(ActionEvent event) {
        try {
            menuItemImageView.setDisable(false);
            menuItemPreferences.setDisable(true);
            swapContent("config.fxml"); // Replace with the FXML file for content 1
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onImageSelected(ActionEvent event) {
        try {
            menuItemImageView.setDisable(true);
            menuItemPreferences.setDisable(false);
            swapContent("imageView.fxml"); // Replace with the FXML file for content 2
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onQuitSelected(ActionEvent event) {
        // ChromaKey is set up to listen to this even on the bus.
        eventBus.fireEvent("CHILD_CLOSE_REQUEST",
                new EventBusData<Object>("CHILD_CLOSE_REQUEST", null));
    }
}

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigController implements Initializable {

    @FXML
    private ListView listBackgrounds;
    @FXML
    private ListView listCamera;
    @FXML
    private Slider sliderLowerHue;
    @FXML
    private Slider sliderLowerSaturation;
    @FXML
    private Slider sliderLowerValue;
    @FXML
    private Slider sliderUpperHue;
    @FXML
    private Slider sliderUpperSaturation;
    @FXML
    private Slider sliderUpperValue;
    @FXML
    private TextField textboxFrameHeight;
    @FXML
    private TextField textboxFrameWidth;
    @FXML
    private Button buttonSaveConfig;
    private final Configuration configuration;

    public ConfigController() {
        // Called before @FXML annotations are injected.
        configuration = Configuration.getInstance();
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Called after @FXML annotations are injected.

        // Let's initialize controller components and show previous configuration if it exists.
        // First we need to populate the Backgrounds list with available background images.
        try {
            ObservableList<String> backgroundFiles = getBackgroundFiles(
                    (String)ConfigKey.BACKGROUND_DIR.getDefault().getValue());
            listBackgrounds.setItems(backgroundFiles);
            if (configuration.getConfig(ConfigKey.BACKGROUND) != null) {
                int index = backgroundFiles.indexOf((String)configuration.getConfig(ConfigKey.BACKGROUND).getValue());
                if (index != -1) {
                    listBackgrounds.getSelectionModel().select(index);
                } else {
                    System.err.println("Failed to set previous configuration option for backgrounds list view.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            System.err.println("Failed to find backgrounds directory.");
        }

        // Next we need to populate the Camera's list with available cameras.
        ObservableList<Integer> cameras = getCameras();
        listCamera.setItems(getCameras());
        if( configuration.getConfig(ConfigKey.CAMERA) != null ) {
            int index = cameras.indexOf((Integer)configuration.getConfig(ConfigKey.CAMERA).getValue());
            if (index != -1) {
                listCamera.getSelectionModel().select(index);
            } else {
                System.err.println("Failed to set previous configuration option for camera list view.");
            }
        }

        // Now set the Lower Bounds for the HSV color to extract
        if( configuration.getConfig(ConfigKey.LOWER_HUE) != null ) {
            sliderLowerHue.setValue((int)configuration.getConfig(ConfigKey.LOWER_HUE).getValue());
        }
        if( configuration.getConfig(ConfigKey.LOWER_SATURATION) != null ) {
            sliderLowerSaturation.setValue((int)configuration.getConfig(ConfigKey.LOWER_SATURATION).getValue());
        }
        if( configuration.getConfig(ConfigKey.LOWER_VALUE) != null ) {
            sliderLowerValue.setValue((int)configuration.getConfig(ConfigKey.LOWER_VALUE).getValue());
        }

        // Now set the Upper Bounds for the HSV color to extract
        if( configuration.getConfig(ConfigKey.UPPER_HUE) != null ) {
            sliderUpperHue.setValue((int)configuration.getConfig(ConfigKey.UPPER_HUE).getValue());
        }
        if( configuration.getConfig(ConfigKey.UPPER_SATURATION) != null ) {
            sliderUpperSaturation.setValue((int)configuration.getConfig(ConfigKey.UPPER_SATURATION).getValue());
        }
        if( configuration.getConfig(ConfigKey.UPPER_VALUE) != null ) {
            sliderUpperValue.setValue((int)configuration.getConfig(ConfigKey.UPPER_VALUE).getValue());
        }

        // Last lets set the Frame Height and Width values.
        if( configuration.getConfig(ConfigKey.FRAME_HEIGHT) != null ) {
            textboxFrameHeight.setText(configuration.getConfig(ConfigKey.FRAME_HEIGHT).getValue().toString());
        }
        if( configuration.getConfig(ConfigKey.FRAME_WIDTH) != null ) {
            textboxFrameWidth.setText(configuration.getConfig(ConfigKey.FRAME_WIDTH).getValue().toString());
        }
    }

    private static ObservableList<Integer> getCameras() {
        ObservableList<Integer> cameras = FXCollections.observableArrayList();
        // Create a VideoCapture object to capture video from the webcam
        VideoCapture capture = new VideoCapture();
        for(int cameraIndex=0; cameraIndex<4; cameraIndex++) {  // Only checking for the first 4 cameras slots.
            if(capture.open(cameraIndex)) {
                capture.release();
                cameras.add(cameraIndex);
            }
        }
        return cameras;
    }

    private static ObservableList<String> getBackgroundFiles(String directoryName) throws IOException, URISyntaxException {
        // Get the resources directory path
        Path resourcesPath = Paths.get(ClassLoader.getSystemResource(directoryName).toURI());

        // Check if the directory exists
        if (!Files.isDirectory(resourcesPath)) {
            throw new IOException("Directory not found: " + resourcesPath);
        }

        // Stream the directory entries and filter for files only
        return Files.list(resourcesPath)
                .filter(file -> Files.isRegularFile(file))
                .map(file -> file.getFileName().toString()) // Get file names only
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    @FXML
    private void handleSaveConfigButtonClick(ActionEvent event) {
        // Let's Update our configuration with all the values we just saved.
        configuration.addConfig(ConfigKey.BACKGROUND,
                new ConfigItem<String>(listBackgrounds.getSelectionModel().getSelectedItem().toString()));
        configuration.addConfig(ConfigKey.CAMERA,
                new ConfigItem<Integer>((int)listCamera.getSelectionModel().getSelectedItem()));
        configuration.addConfig(ConfigKey.LOWER_HUE,
                new ConfigItem<Integer>((int)sliderLowerHue.getValue()));
        configuration.addConfig(ConfigKey.LOWER_SATURATION,
                new ConfigItem<Integer>((int)sliderLowerSaturation.getValue()));
        configuration.addConfig(ConfigKey.LOWER_VALUE,
                new ConfigItem<Integer>((int)sliderLowerValue.getValue()));
        configuration.addConfig(ConfigKey.UPPER_HUE,
                new ConfigItem<Integer>((int)sliderUpperHue.getValue()));
        configuration.addConfig(ConfigKey.UPPER_SATURATION,
                new ConfigItem<Integer>((int)sliderUpperSaturation.getValue()));
        configuration.addConfig(ConfigKey.UPPER_VALUE,
                new ConfigItem<Integer>((int)sliderUpperValue.getValue()));
        configuration.addConfig(ConfigKey.FRAME_HEIGHT,
                new ConfigItem<Integer>(Integer.parseInt(textboxFrameHeight.getText())));
        configuration.addConfig(ConfigKey.FRAME_WIDTH,
                new ConfigItem<Integer>(Integer.parseInt(textboxFrameWidth.getText())));
    }
}

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.WindowEvent;
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
    @FXML
    Label labelLowerHUE;
    @FXML
    Label labelLowerSAT;
    @FXML
    Label labelLowerVAL;
    @FXML
    Label labelUpperHUE;
    @FXML
    Label labelUpperSAT;
    @FXML
    Label labelUpperVAL;
    private final Configuration configuration;
    private final EventBus eventBus;

    public ConfigController() {
        // Called before @FXML annotations are injected.
        eventBus = EventBus.getInstance();
        configuration = Configuration.getInstance();
        eventBus.register(WindowEvent.WINDOW_CLOSE_REQUEST.getName(), (eventData) -> {
            // Acknowledge parent close request that we closed down.
            eventBus.fireEvent("CHILD_CLOSE_ACK",
                    new EventBusData<Object>("CHILD_CLOSE_ACK", "ConfigController"));
        });
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
        int hue = (configuration.getConfig(ConfigKey.LOWER_HUE) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_HUE).getValue() :
                (int)ConfigKey.LOWER_HUE.getDefault().getValue();
        int sat = (configuration.getConfig(ConfigKey.LOWER_SATURATION) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_SATURATION).getValue() :
                (int)ConfigKey.LOWER_SATURATION.getDefault().getValue();
        int val = (configuration.getConfig(ConfigKey.LOWER_VALUE) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_VALUE).getValue() :
                (int)ConfigKey.LOWER_VALUE.getDefault().getValue();
        labelLowerHUE.setText("HUE " + hue);
        // Set Lower hue slider
        sliderLowerHue.setValue(hue);
        sliderLowerHue.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerHue.setValue(roundedValue);
            labelLowerHUE.setText("HUE " + (int)roundedValue);
        });
        labelLowerSAT.setText("SAT " + sat);
        // Set Lower saturation slider
        sliderLowerSaturation.setValue(sat);
        sliderLowerSaturation.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerSaturation.setValue(roundedValue);
            labelLowerSAT.setText("SAT " + (int)roundedValue);
        });
        labelLowerVAL.setText("VAL " + val);
        // Set Lower value slider
        sliderLowerValue.setValue(val);
        sliderLowerValue.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerValue.setValue(roundedValue);
            labelLowerVAL.setText("VAL " + (int)roundedValue);
        });

        // Now set the Upper Bounds for the HSV color to extract
        hue = (configuration.getConfig(ConfigKey.UPPER_HUE) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_HUE).getValue() :
                (int)ConfigKey.UPPER_HUE.getDefault().getValue();
        sat = (configuration.getConfig(ConfigKey.UPPER_SATURATION) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_SATURATION).getValue() :
                (int)ConfigKey.UPPER_SATURATION.getDefault().getValue();
        val = (configuration.getConfig(ConfigKey.UPPER_VALUE) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_VALUE).getValue() :
                (int)ConfigKey.UPPER_VALUE.getDefault().getValue();
        labelUpperHUE.setText("HUE " + hue);
        // Set Upper hue slider
        sliderUpperHue.setValue(hue);
        sliderUpperHue.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperHue.setValue(roundedValue);
            labelUpperHUE.setText("HUE " + (int)roundedValue);
        });
        labelUpperSAT.setText("SAT " + sat);
        // Set Upper saturation slider
        sliderUpperSaturation.setValue(sat);
        sliderUpperSaturation.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperSaturation.setValue(roundedValue);
            labelUpperSAT.setText("SAT " + (int)roundedValue);
        });
        labelUpperVAL.setText("VAL " + val);
        // Set Upper value slider
        sliderUpperValue.setValue(val);
        sliderUpperValue.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperValue.setValue(roundedValue);
            labelUpperVAL.setText("VAL " + (int)roundedValue);
        });

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

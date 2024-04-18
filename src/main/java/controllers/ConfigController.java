package controllers;

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
import java.util.stream.Collectors;

import javafx.fxml.Initializable;
import services.communication.EventBus;
import services.communication.EventBusData;
import services.configuration.ConfigItem;
import services.configuration.ConfigKey;
import services.configuration.Configuration;
import utilities.Utils;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigController implements Initializable {
    // Internal Enum used for configuration HSV Labels, so I only have to change the name 1 time.
    private enum HSVLabels {
        HUE("HUE"),
        SATURATION("SAT"),
        VALUE("VAL");

        private final String label;
        HSVLabels(String label) {
            this.label = label;
        }

        public String getLabel(int value) {
            return "%s %d".formatted(label, value);
        }
    }

    @FXML
    private ListView<String> listBackgrounds;
    @FXML
    private ListView<Integer> listCamera;
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
    @FXML
    Label labelFrameWidth;
    @FXML
    Label labelFrameHeight;
    private final Configuration configuration;
    private final EventBus eventBus;

    public ConfigController() {
        // Called before @FXML annotations are injected.
        eventBus = EventBus.getInstance();
        configuration = Configuration.getInstance();
        eventBus.register(WindowEvent.WINDOW_CLOSE_REQUEST.getName(), (ignoredEventData) -> {
            // Acknowledge parent close request that we closed down.
            eventBus.fireEvent("CHILD_CLOSE_ACK",
                    new EventBusData<Object>("CHILD_CLOSE_ACK", "controllers.ConfigController"));
        });
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Called after @FXML annotations are injected.

        // Let's initialize controller components and show previous configuration if it exists.
        // First Let's set the labels for Frame Width and Frame Height settings.
        labelFrameWidth.setText(ConfigKey.FRAME_WIDTH.getLabel());
        labelFrameHeight.setText(ConfigKey.FRAME_HEIGHT.getLabel());
        // Second we need to populate the Backgrounds list with available background images.
        try {
            ObservableList<String> backgroundFiles = getBackgroundFiles(
                    (String) ConfigKey.BACKGROUND_DIR.getDefault().value());
            listBackgrounds.setItems(backgroundFiles);
            if (configuration.getConfig(ConfigKey.BACKGROUND) != null) {
                int index = backgroundFiles.indexOf((String)configuration.getConfig(ConfigKey.BACKGROUND).value());
                if (index != -1) {
                    listBackgrounds.getSelectionModel().select(index);
                } else {
                    Utils.handleError( "Failed to set previous configuration option for backgrounds list view.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            Utils.handleError("Failed to find backgrounds directory.");
        }

        // Next we need to populate the Camera's list with available cameras.
        ObservableList<Integer> cameras = getCameras();
        listCamera.setItems(getCameras());
        if( configuration.getConfig(ConfigKey.CAMERA) != null ) {
            int index = cameras.indexOf((Integer)configuration.getConfig(ConfigKey.CAMERA).value());
            if (index != -1) {
                listCamera.getSelectionModel().select(index);
            } else {
                Utils.handleError("Failed to set previous configuration option for camera list view.");
            }
        }

        // Now set the Lower Bounds for the HSV color to extract
        int hue = (configuration.getConfig(ConfigKey.LOWER_HUE) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_HUE).value() :
                (int)ConfigKey.LOWER_HUE.getDefault().value();
        int sat = (configuration.getConfig(ConfigKey.LOWER_SATURATION) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_SATURATION).value() :
                (int)ConfigKey.LOWER_SATURATION.getDefault().value();
        int val = (configuration.getConfig(ConfigKey.LOWER_VALUE) != null) ?
                (int)configuration.getConfig(ConfigKey.LOWER_VALUE).value() :
                (int)ConfigKey.LOWER_VALUE.getDefault().value();
        labelLowerHUE.setText(HSVLabels.HUE.getLabel(hue));
        // Set Lower hue slider
        sliderLowerHue.setValue(hue);
        sliderLowerHue.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerHue.setValue(roundedValue);
            labelLowerHUE.setText(HSVLabels.HUE.getLabel((int)roundedValue));
        });
        labelLowerSAT.setText(HSVLabels.SATURATION.getLabel(sat));
        // Set Lower saturation slider
        sliderLowerSaturation.setValue(sat);
        sliderLowerSaturation.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerSaturation.setValue(roundedValue);
            labelLowerSAT.setText(HSVLabels.SATURATION.getLabel((int)roundedValue));
        });
        labelLowerVAL.setText(HSVLabels.VALUE.getLabel(val));
        // Set Lower value slider
        sliderLowerValue.setValue(val);
        sliderLowerValue.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderLowerValue.setValue(roundedValue);
            labelLowerVAL.setText(HSVLabels.VALUE.getLabel((int)roundedValue));
        });

        // Now set the Upper Bounds for the HSV color to extract
        hue = (configuration.getConfig(ConfigKey.UPPER_HUE) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_HUE).value() :
                (int)ConfigKey.UPPER_HUE.getDefault().value();
        sat = (configuration.getConfig(ConfigKey.UPPER_SATURATION) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_SATURATION).value() :
                (int)ConfigKey.UPPER_SATURATION.getDefault().value();
        val = (configuration.getConfig(ConfigKey.UPPER_VALUE) != null) ?
                (int)configuration.getConfig(ConfigKey.UPPER_VALUE).value() :
                (int)ConfigKey.UPPER_VALUE.getDefault().value();
        labelUpperHUE.setText(HSVLabels.HUE.getLabel(hue));
        // Set Upper hue slider
        sliderUpperHue.setValue(hue);
        sliderUpperHue.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperHue.setValue(roundedValue);
            labelUpperHUE.setText(HSVLabels.HUE.getLabel((int)roundedValue));
        });
        labelUpperSAT.setText(HSVLabels.SATURATION.getLabel(sat));
        // Set Upper saturation slider
        sliderUpperSaturation.setValue(sat);
        sliderUpperSaturation.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperSaturation.setValue(roundedValue);
            labelUpperSAT.setText(HSVLabels.SATURATION.getLabel((int)roundedValue));
        });
        labelUpperVAL.setText(HSVLabels.VALUE.getLabel(val));
        // Set Upper value slider
        sliderUpperValue.setValue(val);
        sliderUpperValue.valueProperty().addListener((ignoredObservable, ignoredOldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue());
            sliderUpperValue.setValue(roundedValue);
            labelUpperVAL.setText(HSVLabels.VALUE.getLabel((int)roundedValue));
        });

        // Last lets set the Frame Height and Width values.
        if( configuration.getConfig(ConfigKey.FRAME_HEIGHT) != null ) {
            textboxFrameHeight.setText(configuration.getConfig(ConfigKey.FRAME_HEIGHT).value().toString());
        }
        if( configuration.getConfig(ConfigKey.FRAME_WIDTH) != null ) {
            textboxFrameWidth.setText(configuration.getConfig(ConfigKey.FRAME_WIDTH).value().toString());
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
        // Get the resources directory
        Path resourcesPath = Utils.getRootResource(directoryName);

        // Check if the directory exists
        if (resourcesPath == null || !Files.isDirectory(resourcesPath)) {
            throw new IOException("Directory not found: %s".formatted(resourcesPath));
        }

        // Stream the directory entries and filter for files only
        return Files.list(resourcesPath)
                .filter(Files::isRegularFile)
                .map(file -> file.getFileName().toString()) // Get file names only
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    @FXML
    private void handleSaveConfigButtonClick(ActionEvent ignoredEvent) {
        // Let's Update our configuration with all the values we just saved.
        configuration.addConfig(ConfigKey.BACKGROUND,
                new ConfigItem<>(listBackgrounds.getSelectionModel().getSelectedItem()));
        configuration.addConfig(ConfigKey.CAMERA,
                new ConfigItem<>(listCamera.getSelectionModel().getSelectedItem()));
        configuration.addConfig(ConfigKey.LOWER_HUE,
                new ConfigItem<>((int)sliderLowerHue.getValue()));
        configuration.addConfig(ConfigKey.LOWER_SATURATION,
                new ConfigItem<>((int)sliderLowerSaturation.getValue()));
        configuration.addConfig(ConfigKey.LOWER_VALUE,
                new ConfigItem<>((int)sliderLowerValue.getValue()));
        configuration.addConfig(ConfigKey.UPPER_HUE,
                new ConfigItem<>((int)sliderUpperHue.getValue()));
        configuration.addConfig(ConfigKey.UPPER_SATURATION,
                new ConfigItem<>((int)sliderUpperSaturation.getValue()));
        configuration.addConfig(ConfigKey.UPPER_VALUE,
                new ConfigItem<>((int)sliderUpperValue.getValue()));
        configuration.addConfig(ConfigKey.FRAME_HEIGHT,
                new ConfigItem<>(Integer.parseInt(textboxFrameHeight.getText())));
        configuration.addConfig(ConfigKey.FRAME_WIDTH,
                new ConfigItem<>(Integer.parseInt(textboxFrameWidth.getText())));
    }
}
package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvException;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import services.communication.EventBus;
import services.communication.EventBusData;
import services.configuration.ConfigKey;
import services.configuration.Configuration;
import utilities.MatBuffer;
import utilities.ImageBuffer;
import utilities.Utils;
import java.net.URL;
import java.util.ResourceBundle;

public class ImageViewController  implements Initializable, EventBus.EventListener {
    @FXML
    private ImageView imageView;
    private final VideoCapture capture;
    private final Configuration configuration;
    private final EventBus eventBus;
    private Thread cameraThread;
    private boolean threadStopFlag;
    private MatBuffer matBuffer;
    private ImageBuffer imageBuffer;
    private Mat replacementMat;
    private int cameraIndex;
    private final Size desiredSize;
    public ImageViewController() {
        eventBus = EventBus.getInstance();
        configuration = Configuration.getInstance();
        capture = new VideoCapture();
        if(configuration.getConfig(ConfigKey.CAMERA) != null) {
            cameraIndex = (int)configuration.getConfig(ConfigKey.CAMERA).getValue();
        }
        desiredSize = new Size(
                (int)configuration.getConfig(ConfigKey.FRAME_WIDTH).getValue(),
                (int)configuration.getConfig(ConfigKey.FRAME_HEIGHT).getValue()
        );
        threadStopFlag = false;
    }

    private void captureReset() {
        capture.release();
        capture.open(cameraIndex);
        // Set the resolution of the captured video
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, desiredSize.width);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, desiredSize.height);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        captureReset();

        if (!capture.isOpened()) {
            Utils.handleError("Unable to open configured webcam.");
        }

        String background = (String)configuration.getConfig(ConfigKey.BACKGROUND).getValue();
        // Initialize the Mat buffer for double buffering frame results.
        matBuffer = new MatBuffer();
        // Initialize the image buffer, which is my attempt to double buffer.
        imageBuffer = new ImageBuffer();
        // Load the replacement background image.
        replacementMat = Utils.loadImage(background, desiredSize);
        // Launch processing and displaying frames in a separate thread
        cameraThread = new Thread(this::processAndDisplayFrames);
        cameraThread.start();
        // Listen for application close events, so we can close down the thread politely
        eventBus.register(WindowEvent.WINDOW_CLOSE_REQUEST.getName(), this);
        eventBus.register("SCENE_SWAP_REQUEST", this);
    }

    @Override
    public void onEvent(EventBusData<?> eventData) {
        String data = "";
        if(eventData.getEventData() != null) { data = eventData.getEventData().toString(); }

        if(eventData.getType().equals("SCENE_SWAP_REQUEST")) {
            //System.out.println(data);
            if (!data.equals("imageView.fxml")) { // Only halt if not my own view being changed to.
                // If I'm being asked to halt, it's likely a new object will later be created, so I also need to unregister
                // in the services.communication.EventBus
                eventBus.unregister("SCENE_SWAP_REQUEST", this);
                eventBus.unregister(WindowEvent.WINDOW_CLOSE_REQUEST.getName(), this);
                haltThread();
            }
        } else {
            // If I'm here, I don't unregister because I assume this is the only other event I'm registered to listen
            // for (i.e. Window Close/Application Shutdown).
            haltThread();
        }
    }

    private void haltThread() {
        // Release resources when the window is closed
        threadStopFlag = true;
        capture.release();
        try {
            cameraThread.interrupt(); // This is likely unnecessary but putting this here anyway.
            cameraThread.join(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Acknowledge parent close request that we closed down.
        eventBus.fireEvent("CHILD_CLOSE_ACK",
                new EventBusData<Object>("CHILD_CLOSE_ACK", "ImageController"));
    }

    @SuppressWarnings("BusyWait")
    private void processAndDisplayFrames() {
        //System.out.println("Starting " + Thread.currentThread().getName());

        Mat frame = new Mat();
        Mat hsvFrame = new Mat();
        Mat mask = new Mat();
        Mat maskI = new Mat();
        Mat result;

        // Define lower and upper bounds for green color in HSV (Hue, Saturation, Value)
        int hue = (int)configuration.getConfig(ConfigKey.LOWER_HUE).getValue();
        int sat = (int)configuration.getConfig(ConfigKey.LOWER_SATURATION).getValue();
        int val = (int)configuration.getConfig(ConfigKey.LOWER_VALUE).getValue();
        //System.out.printf("L-H:%d,S:%d,V:%d%n", hue, sat, val);
        Scalar lowerBound = new Scalar(hue, sat, val);  // Lower Green
        hue = (int)configuration.getConfig(ConfigKey.UPPER_HUE).getValue();
        sat = (int)configuration.getConfig(ConfigKey.UPPER_SATURATION).getValue();
        val = (int)configuration.getConfig(ConfigKey.UPPER_VALUE).getValue();
        //System.out.printf("U-H:%d,S:%d,V:%d%n", hue, sat, val);
        Scalar upperBound = new Scalar(hue, sat, val);  // Upper Green
        boolean sizingErrorReported = false;

        while(!threadStopFlag) {
            // Check for interrupt periodically
            if (Thread.interrupted()) {
                System.err.println("Thread Interrupted.");
                break;
            }

            // Capture a frame from the webcam
            capture.read(frame);
            //capture.retrieve(frame); // Takes a single snapshot (-:

            if(frame.empty()) {
                System.err.println("Failed to read frame.");
                // Try to release and re-open capture
                captureReset();
                continue;
            }

            Size actualSize = frame.size();
            if (!actualSize.equals(desiredSize)) {  // If frame size is not the size I desire, resize it.
                if(!sizingErrorReported) { // Only report this error once in the loop.
                    System.err.printf("""
                    Resizing frame from %s to %s
                    Consider setting frame width and height to size supported by camera.
                    """, actualSize, desiredSize);
                    sizingErrorReported = true;
                }
                Imgproc.resize(frame, frame, desiredSize);
            }

            // Convert the frame from BGR to HSV color space
            //hsvFrame.release(); // Doesn't appear necessary
            Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);

            // Create a mask based on the specified color range
            //mask.release(); // Doesn't appear necessary
            Core.inRange(hsvFrame, lowerBound, upperBound, mask);

            // Invert the mask (to select everything except the specified color)
            Core.bitwise_not(mask, maskI);

            // Buffered previous result so that we get rid of motion artifacts.
            // @TODO: is utilities.MatBuffer better than release? Find out!
            //result.release();
            result = matBuffer.getNextFrame();
            // Apply the inverted mask to the original frame
            Core.bitwise_and(frame, frame, result, maskI);
            Mat currentResult = matBuffer.swap(result);

            // Apply the replacement image to the original frame where the mask is applied
            try {
                Core.bitwise_or(replacementMat, replacementMat, currentResult, mask);
            } catch(CvException cve) {
                System.err.println("OpenCV native error in mat replacement.");
                //cve.printStackTrace();
            }
            //currentResult.copyTo(replacementMat, mask);

            // Convert the modified frame to a JavaFX Image
            Image imageToShow = Utils.mat2Image(currentResult);

            // Update the image buffer with the new image
            imageBuffer.updateImage(imageToShow);

            // Update the ImageView on the JavaFX application thread
            Platform.runLater(() -> imageView.setImage(imageBuffer.getImage()));

            // Sleep for a short duration to achieve a target frame rate (e.g., 30 frames per second)
            try {
                Thread.sleep(29); // Sleep for approximately 33 milliseconds (1000 / 30) ~ (33-4)ms for fun
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.err.println("Camera thread interrupted.");
                break;
            }
        }
        //System.out.println("Stopping " + Thread.currentThread().getName());
    }
}
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

import java.net.URL;
import java.util.ResourceBundle;

public class ImageViewController  implements Initializable, EventBus.EventListener {
    @FXML
    private ImageView imageView;
    private VideoCapture capture;
    private final Configuration configuration;
    private final EventBus eventBus;
    private Thread cameraThread;
    private boolean threadStopFlag;
    private MatBuffer matBuffer;
    private ImageBuffer imageBuffer;
    private Mat replacementMat;
    private int cameraIndex;
    private Size desiredSize;
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
        // @TODO: Fix multiple registration issue for this controller!!!
        eventBus.register(WindowEvent.WINDOW_CLOSE_REQUEST.getName(), (eventData) -> {
            haltThread();
        });
        eventBus.register("SCENE_SWAP_REQUEST", this);
    }

    @Override
    public void onEvent(EventBusData eventData) {
        System.out.println(eventData);
        if(eventData.getType().equals("SCENE_SWAP_REQUEST")) {
            if (!eventData.equals("imageView.fxml")) { // Only halt if not my own view being changed to.
                haltThread();
            }
        } else {
            haltThread();
        }
    }

    private void haltThread() {
        // If I'm being asked to halt, its likely a new object will later be created, so I also need to unregister
        // in the EventBus
        eventBus.unregister("SCENE_SWAP_REQUEST", this);
        // Release resources when the window is closed
        threadStopFlag = true;
        capture.release();
        try {
            cameraThread.join(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processAndDisplayFrames() {
        Mat frame = new Mat();
        Mat hsvFrame = new Mat();
        Mat mask = new Mat();
        Mat maskI = new Mat();
        Mat result;

        // Define lower and upper bounds for green color in HSV (Hue, Saturation, Value)
        // @TODO: Set the lower and upper bound with the configuration Singleton.
        Scalar lowerBound = new Scalar(40, 40, 40);  // Lower Green
        Scalar upperBound = new Scalar(80, 255, 255); // Upper Green
        boolean sizingErrorReported = false;

        while(!threadStopFlag) {
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
            // @TODO: is MatBuffer better than release? Find out!
            //result.release();
            result = matBuffer.getNextFrame();
            // Apply the inverted mask to the original frame
            Core.bitwise_and(frame, frame, result, maskI);
            Mat currentResult = matBuffer.swap(result);

            // Apply the replacement image to the original frame where the mask is applied
            // @TODO: Figure out replacementMat bug where this line is crashing OpenCV's
            //   native method, but didn't use to. Maybe has to do with no green in image?
            try {
                Core.bitwise_or(replacementMat, replacementMat, currentResult, mask);
            } catch(CvException cve) {
                cve.printStackTrace();
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
            }
        }
    }
}

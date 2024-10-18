package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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
// Imports for image Capture
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingFXUtils;
import java.io.FileWriter;
// QR Code Imports
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
// Countdown Imports
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.concurrent.CountDownLatch;
import javafx.concurrent.Task;

// @TODO: Add textbox to imageView.fxml and add Capture/Send button to take a snapshot and store/e-mail it to address in
//   textbox!
public class ImageViewController  implements Initializable, EventBus.EventListener {
    @FXML
    private AnchorPane anchorpaneParent;
    @FXML
    private BorderPane borderpaneParent;
    @FXML
    private ImageView imageView;
    @FXML
    private ImageView ivQRCode;
    @FXML
    private TextField txtEmail;
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
            cameraIndex = (int)configuration.getConfig(ConfigKey.CAMERA).value();
        }
        desiredSize = new Size(
                (int)configuration.getConfig(ConfigKey.FRAME_WIDTH).value(),
                (int)configuration.getConfig(ConfigKey.FRAME_HEIGHT).value()
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

    /*
    private void onSceneSet(Scene scene) {
        // This method is called when the Scene is fully set up
        scene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
            if (newWindow != null) {
                System.out.println("Scene width: " + newWindow.getWidth());
                anchorpaneParent.prefWidthProperty().bind(newWindow.widthProperty());
                anchorpaneParent.prefWidthProperty().bind(newWindow.heightProperty());
                anchorpaneParent.setPrefWidth(newWindow.getWidth());
                anchorpaneParent.setPrefHeight(newWindow.getHeight());
                imageView.fitWidthProperty().bind(borderpaneParent.widthProperty());
                imageView.fitHeightProperty().bind(borderpaneParent.heightProperty());
            }
        });
    }
    */

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /*
        // Let's listen for when the Scene becomes available, so we can resize appropriately.
        anchorpaneParent.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if( newScene != null ) {
                onSceneSet(newScene);
            }
        });
         */
        captureReset();

        if (!capture.isOpened()) {
            Utils.handleError("Unable to open configured webcam.");
        }

        String background = (String)configuration.getConfig(ConfigKey.BACKGROUND).value();
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
        if(eventData.eventData() != null) { data = eventData.eventData().toString(); }

        if(eventData.type().equals("SCENE_SWAP_REQUEST")) {
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
        int hue = (int)configuration.getConfig(ConfigKey.LOWER_HUE).value();
        int sat = (int)configuration.getConfig(ConfigKey.LOWER_SATURATION).value();
        int val = (int)configuration.getConfig(ConfigKey.LOWER_VALUE).value();
        //System.out.printf("L-H:%d,S:%d,V:%d%n", hue, sat, val);
        Scalar lowerBound = new Scalar(hue, sat, val);  // Lower Green
        hue = (int)configuration.getConfig(ConfigKey.UPPER_HUE).value();
        sat = (int)configuration.getConfig(ConfigKey.UPPER_SATURATION).value();
        val = (int)configuration.getConfig(ConfigKey.UPPER_VALUE).value();
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

    private String getFilePathHash() throws NoSuchAlgorithmException {
        String dateTime = LocalDateTime.now().toString();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(dateTime.getBytes(StandardCharsets.UTF_8));

        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            hashString.append(String.format("%02x", b));
        }

        return hashString.toString();
    }

    public static void appendToFile(String filePath, String content) {
        try (FileWriter writer = new FileWriter(filePath, true)) { // true for append mode
            writer.write(content);
            writer.write(System.lineSeparator()); // Adds a newline after the content
        } catch (IOException e) {
            System.out.println("An error occurred while appending to the file: " + e.getMessage());
        }
    }

    @FXML
    private void btnCaptureClick(ActionEvent ignoredEvent) {
        CountDownLatch latch = showCountdownPopup();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Wait until the latch is released (i.e., the video finishes)
                latch.await();
                // Resume main thread actions
                Platform.runLater(() -> { // Schedule capture to run on the JFX App, necessary for a ImageView snapshot
                    captureImage();
                });
                return null;
            }
        };

        new Thread(task).start();
    }

    private CountDownLatch showCountdownPopup() {
        // Create a CountDownLatch to pause the execution until the video finishes
        CountDownLatch latch = new CountDownLatch(1);

        // Create a new stage for the video popup
        Stage videoStage = new Stage();
        videoStage.initModality(Modality.WINDOW_MODAL);

        // Load the video file
        File videoFile = Utils.getRootResource("countdown.mp4").toFile();
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        // Create the video popup scene
        StackPane videoPane = new StackPane(mediaView);
        Scene videoScene = new Scene(videoPane, 480, 480);
        videoStage.setScene(videoScene);

        // Show the video popup and play the video
        mediaPlayer.play();
        videoStage.show();

        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.stop();
            videoStage.close();
            latch.countDown();
        });
        return latch;
    }

    private void captureImage() {
        System.out.println("1");
        // Capture the ImageView content into a WritableImage
        WritableImage writableImage = new WritableImage((int) imageView.getFitWidth(), (int) imageView.getFitHeight());
        System.out.println("2");

        // Snapshot the ImageView
        imageView.snapshot(null, writableImage);
        System.out.println("3");

        // Convert WritableImage to BufferedImage and save as PNG
        try {
            System.out.println("4");
            String dirPath = "/Users/trevorhartman/CR/thartmanoftheredwoods.mkdocs/docs";
            String fileHash = String.format("chroma_images/%s.png", getFilePathHash());
            String filePath = String.format("%s/%s", dirPath, fileHash);
            String url = String.format("https://thartmanoftheredwoods.github.io/%s", fileHash);
            File file = new File(filePath);
            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
            appendToFile(String.format("%s/images.txt", dirPath), fileHash);
            txtEmail.setText(url);
            // Put up QR Code
            Image qrCodeImage = generateQRCodeImage(url, 150, 150);
            ivQRCode.setImage(qrCodeImage);
        } catch (NoSuchAlgorithmException nsa) {
            System.out.println("Failed to get md5 hash");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Image generateQRCodeImage(String data, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

}
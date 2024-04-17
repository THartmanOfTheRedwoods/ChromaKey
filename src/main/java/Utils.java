import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayInputStream;
import java.io.File;

public class Utils {

    public static Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public static Mat loadImage(String path, Size size) {
        // All backgrounds are in the resources/backgrounds directory.
        String background = ConfigKey.BACKGROUND_DIR.getDefault().getValue() + "/" + path;
        // System.out.printf("%s%n%s%n%s%n", "~".repeat(80), background, "~".repeat(80));
        String fullPath = Utils.class.getResource(background).getPath();
        File file = new File(fullPath);
        if(!file.exists()) {
            handleError("That file does not exist.");
            return null;
        }
        Mat mat = Imgcodecs.imread(file.getAbsolutePath());
        Imgproc.resize(mat, mat, size);

        return mat;
    }

    public static void handleError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR); // Set alert type to ERROR
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred!");
        alert.setContentText(errorMessage);

        alert.showAndWait(); // Makes the dialog modal
    }
}

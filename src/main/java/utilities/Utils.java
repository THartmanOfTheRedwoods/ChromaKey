package utilities;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import services.configuration.ConfigKey;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("StringTemplateMigration")
public class Utils {

    public static Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public static Path getRootResource(String resourceName) {
        //return Utils.class.getClassLoader().getResource(resourceName);
        try {
            return Paths.get(ClassLoader.getSystemResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            handleError("Failed to find system resource.");
        }
        return null;
    }

    public static Mat loadImage(String path, Size size) {
        // All backgrounds are in the resources/backgrounds directory.
        Path background = getRootResource(ConfigKey.BACKGROUND_DIR.getDefault().value() + "/" + path);
        //System.out.printf("%s%n%s%n%s%n", "~".repeat(80), background, "~".repeat(80));
        if(background == null) {return null;}  // Handle case where the directory can't be located.
        File file = background.toFile();
        if(!file.exists()) {  // handle case where file may not actually exist.
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
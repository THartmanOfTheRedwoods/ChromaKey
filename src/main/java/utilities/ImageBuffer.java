package utilities;

import javafx.scene.image.Image;

public class ImageBuffer {
    private Image currentImage;
    private Image nextImage;

    public synchronized void updateImage(Image newImage) {
        nextImage = newImage;
    }

    public synchronized Image getImage() {
        if (nextImage != null) {
            currentImage = nextImage;
            nextImage = null;
        }
        return currentImage;
    }
}
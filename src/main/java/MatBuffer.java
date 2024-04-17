import org.opencv.core.Mat;

public class MatBuffer {
    private Mat currentFrame;
    private Mat nextFrame;

    public MatBuffer() {
        currentFrame = new Mat();
        nextFrame = new Mat();
    }

    public synchronized Mat swap(Mat newFrame) {
        // Swap the current frame and the next frame
        Mat temp = currentFrame;
        currentFrame = newFrame;
        nextFrame = temp;
        nextFrame.release(); // Clear previous contents.
        return currentFrame;
    }

    public Mat getNextFrame() {
        return nextFrame;
    }
}

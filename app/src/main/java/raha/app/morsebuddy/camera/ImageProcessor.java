package raha.app.morsebuddy.camera;

import androidx.camera.core.ImageProxy;

/**
 * Interface to deliver camera image-frames for processing.
 */
public interface ImageProcessor {
    /**
     * Populates resources and starts the underlying system
     */
    void startProcessing();

    /**
     * Processes ImageProxy image data, e.g. used for CameraX live preview case.
     */
    void processImage(ImageProxy image);

    /**
     * Stops the underlying system and release resources.
     */
    void stopProcessing();
}

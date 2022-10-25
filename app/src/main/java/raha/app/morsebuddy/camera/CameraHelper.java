package raha.app.morsebuddy.camera;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import raha.app.morsebuddy.util.Constants;

/**
 * Simplifies camera interactions.
 * Manages states of the supplied {@link ImageProcessor}.
 * Sends continuous image-frames as {@link ImageProxy} to the supplied {@link ImageProcessor}.
 */
public class CameraHelper {
    private static final String TAG = "CameraHelper";

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private CameraSelector cameraSelector;
    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private Preview previewUseCase;
    @Nullable
    private ImageAnalysis analysisUseCase;
    @Nullable
    private ImageProcessor imageProcessor;

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;

    public CameraHelper(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner, @NonNull PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    }

    /**
     * Sets the required {@link ProcessCameraProvider}.
     * Must be called before calling {@link CameraHelper#start()}, otherwise camera stream will not start.
     *
     * @param cameraProvider the {@link ProcessCameraProvider} to use
     */
    public void setCameraProvider(@Nullable ProcessCameraProvider cameraProvider) {
        this.cameraProvider = cameraProvider;
    }

    /**
     * Sets an optional {@link ImageProcessor}.
     * If set, each image-frame is supplied by calling {@link ImageProcessor#processImage(ImageProxy)} during stream.
     *
     * @param imageProcessor the {@link ImageProcessor} to use
     */
    public void setImageProcessor(@Nullable ImageProcessor imageProcessor) {
        this.imageProcessor = imageProcessor;
    }

    /**
     * Starts camera stream.
     * Have no effect if {@link ProcessCameraProvider} not set already.
     */
    public void start() {
        if (cameraProvider == null) {
            return;
        }
        bindAllCameraUseCases();
        if (imageProcessor != null) {
            imageProcessor.startProcessing();
        }
    }

    /**
     * Stops camera stream.
     */
    public void stop() {
        if (imageProcessor != null) {
            imageProcessor.stopProcessing();
        }
    }

    /**
     * Switch between front-facing and back-facing lenses.
     *
     * @throws CameraInfoUnavailableException if no camera available
     */
    public void switchLensFacing() throws CameraInfoUnavailableException {
        if (cameraProvider == null) {
            return;
        }
        int newLensFacing = lensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        CameraSelector newCameraSelector = new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
        if (cameraProvider.hasCamera(newCameraSelector)) {
            Log.d(TAG, "Set facing to " + newLensFacing);
            lensFacing = newLensFacing;
            cameraSelector = newCameraSelector;
            // Restart
            stop();
            start();
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            Size targetResolution = new Size(Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT);

            // Prepare preview use case
            if (previewUseCase != null) {
                cameraProvider.unbind(previewUseCase);
            }
            Preview.Builder previewBuilder = new Preview.Builder();
            previewBuilder.setTargetResolution(targetResolution);
            previewUseCase = previewBuilder.build();
            previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

            // Prepare analysis use case
            if (analysisUseCase != null) {
                cameraProvider.unbind(analysisUseCase);
            }
            if (imageProcessor != null) {
                imageProcessor.stopProcessing();
            }
            ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder();
            analysisBuilder.setTargetResolution(targetResolution);
            analysisUseCase = analysisBuilder.build();
            analysisUseCase.setAnalyzer(
                    // imageProcessor must use another thread to run the detection underneath,
                    // thus we can just run the analyzer itself on main thread.
                    ContextCompat.getMainExecutor(context),
                    imageProxy -> {
                        if (imageProcessor != null) {
                            imageProcessor.processImage(imageProxy);
                        }
                    });

            // Bind to use cases
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, analysisUseCase);
        }
    }
}

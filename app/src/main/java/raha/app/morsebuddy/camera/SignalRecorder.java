package raha.app.morsebuddy.camera;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import raha.app.morsebuddy.app.MorseBuddy;
import raha.app.morsebuddy.app.TaskExecutor;
import raha.app.morsebuddy.system.KMeansNormalizer;
import raha.app.morsebuddy.system.Translator;
import raha.app.morsebuddy.util.Counter;

/**
 * The recorder device that enables to start/stop recording of camera-stream, calculate the recorded sequence of {@link ImageSample} to generate a signal array.
 * Pass this signal array to {@link Translator} which processes and convert to Morse/Text.
 * <p>
 * Provides functionalities like changing sampling rate, adjusting baseline to optimize translation.
 * <p>
 * Max recording time is defined by {@link SignalRecorder#RECORDER_SESSION_DURATION} after which recording will automatically stopped.
 * However, user can stop recording at any time after recording is started.
 *
 * <b>Important:</b> Instances of this class should be managed by view models, since {@link Callback} methods might be invoked from background threads.
 */
public class SignalRecorder implements ImageProcessor {
    private static final String TAG = "SignalRecorder";
    private static final int RECORDER_SESSION_DURATION = 18;    // Seconds
    private static final int RENDER_GAP = 4;    // Frames

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    /* True means, continuous image-frame from CameraHelper is being processed. */
    private boolean imageProcessing;
    /* True means, a recorder session is running */
    private boolean recording;
    /* True means, cleanUp method was called, thus now in destroyed state. */
    private boolean cleanUpCalled;

    /* Current frame rate for recording */
    private int frameRate;
    /* Current baseline for signal translation */
    private int baseline;
    /* Number of total frame in a recorder session for the current frame rate; for example 20 seconds record @30fps means total of 20x30 frames */
    private int totalFrameCount;
    /* Number of recorded frame at any instant */
    private int recordedFrameCount;
    /* Contains the recorded frames in a complete session; represents the generated signal */
    private int[] frameArray;
    /* Helps track the submitted translation task; so that only the latest result is published */
    private int lastSubmitCode;

    private ImageSample sample;
    private Translator translator;
    private GraphRenderer renderer;
    private Callback callback;

    /* Render will not be called on each frame, it is used to make consecutive render calls */
    private final Counter renderGapCounter;

    public enum FrameRate {
        FPS_30,
        FPS_60
    }

    /**
     * Constructor
     *
     * @param frameRate initial frame rate
     */
    public SignalRecorder(@NonNull FrameRate frameRate) {
        imageProcessing = false;
        recording = false;
        cleanUpCalled = false;

        this.frameRate = frameRate == FrameRate.FPS_30 ? 30 : 60;
        this.baseline = 0;
        this.totalFrameCount = this.frameRate * SignalRecorder.RECORDER_SESSION_DURATION;
        this.recordedFrameCount = 0;
        this.frameArray = new int[this.totalFrameCount];
        this.lastSubmitCode = 0;

        this.sample = ImageSample.zeros();
        this.translator = new Translator(new KMeansNormalizer());
        this.renderer = null;
        this.callback = null;

        this.renderGapCounter = new Counter();
    }

    /**
     * Sets requested frame rate for future recordings.
     * No effect if currently recording.
     *
     * @param frameRate requested frame rate
     * @return true if frame rate was set, false otherwise
     */
    public boolean setFrameRate(FrameRate frameRate) {
        checkForDestroyedState();
        // Changing properties is not allowed when a recording is running
        if (!recording) {
            this.frameRate = frameRate == FrameRate.FPS_30 ? 30 : 60;
            this.totalFrameCount = this.frameRate * SignalRecorder.RECORDER_SESSION_DURATION;
            this.frameArray = new int[this.totalFrameCount];
            // Callback
            if (callback != null) {
                callback.onFrameRateChange(frameRate);
            }
            // Restart if currently processing
            if (imageProcessing) {
                stopProcessing();
                startProcessing();
            }
            Log.d(TAG, "frame rate changed, fps=" + frameRate);
            return true;
        } else {
            Log.e(TAG, "setFrameRate called while translation or recording is running.");
            return false;
        }
    }

    /**
     * Sets the current baseline for signal translation.
     * No effect if currently recording.
     *
     * @param baseline requested baseline
     * @return true if baseline was set, false otherwise
     */
    public boolean setBaseline(int baseline) {
        checkForDestroyedState();
        // Changing properties is not allowed when a recording is running
        if (!recording) {
            this.baseline = baseline;
            Log.d(TAG, "baseline changed, baseline=" + baseline);
            // Callback
            if (callback != null) {
                callback.onBaselineChange(baseline);
            }
            // No restart of image processing, since does not depend on baseline.
            // Now submit for re-translation
            submitTranslationTask(frameArray, baseline);
            return true;
        } else {
            Log.e(TAG, "setBaseline called while translation or recording is running.");
            return false;
        }
    }

    public void setRenderer(GraphRenderer renderer) {
        this.renderer = renderer;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public FrameRate getFrameRate() {
        return this.frameRate == 30 ? FrameRate.FPS_30 : FrameRate.FPS_60;
    }

    public int getBaseline() {
        return baseline;
    }

    /**
     * Starts a recording session.
     * Also, cancels any pending/running translation task.
     * No effect if currently recording.
     */
    public void startRecording() {
        checkForDestroyedState();
        if (recording) {
            return;
        }
        recording = true;
        // Start additional task
        if (renderer != null) {
            renderer.clear();
        }
        recordedFrameCount = 0;
        Arrays.fill(frameArray, 0);
        // Stop any ongoing translation
        cancelAllTranslations(true);

        // Callback
        if (callback != null) {
            callback.onRecordStart();
        }
        Log.d(TAG, "recording started.");
    }

    /**
     * Stops a recording session instantly.
     * Immediately, starts a translation task.
     * No effect if not recording.
     */
    public void stopRecording() {
        checkForDestroyedState();
        if (recording) {
            recording = false;
            // Callback
            if (callback != null) {
                callback.onRecordStop();
            }
            // End additional task
            submitTranslationTask(frameArray, baseline);
            Log.d(TAG, "recording stopped.");
        }
    }

    private void submitTranslationTask(int[] frameArray, int baseline) {
        // Cancel all previously posted tasks
        cancelAllTranslations(false);
        // Guarantees that onTranslationComplete/onTranslationCancel always called after onTranslationBegin
        // In this way, there might be multiple onTranslationBegin calls, but only one onTranslationComplete/onTranslationCancel call
        if (callback != null) {
            callback.onTranslationBegin();
        }
        // Post a new translation task
        Log.d(TAG, "translation submitted with submitCode=" + lastSubmitCode);
        MorseBuddy.getExecutor().execute(new OneShotTranslationTask(lastSubmitCode, frameArray, baseline, translator), new TaskExecutor.Callback<Translator.Result>() {
            @Override
            public void onStart() {
                // No task
            }

            @Override
            public void onComplete(Translator.Result result) {
                // If submitCode does not match, do not publish result.
                // Thus, we will always publish latest result.
                if (callback != null) {
                    if (result.getSubmitCode() == lastSubmitCode) {
                        callback.onTranslationComplete(result);
                    }
                }
            }

        });
    }

    private static class OneShotTranslationTask implements Callable<Translator.Result> {
        private final int submitCode;
        private final int[] givenArray;
        private final int baseline;
        private final Translator translator;

        public OneShotTranslationTask(int submitCode, int[] givenArray, int baseline, @NonNull Translator translator) {
            this.submitCode = submitCode;
            this.givenArray = Arrays.copyOf(givenArray, givenArray.length);
            this.baseline = baseline;
            this.translator = translator;
        }

        @Override
        public Translator.Result call() {
            // Perform blocking translation
            return translator.resolve(submitCode, givenArray, baseline);
        }
    }

    /**
     * Cancels all pending translations.
     * Also, result of any pending translation will not be published.
     */
    public void cancelAllTranslations() {
        cancelAllTranslations(true);
    }

    private void cancelAllTranslations(boolean notify) {
        // Just update the last submitCode, so last posted task's result will not publish.
        this.lastSubmitCode = ThreadLocalRandom.current().nextInt();
        if (notify && callback != null) {
            callback.onTranslationCancel();
        }
    }

    @Override
    public void startProcessing() {
        checkForDestroyedState();
        if (imageProcessing) {
            return;
        }
        imageProcessing = true;
        scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
                    // Calculation of contrast
                    final byte[] samplePixels = sample.getSamplePixels();
                    final byte[] targetPixels = sample.getTargetPixels();
                    int total = 0;
                    for (int i = 0, samplePixelsLength = samplePixels.length; i < samplePixelsLength; i++) {
                        byte samplePixel = samplePixels[i];
                        total += samplePixel & 0xff; // Range 0 to 255, not -128 to 127
                    }
                    int sampleMean = total / samplePixels.length;
                    total = 0;
                    for (int i = 0, targetPixelsLength = targetPixels.length; i < targetPixelsLength; i++) {
                        byte targetPixel = targetPixels[i];
                        total += targetPixel & 0xff; // Range 0 to 255, not -128 to 127
                    }
                    int targetMean = total / targetPixels.length;

                    final int contrast = Math.max(targetMean - sampleMean, 0);

                    if (recording) {
                        // Recording
                        if (recordedFrameCount >= totalFrameCount) {
                            recordedFrameCount = 0;
                            // Recording ended
                            stopRecording();
                            return;
                        }
                        // Save
                        frameArray[recordedFrameCount] = contrast;

                        // Render
                        if (renderer != null) {
                            if (renderGapCounter.count() >= SignalRecorder.RENDER_GAP) {
                                renderer.render(Arrays.copyOf(frameArray, frameArray.length));
                                renderGapCounter.reset();
                            }
                        }
                        recordedFrameCount++;
                    }

                    //Log.d(TAG, "frame running, key=" + (contrastDetected ? '*' : '|') + " value=" + contrast);
                }, 0L
                // Delay between two consecutive image-frame = 1 / sampleRate * 1000 milliseconds
                , (1000 / frameRate)
                , TimeUnit.MILLISECONDS);

        Log.d(TAG, "processing started.");
    }

    @Override
    public void processImage(ImageProxy image) {
        checkForDestroyedState();
        // Only process image if imageProcessing is true
        if (imageProcessing) {
            // ImageFormat.YUV_420_888
            // Obtaining the Y-plane raw bytes
            ImageProxy.PlaneProxy y = image.getPlanes()[0];
            ByteBuffer buffer = y.getBuffer();
            byte[] yBytes = new byte[buffer.remaining()];
            buffer.get(yBytes);
            synchronized (this) {
                // Update image-frame sample at this instant
                sample = new ImageSample(y.getRowStride(), y.getPixelStride(), image.getHeight(), image.getWidth(), yBytes);
            }
            image.close();
        }
    }

    @Override
    public void stopProcessing() {
        checkForDestroyedState();
        if (imageProcessing) {
            // Stop any currently running translation or recorder session
            stopRecording();
            // Cancel scheduled task
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            imageProcessing = false;
            Log.d(TAG, "processing stopped.");
        }
    }

    /**
     * Cleans up everything, release resources.
     * This is the last method to call.
     * After this, any method invocation will produce exception.
     */
    public void cleanUp() {
        if (cleanUpCalled) {
            return;
        }
        stopProcessing();
        scheduledExecutor.shutdownNow();

        // Cancel any pending or running tasks
        cancelAllTranslations(true);

        cleanUpCalled = true;
    }

    private void checkForDestroyedState() {
        if (cleanUpCalled) {
            throw new IllegalStateException(TAG + "->" + "method invocation after cleanUp() called.");
        }
    }

    public boolean isImageProcessing() {
        return imageProcessing;
    }

    public boolean isRecording() {
        return recording;
    }

    public interface GraphRenderer {
        void render(@NonNull int[] valueArray);

        void clear();
    }

    public interface Callback {
        void onFrameRateChange(@NonNull FrameRate newFrameRate);

        void onBaselineChange(int newBaseline);

        void onRecordStart();

        void onRecordStop();

        void onTranslationBegin();

        void onTranslationCancel();

        void onTranslationComplete(@NonNull Translator.Result result);
    }
}

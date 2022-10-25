package raha.app.morsebuddy.camera;

import android.graphics.Rect;

import raha.app.morsebuddy.util.Constants;

/**
 * Lightweight & easily-analyzable data holder that represents a byte-matrix of size {@link ImageSample#SAMPLE_SIZE} x {@link ImageSample#SAMPLE_SIZE}.
 * Contains y-plane values from the {@link android.graphics.ImageFormat#YUV_420_888} format.
 * Used to detect signal by calculating luminance contrast between target and whole image.
 */
class ImageSample {
    private static final int SAMPLE_SIZE = Constants.SAMPLE_SIZE_IN_PIXEL;
    private static final int TARGET_SIZE = Constants.TARGET_SIZE_IN_PIXEL;

    private final byte[] sample;
    private final byte[] target;

    public ImageSample(int rowStride, int pixelStride, int imageWidth, int imageHeight, byte[] imagePlane) {
        //Log.d("ImageSample", "rS=" + rowStride + " pS=" + pixelStride + " w=" + imageWidth + " h=" + imageHeight);

        /* Offset between two neighboring sample-pixels in the real plane */
        final int pixelOffset = imageWidth / SAMPLE_SIZE;

        // Calculating bounds
        final Rect sampleRect = new Rect(0, ((imageHeight - imageWidth) / 2), imageWidth, ((imageHeight + imageWidth) / 2));
        final Rect targetRect = new Rect(((SAMPLE_SIZE - TARGET_SIZE) / 2), ((SAMPLE_SIZE - TARGET_SIZE) / 2), ((SAMPLE_SIZE + TARGET_SIZE) / 2), ((SAMPLE_SIZE + TARGET_SIZE) / 2));

        //Log.d("ImageSample", "sampleRect=" + sampleRect + " targetRect=" + targetRect);

        // Populating sample matrix
        this.sample = new byte[SAMPLE_SIZE * SAMPLE_SIZE];
        int sIndex = 0;
        for (int x = 0; x < imageWidth; x += pixelOffset) {
            for (int y = 0; y < imageHeight; y += pixelOffset) {
                if (y < sampleRect.top || y >= sampleRect.bottom || x < sampleRect.left || x >= sampleRect.right)
                    continue;
                int index = rowStride * pixelStride * x + y * pixelStride;
                sample[sIndex++] = imagePlane[index];
            }
        }

        // Populating target matrix
        this.target = new byte[TARGET_SIZE * TARGET_SIZE];
        int targetIndex = 0;
        for (int x = 0; x < SAMPLE_SIZE; ++x) {
            for (int y = 0; y < SAMPLE_SIZE; ++y) {
                if (y < targetRect.top || y >= targetRect.bottom || x < targetRect.left || x >= targetRect.right) {
                    continue;
                }
                int index = SAMPLE_SIZE * x + y;
                target[targetIndex++] = sample[index];
                //sample[index] = 0;
            }
        }
    }

    /**
     * Generates a new {@link ImageSample} with all pixels set to zero.
     *
     * @return new {@link ImageSample} instance
     */
    public static ImageSample zeros() {
        return new ImageSample(SAMPLE_SIZE, 1, SAMPLE_SIZE, SAMPLE_SIZE, new byte[SAMPLE_SIZE * SAMPLE_SIZE]);
    }

    /**
     * Provides a linear byte-array, containing targeted pixels in top-to-bottom fashion.
     *
     * @return byte-array of size {@link ImageSample#TARGET_SIZE}^2
     */
    public byte[] getTargetPixels() {
        return target;
    }

    /**
     * Provides a linear byte-array, containing all sample pixels in top-to-bottom fashion.
     *
     * @return byte-array of size {@link ImageSample#SAMPLE_SIZE}^2
     */
    public byte[] getSamplePixels() {
        return sample;
    }
}

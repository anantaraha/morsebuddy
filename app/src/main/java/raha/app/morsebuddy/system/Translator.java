package raha.app.morsebuddy.system;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import raha.app.morsebuddy.util.MorseMap;

/**
 * The morse translator that takes a byte-array representing recorded signal of a session, processes it and translates to produce Morse and equivalent text.
 *
 * Uses a {@link ArrayNormalizer} to transform signal array to contain only the integers defined by {@link IntegerRepresentation}.
 */
public class Translator {
    private static final String TAG = "Translator";

    public static final class IntegerRepresentation {
        public static final int MINUS_1 = -1;
        public static final int MINUS_3 = -3;
        public static final int MINUS_7 = -7;
        public static final int PLUS_1 = 1;
        public static final int PLUS_3 = 3;
    }

    private final ArrayNormalizer normalizer;

    public Translator(@NonNull ArrayNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Translates the given signal array and produce morse/equivalent text result.
     * <b>Important:</b> This is a blocking method, so invocation must be done from background threads.
     * @param submitCode submit code for the one time task
     * @param givenArray given signal array
     * @param baseline given baseline for translation
     * @return result for the translation
     */
    @Nullable
    public synchronized Result resolve(int submitCode, @NonNull int[] givenArray, int baseline) {
        Result result = new Result(submitCode, baseline, false, null, null, null);
        // Subtracting baseline from each element
        // Also leave if given array is all zero
        boolean nonZeroFound = false;
        for (int i = 0; i < givenArray.length; ++i) {
            givenArray[i] = Math.max(givenArray[i] - baseline, 0);
            if (givenArray[i] != 0) {
                nonZeroFound = true;
            }
        }
        if (!nonZeroFound) {
            return result;
        }

        // Intentional delay
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Remove leading and trailing space
        int startIndex = 0;
        while (startIndex < givenArray.length) {
            if (givenArray[startIndex] > 0) {
                break;
            }
            startIndex++;
        }
        int endIndex = givenArray.length - 1;
        while (endIndex >= 0) {
            if (givenArray[endIndex] > 0) {
                break;
            }
            endIndex--;
        }
        int[] trimmedArray = Arrays.copyOfRange(givenArray, startIndex, endIndex);

        Log.d(TAG, "trimmed array=" + Arrays.toString(trimmedArray));

        // Preparing required array for handing to normalizer
        int[] newArray = new int[trimmedArray.length];
        int index = 0;
        boolean lastWasZero = false;
        int count = 0;
        for (int value : trimmedArray) {
            if (value == 0) {
                // Means space
                if (lastWasZero) {
                    --count;
                } else {
                    newArray[index++] = count;
                    lastWasZero = true;
                    count = -1;
                }
            } else {
                // Means elements
                if (lastWasZero) {
                    newArray[index++] = count;
                    lastWasZero = false;
                    count = 1;
                } else {
                    ++count;
                }
            }
        }
        // Last element
        if (index < newArray.length) {
            newArray[index++] = count;
        }
        int[] normalizableArray = Arrays.copyOf(newArray, index);
        // Ensuring array contains both negative and positive integers.
        // Then size must be >= 2.
        if (normalizableArray.length < 2) {
            return result;
        }

        Log.d(TAG, "normalizable" + Arrays.toString(normalizableArray));

        // Normalize
        normalizer.reset();
        int[] normalizedArray = normalizer.normalize(normalizableArray);

        // Building morse
        StringBuilder morseBuilder = new StringBuilder();
        for ( Integer integer : normalizedArray) {
            if (integer == Translator.IntegerRepresentation.MINUS_1) {
                morseBuilder.append(MorseMap.SIGN_SPACE_1U);
            } else if (integer == Translator.IntegerRepresentation.MINUS_3) {
                morseBuilder.append(MorseMap.SIGN_SPACE_3U);
            } else if (integer == Translator.IntegerRepresentation.MINUS_7) {
                morseBuilder.append(MorseMap.SIGN_SPACE_7U);
            } else if (integer == Translator.IntegerRepresentation.PLUS_1) {
                morseBuilder.append(MorseMap.SIGN_DOT);
            } else if (integer == Translator.IntegerRepresentation.PLUS_3) {
                morseBuilder.append(MorseMap.SIGN_DASH);
            }
        }

        // Converting into text
        StringBuilder outputBuilder = new StringBuilder();
        String[] morseWords = morseBuilder.toString().split(MorseMap.SIGN_SPACE_7U);
        for (String morseWord : morseWords) {
            String[] morseChars = morseWord.split(MorseMap.SIGN_SPACE_3U);
            for (String morseChar : morseChars) {
                Character asciiChar = MorseMap.morseToLetter(morseChar);
                if (asciiChar != null) {
                    outputBuilder.append(asciiChar);
                }
            }
            outputBuilder.append(' ');
        }

        // Prepare final result
        result.success = true;
        result.array = normalizedArray;
        result.morse = morseBuilder.toString();
        result.output = outputBuilder.toString().trim();
        return result;
    }

    public static class Result {
        private final int submitCode;
        private final int baseline;
        private boolean success;
        private int[] array;
        private String morse;
        private String output;

        public Result(int submitCode, int baseline, boolean success, int[] array, String morse, String output) {
            this.submitCode = submitCode;
            this.success = success;
            this.baseline = baseline;
            this.array = array;
            this.morse = morse;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getSubmitCode() {
            return submitCode;
        }

        public int[] getArray() {
            return array;
        }

        public int getBaseline() {
            return baseline;
        }

        public String getMorse() {
            return morse;
        }

        public String getOutput() {
            return output;
        }

        @NonNull
        @Override
        public String toString() {
            return "Result{" +
                    "submitCode=" + submitCode +
                    ", baseline=" + baseline +
                    ", success=" + success +
                    ", morse='" + morse + '\'' +
                    ", output='" + output + '\'' +
                    '}';
        }
    }

    public interface ArrayNormalizer {
        @NonNull
        int[] normalize(@NonNull int[] source);
        void reset();
    }
}

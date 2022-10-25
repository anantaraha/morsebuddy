package raha.app.morsebuddy.system;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import raha.app.morsebuddy.clustering.KMeans;

/**
 * Takes an integer array as input.
 * Calculates and groups the integers into clusters representing various time durations for various morse elements (dot, dash, space, etc).
 * Outputs an array which contains only integers defined in {@link Translator.IntegerRepresentation}:
 * -7: Space between words
 * -3: Space between letters
 * -1: Space between dot and dash
 * 1: Dot
 * 3: Dash
 * Hence, the output array can be mapped to morse symbols directly.
 * Uses {@link KMeans} algorithm for clustering integers.
 */
public class KMeansNormalizer implements Translator.ArrayNormalizer {
    private static final String TAG = "ClusterBasedNormalizer";
    private static final int KMEANS_MAX_ITERATION = 100;

    /**
     * The user must ensure that the sourceArray contains <b>both positive and negative integer (except zero)</b>, where positive represents a dash or dot, and negative represents a space.
     * If positive or negative number is absent, then it will raise exception.
     *
     * @param sourceArray given array containing only positive and negative integers.
     * @return normalized array.
     */
    @NonNull
    @Override
    public int[] normalize(@NonNull int[] sourceArray) {
        Log.d(TAG, "before normalization=" + Arrays.toString(sourceArray));

        // Calculate centroids and apply kMeans
        final List<Integer> negativeList = new ArrayList<>();
        final List<Integer> positiveList = new ArrayList<>();
        for (Integer integer : sourceArray) {
            if (integer < 0) {
                negativeList.add(integer);
            } else if (integer > 0) {
                positiveList.add(integer);
            }
        }
        // Raise exception if both negative or positive values not found
        if (negativeList.size() == 0 || positiveList.size() == 0 || (positiveList.size() + negativeList.size() != sourceArray.length)) {
            throw new IllegalArgumentException("Source array must contain both negative and positive integers (except zero).");
        }
        int _oneUnit = Collections.max(negativeList);   // -1 unit
        int _sevenUnit = Collections.min(negativeList); // -7 unit
        int _threeUnit = (_oneUnit + _sevenUnit) / 2;   // -3 unit
        int oneUnit = Collections.min(positiveList);    // 1 unit
        int threeUnit = Collections.max(positiveList);  // 3 unit
        KMeans.Centroid[] centroids = new KMeans.Centroid[]{new KMeans.Centroid(oneUnit), new KMeans.Centroid(threeUnit), new KMeans.Centroid(_oneUnit), new KMeans.Centroid(_threeUnit), new KMeans.Centroid(_sevenUnit)};
        KMeans.Element[] elements = new KMeans.Element[sourceArray.length];
        for (int i = 0, sourceSize = sourceArray.length; i < sourceSize; i++) {
            int integer = sourceArray[i];
            elements[i] = new KMeans.Element(integer);
        }
        Pair<KMeans.Centroid[], KMeans.Element[]> clustered = KMeans.fit(centroids, elements, KMEANS_MAX_ITERATION);

        // Mapping clustered centers to our integers
        KMeans.Centroid[] clusteredCentroids = clustered.first;
        KMeans.Element[] clusteredElements = clustered.second;
        Arrays.sort(clusteredCentroids);
        Map<Integer, Integer> map = new HashMap<>();
        map.put(clusteredCentroids[0].getValue(), Translator.IntegerRepresentation.MINUS_7);
        map.put(clusteredCentroids[1].getValue(), Translator.IntegerRepresentation.MINUS_3);
        map.put(clusteredCentroids[2].getValue(), Translator.IntegerRepresentation.MINUS_1);
        map.put(clusteredCentroids[3].getValue(), Translator.IntegerRepresentation.PLUS_1);
        map.put(clusteredCentroids[4].getValue(), Translator.IntegerRepresentation.PLUS_3);

        int[] normalized = new int[clusteredElements.length];
        for (int i = 0, secondLength = clusteredElements.length; i < secondLength; i++) {
            Integer normal = map.get(clusteredElements[i].getCenter());
            normalized[i] = (normal == null ? (clusteredElements[i].getCenter() < 0 ? Translator.IntegerRepresentation.MINUS_1 : Translator.IntegerRepresentation.PLUS_1) : normal);
        }

        Log.d(TAG, "normalized=" + Arrays.toString(normalized));

        return normalized;
    }

    @Override
    public void reset() {
        // Nothing to do for now.
    }
}
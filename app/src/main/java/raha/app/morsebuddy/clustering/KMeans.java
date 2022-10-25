package raha.app.morsebuddy.clustering;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KMeans {
    private static final String TAG = "KMeans";

    @NonNull
    public static Pair<Centroid[], Element[]> fit(@NonNull Centroid[] centroids, @NonNull Element[] elements, int maxIteration) {
        final List<Centroid> centroidList = new ArrayList<>(Arrays.asList(centroids));
        final List<Element> elementList = new ArrayList<>(Arrays.asList(elements));

        // Iterate following process
        int iteration = 0;
        while (iteration++ < maxIteration) {
            Log.d(TAG, "iteration " + iteration + " started.");

            // Clear each centroid's elementList
            for (Centroid centroid : centroids) {
                centroid.elementList.clear();
            }

            // Assign each element to its nearest centroid's cluster
            for (Element element : elementList) {
                Centroid minCentroid = new Centroid(Integer.MAX_VALUE);
                for (Centroid centroid : centroids) {
                    // Positive and negative numbers must not be on the same cluster
                    if (Math.signum(element.value) == Math.signum(centroid.value)) {
                        if (Math.abs(element.value - centroid.value) < Math.abs(element.value - minCentroid.value)) {
                            minCentroid = centroid;
                        }
                    }
                }
                minCentroid.elementList.add(element);
                element.center = minCentroid.value;
            }

            // Now, calculate mean and update centroid
            for (Centroid centroid : centroidList) {
                centroid.value = centroid.calculateMean();
            }
            Log.d(TAG, KMeans.printCentroids(centroids));
        }

        return new Pair<>(centroids, elements);
    }

    private static String printCentroids(Centroid[] centroids) {
        StringBuilder builder = new StringBuilder();
        for (Centroid centroid : centroids) {
            builder.append(" \n").append("center=").append(centroid.value).append(", elements=").append(centroid.elementList.toString());
        }
        return builder.toString();
    }

    public static class Element {
        private final int value;
        private int center;

        public Element(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public int getCenter() {
            return center;
        }

        @NonNull
        @Override
        public String toString() {
            return "" + value;
        }
    }

    public static class Centroid implements Comparable<Centroid> {
        private int value;
        private final List<Element> elementList;

        public Centroid(int value) {
            this.value = value;
            elementList = new ArrayList<>();
        }

        private int calculateMean() {
            if (elementList.size() == 0) {
                return value;
            }
            int sum = 0;
            for (Element element :
                    elementList) {
                sum += element.value;
            }
            return sum / elementList.size();
        }

        public int getValue() {
            return value;
        }

        public List<Element> getElementList() {
            return elementList;
        }

        @Override
        public int compareTo(Centroid centroid) {
            return this.value - centroid.value;
        }
    }
}
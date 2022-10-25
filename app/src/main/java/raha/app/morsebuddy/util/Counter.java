package raha.app.morsebuddy.util;

public class Counter {
    private int n;

    public Counter() {
        n = 0;
    }

    public int count() {
        return ++n;
    }

    public void reset() {
        n = 0;
    }
}

package raha.app.morsebuddy.app;

import android.app.Application;

public class MorseBuddy extends Application {
    private static TaskExecutor executor;

    @Override
    public void onCreate() {
        super.onCreate();

        executor = new TaskExecutor();
    }

    public static TaskExecutor getExecutor() {
        return executor;
    }
}

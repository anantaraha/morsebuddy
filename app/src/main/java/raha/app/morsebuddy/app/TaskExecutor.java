package raha.app.morsebuddy.app;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskExecutor {
    private final ExecutorService sService;
    private final Handler handler;

    TaskExecutor() {
        this.sService = new ThreadPoolExecutor(0, 1,
                3L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        this.handler = new Handler(Looper.getMainLooper());
    }

    public <R> void execute(@NonNull Callable<R> callable, @Nullable Callback<R> callback) {
        sService.execute(() -> {
            handler.post(() -> {
                if (callback != null) {
                    callback.onStart();
                }
            });
            R r = null;
            try {
                r = callable.call();
            } catch (Exception e) {
                // Ignore
                e.printStackTrace();
            } finally {
                R result = r;
                handler.post(() -> {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                });
            }
        });
    }

    public void stop() {
        sService.shutdown();
    }

    public interface Callback<R> {
        void onStart();

        void onComplete(R result);
    }
}

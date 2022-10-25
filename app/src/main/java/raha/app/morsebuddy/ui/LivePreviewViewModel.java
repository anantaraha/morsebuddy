package raha.app.morsebuddy.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import raha.app.morsebuddy.camera.SignalRecorder;
import raha.app.morsebuddy.system.Translator;

public class LivePreviewViewModel extends AndroidViewModel {
    private static final String TAG = "LivePreviewViewModel";

    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;
    private final MutableLiveData<SignalRecorder.FrameRate> recorderFrameRate;
    private final MutableLiveData<Integer> recorderBaseline;
    private final MutableLiveData<Translator.Result> translationResult;
    private final MutableLiveData<RecorderState> recorderState;
    private final MutableLiveData<TranslatorState> translatorState;
    private final MutableLiveData<int[]> graphValueArray;

    enum RecorderState {
        RECORDING,
        IDLE
    }

    enum TranslatorState {
        TRANSLATING,
        IDLE
    }

    /*
    Activity will keep reference and give commands on this instance.
     */
    private SignalRecorder signalRecorder;


    /**
     * Create an instance which interacts with the camera service via the given application context.
     */
    public LivePreviewViewModel(@NonNull Application application) {
        super(application);

        // Initialize variables
        signalRecorder = new SignalRecorder(SignalRecorder.FrameRate.FPS_60);
        recorderFrameRate = new MutableLiveData<>(signalRecorder.getFrameRate());
        recorderBaseline = new MutableLiveData<>(signalRecorder.getBaseline());
        recorderState = new MutableLiveData<>(RecorderState.IDLE);
        translatorState = new MutableLiveData<>(TranslatorState.IDLE);
        translationResult = new MutableLiveData<>();
        graphValueArray = new MutableLiveData<>();

        // Setting listeners
        signalRecorder.setRenderer(new SignalRecorder.GraphRenderer() {
            @Override
            public void render(@NonNull int[] valueArray) {
                Log.d(TAG, "received GraphRenderer.render call from SignalRecorder.");
                graphValueArray.postValue(valueArray);
            }

            @Override
            public void clear() {
                Log.d(TAG, "received GraphRenderer.clear call from SignalRecorder.");
                graphValueArray.postValue(null);
            }
        });
        signalRecorder.setCallback(new SignalRecorder.Callback() {
            @Override
            public void onFrameRateChange(@NonNull SignalRecorder.FrameRate newFrameRate) {
                Log.d(TAG, "received Callback.onFrameRateChange call from SignalRecorder.");
                recorderFrameRate.postValue(newFrameRate);
            }

            @Override
            public void onBaselineChange(int newBaseline) {
                Log.d(TAG, "received Callback.onBaselineChange call from SignalRecorder.");
                recorderBaseline.postValue(newBaseline);
            }

            @Override
            public void onRecordStart() {
                Log.d(TAG, "received Callback.onRecordStart call from SignalRecorder.");
                recorderState.postValue(RecorderState.RECORDING);
            }

            @Override
            public void onRecordStop() {
                Log.d(TAG, "received Callback.onRecordStop call from SignalRecorder.");
                recorderState.postValue(RecorderState.IDLE);
            }

            @Override
            public void onTranslationBegin() {
                Log.d(TAG, "received Callback.onTranslationBegin call from SignalRecorder.");
                translatorState.postValue(TranslatorState.TRANSLATING);
            }

            @Override
            public void onTranslationCancel() {
                Log.d(TAG, "received Callback.onTranslationCancel call from SignalRecorder.");
                translatorState.postValue(TranslatorState.IDLE);
            }

            @Override
            public void onTranslationComplete(@NonNull Translator.Result result) {
                Log.d(TAG, "received Callback.onTranslationComplete call from SignalRecorder.");
                translationResult.postValue(result);
                translatorState.postValue(TranslatorState.IDLE);
            }
        });
    }

    public SignalRecorder getSignalRecorder() {
        return signalRecorder;
    }

    public LiveData<SignalRecorder.FrameRate> getRecorderFrameRate() {
        return recorderFrameRate;
    }

    public LiveData<Integer> getRecorderBaseline() {
        return recorderBaseline;
    }

    public LiveData<RecorderState> getRecorderState() {
        return recorderState;
    }

    public LiveData<TranslatorState> getTranslatorState() {
        return translatorState;
    }

    public LiveData<Translator.Result> getTranslationResult() {
        return translationResult;
    }

    public LiveData<int[]> getGraphValueArray() {
        return graphValueArray;
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
        if (cameraProviderLiveData == null) {
            cameraProviderLiveData = new MutableLiveData<>();
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication());
            cameraProviderFuture.addListener(
                    () -> {
                        try {
                            cameraProviderLiveData.setValue(cameraProviderFuture.get());
                        } catch (ExecutionException | InterruptedException e) {
                            // Handle any errors (including cancellation) here.
                            Log.e(TAG, "Unhandled exception", e);
                        }
                    },
                    ContextCompat.getMainExecutor(getApplication()));
        }

        return cameraProviderLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        //signalRecorder.cleanUp();
        signalRecorder = null;
        Log.d(TAG, "view model cleared.");
    }
}

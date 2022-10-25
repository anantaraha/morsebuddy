package raha.app.morsebuddy.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;

import raha.app.morsebuddy.R;
import raha.app.morsebuddy.camera.CameraHelper;
import raha.app.morsebuddy.camera.SignalRecorder;
import raha.app.morsebuddy.ui.view.GraphView;

public class LivePreviewActivity extends AppCompatActivity {
    private static final String TAG = "LivePreviewActivity";
    private static final String PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private CameraHelper cameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_live_preview);

        /* Checking permissions */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(LivePreviewActivity.this, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Start all
            startAll();
        } else {
            // Request permissions
            requestPermissions(new String[]{PERMISSION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted, now start all
                startAll();
            } else {
                // Not granted, exit
                Toast.makeText(getApplicationContext(), "Permission denied, cannot access camera.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startAll() {
        LivePreviewViewModel viewModel = new ViewModelProvider(LivePreviewActivity.this, (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(LivePreviewViewModel.class);

        /* Component initialization */

        TextView textMorse = findViewById(R.id.text_morse);
        textMorse.setMovementMethod(new ScrollingMovementMethod());
        textMorse.setHorizontallyScrolling(true);
        TextView textOutput = findViewById(R.id.text_output);
        CircularProgressIndicator loading = findViewById(R.id.loading_bar);
        loading.hide();

        MaterialButton btnFrameRate = findViewById(R.id.btn_frame_rate);
        btnFrameRate.setOnClickListener(v -> {
            // Since recorder control methods are being directly called, status should be retrieved from recorder directly;
            // For example, viewModel.getFrameRate.getValue() might return inaccurate value, since they are updated from background thread.
            if (viewModel.getSignalRecorder().getFrameRate() == SignalRecorder.FrameRate.FPS_60) {
                viewModel.getSignalRecorder().setFrameRate(SignalRecorder.FrameRate.FPS_30);
            } else if (viewModel.getSignalRecorder().getFrameRate() == SignalRecorder.FrameRate.FPS_30) {
                viewModel.getSignalRecorder().setFrameRate(SignalRecorder.FrameRate.FPS_60);
            }
        });
        MaterialButton btnLens = findViewById(R.id.btn_lens);
        btnLens.setOnClickListener(v -> {
            try {
                cameraHelper.switchLensFacing();
            } catch (CameraInfoUnavailableException e) {
                // Falls through
                Toast.makeText(this,
                                "This device does not have the selected lens",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });
        FloatingActionButton btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(v -> {
            if (viewModel.getSignalRecorder().isRecording()) {
                viewModel.getSignalRecorder().stopRecording();
            } else {
                viewModel.getSignalRecorder().startRecording();
            }
        });

        PreviewView previewView = findViewById(R.id.preview_view);
        GraphView graphView = findViewById(R.id.visualizer_view);

        cameraHelper = new CameraHelper(getApplicationContext(), LivePreviewActivity.this, previewView);
        cameraHelper.setImageProcessor(viewModel.getSignalRecorder());

        Slider baselineSlider = findViewById(R.id.slider_baseline);
        // Initial state of the slider
        float initialBaseline = viewModel.getRecorderBaseline().getValue() != null ? viewModel.getRecorderBaseline().getValue() : 0f;
        baselineSlider.addOnChangeListener((slider, value, fromUser) -> {
            int newBaseline = (int) (value * 127f / slider.getValueTo());
            graphView.setBaseLine(newBaseline);
        });
        baselineSlider.setValue(initialBaseline);
        baselineSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int newBaseline = (int) (slider.getValue() * 127f / slider.getValueTo());
                viewModel.getSignalRecorder().setBaseline(newBaseline);
            }
        });

        viewModel.getGraphValueArray().observe(LivePreviewActivity.this, ints -> {
            Log.d(TAG, "received value array from ViewModel.");
            if (ints != null) {
                graphView.render(ints);
            } else {
                graphView.clear();
            }
        });

        viewModel.getRecorderFrameRate().observe(LivePreviewActivity.this, frameRate -> {
            Log.d(TAG, "received frame rate from ViewModel. newFrameRate=" + frameRate);
            btnFrameRate.setIconResource(frameRate == SignalRecorder.FrameRate.FPS_30 ? R.drawable.ic_30fps : R.drawable.ic_60fps);
        });
        viewModel.getRecorderState().observe(LivePreviewActivity.this, recorderState -> {
            Log.d(TAG, "received recorder state from ViewModel. newState=" + recorderState);
            if (recorderState == LivePreviewViewModel.RecorderState.RECORDING) {
                btnRecord.setImageResource(R.drawable.ic_record_stop);
                // Disable all
                btnFrameRate.setEnabled(false);
                btnLens.setEnabled(false);
                baselineSlider.setEnabled(false);
            } else if (recorderState == LivePreviewViewModel.RecorderState.IDLE) {
                btnRecord.setImageResource(R.drawable.ic_record_start);
                // Disable all
                btnFrameRate.setEnabled(true);
                btnLens.setEnabled(true);
                baselineSlider.setEnabled(true);
            }
        });
        viewModel.getTranslatorState().observe(LivePreviewActivity.this, translatorState -> {
            Log.d(TAG, "received translator state from ViewModel. newState=" + translatorState);
            // Loading animation
            if (translatorState == LivePreviewViewModel.TranslatorState.TRANSLATING) {
                loading.show();
                textMorse.setText("");
                textOutput.setText("");
            } else if (translatorState == LivePreviewViewModel.TranslatorState.IDLE) {
                // Loading animation
                loading.hide();
            }
        });
        viewModel.getTranslationResult().observe(LivePreviewActivity.this, result -> {
            Log.d(TAG, "received translation result from ViewModel. result=" + result);
            String morse = result.getMorse();
            String output = result.getOutput();
            if (morse != null) {
                textMorse.setText(morse);
            }
            if (output != null) {
                textOutput.setText(output);
            }
            if (!result.isSuccess()) {
                Toast.makeText(getApplicationContext(), R.string.cannot_translate, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getProcessCameraProvider()
                .observe(
                        LivePreviewActivity.this,
                        provider -> {
                            Log.d(TAG, "received camera provider from ViewModel.");
                            cameraHelper.setCameraProvider(provider);
                            cameraHelper.start();
                        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
        Log.d(TAG, "on destroy called.");
    }

}

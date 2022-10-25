package raha.app.morsebuddy.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import raha.app.morsebuddy.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.card_camera_morse).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LivePreviewActivity.class)));
    }
}
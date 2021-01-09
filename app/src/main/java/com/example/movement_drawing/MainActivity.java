package com.example.movement_drawing;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // for log statements
    private static final String TAG = "MainActivity";

    LinearLayout canvasLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvasLinearLayout = findViewById(R.id.canvasViewLayout);
        PaintView paintView = new PaintView(getApplicationContext());
        canvasLinearLayout.addView(paintView);
    }
}

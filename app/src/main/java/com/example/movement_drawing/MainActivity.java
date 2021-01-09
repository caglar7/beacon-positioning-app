package com.example.movement_drawing;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // for log statements
    private static final String TAG = "MainActivity";

    private LinearLayout canvasLinearLayout;
    private PaintView paintView;

    // create direction buttons, 8 directions
    private Button buttonRight, buttonLeft, buttonTop, buttonDown;
    private Button buttonRightTop, buttonRightBottom;
    private Button buttonLeftTop, buttonLeftBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize variables
        buttonLeftTop = findViewById(R.id.button_LeftTop);
        buttonTop = findViewById(R.id.button_Top);
        buttonRightTop = findViewById(R.id.button_RightTop);
        buttonLeft = findViewById(R.id.button_Left);
        buttonRight = findViewById(R.id.button_Right);
        buttonLeftBottom = findViewById(R.id.button_LeftBottom);
        buttonDown = findViewById(R.id.button_Bottom);
        buttonRightBottom = findViewById(R.id.button_RightBottom);


        // add canvas to the linear layout view
        canvasLinearLayout = findViewById(R.id.canvasViewLayout);
        paintView = new PaintView(getApplicationContext());
        canvasLinearLayout.addView(paintView);

        // set listener for all the buttons
        setOnClickListeners();

    }

    private void setOnClickListeners()
    {
        //  1
        buttonLeftTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-1, 1);
            }
        });
        //  2
        buttonTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(0, 1);
            }
        });
        //  3
        buttonRightTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(1, 1);
            }
        });
        //  4
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-1, 0);
            }
        });
        //  5
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(1, 0);
            }
        });
        //  6
        buttonLeftBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-1, -1);
            }
        });
        //  7
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(0, -1);
            }
        });
        // 8
        buttonRightBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(1, -1);
            }
        });
    }
}

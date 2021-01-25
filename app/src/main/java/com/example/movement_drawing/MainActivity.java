package com.example.movement_drawing;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // for log statements
    private static final String TAG = "MainActivity";

    private LinearLayout canvasLinearLayout;
    private PaintView paintView;

    // create direction buttons, 8 directions
    private ImageButton buttonRight, buttonLeft, buttonDown;
    private ImageButton buttonRightTop, buttonRightBottom;
    private ImageButton buttonLeftTop, buttonLeftBottom;

    // for scale text
    public static TextView tv_ScaleLevel;

    // variables for step counter
    private Random rand = new Random();
    private SensorManager sensorManager;
    private Sensor detectSensor;
    private boolean moving;
    private float rangeMin = .64f;
    private float rangeMax = .76f;

    @Override
    protected void onResume() {
        super.onResume();
        moving = true;
        // register sensor again
        if(detectSensor!=null)
        {
            sensorManager.registerListener(MainActivity.this, detectSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else
        {
            Toast.makeText(this, "sensor not found!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "sensor not found!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        moving=false;
        // we only use step detector for now
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize variables
        buttonLeftTop = findViewById(R.id.button_LeftTop);
        buttonRightTop = findViewById(R.id.button_RightTop);
        buttonLeft = findViewById(R.id.button_Left);
        buttonRight = findViewById(R.id.button_Right);
        buttonLeftBottom = findViewById(R.id.button_LeftBottom);
        buttonDown = findViewById(R.id.button_Bottom);
        buttonRightBottom = findViewById(R.id.button_RightBottom);
        tv_ScaleLevel = findViewById(R.id.tv_Scale);


        // add canvas to the linear layout view
        canvasLinearLayout = findViewById(R.id.canvasViewLayout);
        paintView = new PaintView(getApplicationContext());
        canvasLinearLayout.addView(paintView);

        // set listener for all the buttons
        setOnClickListeners();

        // get step sensor and register
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        detectSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        sensorManager.registerListener(MainActivity.this, detectSensor, SensorManager.SENSOR_DELAY_NORMAL);
        moving = true;

    }

    private void setOnClickListeners()
    {
        //  1
        buttonLeftTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-45);
            }
        });
        //  3
        buttonRightTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(45);
            }
        });
        //  4
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-90);
            }
        });
        //  5
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(90);
            }
        });
        //  6
        buttonLeftBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(-135);
            }
        });
        //  7
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(180);
            }
        });
        // 8
        buttonRightBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.changeDirection(135);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // each step, update drawing with proper step increment
        if(moving)
        {
            float stepDistance = rangeMin + (rangeMax-rangeMin)*rand.nextFloat();
            paintView.updateTheDrawing(stepDistance);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

/*
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";

    Random rand = new Random();
    TextView textSteps;
    TextView textDistance;
    SensorManager sensorManager;
    Sensor detectSensor;
    Button buttonReset;

    boolean moving;
    int steps = 0;
    double distance = 0;
    double rangeMin = .64;
    double rangeMax = .76;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textSteps = (TextView) findViewById(R.id.text_Steps);
        textDistance = findViewById(R.id.text_Distance);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        detectSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        sensorManager.registerListener(MainActivity.this, detectSensor, SensorManager.SENSOR_DELAY_NORMAL);
        moving = true;

        buttonReset = findViewById(R.id.button_Reset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                steps = 0;
                distance = 0;
                textDistance.setText("0.0 meters");
                textSteps.setText("0");
            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
        moving = true;
        //Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(detectSensor != null)
        {
            sensorManager.registerListener(MainActivity.this, detectSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            Toast.makeText(this, "Sensor not found!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        moving = false;
        sensorManager.unregisterListener(this);
        steps = 0;
        distance = 0d;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(moving)
        {
            steps += (int) event.values[0];
            textSteps.setText(String.valueOf(steps));

            double stepDistance = rangeMin + (rangeMax-rangeMin)*rand.nextDouble();
            distance+=stepDistance;
            String disString = String.format("%.1f", distance);
            textDistance.setText(disString + " meters");

            Log.d(TAG, "steps: " + (int)event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
 */
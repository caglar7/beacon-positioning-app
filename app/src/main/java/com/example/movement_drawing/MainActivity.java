package com.example.movement_drawing;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener, BeaconConsumer{

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

    // BEACON VARIABLES
    private BeaconManager beaconManager;
    private Region beaconRegion;
    private Button button_StartStop;
    private Boolean doMonitoring = false;
    private String textStart, textStop;
    private int colorStartScanning, colorStopScanning;

    // beacon layouts
    private static final String ALTBEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private static final String EDDYSTONE_TLM_LAYOUT = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
    private static final String EDDYSTONE_UID_LAYOUT = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
    private static final String EDDYSTONE_URL_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v";
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    // beacon positioning variables
    private double approachIndex = 0, moveAwayIndex = 0;
    private int measureCount = 0;
    private double prevDistance = 0;
    private double currentDistance = 0;
    private double distanceThres = 0.1;
    private Boolean firstScanning = true;       // just for now, change later

    // THIS VARIABLE BLOCK IS FOR MOVEMENT DRAWING UPDATE
    // FOR DISTANCE CALCULATION, values from power regression prediction
    private double coefficientA = 0.89d;
    private double coefficientB = 7.62d;
    private double coefficientC = 0.24d;
    // for running average code
    private int measureAmount = 10;             // increments by 10 for now
    private ArrayList<Double> distanceList = new ArrayList<Double>();

    // for test
    private double distanceValue = 0;

    @Override
    protected void onResume()
    {
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
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        moving=false;
        // we only use step detector for now
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        sensorManager.unregisterListener(this);
        beaconManager.unbind(this);
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);

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

        // FOR BEACONS
        // beaconManager singleton
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // beacon layouts to scan
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(ALTBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_TLM_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_URL_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));

        // set scan period for beaconServiceConnect
        try {
            beaconManager.setForegroundScanPeriod(500l);
            beaconManager.setForegroundBetweenScanPeriod(0l);
        }catch(Exception e)
        {
            e.printStackTrace();
        }

        // bind beaconManager to this activity, set running average period
        beaconManager.bind(this);
        beaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000L);

        // get button and set listeners
        textStart = getResources().getString(R.string.start_scanning);
        textStop = getResources().getString(R.string.stop_scanning);
        colorStartScanning = getResources().getColor(R.color.colorStartScanning);
        colorStopScanning = getResources().getColor(R.color.colorStopScanning);
        button_StartStop = findViewById(R.id.button_StartStop);
        button_StartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String buttonText = button_StartStop.getText().toString();
                if(buttonText == textStart)
                {
                    button_StartStop.setBackgroundColor(colorStopScanning);
                    button_StartStop.setText(textStop);
                    startMonitoring();
                }
                else if(buttonText == textStop)
                {
                    button_StartStop.setBackgroundColor(colorStartScanning);
                    button_StartStop.setText(textStart);
                    stopMonitoring();
                }
            }
        });
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
    public void onSensorChanged(SensorEvent event)
    {
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

    // BEACON SERVICE
    @Override
    public void onBeaconServiceConnect()
    {
        try {
            beaconManager.updateScanPeriods();
        }catch(Exception e)
        {
            e.printStackTrace();
        }

        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if(doMonitoring)
                {
                    for(Beacon b: beacons)
                    {
                        b.setHardwareEqualityEnforced(true);

                        double txValue = b.getTxPower();
                        double rssiValue = b.getRssi();
                        distanceValue = getDistanceForDevice(txValue, rssiValue);
                        String distanceString = String.format("%.1f", distanceValue);
                        Log.d(TAG, "Distance: " + distanceString);

                        paintView.updateBeaconCircles((float)distanceValue);
                    }
                }
            }
        });
    }

    private void startMonitoring()
    {
        doMonitoring = true;

        Log.d(TAG, "monitoring started");
        try{
            beaconManager.startRangingBeaconsInRegion(new Region("myRegion", null, null, null));
        }
        catch(RemoteException e) {Log.d(TAG, "error on start monitoring"); }
    }

    private void stopMonitoring()
    {
        doMonitoring = false;
        Log.d(TAG, "monitoring stopped");
        try{
            beaconManager.stopRangingBeaconsInRegion(new Region("myRegion", null, null, null));
        }
        catch(RemoteException e) {Log.d(TAG, "error on stop monitoring"); }
    }

    private double getDistanceForDevice(double tx, double rssi)
    {
        // not first out last in, just weird values are out
        double ratio = rssi/tx;
        double dis = coefficientA*(Math.pow(ratio, coefficientB)) + coefficientC;

        // add distance to the list
        distanceList.add(dis);

        // running average code with 10% top and bottom are out
        int throwAmount = measureAmount/10;
        if(distanceList.size() >= measureAmount)
        {
            for(int i=0; i<throwAmount; i++)
            {
                // initialize and assign first element
                double topValue, bottomValue;
                int topIndex = 0, bottomIndex = 0;
                topValue = bottomValue = distanceList.get(0);

                // find top value and remove it
                for(double d: distanceList)
                {
                    if(d >= topValue)
                        topValue = d;
                }
                topIndex = distanceList.indexOf(topValue);
                distanceList.remove(topIndex);

                // find bottom value and remove it
                for(double d: distanceList)
                {
                    if(d<=bottomValue)
                        bottomValue = d;
                }
                bottomIndex = distanceList.indexOf(bottomValue);
                distanceList.remove(bottomIndex);
            }
        }

        double total = 0;
        double listSize = distanceList.size();
        for(double d: distanceList)
        {
            total+=d;
        }
        double runAverage = total/listSize;

        return runAverage;
    }
}

// APPROACH AND MOVE AWAY CODE
/*
for(Beacon b: beacons)
{
    b.setHardwareEqualityEnforced(true);

    prevDistance = currentDistance;
    currentDistance = b.getDistance();
    if(firstScanning)
    {
        prevDistance = currentDistance;
        firstScanning = false;
    }

    // GET APPROACH AND MOVE AWAY AMOUNTS
    if((currentDistance+distanceThres) < prevDistance)
    {
        // approaching
        approachIndex++;
    }
    else if(currentDistance > (prevDistance+distanceThres))
    {
        // moving away
        moveAwayIndex++;
    }
    else
    {
        approachIndex++;
        moveAwayIndex++;
    }

    measureCount++;
    if(measureCount >= measureAmount)
    {
        int percentageApproach = 0, percentageMoveAway = 0;
        if(approachIndex!=0 || moveAwayIndex!=0)
        {
            double divider = approachIndex+moveAwayIndex;
            percentageApproach = (int)((approachIndex/divider) * 100);
            percentageMoveAway = (int)((moveAwayIndex/divider) * 100);
            Toast.makeText(MainActivity.this, "Approach: " +
                    percentageApproach + "% --- MoveAway: " + percentageMoveAway + "%",
                    Toast.LENGTH_SHORT).show();
        }

        // reset index and parameters
        approachIndex = 0;
        moveAwayIndex = 0;
        measureCount = 0;
    }
}
 */
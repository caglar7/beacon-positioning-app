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
    private double measureAmount = 10f;
    private double compareDistance = 0;
    private ArrayList<Double> distanceList = new ArrayList<Double>();

    // for test delete
    private int count1 = 1;

    @Override
    protected void onResume()
    {
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
    protected void onPause()
    {
        super.onPause();
        moving=false;
        // we only use step detector for now
        sensorManager.unregisterListener(this);
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

                        double distance = b.getDistance();

                        // GET APPROACH AND MOVE AWAY AMOUNTS
                        if(compareDistance != 0)
                        {
                            if(distance > compareDistance)
                            {
                                moveAwayIndex++;
                            }
                            else if(distance < compareDistance)
                            {
                                approachIndex++;
                            }
                        }

                        compareDistance = getPrevAvgDistance(distance);
                        String disString = String.format("%.1f", distance);

                        // test
                        Log.d(TAG, "distance " + count1 + ": " + disString);
                        count1++;
                        if(distanceList.size() == measureAmount)
                        {
                            distanceList.clear();
                            Log.d(TAG, "PERIOD DONE\n" + "approachIndex: " + approachIndex +
                                    "\nmoveAwayIndex: " + moveAwayIndex);
                            approachIndex = 0;
                            moveAwayIndex = 0;
                        }

                        // when distanceList reaches measureAmount, calculate and reset
                        /*
                        if(distanceList.size() == measureAmount)
                        {
                            distanceList.clear();
                            double approachPercentage = 0, moveAwayPercentage = 0;
                            if(approachIndex!=0 || moveAwayIndex!=0)
                            {
                                approachPercentage = (approachIndex/(approachIndex+moveAwayIndex)) * 100;
                                moveAwayPercentage = (moveAwayIndex/((approachIndex+moveAwayIndex))) * 100;
                            }else {
                                Toast.makeText(MainActivity.this, "both indexes were 0", Toast.LENGTH_SHORT).show();
                            }

                            // after done calculating, reset index values
                            approachIndex = 0;
                            moveAwayIndex = 0;

                            // show the percentage values
                            Toast.makeText(MainActivity.this, "Approach: " + approachPercentage +
                                    " MoveAway: " + moveAwayPercentage, Toast.LENGTH_LONG).show();
                        }
                        */
                    }
                }
            }
        });
    }

    private double getPrevAvgDistance(double dis)
    {
        double prevAverage;

        // add distance and get average
        distanceList.add(dis);
        double total = 0;
        for(double d: distanceList)
            total+=d;
        prevAverage = total / (distanceList.size());

        return prevAverage;
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
}

/*
public class MainActivity extends AppCompatActivity implements BeaconConsumer{
    private BeaconManager beaconManager;
    private Region beaconRegion;
    private Button startButton;
    private Button stopButton;
    private TextView beaconIDText;
    private TextView beaconDistancesText;
    private Boolean doMonitoring = false;
    ArrayList<Beacon> currentBeaconList = new ArrayList<Beacon>();
    ArrayList<Beacon> allDetectedBeaconsList = new ArrayList<Beacon>();


    // BEACON LAYOUTS
    private static final String ALTBEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private static final String EDDYSTONE_TLM_LAYOUT = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
    private static final String EDDYSTONE_UID_LAYOUT = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
    private static final String EDDYSTONE_URL_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v";
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";


    // for calibration currently
    float totalRssi = 0f;
    float currentRssi;
    float rssiIndex = 1;

    // this call here just before the onCreate method
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);

        // for beaconManager singleton
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

        // bind beaconManager to this activity
        beaconManager.bind(this);

        // set average distance measurement period
        beaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000L);

        // get element from xml
        startButton = (Button) findViewById(R.id.button_Start);
        stopButton = (Button) findViewById(R.id.button_Stop);
        beaconIDText = (TextView) findViewById(R.id.textBeaconUUID);
        beaconDistancesText = (TextView) findViewById(R.id.text_Distances);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMonitoring();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMonitoring();
            }
        });
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        beaconManager.unbind(this);
    }


    // this service is called on a period looking for beacons around
    // later change this scan period for beacon data, default 1.1ms is not enough
    @Override
    public void onBeaconServiceConnect() {
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
                    // get prev beaconlist and clear current
                    ArrayList<Beacon> prevBeaconList = new ArrayList<Beacon>();
                    for(Beacon prevB: currentBeaconList)
                    {
                        prevBeaconList.add(prevB);
                    }
                    currentBeaconList.clear();

                    int beaconIndex = 1;
                    for(Beacon b: beacons)
                    {
                        b.setHardwareEqualityEnforced(true);

                        // put beacons on the list
                        currentBeaconList.add(b);
                        AddDetectedBeacon(b);

                        // for calibratio
                        //currentRssi = b.getRssi();
                        //totalRssi += currentRssi;
                        //float averageRssi = totalRssi / rssiIndex;
                        //beaconDistances.setText("total RSSI  : " + totalRssi + "\n");
                        //beaconDistances.setText(beaconDistances.getText() + "index       : " + rssiIndex + "\n");
                        //beaconDistances.setText(beaconDistances.getText() + "average RSSI: " + averageRssi + " dbm");
                        //rssiIndex+=1;
                        // for calibration

                        beaconIndex+=1;
                    }
                    // check if there is any update on beacon ID text
                    boolean prevCurrentEqual = equalCheck(prevBeaconList, currentBeaconList);
                    if(prevCurrentEqual == false)
                    {
                        beaconIDText.setText("");
                        for(Beacon idB: allDetectedBeaconsList)
                        {
                            if(currentBeaconList.contains(idB))
                            {
                                String id = idB.getId1().toString();
                                int bIndex = getBeaconIndex(idB)+1;
                                beaconIDText.setText(beaconIDText.getText() + "Beacon "+bIndex+": " + id + "\n");
                            }
                        }
                    }

                    // print distances for each beacon detected
                    beaconDistancesText.setText("");
                    for(Beacon disB: allDetectedBeaconsList)
                    {
                        int bIndex = getBeaconIndex(disB)+1;
                        if(currentBeaconList.contains(disB))
                        {
                            int cIndex = currentBeaconList.indexOf(disB);
                            String dis = String.format("%.1f", currentBeaconList.get(cIndex).getDistance()) + " meters";
                            if(dis == "0.0 meters")
                                dis = "0.1 meters";
                            beaconDistancesText.setText(beaconDistancesText.getText() + "Beacon "+bIndex+": "+dis+"\n");
                        }
                        else
                        {
                            String m = "NO SIGNAL";
                            beaconDistancesText.setText(beaconDistancesText.getText() + "Beacon "+bIndex+ ": " +m + "\n");
                        }
                    }
                }
                else
                {
                    // clear id and distance text
                    beaconIDText.setText("");
                    beaconDistancesText.setText("");
                    allDetectedBeaconsList.clear();
                    currentBeaconList.clear();
                }
            }
        });
    }

    private void startMonitoring()
    {
        doMonitoring = true;

        // for calibration
        totalRssi = 0f;
        rssiIndex = 1f;

        Log.d("beaconTag", "monitoring started");
        try{
            beaconManager.startRangingBeaconsInRegion(new Region("myRegion", null, null, null));
        }
        catch(RemoteException e) {Log.d("beaconTag", "error on start monitoring"); }
    }

    private void stopMonitoring()
    {
        doMonitoring = false;

        Log.d("beaconTag", "monitoring stopped");
        try{
            beaconManager.stopRangingBeaconsInRegion(new Region("myRegion", null, null, null));
        }
        catch(RemoteException e) {Log.d("beaconTag", "error on stop monitoring"); }
    }

    private void AddDetectedBeacon(Beacon dBeacon)
    {
        if(!allDetectedBeaconsList.contains(dBeacon))
        {
            allDetectedBeaconsList.add(dBeacon);
        }
    }

    private int getBeaconIndex(Beacon b)
    {
        return allDetectedBeaconsList.indexOf(b);
    }

    private boolean equalCheck(ArrayList prev, ArrayList current)
    {
        if(prev.size() != current.size())
            return false;

        int contains = 0;
        for(int i=0; i<current.size(); i++)
        {
            if(prev.contains(current.get(i)))
            {
                contains+=1;
            }
        }

        if(contains == current.size())
            return true;
        else
            return false;
    }

}
 */
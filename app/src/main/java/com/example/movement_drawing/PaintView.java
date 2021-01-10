package com.example.movement_drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import java.util.Timer;
import java.util.TimerTask;

// for test cases
// with timer user will be moving at 1m/s
// direction will be the inputs

// initially screen width is 10 meters, calculate that way

// attention on y direction

public class PaintView extends View {
    private static final String TAG = "PaintView";

    public LayoutParams params;
    private Path path = new Path();
    private Paint brush = new Paint();
    private Paint brushIcon = new Paint();
    private float pointX;
    private float pointY;
    private float incrementPerMeter;
    private float screenScale = 10f;       // 10 meters for canvas width

    private Timer updateTimer;
    private long updatePeriod = 1000l;

    // at first, horizontal right is direction
    private int horizontalDir = 1;
    private int verticalDir = 0;

    // Colors
    private Color colorBackground;

    private boolean checkStart = false;

    // constructor of the class here
    public PaintView(Context context) {
        super(context);
        checkStart = true;

        // brush to draw path
        brush.setAntiAlias(true);
        brush.setColor(getResources().getColor(R.color.colorPath));
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeWidth(20f);

        // brush to draw user character
        brushIcon.setAntiAlias(true);
        brushIcon.setColor(getResources().getColor(R.color.colorUserIcon));
        brushIcon.setStyle(Paint.Style.STROKE);
        brushIcon.setStrokeJoin(Paint.Join.ROUND);
        brushIcon.setStrokeWidth(50f);

        // layout settings
        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // run method every second
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(checkStart == false)
                {
                    updateTheDrawing();
                }
            }
        }, 0, updatePeriod);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // canvas background color
        canvas.drawColor(getResources().getColor(R.color.colorCanvasBackground));

        // put pointer on the center at first
        // init increment per meter in terms of screen coordinates
        if(checkStart)
        {
            checkStart = false;
            pointX = canvas.getWidth()/2;
            pointY = canvas.getHeight()/2;
            incrementPerMeter = ( (float) canvas.getWidth()) / screenScale;
            path.moveTo(pointX, pointY);
        }

        canvas.drawPath(path, brush);
        canvas.drawCircle(pointX, pointY, 10, brushIcon);
    }

    // update points, paths and refresh canvas
    private void updateTheDrawing()
    {
        pointX += (horizontalDir) * incrementPerMeter;
        pointY += (-verticalDir) * incrementPerMeter;
        path.lineTo(pointX, pointY);

        postInvalidate();
    }

    // update direction from main
    public void changeDirection(int hDir, int vDir)
    {
        horizontalDir = hDir;
        verticalDir = vDir;
    }
}



// FOR SOME INFO
// TOUCH FOR TESTING IF IT WORKS
// DELETE LATER
    /*
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // get the point of touch
        pointX = event.getX();
        pointY = event.getY();

        // get the action and register path
        // not drawing yet here
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(pointX, pointY);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(pointX, pointY);
                break;
            default:
                return false;
        }
        postInvalidate();
        return false;
    }
     */
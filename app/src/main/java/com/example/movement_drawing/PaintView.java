package com.example.movement_drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

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
    private Paint brushArrow = new Paint();
    private float iconRadius = 10f;

    private float pointX;
    private float pointY;
    private float incrementPerMeter;
    private float screenScale = 10f;       // 10 meters for canvas width

    private Timer updateTimer;
    private long updatePeriod = 1000l;

    // at first, horizontal right is direction
    private int horizontalDir = 1;
    private int verticalDir = 0;

    // FOR DIRECTION ILLUSTRATION
    private Drawable directionImage;
    private float imageRange;

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
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeWidth(20f);

        // brush to draw user character
        brushIcon.setAntiAlias(true);
        brushIcon.setColor(getResources().getColor(R.color.colorUserIcon));
        brushIcon.setStyle(Paint.Style.STROKE);
        brushIcon.setStrokeJoin(Paint.Join.ROUND);
        brushIcon.setStrokeCap(Paint.Cap.ROUND);
        brushIcon.setStrokeWidth(50f);

        //
        brushArrow.setAntiAlias(true);
        brushArrow.setColor(getResources().getColor(R.color.colorUserIcon));
        brushArrow.setStyle(Paint.Style.STROKE);
        brushArrow.setStrokeJoin(Paint.Join.ROUND);
        brushArrow.setStrokeCap(Paint.Cap.ROUND);
        brushArrow.setStrokeWidth(15f);

        // layout settings
        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // get direction image from drawable
        directionImage = ResourcesCompat.getDrawable(getResources(), R.drawable.small_arrow, null);
        directionImage.setColorFilter(new PorterDuffColorFilter(
                getResources().getColor(R.color.colorDirectionArrow),PorterDuff.Mode.MULTIPLY));
        imageRange = 7*iconRadius;

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
    protected void onDraw(Canvas canvas)  {
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
        canvas.drawCircle(pointX, pointY, iconRadius, brushIcon);

        directionImage.setBounds( (int)(pointX+35), (int)(pointY-35), (int)(pointX+105), (int)(pointY+35));
        directionImage.draw(canvas);

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




// TRIED METHODS

// FOR SOME INFO
// TOUCH FOR TESTING IF IT WORKS
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


// DRAWING ARROW IN THE DIRECTION (NOT WORKING GOOD ENOUGH)

    /*
    private void assignArrowPoints(int hDir, int vDir)
    {
        // first point assigned
        arrowFirstPointX = pointX + (horizontalDir * (10 * iconRadius));
        arrowFirstPointY = pointY - (verticalDir * (10 * iconRadius));

        // directions from the user character circle
        int pointSecond_hDir = 0, pointSecond_vDir = 0;
        int pointThird_hDir = 0, pointThird_vDir = 0;

        if(hDir != 0 && vDir != 0)
        {
            // this covers 4 directions
            pointSecond_hDir = -hDir;
            pointSecond_vDir = vDir;
            pointThird_hDir = hDir;
            pointThird_vDir = -vDir;
        }
        else if(hDir == 0)
        {
            // this covers 2 directions
            pointSecond_hDir = 1;
            pointSecond_vDir = 0;
            pointThird_hDir = -1;
            pointThird_vDir = 0;
        }
        else if(vDir == 0)
        {
            // this covers 2 directions
            pointSecond_hDir = 0;
            pointSecond_vDir = 1;
            pointThird_hDir = 0;
            pointThird_vDir = -1;
        }

        // assign second arrow point
        arrowSecondPointX = pointX + (pointSecond_hDir * (6 * iconRadius));
        arrowSecondPointY = pointY - (pointSecond_vDir * (6 * iconRadius));

        // assign third arrow point
        arrowThirdPointX = pointX + (pointThird_hDir * (6 * iconRadius));
        arrowThirdPointY = pointY - (pointThird_vDir * (6 * iconRadius));
    }

     */
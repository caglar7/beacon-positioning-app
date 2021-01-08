package com.example.movement_drawing;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class PaintView extends View {

    public LayoutParams params;
    private Path path = new Path();
    private Paint brush = new Paint();
    private Paint brushIcon = new Paint();
    private float pointX;
    private float pointY;

    // constructor of the class here
    public PaintView(Context context) {
        super(context);

        brush.setAntiAlias(true);
        brush.setColor(Color.GRAY);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeWidth(20f);

        brushIcon.setAntiAlias(true);
        brushIcon.setColor(Color.BLUE);
        brushIcon.setStyle(Paint.Style.STROKE);
        brushIcon.setStrokeJoin(Paint.Join.ROUND);
        brushIcon.setStrokeWidth(50f);

        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    // TOUCH FOR TESTING IF IT WORKS
    // DELETE LATER
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

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, brush);
        canvas.drawCircle(pointX, pointY, 10, brushIcon);
    }
}

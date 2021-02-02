package com.example.movement_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import java.util.ArrayList;

// for test cases
// with timer user will be moving at 1m/s
// direction will be the inputs

// initially screen width is 10 meters, calculate that way

// attention on y direction

public class PaintView extends View {
    private static final String TAG = "PaintView";

    private boolean changeScale = false;
    public LayoutParams params;
    private Path path = new Path();
    private Paint brush = new Paint();
    private Paint brushIcon = new Paint();
    private Paint brushArrow = new Paint();
    private Paint brushBeaconRadius = new Paint();
    private Paint brushIntersections = new Paint();
    private Paint brushBeaconRegion = new Paint();
    private Paint brushCleaning = new Paint();          // will be deleted, test for now
    private int iconRadius = 10;

    private float pointX;
    private float pointY;
    private float pixelPerMeter;

    // SCREEN SCALING
    private float canvasScale = 1f;
    private float initialScreenScale = 10f;             // not changing 10m
    private float screenScale = 10f;                    // changing one
    private float leftBoundary, topBoundary;
    private float rightBoundary, downBoundary;
    private float leftEdge, topEdge, rightEdge, downEdge;
    private boolean setInitBoundaries = false;
    private boolean setScale20 = true;              // 20m screen scale at first

    // at first, horizontal right is direction
    private int horizontalDir = 0;
    private int verticalDir = 1;

    // FOR DIRECTION ILLUSTRATION
    private Drawable directionImage;
    private Bitmap bitmapDirectionOriginal;
    private Bitmap bitmapDirectionResult;
    private int imageRange;
    private float directionImageX, directionImageY;
    private float offsetOrtho, offsetAngular;

    // Colors
    private Color colorBackground;

    private boolean checkStart = false;

    private float rotation = -90;

    // indent level for boundary check
    private float indentValue = 50;

    // for drawing beacon circles
    private float beaconRadius = 0;
    private Boolean drawBeaconCircle = false;
    private ArrayList<Float> beaconCircles_XPoints = new ArrayList<Float>();
    private ArrayList<Float> beaconCircles_YPoints = new ArrayList<Float>();
    private ArrayList<Float> beaconCircles_RValues = new ArrayList<Float>();
    private float scanPeriod = 5;     // draw circle once in every period
    private float scanCheck = 0;

    // these lists are for determining better beacon circles
    private ArrayList<Float> average_RValues = new ArrayList<Float>();
    private ArrayList<Float> average_XPoints = new ArrayList<Float>();
    private ArrayList<Float> average_YPoints = new ArrayList<Float>();
    private int topAverageRange = 2;            // take top 2 points for avg

    // centroid positioning system parameters
    private ArrayList<Float> intersection_XPoints = new ArrayList<Float>();
    private ArrayList<Float> intersection_YPoints = new ArrayList<Float>();
    private float intersectionRadius = 0.25f;
    private float intersectionTotalX = 0;
    private float intersectionTotalY = 0;
    private float intersectionCenterX = 0;
    private float intersectionCenterY = 0;
    private float beaconRegionR = 2.5f;
    // deleting some of the intersection points, furthest point for better accuracy
    private Boolean checkIntersectionClean = false;
    private int cleanStartSize = 20;
    private float furtherRange = 10;                // furthest 20 percent points
    private float intersectionFurthestDistance;
    private float furtherBoundary;

    // for test
    private float cleaningRange;

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

        // temp arrow image brush
        brushArrow.setAntiAlias(true);
        brushArrow.setColor(getResources().getColor(R.color.colorUserIcon));
        brushArrow.setStyle(Paint.Style.STROKE);
        brushArrow.setStrokeJoin(Paint.Join.ROUND);
        brushArrow.setStrokeCap(Paint.Cap.ROUND);
        brushArrow.setStrokeWidth(15f);

        brushBeaconRadius.setAntiAlias(true);
        brushBeaconRadius.setColor(getResources().getColor(R.color.colorBeaconRadius));
        brushBeaconRadius.setStyle(Paint.Style.FILL);
        brushBeaconRadius.setStrokeJoin(Paint.Join.ROUND);
        brushBeaconRadius.setStrokeCap(Paint.Cap.ROUND);
        brushBeaconRadius.setStrokeWidth(0f);

        brushIntersections.setAntiAlias(true);
        brushIntersections.setColor(getResources().getColor(R.color.colorIntersections));
        brushIntersections.setStyle(Paint.Style.FILL);
        brushIntersections.setStrokeJoin(Paint.Join.ROUND);
        brushIntersections.setStrokeCap(Paint.Cap.ROUND);
        brushIntersections.setStrokeWidth(0f);

        brushBeaconRegion.setAntiAlias(true);
        brushBeaconRegion.setColor(getResources().getColor(R.color.colorBeaconRegion));
        brushBeaconRegion.setStyle(Paint.Style.FILL);
        brushBeaconRegion.setStrokeJoin(Paint.Join.ROUND);
        brushBeaconRegion.setStrokeCap(Paint.Cap.ROUND);
        brushBeaconRegion.setStrokeWidth(0f);

        brushCleaning.setAntiAlias(true);
        brushCleaning.setColor(getResources().getColor(R.color.colorBeaconRegion));
        brushCleaning.setStyle(Paint.Style.STROKE);
        brushCleaning.setStrokeJoin(Paint.Join.ROUND);
        brushCleaning.setStrokeCap(Paint.Cap.ROUND);
        brushCleaning.setStrokeWidth(20f);


        // layout settings
        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // to set arrow image properly, these are offsets
        imageRange = 7*iconRadius;
        offsetOrtho = 4*iconRadius;
        offsetAngular = 4*iconRadius;           // for now


        // get direction image from drawable
        //directionImage = ResourcesCompat.getDrawable(getResources(), R.drawable.small_arrow, null);
        bitmapDirectionOriginal = BitmapFactory.decodeResource(this.getResources(), R.drawable.small_arrow);
        bitmapDirectionResult = Bitmap.createScaledBitmap(bitmapDirectionOriginal, imageRange, imageRange, false);


    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // put pointer on the center at first
        // init increment per meter in terms of screen coordinates
        if(checkStart)
        {
            checkStart = false;
            pointX = canvasWidth/2;
            pointY = canvasHeight/2;
            pixelPerMeter = ( (float)canvasWidth) / screenScale;
            intersectionRadius *= pixelPerMeter;
            beaconRegionR *= pixelPerMeter;
            path.moveTo(pointX, pointY);

            // for test
            cleaningRange = 100*pixelPerMeter;
        }

        if(setInitBoundaries == false)
        {
            // set init edge points first
            leftEdge = 0;
            topEdge = 0;
            rightEdge = canvasWidth;
            downEdge = canvasHeight;

            setInitBoundaries = true;
            leftBoundary = leftEdge + indentValue;
            topBoundary = topEdge + indentValue;
            rightBoundary = rightEdge - indentValue;
            downBoundary = downEdge - indentValue;
        }

        // check the boundaries, update boundaries, 10 meters range
        if((pointX < leftBoundary) || (pointX > rightBoundary) ||
                (pointY < topBoundary) || (pointY > downBoundary)
                || (setScale20 == true))
        {
            setScale20 = false;
            changeScale = true;
            screenScale += 10;

            // edge points for different scales
            float widthHalf = ((float)canvasWidth / 2);
            float heightHalf = ((float)canvasHeight / 2);
            leftEdge = -(widthHalf * ((screenScale/initialScreenScale)-1));
            topEdge = -(heightHalf * ((screenScale/initialScreenScale)-1));
            rightEdge = canvasWidth + (widthHalf * ((screenScale/initialScreenScale)-1));
            downEdge = canvasHeight + (heightHalf * ((screenScale/initialScreenScale)-1));

            // boundary points
            leftBoundary = leftEdge + indentValue;
            topBoundary = topEdge + indentValue;
            rightBoundary = rightEdge - indentValue;
            downBoundary = downEdge - indentValue;
        }

        canvas.save();
        // scaling condition
        if(changeScale)
        {
            changeScale = false;
            canvasScale = initialScreenScale/screenScale;
            MainActivity.tv_ScaleLevel.setText("<--- " + (int)screenScale + " METERS --->");
        }
        canvas.scale(canvasScale, canvasScale, canvasWidth/2, canvasHeight/2);

        // CANVAS BACKGROUND DRAW
        canvas.drawColor(getResources().getColor(R.color.colorCanvasBackground));

        // BEACON CIRCLES
        for(int i=0; i<beaconCircles_RValues.size(); i++)
        {
            float centerX = beaconCircles_XPoints.get(i);
            float centerY = beaconCircles_YPoints.get(i);
            float circleR = beaconCircles_RValues.get(i);
            canvas.drawCircle(centerX, centerY, circleR, brushBeaconRadius);
        }

        // INTERSECTION POINTS
        intersectionTotalX = 0f;
        intersectionTotalY = 0f;
        int intersectionSize = intersection_XPoints.size();
        if(intersectionSize > 0)
        {
            for(int i=0; i<intersection_XPoints.size(); i++)
            {
                float interX = intersection_XPoints.get(i);
                float interY = intersection_YPoints.get(i);
                canvas.drawCircle(interX, interY, intersectionRadius, brushIntersections);
                intersectionTotalX += interX;
                intersectionTotalY += interY;
            }
            intersectionCenterX = intersectionTotalX/((float)intersectionSize);
            intersectionCenterY = intersectionTotalY/((float)intersectionSize);
        }

        // INTERSECTION CLEANING
        if((checkIntersectionClean == true) && (intersectionSize > cleanStartSize))
        {
            intersectionFurthestDistance = 0;
            ArrayList<Float> intersectionDistances = new ArrayList<Float>();
            checkIntersectionClean = false;
            // first find furthest intersection point distance
            for(int i=0; i<intersection_XPoints.size(); i++)
            {
                float interX = intersection_XPoints.get(i);
                float interY = intersection_YPoints.get(i);
                double xDiffSquare = Math.pow((interX-intersectionCenterX), 2);
                double yDiffSquare = Math.pow((interY-intersectionCenterY), 2);
                double dis = Math.sqrt(xDiffSquare + yDiffSquare);

                intersectionDistances.add((float)dis);
                if(dis > intersectionFurthestDistance)
                {
                    intersectionFurthestDistance = (float)dis;
                }
            }
            // cleaning points further from the boundary
            furtherBoundary = ((100f-furtherRange)/100f) * intersectionFurthestDistance;
            ArrayList<Float> tempPointsX = new ArrayList<Float>();
            ArrayList<Float> tempPointsY = new ArrayList<Float>();

            for(int d=0; d<intersectionSize; d++)
            {
                float dis = intersectionDistances.get(d);
                if(dis < furtherBoundary)
                {
                    tempPointsX.add(intersection_XPoints.get(d));
                    tempPointsY.add(intersection_YPoints.get(d));
                }
            }
            intersection_XPoints.clear();
            intersection_YPoints.clear();
            intersection_XPoints.addAll(tempPointsX);
            intersection_YPoints.addAll(tempPointsY);
            cleaningRange = furtherBoundary;
        }

        // draw red beacon region
        if(intersectionSize>0)
            canvas.drawCircle(intersectionCenterX, intersectionCenterY, beaconRegionR, brushBeaconRegion);

        // draw cleaning boundary, range is assign when we got a proper value to observe
        if(intersectionSize > 0)
            canvas.drawCircle(intersectionCenterX, intersectionCenterY, cleaningRange, brushCleaning);

        // draw circle and path
        canvas.drawPath(path, brush);
        canvas.drawCircle(pointX, pointY, iconRadius, brushIcon);

        // create proper rotate bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        int bitmapW = bitmapDirectionResult.getWidth();
        int bitmapH = bitmapDirectionResult.getHeight();
        Bitmap rotatedDirectionImage = Bitmap.createBitmap(bitmapDirectionResult, 0, 0, bitmapW, bitmapH, matrix, true);

        // assign top left and draw arrows
        assignArrowTopLeft(pointX, pointY, horizontalDir, verticalDir);
        canvas.drawBitmap(rotatedDirectionImage, directionImageX, directionImageY, brushArrow);


        canvas.restore();
    }

    // update points, paths and refresh canvas
    // update the drawing at every step
    // with proper increment
    public void updateTheDrawing(float stepLength)
    {
        if(pixelPerMeter >0)
        {
            // find pixel value of one step using pixelPerMeter
            float incrementPerStep = pixelPerMeter * stepLength;
            if(horizontalDir != 0 && verticalDir != 0)
            {
                incrementPerStep = ((incrementPerStep) / (float)Math.sqrt(2d));
            }

            pointX += (horizontalDir) * incrementPerStep;
            pointY += (-verticalDir) * incrementPerStep;
            path.lineTo(pointX, pointY);

            postInvalidate();
        }
    }

    public void updateBeaconCircles(float bRadius)
    {
        if(pixelPerMeter > 0)
        {
            float topAverageX=0, topAverageY=0, topAverageR=0;
            beaconRadius = pixelPerMeter * bRadius;

            // run once in every scanPeriod
            // take top 2 values from the measurements and draw circle
            scanCheck++;
            average_RValues.add(beaconRadius);
            average_XPoints.add(pointX);
            average_YPoints.add(pointY);
            if(scanCheck != scanPeriod)
               return;
            else
            {
                scanCheck = 0f;
                for(int i=0; i<average_XPoints.size(); i++)
                {
                    topAverageX += average_XPoints.get(i);
                    topAverageY += average_YPoints.get(i);
                    topAverageR += average_RValues.get(i);
                }
                topAverageX = topAverageX/scanPeriod;
                topAverageY = topAverageY/scanPeriod;
                topAverageR = topAverageR/scanPeriod;

                // clear lists the second period
                average_RValues.clear();
                average_XPoints.clear();
                average_YPoints.clear();
            }

            // if you are just standing, don't draw circles yet
            if(beaconCircles_XPoints.size() > 0)
            {
                float lastX = beaconCircles_XPoints.get(beaconCircles_XPoints.size()-1);
                float lastY = beaconCircles_YPoints.get(beaconCircles_YPoints.size()-1);
                if(lastX == topAverageX && lastY==topAverageY)
                    return;
            }

            // before adding new beacon circle to the list
            // find intersection points, using topAverage values for this case
            // later we gonna use normal average values
            if(beaconCircles_RValues.size() > 0)
            {
                for(int i=0; i<beaconCircles_RValues.size(); i++)
                {
                    // check for the intersection at first
                    float compareCircleR = beaconCircles_RValues.get(i);
                    float compareCircleX = beaconCircles_XPoints.get(i);
                    float compareCircleY = beaconCircles_YPoints.get(i);
                    float xDiffSquare = (float)Math.pow((topAverageX-compareCircleX), 2);
                    float yDiffSquare = (float)Math.pow((topAverageY-compareCircleY), 2);
                    float circleDistance = (float)Math.sqrt(xDiffSquare+yDiffSquare);
                    if(circleDistance <= (topAverageR+compareCircleR))
                    {
                        // when there is an intersection
                        if(circleDistance != 0)
                        {
                            // 2 intersection points
                            double d = circleDistance;
                            double a = ( (Math.pow(compareCircleR,2)) - (Math.pow(topAverageR,2))
                                        + (Math.pow(d, 2)) ) / (d * 2);
                            double h = Math.sqrt((Math.pow(compareCircleR, 2)) - Math.pow(a, 2));
                            double xcenter = compareCircleX + ( (a *(topAverageX-compareCircleX)) / d);
                            double ycenter = compareCircleY + ( (a *(topAverageY-compareCircleY)) / d);

                            // intersection point1
                            double interX1 = xcenter + ((h*(topAverageY-compareCircleY)) / d);
                            double interY1 = ycenter - ((h*(topAverageX-compareCircleX)) / d);

                            // intersection point2
                            double interX2 = xcenter - ((h*(topAverageY-compareCircleY)) / d);
                            double interY2 = ycenter + ((h*(topAverageX-compareCircleX)) / d);

                            if(!Double.isNaN(interX1))
                            {
                                intersection_XPoints.add((float)interX1);
                                intersection_YPoints.add((float)interY1);
                                intersection_XPoints.add((float)interX2);
                                intersection_YPoints.add((float)interY2);
                            }
                            checkIntersectionClean = true;
                        }
                    }
                }
            }

            // add beacon circle to the list
            beaconCircles_XPoints.add(topAverageX);
            beaconCircles_YPoints.add(topAverageY);
            beaconCircles_RValues.add(topAverageR);

            String tavgR = String.format("%.1f", topAverageR/pixelPerMeter);
            Toast.makeText(getContext(), "Beacon Radius: " + tavgR, Toast.LENGTH_SHORT).show();

            postInvalidate();
        }
    }

    // update direction from main
    public void changeDirection(float userRotation)
    {
        // get current angle
        float currentAngle = getDegree(horizontalDir, verticalDir);

        // get rotated angle and turn radians
        float rotatedAngle = currentAngle - userRotation;
        double rotatedAngle_r = Math.toRadians(rotatedAngle);

        // get horizontal and vertical sin and cos
        double cosH = Math.cos(rotatedAngle_r);
        double sinV = Math.sin(rotatedAngle_r);

        // values will have -1 0 or 1
        int rotated_hDir = (int)Math.rint(cosH);
        int rotated_vDir = (int)Math.rint(sinV);

        // assign rotated direction to global variables
        horizontalDir = rotated_hDir;
        verticalDir = rotated_vDir;
        rotation = -rotatedAngle;

        postInvalidate();
    }

    private float getDegree(int hDir, int vDir)
    {
        float resultDegree = 0;
        if(hDir == 1)
        {
            resultDegree = 45 * vDir;
        }
        else if(hDir == 0)
        {
            resultDegree = 90 * vDir;
        }
        else if(hDir == -1)
        {
            resultDegree = 180 - (vDir * 45);
        }

        if(resultDegree < 0)
        {
            resultDegree+=360;
        }
        return resultDegree;
    }

    // ASSIGN directionImage X Y TO HAVE ARROW IMAGE AT PROPER POSITIONS
    private void assignArrowTopLeft(float centerX, float centerY, int hDir, int vDir)
    {
        float tempX = 0, tempY = 0;

        if(hDir == 0)       // orthogonal cases
        {
            tempX = centerX - (imageRange/2);
            tempY = centerY - (vDir * offsetOrtho);
            if(vDir == 1)
                tempY -= imageRange;
        }else if(vDir == 0)
        {
            tempY = centerY -(imageRange/2);
            tempX = centerX + (hDir * offsetOrtho);
            if(hDir == -1)
                tempX -= imageRange;
        }else               // angled cases
        {
            float offsetAxis = offsetAngular / (float)(Math.sqrt(2));
            tempX = centerX + (hDir * offsetAxis);
            tempY = centerY - (vDir * offsetAxis);
            if(hDir==-1)
                tempX -= imageRange;
            if(vDir==1)
                tempY -= imageRange;
        }

        // assign values to direction image top left points
        directionImageX = tempX;
        directionImageY = tempY;

        // hard code here for angular directions
        if(hDir == 1 && vDir == -1)
        {
            directionImageX -= 2.5*iconRadius;
            directionImageY -= 2.5*iconRadius;
        }
        if(hDir == 1 && vDir == 1)
        {
            directionImageX -= 2.5*iconRadius;
            directionImageY -= 0.5*iconRadius;
        }
        if(hDir == -1 && vDir == -1)
        {
            directionImageX -= 0.5*iconRadius;
            directionImageY -= 2.5*iconRadius;
        }
        if(hDir == -1 && vDir == 1)
        {
            directionImageX -= 0.5*iconRadius;
            directionImageY -= 0.5*iconRadius;
        }
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


// this assigns 2 boundary points for direction image
// boundary points are initialized above
    /*
    private void assignBoundPoints(int hDir, int vDir) {
        // ASSIGN TOP LEFT BOUNDARY
        // arrowPointX1 and arrowPointY1
        if(hDir != 0 && vDir != 0)      // covers 4 directions
        {
            if( (hDir+vDir) == 0 )
            {
                arrowPoint_x1 = pointX + (float)(hDir*hypo1);
                arrowPoint_y1 = pointY;
            }
            else
            {
                arrowPoint_x1 = pointX;
                arrowPoint_y1 = pointY - (float)(hDir*hypo1);
            }
        }
        else if(hDir == 0)
        {
            arrowPoint_x1 = pointX - (vDir * (imageRange/2));
            arrowPoint_y1 = pointY - (vDir * (imageRange/2));
        }
        else if(vDir == 0)
        {
            arrowPoint_x1 = pointX + (hDir * (imageRange/2));
            arrowPoint_y1 = pointY - (hDir * (imageRange/2));
        }

        // get center and arrowPoint2 angle
        double centerAngle = getDegree(hDir, vDir);
        double arrowPoint2_Angle = centerAngle - 45;
        if (arrowPoint2_Angle < 0)
            arrowPoint2_Angle+=360;

        double centerAngle_Radians = Math.toRadians(centerAngle);
        double arrowPoint2_Angle_Radians = Math.toRadians(arrowPoint2_Angle);

        // find arrow point2 directions from its angle
        int arrowPoint2_hDir, arrowPoint2_vDir;

        // GETS CENTER, ASSIGNS DIRECTIONS TO ARROW POINT 2
        if((arrowPoint2_Angle%90) == 0)
        {
            arrowPoint2_hDir = (int)Math.cos(arrowPoint2_Angle_Radians);
            arrowPoint2_vDir = (int)Math.sin(arrowPoint2_Angle_Radians);
        }
        else
        {
            if(arrowPoint2_Angle < 180)
                arrowPoint2_vDir = 1;
            else
                arrowPoint2_vDir = -1;

            if(Math.cos(arrowPoint2_Angle_Radians) > 0)
                arrowPoint2_hDir = 1;
            else
                arrowPoint2_hDir = -1;
        }

        // ASSIGN arrowPoint2 x and y

        if((arrowPoint2_Angle%90) == 0)
        {
            arrowPoint_x2 = arrowPoint_x1 + (float)(arrowPoint2_hDir * hypo2);
            arrowPoint_y2 = arrowPoint_y1 - (float)(arrowPoint2_vDir * hypo2);
        }
        else
        {
            arrowPoint_x2 = arrowPoint_x1 + (arrowPoint2_hDir * imageRange);
            arrowPoint_y2 = arrowPoint_y1 - (arrowPoint2_vDir * imageRange);
        }
    }
     */


// look for the top 2 values in topAverage lists
/*
for(int i=0; i<topAverageRange; i++)
{
    float topValueR = average_RValues.get(0);
    float topPointX, topPointY;
    for(float r: average_RValues)
    {
        if(r > topValueR)
        {
            topValueR = r;
        }
    }
    int topIndex = average_RValues.indexOf(topValueR);
    topPointX = average_XPoints.get(topIndex);
    topPointY = average_YPoints.get(topIndex);
    topAverageR += topValueR;
    topAverageX += topPointX;
    topAverageY += topPointY;
    average_RValues.remove(topIndex);
    average_XPoints.remove(topIndex);
    average_YPoints.remove(topIndex);
}
// find topAverage R, x and y points, 2 will change
topAverageR = topAverageR / topAverageRange;
topAverageX = topAverageX / topAverageRange;
topAverageY = topAverageY / topAverageRange;
*/
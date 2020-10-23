package inc.flide.vim8.views.mainKeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import inc.flide.vim8.MainInputMethodService;
import inc.flide.vim8.R;
import inc.flide.vim8.geometry.Circle;
import inc.flide.vim8.geometry.GeometricUtilities;
import inc.flide.vim8.geometry.LineSegment;
import inc.flide.vim8.keyboardActionListners.MainKeyboardActionListener;
import inc.flide.vim8.structures.Constants;
import inc.flide.vim8.structures.FingerPosition;
import inc.flide.vim8.utilities.Utilities;

public class XboardView extends View {

    private MainKeyboardActionListener actionListener;

    private Circle circle;

    private Paint paint = new Paint();
    private Paint shader_paint = new Paint();
    private Path path = new Path();

    float pathPos[]=new float[2];
    Context context;

    GestureDetector gestureDetector;

    public XboardView(Context context) {
        super(context);
        initialize(context);
    }

    public XboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
        gestureDetector = new GestureDetector(context,new GestureListener());
        this.context = context;



    }


    public XboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        actionListener = new MainKeyboardActionListener((MainInputMethodService) context, this);
        setHapticFeedbackEnabled(true);
    }

    private final int offset = 15;
    private final int lengthOfLineDemarcatingSectors = 250;

    @Override
    public void onDraw(Canvas canvas) {

        paint.setARGB(255, 255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);

        //background colouring
        canvas.drawColor(paint.getColor());

        paint.setARGB(255, 0, 0, 0);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        //applying the condition for switch

        SharedPreferences sp = this.getContext().getSharedPreferences(this.getContext().getString(R.string.basic_preference_file_name), Activity.MODE_PRIVATE);
        String current_switch_value = sp.getString(this.getContext().getString(R.string.switch_mode),"");

        String setValue = "true";

        if(current_switch_value.equals(setValue))
        {
            if (path != null) {
                final short steps = 150;
                final byte stepDistance = 5;
                final byte maxTrailRadius = 14;
                PathMeasure pathMeasure = new PathMeasure();
                pathMeasure.setPath(path, false);
                Random random = new Random();
                final float pathLength = pathMeasure.getLength();
                for (short i = 1; i <= steps; i++) {
                    final float distance = pathLength - i * stepDistance;
                    if (distance >= 0) {
                        final float trailRadius = maxTrailRadius * (1 - (float) i / steps);
                        pathMeasure.getPosTan(distance, pathPos, null);
                        final float x = pathPos[0] + random.nextFloat() - trailRadius;
                        final float y = pathPos[1] + random.nextFloat() - trailRadius;
                        shader_paint.setShader(new RadialGradient(
                                x,
                                y,
                                trailRadius > 0 ? trailRadius : Float.MIN_VALUE,
                                ColorUtils.setAlphaComponent(Color.GREEN, random.nextInt(0xff)),
                                Color.TRANSPARENT,
                                Shader.TileMode.CLAMP
                        ));
                        canvas.drawCircle(x, y, trailRadius, shader_paint);
                    }
                }
            }
            canvas.drawPath(path,shader_paint);

        }

        //The centre circle
        canvas.drawCircle(circle.getCentre().x, circle.getCentre().y, circle.getRadius(), paint);
        //The lines demarcating the sectors
        List<LineSegment> sectorDemarcatingLines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int angle = 45 + (i * 90);
            PointF startingPoint = circle.getPointOnCircumferenceAtDegreeAngle(angle);
            LineSegment lineSegment = new LineSegment(startingPoint, angle, lengthOfLineDemarcatingSectors);
            sectorDemarcatingLines.add(lineSegment);

            canvas.drawLine(lineSegment.getStartingPoint().x, lineSegment.getStartingPoint().y, lineSegment.getEndPoint().x, lineSegment.getEndPoint().y, paint);
        }

        //the text along the lines
        paint.setTextSize(Constants.TEXT_SIZE);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.black));
        Typeface font = Typeface.createFromAsset(getContext().getAssets(),
                "SF-UI-Display-Regular.otf");
        paint.setTypeface(font);


        float characterHeight = paint.getFontMetrics().descent - paint.getFontMetrics().ascent;
        String charactersToDisplay = getCharacterSetToDisplay();
        List<PointF> listOfPointsOfDisplay = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LineSegment currentLine = sectorDemarcatingLines.get(i);
            listOfPointsOfDisplay.addAll(getCharacterDisplayPointsOnTheLineSegment(currentLine, 4, characterHeight));
        }
        float[] pointsOfDisplay = Utilities.convertPointFListToPrimitiveFloatArray(listOfPointsOfDisplay);
        canvas.drawPosText(charactersToDisplay, pointsOfDisplay, paint);

    }


    private String getCharacterSetToDisplay() {
        String characterSetSmall = "nomufv!weilhkj@,tscdzg.'yabrpxq?";
        String characterSetCaps = "NOMUFV!WEILHKJ@_TSCDZG-\"YABRPXQ*";

        if (actionListener.areCharactersCapitalized()) {
            return characterSetCaps;
        }

        return characterSetSmall;
    }

    private List<PointF> getCharacterDisplayPointsOnTheLineSegment(LineSegment lineSegment, int numberOfCharactersToDisplay, float height) {
        List<PointF> pointsOfCharacterDisplay = new ArrayList<>();

        //Assuming we got to derive 4 points
        double spacingBetweenPoints = lineSegment.getLength() / numberOfCharactersToDisplay;

        for (int i = 0; i < 4; i++) {
            PointF nextPoint = GeometricUtilities.findPointSpecifiedDistanceAwayInGivenDirection(lineSegment.getStartingPoint(), lineSegment.getDirectionOfLineInDegree(), (spacingBetweenPoints * i));
            PointF displayPointInAntiClockwiseDirection = new PointF(nextPoint.x + computeAntiClockwiseXOffset(lineSegment, height)
                    , nextPoint.y + computeAntiClockwiseYOffset(lineSegment, height));

            PointF displayPointInClockwiseDirection = new PointF(nextPoint.x + computeClockwiseXOffset(lineSegment, height)
                    , nextPoint.y + computeClockwiseYOffset(lineSegment, height));

            pointsOfCharacterDisplay.add(displayPointInAntiClockwiseDirection);
            pointsOfCharacterDisplay.add(displayPointInClockwiseDirection);
        }
        return pointsOfCharacterDisplay;
    }

    private float computeClockwiseYOffset(LineSegment lineSegment, float height) {
        double angle = lineSegment.getDirectionOfLineInDegree();
        boolean isXDirectionPositive = (angle > 0 && angle < 90) || (angle > 270 && angle < 360);

        if (lineSegment.isSlopePositive()) {
            return offset + (isXDirectionPositive ? height : -height);
        }


        return 0;
    }

    private float computeAntiClockwiseYOffset(LineSegment lineSegment, float height) {
        double angle = lineSegment.getDirectionOfLineInDegree();
        boolean isXDirectionPositive = (angle > 0 && angle < 90) || (angle > 270 && angle < 360);
        if (lineSegment.isSlopePositive()) {
            return offset;
        }
        return isXDirectionPositive ? -height : height;
    }

    private float computeClockwiseXOffset(LineSegment lineSegment, float height) {
        double angle = lineSegment.getDirectionOfLineInDegree();
        boolean isXDirectionPositive = (angle > 0 && angle < 90) || (angle > 270 && angle < 360);

        if (lineSegment.isSlopePositive()) {
            return 0;
        }
        return isXDirectionPositive ? height : -height;
    }

    private float computeAntiClockwiseXOffset(LineSegment lineSegment, float height) {
        double angle = lineSegment.getDirectionOfLineInDegree();
        boolean isXDirectionPositive = (angle > 0 && angle < 90) || (angle > 270 && angle < 360);

        if (lineSegment.isSlopePositive()) {
            return isXDirectionPositive ? height : -height;
        }
        return 0;
    }

    private FingerPosition getCurrentFingerPosition(PointF position) {
        if (circle.isPointInsideCircle(position)) {
            return FingerPosition.INSIDE_CIRCLE;
        } else {
            return circle.getSectorOfPoint(position);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        // event when double tap occurs


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            path.addCircle(x, y, 50, Path.Direction.CW);

            // clean drawing area on double tap
            path.reset();

            Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");

            return true;
        }

    }

        @Override
       public boolean onTouchEvent(MotionEvent e) {
        PointF position = new PointF((int) e.getX(), (int) e.getY());
        FingerPosition currentFingerPosition = getCurrentFingerPosition(position);
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                 actionListener.movementStarted(currentFingerPosition);
                 path.moveTo(e.getX(),e.getY());
                return true;

            case MotionEvent.ACTION_MOVE:
                actionListener.movementContinues(currentFingerPosition);
                path.lineTo(e.getX(), e.getY());
                break;

            case MotionEvent.ACTION_UP:
                actionListener.movementEnds();
            case MotionEvent.ACTION_POINTER_DOWN:
                 path.reset();
                break;
            default:
                return false;
        }
            gestureDetector.onTouchEvent(e);
            // Schedules a repaint.

            invalidate();
        return true;
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            //Landscape is just un-usable right now.
            // TODO: Landscape mode requires more clarity, what exactly do you want to do?
            width = Math.round(1.33f * height);
        } else  // Portrait mode
        {
            height = Math.round(0.8f * width);
        }

        SharedPreferences sp = this.getContext().getSharedPreferences(this.getContext().getString(R.string.basic_preference_file_name), Activity.MODE_PRIVATE);
        float spRadiusValue = sp.getFloat(this.getContext().getString(R.string.x_board_circle_radius_size_factor_key), 0.3f);
        float radius = (spRadiusValue * width) / 2;

        PointF centre = new PointF((width / 2), (height / 2));
        centre.x = centre.x + ((sp.getInt(this.getContext().getString(R.string.x_board_circle_centre_x_offset_key), 0)) * 26);
        centre.y = centre.y + ((sp.getInt(this.getContext().getString(R.string.x_board_circle_centre_y_offset_key), 0)) * 26);

        circle = new Circle(centre, radius);

        setMeasuredDimension(width, height);
    }

}

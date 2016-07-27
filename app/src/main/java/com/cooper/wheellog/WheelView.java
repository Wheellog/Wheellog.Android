package com.cooper.wheellog;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Locale;

public class WheelView extends View {

//    Context mContext;
    Paint outerArcPaint;
    Paint innerArcPaint;
    Paint speedTextPaint;
    final RectF outerRect = new RectF();
    final RectF innerRect = new RectF();
    final Rect speedTextRect = new Rect();
    float speedTextSize;
    float speedTextKPHSize;
    float speedTextKPHHeight;

    int mMaxSpeed = 300;
    int mSpeed = 10;
    int mBattery = 10;
    int mTemperature = 10;

    float outerStrokeWidth;
    float innerStrokeWidth;
    float inner_outer_padding;
    float inner_text_padding;
    private final Rect boundaryOfText = new Rect();

    boolean refreshDisplay = false;

    int targetSpeed = 0;
    int currentSpeed = 0;
    int targetTemperature = 112;
    int currentTemperature = 112;
    int targetBattery = 0;
    int currentBattery = 0;

    private Handler refreshHandler = new Handler();

    private Runnable refreshRunner = new Runnable() {
        @Override
        public void run() {
            if (refreshDisplay) {
                invalidate();
                refreshHandler.postDelayed(refreshRunner, 30);
            }
        }
    };

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WheelView,
                0, 0);

        outerStrokeWidth = a.getDimension(R.styleable.WheelView_outer_thickness, 35);
        innerStrokeWidth = a.getDimension(R.styleable.WheelView_inner_thickness, 25);
        inner_outer_padding = a.getDimension(R.styleable.WheelView_inner_outer_padding, 5);
        inner_text_padding = a.getDimension(R.styleable.WheelView_inner_text_padding, 5);

        outerArcPaint = new Paint();
        outerArcPaint.setAntiAlias(true);
        outerArcPaint.setStrokeWidth(outerStrokeWidth);
        outerArcPaint.setStyle(Paint.Style.STROKE);

        innerArcPaint = new Paint();
        innerArcPaint.setAntiAlias(true);
        innerArcPaint.setStrokeWidth(innerStrokeWidth);
        innerArcPaint.setStyle(Paint.Style.STROKE);

//        AssetManager am = getContext().getApplicationContext().getAssets();
//        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(),
//                String.format(Locale.US, "fonts/%s", "SquareHead.ttf"));
        Typeface typeface = Typefaces.get(getContext(), "fonts/SquareHead.ttf");
        speedTextPaint = new Paint();
        speedTextPaint.setColor(Color.WHITE);
        speedTextPaint.setTextAlign(Paint.Align.CENTER);
        speedTextPaint.setTypeface(typeface);
    }

    public void setSpeed(int speed) {
        if (mSpeed == speed)
            return;

        mSpeed = speed;
        mSpeed = mSpeed < 0 ? 0 : mSpeed;
        mSpeed = mSpeed > mMaxSpeed ? mMaxSpeed : mSpeed;

        targetSpeed = Math.round(((float) mSpeed / mMaxSpeed) * 112);
        refresh();
    }

    public void setBattery(int battery) {
        if (mBattery == battery)
            return;

        mBattery = battery;
        mBattery = mBattery > 100 ? 100 : mBattery;
        mBattery = mBattery < 0 ? 0 : mBattery;

        targetBattery = Math.round(((float) 40 / 100) * mBattery);
        refresh();
    }

    public void setTemperature(int temperature) {
        if (mTemperature == temperature)
            return;
        mTemperature = temperature;
        mTemperature = mTemperature > 80 ? 80 : mTemperature;
        mTemperature = mTemperature < 0 ? 0 : mTemperature;
        targetTemperature = 112 - Math.round(((float) 40 / 80) * mTemperature);
        refresh();
    }

    private void refresh() {
        if (!refreshDisplay) {
            refreshDisplay = true;
            refreshHandler.postDelayed(refreshRunner, 30);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ww = (float) w - xpad - outerStrokeWidth;
        float diameter = ww;
        float radius = diameter / 2;

        float center_x = w / 2;
        float center_y = (ww/2)+(outerStrokeWidth/2) + getPaddingTop();

        float left = center_x - radius;
        float top = center_y - radius;
        float right = center_x + radius;
        float bottom = center_y + radius;

        outerRect.set(left, top, right, bottom);

        diameter = diameter - (outerStrokeWidth/2) - (innerStrokeWidth*2) - (inner_outer_padding*2);
        radius = diameter / 2;

        left = center_x - radius;
        top = center_y - radius;
        right = center_x + radius;
        bottom = center_y + radius;

        innerRect.set(left, top, right, bottom);

        int innerArcHypot = Math.round((innerRect.right - innerRect.left) - (innerStrokeWidth));//-(inner_text_padding*2));
        int speedTextWidth = (int) Math.round(Math.sqrt(2*Math.pow(innerArcHypot/2, 2)));

        speedTextRect.set(
                Math.round(center_x - (speedTextWidth/2)),
                Math.round(center_y - (speedTextWidth/2)),
                Math.round(center_x + (speedTextWidth/2)),
                Math.round(center_y + (speedTextWidth/2)));

        speedTextSize = calculateFontSize(boundaryOfText, speedTextRect, "00", speedTextPaint);

        speedTextRect.set(boundaryOfText);
        speedTextRect.top = Math.round(center_y - (boundaryOfText.height()/2));
        speedTextRect.bottom = Math.round(speedTextRect.top + boundaryOfText.height());

        int speedTextKPHWidth = speedTextWidth / 2;
        Rect speedTextKPHRect = new Rect(
                Math.round(center_x - (speedTextKPHWidth/2)),
                Math.round(center_y - (speedTextKPHWidth/2)),
                Math.round(center_x + (speedTextKPHWidth/2)),
                Math.round(center_y + (speedTextKPHWidth/2)));

        speedTextKPHSize = calculateFontSize(boundaryOfText, speedTextKPHRect, "KPH", speedTextPaint);
        speedTextKPHHeight = boundaryOfText.height();
    }

    private int dpToPx(int dp) {
        return (int) getContext().getResources().getDisplayMetrics().density * dp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        currentSpeed = updateCurrentValue(targetSpeed, currentSpeed);
        currentTemperature = updateCurrentValue(targetTemperature, currentTemperature);
        currentBattery = updateCurrentValue(targetBattery, currentBattery);

        outerArcPaint.setColor(getContext().getResources().getColor(R.color.speed_dial));
        for (int i = 0; i <= 112; i++) {
            if (i == currentSpeed)
                outerArcPaint.setColor(0x40000000);

            float value = (float) (144+(i*2.25));
            canvas.drawArc(outerRect, value, 1.5F, false, outerArcPaint);
        }


        innerArcPaint.setColor(getContext().getResources().getColor(R.color.battery_dial));
        for (int i = 0; i <= 112; i++) {
            if (i == currentBattery)
                innerArcPaint.setColor(0x40000000);
            if (i < 40 || i > 72) {
                float value = (float) (144 + (i * 2.25));
                canvas.drawArc(innerRect, value, 1.5F, false, innerArcPaint);
            }
            if (i == currentTemperature)
                innerArcPaint.setColor(getContext().getResources().getColor(R.color.temperature_dial));
        }

        String speedString = String.format(Locale.US, "%02d", Math.round(mSpeed/10));
        speedTextPaint.setTextSize(speedTextSize);
        canvas.drawText(speedString,speedTextRect.centerX(),speedTextRect.centerY()+(speedTextRect.height()/3), speedTextPaint);
        speedTextPaint.setTextSize(speedTextKPHSize);
        canvas.drawText("KPH", speedTextRect.centerX(),speedTextRect.bottom+(speedTextKPHHeight), speedTextPaint);

        if (currentSpeed != targetSpeed ||
                currentBattery != targetBattery ||
                currentTemperature != targetTemperature)
            refreshDisplay = true;
        else
            refreshDisplay = false;

    }

    private int updateCurrentValue(int target, int current) {
        if (target > current)
            return current+1;
        else if (current > target)
            return current-1;
        else
            return target;
    }

    private static float calculateFontSize(@NonNull Rect textBounds, @NonNull Rect textContainer, @NonNull String text, @NonNull Paint textPaint) {

        // Further optimize this method by passing in a reference of the Paint object
        // instead of instantiating it with every call.
//        final Paint textPaint = new Paint();
        int stage = 1;
        float textSize = 0;

        while(stage < 3) {
            if (stage == 1) textSize += 10;
            else
            if (stage == 2) textSize -= 1;

            textPaint.setTextSize(textSize);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            textBounds.offsetTo(textContainer.left, textContainer.top);

            boolean fits = textContainer.contains(textBounds);
            if (stage == 1 && !fits) stage++;
            else
            if (stage == 2 &&  fits) stage++;
        }

        return textSize;
    }

}

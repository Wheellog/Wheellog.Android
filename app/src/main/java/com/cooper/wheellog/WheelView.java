package com.cooper.wheellog;

import android.content.Context;
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
import android.view.View;

import java.util.Locale;

public class WheelView extends View {

    private Paint outerArcPaint;
    Paint innerArcPaint;
    Paint innerArcTextPaint;
    Paint speedTextPaint;
    Paint boxPaint;
    Paint textPaint;

    private final RectF outerRect = new RectF();
    final RectF innerRect = new RectF();
    final Rect tlRect = new Rect();
    final Rect trRect = new Rect();
    final Rect blRect = new Rect();
    final Rect brRect = new Rect();

    final Rect speedTextRect = new Rect();
    final Rect batteryTextRect = new Rect();
    final Rect temperatureTextRect = new Rect();

    float innerTextSize;
    float speedTextSize;
    float speedTextKPHSize;
    float speedTextKPHHeight;

    private int mMaxSpeed = 300;
    private int mSpeed = 0;
    private int mBattery = 0;
    private int mTemperature = 0;
    private String mCurrentTime;
    private Double mDistance;
    private Double mTotalDistance;
    private Double mTopSpeed;

    float outerStrokeWidth;
    float innerStrokeWidth;
    float inner_outer_padding;
    float inner_text_padding;
    float box_top_padding;
    float box_outer_padding;
    float box_inner_padding;
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

        if (isInEditMode())
            return;

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WheelView,
                0, 0);

        outerStrokeWidth = a.getDimension(R.styleable.WheelView_outer_thickness, dpToPx(40));
        innerStrokeWidth = a.getDimension(R.styleable.WheelView_inner_thickness, dpToPx(30));
        inner_outer_padding = a.getDimension(R.styleable.WheelView_inner_outer_padding, dpToPx(5));
        inner_text_padding = a.getDimension(R.styleable.WheelView_inner_text_padding, dpToPx(0));
        box_top_padding = a.getDimension(R.styleable.WheelView_box_top_padding, dpToPx(20));
        box_outer_padding = a.getDimension(R.styleable.WheelView_box_outer_padding, dpToPx(20));
        box_inner_padding = a.getDimension(R.styleable.WheelView_box_inner_padding, dpToPx(10));

        outerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerArcPaint.setAntiAlias(true);
        outerArcPaint.setStrokeWidth(outerStrokeWidth);
        outerArcPaint.setStyle(Paint.Style.STROKE);

        innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerArcPaint.setAntiAlias(true);
        innerArcPaint.setStrokeWidth(innerStrokeWidth);
        innerArcPaint.setStyle(Paint.Style.STROKE);

        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(getContext().getResources().getColor(R.color.arc_dim));
        boxPaint.setStyle(Paint.Style.FILL);

        Typeface tfSquareHead = Typefaces.get(getContext(), "fonts/SquareHead.ttf");
        Typeface tfCone = Typefaces.get(getContext(), "fonts/Cone.otf");

        speedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        speedTextPaint.setColor(Color.WHITE);
        speedTextPaint.setTextAlign(Paint.Align.CENTER);
        speedTextPaint.setTypeface(tfSquareHead);

        innerArcTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerArcTextPaint.setColor(Color.WHITE);
        innerArcTextPaint.setTextAlign(Paint.Align.CENTER);
        innerArcTextPaint.setTypeface(tfCone);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(tfCone);
    }

    public void setSpeed(int speed) {
        if (mSpeed == speed)
            return;

        mSpeed = speed;
        mSpeed = mSpeed < 0 ? 0 : mSpeed;
        mSpeed = mSpeed > mMaxSpeed ? mMaxSpeed : mSpeed;

        targetSpeed = Math.round(((float) mSpeed / mMaxSpeed) * 112);
    }

    public void setBattery(int battery) {
        if (mBattery == battery)
            return;

        mBattery = battery;
        mBattery = mBattery > 100 ? 100 : mBattery;
        mBattery = mBattery < 0 ? 0 : mBattery;

        targetBattery = Math.round(((float) 40 / 100) * mBattery);
    }

    public void setTemperature(int temperature) {
        if (mTemperature == temperature)
            return;
        mTemperature = temperature;
        mTemperature = mTemperature > 80 ? 80 : mTemperature;
        mTemperature = mTemperature < 0 ? 0 : mTemperature;
        targetTemperature = 112 - Math.round(((float) 40 / 80) * mTemperature);
    }

    public void setRideTime(String currentTime) {
        mCurrentTime = currentTime;
    }

    public void setDistance(Double distance) {
        mDistance = distance;
    }

    public void setTotalDistance(Double totalDistance) {
        mTotalDistance = totalDistance;
    }

    public void setTopSpeed(Double topSpeed) {
        mTopSpeed = topSpeed;
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

        if (isInEditMode())
            return;

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ww = (float) w - xpad - outerStrokeWidth;
        float diameter = ww;
        float radius = diameter / 2;

        float center_x = w / 2;
        float center_y = (ww/2)+(outerStrokeWidth/2) + getPaddingTop();

        float orLeft = center_x - radius;
        float orTop = center_y - radius;
        float orRight = center_x + radius;
        float orBottom = center_y + radius;

        outerRect.set(orLeft, orTop, orRight, orBottom);

        diameter = diameter - outerStrokeWidth - innerStrokeWidth - (inner_outer_padding*2);
        radius = diameter / 2;

        float left = center_x - radius;
        float top = center_y - radius;
        float right = center_x + radius;
        float bottom = center_y + radius;

        innerRect.set(left, top, right, bottom);

        int innerArcHypot = Math.round((innerRect.right - innerRect.left) - (innerStrokeWidth)-inner_text_padding);
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

        speedTextKPHSize = calculateFontSize(boundaryOfText, speedTextKPHRect, "km/h", textPaint);
        speedTextKPHHeight = boundaryOfText.height();

        int innerTextRectWidth = Math.round(innerStrokeWidth);
        batteryTextRect.set(
                Math.round(center_x-(diameter/2)-(innerTextRectWidth/2)),
                Math.round(center_y-(innerTextRectWidth/2)),
                Math.round((center_x-(diameter/2))+(innerTextRectWidth/2)),
                Math.round(center_y+(innerTextRectWidth/2)));
        temperatureTextRect.set(
                Math.round(center_x+(diameter/2)-(innerTextRectWidth/2)),
                Math.round(center_y-(innerTextRectWidth/2)),
                Math.round((center_x+(diameter/2))+(innerTextRectWidth/2)),
                Math.round(center_y+(innerTextRectWidth/2)));
        innerTextSize = calculateFontSize(boundaryOfText, batteryTextRect, "88%", innerArcTextPaint);

        int tTop = Math.round(orTop+(orBottom*0.7F)+(outerStrokeWidth/2)+box_top_padding);
        int height = Math.round((getHeight() - tTop - box_inner_padding - box_outer_padding)/2);
        int width = Math.round((getWidth()-(box_outer_padding*2)-box_inner_padding)/2);
        int tBottom = tTop+height;
        int lLeft = Math.round(box_outer_padding);
        int lRight = lLeft+width;
        int bTop = Math.round(tBottom+box_inner_padding);
        int bBottom = bTop + height;
        int rLeft = Math.round(lRight + box_inner_padding);
        int rRight = rLeft+width;

        tlRect.set(lLeft, tTop, lRight, tBottom);
        blRect.set(lLeft, bTop, lRight, bBottom);
        trRect.set(rLeft, tTop, rRight, tBottom);
        brRect.set(rLeft, bTop, rRight, bBottom);

        refresh();
    }

    private int dpToPx(int dp) {
        return (int) getContext().getResources().getDisplayMetrics().density * dp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode())
            return;

        currentSpeed = updateCurrentValue(targetSpeed, currentSpeed);
        currentTemperature = updateCurrentValue(targetTemperature, currentTemperature);
        currentBattery = updateCurrentValue(targetBattery, currentBattery);

        //####################################################
        //################# DRAW OUTER ARC ###################
        //####################################################

        outerArcPaint.setColor(getContext().getResources().getColor(R.color.arc_dim));
        canvas.drawArc(outerRect, 144, 252, false, outerArcPaint);

        outerArcPaint.setColor(getContext().getResources().getColor(R.color.speed_dial));
        for (int i = 0; i <= currentSpeed; i++) {
            float value = (float) (144+(i*2.25));
            canvas.drawArc(outerRect, value, 1.5F, false, outerArcPaint);
        }

        //####################################################
        //################# DRAW INNER ARC ###################
        //####################################################

        innerArcPaint.setColor(getContext().getResources().getColor(R.color.arc_dim));
        canvas.drawArc(innerRect, 144, 90, false, innerArcPaint);
        canvas.drawArc(innerRect, 306, 90, false, innerArcPaint);

        innerArcPaint.setColor(getContext().getResources().getColor(R.color.battery_dial));
        for (int i = 0; i < 112; i++) {
            if (i == currentTemperature)
                innerArcPaint.setColor(getContext().getResources().getColor(R.color.temperature_dial));
            if (i < currentBattery || i >= currentTemperature) {
                float value = (144 + (i * 2.25F));
                canvas.drawArc(innerRect, value, 1.5F, false, innerArcPaint);
            }
        }

        //####################################################
        //################# DRAW SPEED TEXT ##################
        //####################################################

        String speedString = String.format(Locale.US, "%02d", Math.round(mSpeed/10));
        speedTextPaint.setTextSize(speedTextSize);
        canvas.drawText(speedString,speedTextRect.centerX(),speedTextRect.centerY()+(speedTextRect.height()/3), speedTextPaint);
        textPaint.setTextSize(speedTextKPHSize);
        canvas.drawText("km/h", speedTextRect.centerX(),speedTextRect.bottom+(speedTextKPHHeight), textPaint);


        //####################################################
        //######## DRAW BATTERY AND TEMPERATURE TEXT #########
        //####################################################

        canvas.save();
        canvas.rotate((144+(currentBattery * 2.25F)-180), outerRect.centerY(), outerRect.centerX());
        String batteryString = String.format(Locale.US, "%02d%%", mBattery);
        innerArcTextPaint.setTextSize(innerTextSize);
        canvas.drawText(batteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), innerArcTextPaint);
        canvas.restore();
        canvas.save();
        canvas.rotate((143.5F+(currentTemperature * 2.25F)), outerRect.centerY(), outerRect.centerX());
        String temperatureString = String.format(Locale.US, "%02dC", mTemperature);
        canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), innerArcTextPaint);
        canvas.restore();

        //####################################################
        //############# DRAW BOTTOM RECTANGLES ###############
        //####################################################

        canvas.drawRect(tlRect,boxPaint);
        canvas.drawRect(trRect,boxPaint);
        canvas.drawRect(blRect,boxPaint);
        canvas.drawRect(brRect,boxPaint);

        //####################################################
        //############### DRAW RECTANGLE TEXT ################
        //####################################################

        textPaint.setTextSize(80);
        float textOffset = tlRect.height()/3;
        textPaint.getTextBounds("TEST", 0, 4, boundaryOfText);
        canvas.drawText("RIDE TIME", tlRect.centerX(), tlRect.centerY()-textOffset+boundaryOfText.height(), textPaint);
        canvas.drawText("TOP SPEED", trRect.centerX(), trRect.centerY()-textOffset+boundaryOfText.height(), textPaint);
        canvas.drawText("DISTANCE", blRect.centerX(), blRect.centerY()-textOffset+boundaryOfText.height(), textPaint);
        canvas.drawText("TOTAL", brRect.centerX(), brRect.centerY()-textOffset+boundaryOfText.height(), textPaint);

        textPaint.setTextSize(100);
        canvas.drawText(mCurrentTime, tlRect.centerX(), tlRect.centerY()+textOffset, textPaint);
        canvas.drawText(String.format(Locale.US, "%.1f km/h", mTopSpeed), trRect.centerX(), trRect.centerY()+textOffset, textPaint);
        canvas.drawText(String.format(Locale.US, "%.2f km", mDistance), blRect.centerX(), blRect.centerY()+textOffset, textPaint);
        canvas.drawText(String.format(Locale.US, "%.0f km", mTotalDistance), brRect.centerX(), brRect.centerY()+textOffset, textPaint);
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

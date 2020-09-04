package com.cooper.wheellog.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.cooper.wheellog.R;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.Typefaces;

import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

import static com.cooper.wheellog.utils.MathsUtil.dpToPx;
import static com.cooper.wheellog.utils.MathsUtil.kmToMiles;

public class WheelView extends View {

    private Paint outerArcPaint;
    Paint innerArcPaint;
    Paint textPaint;

    private final RectF outerArcRect = new RectF();
    final RectF innerArcRect = new RectF();
    final Rect tlRect = new Rect();
    final Rect trRect = new Rect();
    final Rect mlRect = new Rect();
    final Rect mrRect = new Rect();
    final Rect blRect = new Rect();
    final Rect brRect = new Rect();
    final Rect[] boxRects = {tlRect, trRect, mlRect, mrRect, blRect, brRect};
    final ViewBlockInfo[] mViewBlocks;

    final Rect speedTextRect = new Rect();
    final Rect batteryTextRect = new Rect();
    final Rect temperatureTextRect = new Rect();

    float speedTextSize;
    float speedTextKPHSize;
    float speedTextKPHHeight;
    float innerArcTextSize;
    float boxTextSize;
    float boxTextHeight;

    private int mMaxSpeed = 300;
    private boolean mTrueBattery = false;
    private boolean mCurrentOnDial = false;

    private boolean mUseMPH = false;
    private int mSpeed = 0;
    private int mWarningSpeed = 0;
    private int mBattery = 0;
    private int mBatteryLowest = 101;
    private int mTemperature = 0;
    private String mCurrentTime = "00:00:00";
    private Double mDistance = 0.0;
    private Double mTotalDistance = 0.0;
    private Double mTopSpeed = 0.0;
    private Double mVoltage = 0.0;
    private Double mCurrent = 0.0;
    private Double mAverageSpeed = 0.0;


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
    int targetCurrent = 0;
    int currentSpeed = 0;
    int currentCurrent = 0;
    int targetTemperature = 112;
    int currentTemperature = 112;
    int targetBattery = 0;
    int targetBatteryLowest = 0;
    int currentBattery = 0;
    Bitmap mTextBoxesBitmap;
    Canvas mCanvas;

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

    private ViewBlockInfo[] getViewBlockInfo() {
        return new ViewBlockInfo[]{
                new ViewBlockInfo(getResources().getString(R.string.voltage),
                        () -> String.format(Locale.US, "%.2f " + getResources().getString(R.string.volt), mVoltage)),
                new ViewBlockInfo(getResources().getString(R.string.average_riding_speed),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(mAverageSpeed));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), mAverageSpeed);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.riding_time),
                        () -> mCurrentTime),
                new ViewBlockInfo(getResources().getString(R.string.top_speed),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(mTopSpeed));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), mTopSpeed);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.distance),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.2f " + getResources().getString(R.string.milli), kmToMiles(mTopSpeed));
                            } else {
                                if (mDistance < 1) {
                                    return String.format(Locale.US, "%.0f " + getResources().getString(R.string.metre), mDistance * 1000);
                                } else {
                                    return String.format(Locale.US, "%.2f " + getResources().getString(R.string.km), mDistance);
                                }
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.total),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.0f " + getResources().getString(R.string.milli), kmToMiles(mTotalDistance));
                            } else {
                                return String.format(Locale.US, "%.0f " + getResources().getString(R.string.km), mTotalDistance);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.current),
                        () -> String.format(Locale.US, "%.2f ", mCurrent)),
                new ViewBlockInfo(getResources().getString(R.string.power),
                        () -> String.format(Locale.US, "%.2f " + getResources().getString(R.string.watt), WheelData.getInstance().getPowerDouble())),
                new ViewBlockInfo(getResources().getString(R.string.temperature),
                        () -> String.format(Locale.US, "%d ", WheelData.getInstance().getTemperature())),
                new ViewBlockInfo(getResources().getString(R.string.temperature2),
                        () -> String.format(Locale.US, "%d ", WheelData.getInstance().getTemperature2())),
                new ViewBlockInfo(getResources().getString(R.string.average_speed),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageSpeedDouble()));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), WheelData.getInstance().getAverageSpeedDouble());
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.ride_time),
                        () -> WheelData.getInstance().getRideTimeString()),
                new ViewBlockInfo(getResources().getString(R.string.wheel_distance),
                        () -> {
                            if (mUseMPH) {
                                return String.format(Locale.US, "%.2f " + getResources().getString(R.string.milli), kmToMiles(WheelData.getInstance().getWheelDistanceDouble()));
                            } else {
                                return String.format(Locale.US, "%.3f " + getResources().getString(R.string.km), WheelData.getInstance().getWheelDistanceDouble());
                            }
                        })
        };
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            mMaxSpeed = 300;
            mSpeed = 150;
            targetSpeed = Math.round(((float) mSpeed / mMaxSpeed) * 112);
            currentSpeed = targetSpeed;

            mTemperature = 35;
            targetTemperature = 112 - Math.round(((float) 40 / 80) * mTemperature);
            currentTemperature = targetTemperature;

            mBattery = 50;
            mBatteryLowest = 30;
            targetBatteryLowest = 10;
            targetBattery = Math.round(((float) 40 / 100) * mBattery);
            currentBattery = targetBattery;
        }

        mViewBlocks = getViewBlockInfo();

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WheelView,
                0, 0);

        outerStrokeWidth = a.getDimension(R.styleable.WheelView_outer_thickness, dpToPx(context, 40));
        innerStrokeWidth = a.getDimension(R.styleable.WheelView_inner_thickness, dpToPx(context, 30));
        inner_outer_padding = a.getDimension(R.styleable.WheelView_inner_outer_padding, dpToPx(context, 5));
        inner_text_padding = a.getDimension(R.styleable.WheelView_inner_text_padding, 0);
        box_top_padding = a.getDimension(R.styleable.WheelView_box_top_padding, dpToPx(context, 20));
        box_outer_padding = a.getDimension(R.styleable.WheelView_box_outer_padding, dpToPx(context, 20));
        box_inner_padding = a.getDimension(R.styleable.WheelView_box_inner_padding, dpToPx(context, 10));

        outerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerArcPaint.setAntiAlias(true);
        outerArcPaint.setStrokeWidth(outerStrokeWidth);
        outerArcPaint.setStyle(Paint.Style.STROKE);

        innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerArcPaint.setAntiAlias(true);
        innerArcPaint.setStrokeWidth(innerStrokeWidth);
        innerArcPaint.setStyle(Paint.Style.STROKE);

        Typeface tfTest = Typefaces.get(getContext(), "fonts/prime.otf");
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(tfTest);
    }

    public void setMaxSpeed(int maxSpeed) {
        mMaxSpeed = maxSpeed;
    }

    public void setUseMPH(boolean use_mph) {
        mUseMPH = use_mph;
    }

    public void updateViewBlocksVisibility(Set<String> viewBlocks) {
        for (ViewBlockInfo block : mViewBlocks) {
            block.setEnabled(viewBlocks.contains(block.getTitle()));
        }
    }

    public void setBetterPercent(boolean betterPercent) {
        mTrueBattery = betterPercent;
    }

    public void setCurrentOnDial(boolean currentOnDial) {
        Timber.i("Change dial type to %b", currentOnDial);
        mCurrentOnDial = currentOnDial;
    }
    public void resetBatteryLowest() {
        mBatteryLowest = 101;
        refresh();
    }
    public void setSpeed(int speed) {
        if (mSpeed == speed)
            return;

        mSpeed = speed;
        mSpeed = mSpeed < 0 ? 0 : mSpeed;
        speed = speed > mMaxSpeed ? mMaxSpeed : speed;

        targetSpeed = Math.round(((float) speed / mMaxSpeed) * 112);
        refreshDrawableState();
    }

    public void setWarningSpeed(int speed) {
        mWarningSpeed = speed*10;
    }

    public void setBattery(int battery) {
        if (mBattery == battery)
            return;

        mBattery = battery;

        mBattery = mBattery > 100 ? 100 : mBattery;
        mBattery = mBattery < 0 ? 0 : mBattery;

        targetBattery = Math.round(((float) 40 / 100) * mBattery);
        if (mBattery > 0) {
            mBatteryLowest = mBatteryLowest > mBattery ? mBattery : mBatteryLowest;
            //mBatteryLowest = mBattery - 28;
        } else {
            mBatteryLowest = mBatteryLowest > 100 ? mBatteryLowest : mBattery;
        }

        targetBatteryLowest = Math.round(((float) 40 / 100) * mBatteryLowest);
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

    public void setRideTime(String currentTime) {
        if (mCurrentTime.equals(currentTime))
            return;
        mCurrentTime = currentTime;
        refresh();
    }

    public void setDistance(Double distance) {
        if (mDistance.equals(distance))
            return;
        mDistance = distance;
        refresh();
    }

    public void setTotalDistance(Double totalDistance) {
        if (mTotalDistance.equals(totalDistance))
            return;
        mTotalDistance = totalDistance;
        refresh();
    }

    public void setTopSpeed(Double topSpeed) {
        if (mTopSpeed.equals(topSpeed))
            return;
        mTopSpeed = topSpeed;
        refresh();
    }

    public void setVoltage(Double voltage) {
        if (mVoltage.equals(voltage))
            return;
        mVoltage = voltage;
        refresh();
    }

    public void setAverageSpeed(Double avg_speed) {
        if (mAverageSpeed.equals(avg_speed))
            return;
        mAverageSpeed = avg_speed;
        refresh();
    }
	
    public void setCurrent(Double current) {
        if (mCurrent.equals(current))
            return;
        mCurrent = current;

        current = current/10;
        current = Math.abs(current) > mMaxSpeed ? mMaxSpeed : current;


        targetCurrent = (int) Math.round(( current /(mMaxSpeed)) * 112);
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

        boolean landscape = w > h;

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ww;

        if (landscape)
            ww = (float) h - xpad;
        else
            ww = (float) w - xpad;

        float oaDiameter = ww - outerStrokeWidth;
        float oaRadius = oaDiameter / 2;

        float center_x = w / 2;
        float center_y;

        if (landscape)
            center_y = h / 2;
        else
            center_y = (ww/2) + getPaddingTop();

        float orLeft = center_x - oaRadius;
        float orTop = center_y - oaRadius;
        float orRight = center_x + oaRadius;
        float orBottom = center_y + oaRadius;

        outerArcRect.set(orLeft, orTop, orRight, orBottom);


        float iaDiameter = oaDiameter - outerStrokeWidth - innerStrokeWidth - (inner_outer_padding*2);
        float iaRadius = iaDiameter / 2;

        float left = center_x - iaRadius;
        float top = center_y - iaRadius;
        float right = center_x + iaRadius;
        float bottom = center_y + iaRadius;

        innerArcRect.set(left, top, right, bottom);


        int innerArcHypot = Math.round((innerArcRect.right - innerArcRect.left) - (innerStrokeWidth)-inner_text_padding);
        int speedTextRectSize = (int) Math.round(Math.sqrt(2*Math.pow(innerArcHypot/2, 2)));

        speedTextRect.set(
                Math.round(center_x - (speedTextRectSize/2)),
                Math.round(center_y - (speedTextRectSize/2)),
                Math.round(center_x + (speedTextRectSize/2)),
                Math.round(center_y + (speedTextRectSize/2)));

        speedTextSize = calculateFontSize(boundaryOfText, speedTextRect, "00", textPaint);

        speedTextRect.set(boundaryOfText);
        speedTextRect.top = Math.round(center_y - (boundaryOfText.height()/2) - (boundaryOfText.height()/10));
        speedTextRect.bottom = Math.round(speedTextRect.top + boundaryOfText.height());

        int speedTextKPHRectSize = speedTextRectSize / 2;
        Rect speedTextKPHRect = new Rect(
                Math.round(center_x - (speedTextKPHRectSize/2)),
                Math.round(center_y - (speedTextKPHRectSize/2)),
                Math.round(center_x + (speedTextKPHRectSize/2)),
                Math.round(center_y + (speedTextKPHRectSize/2)));

        speedTextKPHSize = calculateFontSize(boundaryOfText, speedTextKPHRect, getResources().getString(R.string.kmh), textPaint);
        speedTextKPHHeight = boundaryOfText.height();


        int innerTextRectWidth = Math.round(innerStrokeWidth);
        batteryTextRect.set(
                Math.round(center_x-(iaDiameter/2)-(innerTextRectWidth/2)),
                Math.round(center_y-(innerTextRectWidth/2)),
                Math.round((center_x-(iaDiameter/2))+(innerTextRectWidth/2)),
                Math.round(center_y+(innerTextRectWidth/2)));
        temperatureTextRect.set(
                Math.round(center_x+(iaDiameter/2)-(innerTextRectWidth/2)),
                Math.round(center_y-(innerTextRectWidth/2)),
                Math.round((center_x+(iaDiameter/2))+(innerTextRectWidth/2)),
                Math.round(center_y+(innerTextRectWidth/2)));
        innerArcTextSize = calculateFontSize(boundaryOfText, batteryTextRect, "88%", textPaint);


        if (landscape) {
            int tTop = getPaddingTop();
            int height = Math.round((getHeight() - tTop - (box_inner_padding * 2) - getPaddingBottom()) / 3);
            int tBottom = tTop + height;
            int mTop = Math.round(tBottom + box_inner_padding);
            int mBottom = mTop + height;
            int bTop = Math.round(mBottom + box_inner_padding);
            int bBottom = bTop + height;

            int lLeft = Math.round(getPaddingLeft());
            int lRight = Math.round(((w - oaDiameter) / 2) - (outerStrokeWidth/2) - getPaddingLeft());// Math.round(center_y - (box_inner_padding / 2));
            int rLeft = Math.round(w-lRight);
            int rRight = rLeft + lRight  -getPaddingLeft();

            tlRect.set(lLeft, tTop, lRight, tBottom);
            trRect.set(rLeft, tTop, rRight, tBottom);
            mlRect.set(lLeft, mTop, lRight, mBottom);
            mrRect.set(rLeft, mTop, rRight, mBottom);
            blRect.set(lLeft, bTop, lRight, bBottom);
            brRect.set(rLeft, bTop, rRight, bBottom);

            Rect tempRect = new Rect(lLeft, tTop, lRight, tTop + (tlRect.height() / 3));
            boxTextSize = calculateFontSize(boundaryOfText, tempRect, getResources().getString(R.string.top_speed) + "W", textPaint);
            boxTextHeight = boundaryOfText.height();
        } else {
            int tTop = (int) Math.round(outerArcRect.top + oaRadius + box_top_padding + (Math.cos(Math.toRadians(54)) * (oaRadius + (outerStrokeWidth / 2))));
            int height = Math.round((getHeight() - tTop - (box_inner_padding * 2) - getPaddingBottom()) / 3);
            int tBottom = tTop + height;
            int mTop = Math.round(tBottom + box_inner_padding);
            int mBottom = mTop + height;
            int bTop = Math.round(mBottom + box_inner_padding);
            int bBottom = bTop + height;

            int lLeft = Math.round(getPaddingLeft());
            int lRight = Math.round(center_y - (box_inner_padding / 2));
            int rLeft = Math.round(center_y + (box_inner_padding / 2));
            int rRight = getWidth() - getPaddingRight();

            tlRect.set(lLeft, tTop, lRight, tBottom);
            trRect.set(rLeft, tTop, rRight, tBottom);
            mlRect.set(lLeft, mTop, lRight, mBottom);
            mrRect.set(rLeft, mTop, rRight, mBottom);
            blRect.set(lLeft, bTop, lRight, bBottom);
            brRect.set(rLeft, bTop, rRight, bBottom);

            Rect tempRect = new Rect(lLeft, tTop, lRight, tTop + (tlRect.height() / 3));
            boxTextSize = calculateFontSize(boundaryOfText, tempRect, getResources().getString(R.string.top_speed) + "W", textPaint);
            boxTextHeight = boundaryOfText.height();
        }

        mTextBoxesBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mTextBoxesBitmap);
        redrawTextBoxes();

        refresh();
    }

    private void drawTextBox(String header, String value, Canvas canvas, Rect rect)
    {
        if (header.length() > 10) {
            textPaint.setTextSize(Math.min(boxTextSize, calculateFontSize(boundaryOfText, rect, header, textPaint)));
            canvas.drawText(header, rect.centerX(), rect.centerY() - (box_inner_padding / 2), textPaint);
            textPaint.setTextSize(boxTextSize);
        } else {
            canvas.drawText(header, rect.centerX(), rect.centerY() - (box_inner_padding / 2), textPaint);
        }
        canvas.drawText(value, rect.centerX(), rect.centerY() + boxTextHeight, textPaint);
    }

    public void redrawTextBoxes() {
        if (mTextBoxesBitmap == null || getHeight() == getWidth()) {
            return;
        }

        mTextBoxesBitmap.eraseColor(Color.TRANSPARENT);

        textPaint.setColor(getContext().getResources().getColor(R.color.wheelview_text));
        textPaint.setTextSize(boxTextSize);

        try {
            int i = 0;
            for (ViewBlockInfo block : mViewBlocks) {
                if (block.getEnabled() && i < 6) {
                    drawTextBox(block.getTitle(), block.getValue(), mCanvas, boxRects[i++]);
                }
            }
        } catch (Exception e) {
            Timber.i("Draw exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int currentDial = 0;
        if (mCurrentOnDial) {
            currentCurrent = updateCurrentValue2(targetCurrent, currentCurrent);
            currentDial = currentCurrent;
        } else {
            currentSpeed = updateCurrentValue(targetSpeed, currentSpeed);
            currentDial = currentSpeed;
        }
        currentTemperature = updateCurrentValue(targetTemperature, currentTemperature);
        currentBattery = updateCurrentValue(targetBattery, currentBattery);

        //####################################################
        //################# DRAW OUTER ARC ###################
        //####################################################

        outerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_arc_dim));
        canvas.drawArc(outerArcRect, 144, 252, false, outerArcPaint);
        if (currentDial >= 0) {
            outerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_main_positive_dial));
        } else {
            outerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_main_negative_dial));
        }
        currentDial = Math.abs(currentDial);
        //###########TEST purp
        //currentSpeed = (int) Math.round(( mCurrent /(10*mMaxSpeed)) * 112);
        //########### <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<,

        for (int i = 0; i < currentDial; i++) {
            float value = (float) (144 + (i * 2.25));
            canvas.drawArc(outerArcRect, value, 1.5F, false, outerArcPaint);
        }

        //####################################################
        //################# DRAW INNER ARC ###################
        //####################################################

        innerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_arc_dim));
        canvas.drawArc(innerArcRect, 144, 90, false, innerArcPaint);
        canvas.drawArc(innerArcRect, 306, 90, false, innerArcPaint);

        innerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_battery_dial));
        for (int i = 0; i < 112; i++) {
            if (i == targetBatteryLowest)
                innerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_battery_low_dial));
            if (i == currentTemperature)
                innerArcPaint.setColor(getContext().getResources().getColor(R.color.wheelview_temperature_dial));
            if (i < currentBattery || i >= currentTemperature) {
                float value = (144 + (i * 2.25F));
                canvas.drawArc(innerArcRect, value, 1.5F, false, innerArcPaint);
            }
        }

        //####################################################
        //################# DRAW SPEED TEXT ##################
        //####################################################

        int speed = mUseMPH ? Math.round(kmToMiles(mSpeed)) : mSpeed;

        String speedString;
        if (speed < 100)
            speedString = String.format(Locale.US, "%.1f", speed / 10.0);
        else
            speedString = String.format(Locale.US, "%02d", Math.round(speed / 10.0));

        if (mWarningSpeed > 0 && mSpeed >= mWarningSpeed)
            textPaint.setColor(getContext().getResources().getColor(R.color.accent));
        else
            textPaint.setColor(getContext().getResources().getColor(R.color.wheelview_speed_text));

        textPaint.setTextSize(speedTextSize);
        canvas.drawText(speedString, outerArcRect.centerX(), speedTextRect.centerY() + (speedTextRect.height() / 2), textPaint);
        textPaint.setTextSize(speedTextKPHSize);
        textPaint.setColor(getContext().getResources().getColor(R.color.wheelview_text));
        String metric = mUseMPH ? getResources().getString(R.string.mph) : getResources().getString(R.string.kmh);
        canvas.drawText(metric, outerArcRect.centerX(), speedTextRect.bottom + (speedTextKPHHeight * 1.25F), textPaint);

        //####################################################
        //######## DRAW BATTERY AND TEMPERATURE TEXT #########
        //####################################################

        if (mTemperature > 0 && mBattery > -1) {
            textPaint.setTextSize(innerArcTextSize);
            canvas.save();
            if (getWidth() > getHeight())
                canvas.rotate((144 + (currentBattery * 2.25F) - 180), innerArcRect.centerX(), innerArcRect.centerY());
            else
                canvas.rotate((144 + (currentBattery * 2.25F) - 180), innerArcRect.centerY(), innerArcRect.centerX());

            String bestbatteryString = String.format(Locale.US, "%02d%%", mBattery);
            canvas.drawText(bestbatteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint);
            canvas.restore();
            canvas.save();
            /// true battery
            if (mTrueBattery) {
                if (getWidth() > getHeight())
                    canvas.rotate((144 + (-3.3F * 2.25F) - 180), innerArcRect.centerX(), innerArcRect.centerY());
                else
                    canvas.rotate((144 + (-2 * 2.25F) - 180), innerArcRect.centerY(), innerArcRect.centerX());

                String batteryString = String.format(Locale.US, "%s", "true");
                canvas.drawText(batteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint);
                canvas.restore();
                canvas.save();
            }

            if (getWidth() > getHeight())
                canvas.rotate((143.5F + (currentTemperature * 2.25F)), innerArcRect.centerX(), innerArcRect.centerY());
            else
                canvas.rotate((143.5F + (currentTemperature * 2.25F)), innerArcRect.centerY(), innerArcRect.centerX());
            String temperatureString = String.format(Locale.US, "%02dC", mTemperature);
            canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint);
            canvas.restore();
        }

        // Draw text blocks bitmap
        canvas.drawBitmap(mTextBoxesBitmap, 0, 0, null);

        refreshDisplay = currentSpeed != targetSpeed ||
                currentCurrent != targetCurrent ||
                currentBattery != targetBattery ||
                currentTemperature != targetTemperature;
    }

    private int updateCurrentValue(int target, int current) {
        if (target > current)
            return current+1;
        else if (current > target)
            return current-1;
        else
            return target;
    }

    private int updateCurrentValue2(int target, int current) {
        if (target > 0) {
            if (target > current)
                return target;
            else if (current > target)
                return current - 1;
            else
                return target;
        } else {
            if (target < current)
                return target;
            else if (current > target)
                return current + 1;
            else
                return target;
        }

    }

    private float calculateFontSize(@NonNull Rect textBounds, @NonNull Rect textContainer, @NonNull String text, @NonNull Paint textPaint) {
        textPaint.setTextSize(100);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);

        int h = textBounds.height();
        float w = textPaint.measureText(text);

        float target_h = (float) textContainer.height()*1.0f;
        float target_w = (float) textContainer.width()*1.0f;

        float size_h = ((target_h/h)*100f);
        float size_w = ((target_w/w)*100f);

        float result = size_h <= size_w ? size_h : size_w;
        textPaint.setTextSize(result);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        return result;
    }
}

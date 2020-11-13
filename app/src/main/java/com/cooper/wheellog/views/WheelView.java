package com.cooper.wheellog.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.math.MathUtils;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.BuildConfig;
import com.cooper.wheellog.R;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;
import com.cooper.wheellog.utils.ReflectUtil;

import java.lang.reflect.Field;
import java.util.*;

import timber.log.Timber;

import static com.cooper.wheellog.utils.MathsUtil.*;

public class WheelView extends View {

    private Paint outerArcPaint;
    Paint innerArcPaint;
    Paint textPaint;

    private final RectF outerArcRect = new RectF();
    final RectF innerArcRect = new RectF();
    final ViewBlockInfo[] mViewBlocks;
    float oaDiameter;

    final RectF speedTextRect = new RectF();
    final RectF batteryTextRect = new RectF();
    final RectF temperatureTextRect = new RectF();
    Path modelTextPath;
    Paint modelTextPaint;
    Paint versionPaint;

    float speedTextSize;
    float speedTextKPHSize;
    float speedTextKPHHeight;
    float innerArcTextSize;
    float boxTextSize;
    float boxTextHeight;

    private int mSpeed = 0;
    private int mBattery = 0;
    private int mBatteryLowest = 101;
    private int mTemperature = 0;
    private int mMaxTemperature = 0;
    private String mCurrentTime = "00:00:00";
    private Double mDistance = 0.0;
    private Double mTotalDistance = 0.0;
    private Double mTopSpeed = 0.0;
    private Double mVoltage = 0.0;
    private Double mCurrent = 0.0;
    private Double mPwm = 0.0;
    private Double mMaxPwm = 0.0;
    private Double mAverageSpeed = 0.0;

    private String mWheelModel = "";
    private String versionString = String.format("ver %s %s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE);

    float outerStrokeWidth;
    float innerStrokeWidth;
    float inner_outer_padding;
    float inner_text_padding;
    float box_top_padding;
    float box_outer_padding;
    float box_inner_padding;
    float center_x;
    float center_y;

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
        Boolean useMph = WheelLog.AppConfig.getUseMph();
        return new ViewBlockInfo[]{
                new ViewBlockInfo(getResources().getString(R.string.pwm),
                        () -> String.format(Locale.US, "%.2f%%", mPwm)),
                new ViewBlockInfo(getResources().getString(R.string.max_pwm),
                        () -> String.format(Locale.US, "%.2f%%", mMaxPwm)),
                new ViewBlockInfo(getResources().getString(R.string.voltage),
                        () -> String.format(Locale.US, "%.2f " + getResources().getString(R.string.volt), mVoltage)),
                new ViewBlockInfo(getResources().getString(R.string.average_riding_speed),
                        () -> {
                            if (useMph) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(mAverageSpeed));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), mAverageSpeed);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.riding_time),
                        () -> mCurrentTime),
                new ViewBlockInfo(getResources().getString(R.string.top_speed),
                        () -> {
                            if (useMph) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(mTopSpeed));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), mTopSpeed);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.distance),
                        () -> {
                            if (useMph) {
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
                            if (useMph) {
                                return String.format(Locale.US, "%.0f " + getResources().getString(R.string.milli), kmToMiles(mTotalDistance));
                            } else {
                                return String.format(Locale.US, "%.0f " + getResources().getString(R.string.km), mTotalDistance);
                            }
                        }),
                new ViewBlockInfo(getResources().getString(R.string.current),
                        () -> String.format(Locale.US, "%.2f " + getResources().getString(R.string.amp), mCurrent)),
                new ViewBlockInfo(getResources().getString(R.string.power),
                        () -> String.format(Locale.US, "%.2f " + getResources().getString(R.string.watt), WheelData.getInstance().getPowerDouble()), false),
                new ViewBlockInfo(getResources().getString(R.string.temperature),
                        () -> String.format(Locale.US, "%d ℃", WheelData.getInstance().getTemperature()), false),
                new ViewBlockInfo(getResources().getString(R.string.temperature2),
                        () -> String.format(Locale.US, "%d ℃", WheelData.getInstance().getTemperature2()), false),
                new ViewBlockInfo(getResources().getString(R.string.average_speed),
                        () -> {
                            if (useMph) {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageSpeedDouble()));
                            } else {
                                return String.format(Locale.US, "%.1f " + getResources().getString(R.string.kmh), WheelData.getInstance().getAverageSpeedDouble());
                            }
                        }, false),
                new ViewBlockInfo(getResources().getString(R.string.ride_time),
                        () -> WheelData.getInstance().getRideTimeString(), false),
                new ViewBlockInfo(getResources().getString(R.string.wheel_distance),
                        () -> {
                            if (useMph) {
                                return String.format(Locale.US, "%.2f " + getResources().getString(R.string.milli), kmToMiles(WheelData.getInstance().getWheelDistanceDouble()));
                            } else {
                                return String.format(Locale.US, "%.3f " + getResources().getString(R.string.km), WheelData.getInstance().getWheelDistanceDouble());
                            }
                        }, false)
        };
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            WheelLog.AppConfig = AppConfig.getInstance(context);
            mSpeed = 380;
            targetSpeed = Math.round(((float) mSpeed / 500) * 112);
            currentSpeed = targetSpeed;

            mTemperature = 35;
            mMaxTemperature = 70;
            targetTemperature = 112 - Math.round(((float) 40 / 80) * mTemperature);
            currentTemperature = targetTemperature;

            mBattery = 50;
            mBatteryLowest = 30;
            targetBatteryLowest = 10;
            targetBattery = Math.round(((float) 40 / 100) * mBattery);
            currentBattery = targetBattery;
            mWheelModel = "GotInSong Z10";
            try {
                WheelData wd = new WheelData();
                Field wdField = WheelData.class.getDeclaredField("mInstance");
                wdField.setAccessible(true);
                wdField.set(null, wd);

                ReflectUtil.SetPrivateField(wd, "mCalculatedPwm", 0.05d);
                ReflectUtil.SetPrivateField(wd, "mMaxPwm", 0.97d);
            } catch (Exception ignored) {
            }
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

        Typeface tfTest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? getResources().getFont(R.font.prime_regular)
                : ResourcesCompat.getFont(context, R.font.prime_regular);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(tfTest);

        modelTextPaint = new Paint(textPaint);
        modelTextPaint.setColor(getContext().getResources().getColor(R.color.wheelview_text));

        versionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        versionPaint.setTextAlign(Paint.Align.RIGHT);
        versionPaint.setColor(getContext().getResources().getColor(R.color.wheelview_versiontext));
        versionPaint.setTypeface(tfTest);
    }

    public void setWheelModel(String mWheelModel) {
        if (!this.mWheelModel.equals(mWheelModel)) {
            this.mWheelModel = mWheelModel;
            calcModelTextSize();
        }
    }

    public void updateViewBlocksVisibility(String[] viewBlocks) {
        for (ViewBlockInfo block : mViewBlocks) {
            block.setEnabled(false);
            block.setIndex(-1);
        }

        int index = 0;
        for (String title : viewBlocks) {
            for (ViewBlockInfo block : mViewBlocks) {
                if (block.getTitle().equals(title))
                {
                    block.setIndex(index++);
                    block.setEnabled(true);
                    break;
                }
            }
        }
        Arrays.sort(mViewBlocks);
    }
    
    public void resetBatteryLowest() {
        mBatteryLowest = 101;
        refresh();
    }

    public void setSpeed(int speed) {
        if (mSpeed == speed)
            return;

        mSpeed = speed;
        int maxSpeed = WheelLog.AppConfig.getMaxSpeed() * 10;
        speed = speed > maxSpeed ? maxSpeed : speed;

        targetSpeed = Math.round(((float) Math.abs(speed) / maxSpeed) * 112);
        refresh();
    }
    
    public void setBattery(int battery) {
        if (mBattery == battery)
            return;

        mBattery = MathUtils.clamp(battery, 0, 100);

        targetBattery = Math.round(((float) 40 / 100) * mBattery);
        if (mBattery > 0) {
            mBatteryLowest = Math.min(mBatteryLowest, mBattery);
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
        mTemperature = MathUtils.clamp(temperature, 0, 80);
        targetTemperature = 112 - Math.round(((float) 40 / 80) * mTemperature);
        refresh();
    }

    public void setMaxTemperature(int temperature) {
        if (mMaxTemperature == temperature)
            return;
        mMaxTemperature = temperature;
        refresh();
    }

    public void setMaxPwm(double pwm) {
        if (mMaxPwm == pwm)
            return;
        mMaxPwm = pwm;
        refresh();
    }

    public void setPwm(double pwm) {
        if (mPwm == pwm)
            return;
        mPwm = pwm;
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
        int maxSpeed = WheelLog.AppConfig.getMaxSpeed();
        current = Math.abs(current) > maxSpeed ? (double)maxSpeed : current;
        targetCurrent = (int) Math.round((current / (double)maxSpeed) * 112);
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

        if (landscape) {
            ww = (float) h - xpad;
        } else {
            ww = (float) w - xpad;
        }

        outerStrokeWidth = ww / 8;
        innerStrokeWidth = Math.round(outerStrokeWidth * 0.6);

        oaDiameter = ww - outerStrokeWidth;
        float oaRadius = oaDiameter / 2;

        center_x = w / 2f;

        if (landscape)
            center_y = h / 2f;
        else
            center_y = (ww / 2) + getPaddingTop();

        float orLeft = center_x - oaRadius;
        float orTop = center_y - oaRadius;
        float orRight = center_x + oaRadius;
        float orBottom = center_y + oaRadius;

        outerArcRect.set(orLeft, orTop, orRight, orBottom);
        outerArcPaint.setStrokeWidth(outerStrokeWidth);

        float iaDiameter = oaDiameter - outerStrokeWidth - innerStrokeWidth - (inner_outer_padding * 2);
        float iaRadius = iaDiameter / 2;

        float left = center_x - iaRadius;
        float top = center_y - iaRadius;
        float right = center_x + iaRadius;
        float bottom = center_y + iaRadius;

        innerArcRect.set(left, top, right, bottom);

        int innerArcHypot = Math.round((innerArcRect.right - innerArcRect.left) - (innerStrokeWidth) - inner_text_padding);
        int speedTextRectSize = (int) Math.round(Math.sqrt(2 * Math.pow(innerArcHypot / 2f, 2)));

        speedTextRect.set(
                center_x - speedTextRectSize / 2f,
                center_y - speedTextRectSize / 2f,
                center_x + speedTextRectSize / 2f,
                center_y + speedTextRectSize / 2f);

        speedTextSize = calculateFontSize(boundaryOfText, speedTextRect, "00", textPaint);

        speedTextRect.set(boundaryOfText);
        speedTextRect.top = Math.round(center_y - boundaryOfText.height() / 2f - boundaryOfText.height() / 10f);
        speedTextRect.bottom = Math.round(speedTextRect.top + boundaryOfText.height());

        int speedTextKPHRectSize = speedTextRectSize / 3;
        RectF speedTextKPHRect = new RectF(
                center_x - speedTextKPHRectSize / 2f,
                center_y - speedTextKPHRectSize / 2f,
                center_x + speedTextKPHRectSize / 2f,
                center_y + speedTextKPHRectSize / 2f);

        speedTextKPHSize = calculateFontSize(
                boundaryOfText,
                speedTextKPHRect,
                getResources().getString(R.string.kmh),
                textPaint);

        speedTextKPHHeight = boundaryOfText.height();

        int innerTextRectWidth = Math.round(innerStrokeWidth);
        batteryTextRect.set(
                center_x - (iaDiameter / 2) - (innerTextRectWidth / 2f),
                center_y - (innerTextRectWidth / 2f),
                (center_x - (iaDiameter / 2)) + (innerTextRectWidth / 2f),
                center_y + (innerTextRectWidth / 2f));
        temperatureTextRect.set(
                center_x + (iaDiameter / 2) - (innerTextRectWidth / 2f),
                center_y - (innerTextRectWidth / 2f),
                (center_x + (iaDiameter / 2)) + (innerTextRectWidth / 2f),
                center_y + (innerTextRectWidth / 2f));
        innerArcTextSize = calculateFontSize(boundaryOfText, batteryTextRect, "88%", textPaint);
        innerArcPaint.setStrokeWidth(innerStrokeWidth);

        calcModelTextSize();

        mTextBoxesBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mTextBoxesBitmap);
        if ((landscape && (float) w / h > 1.4) || (!landscape && (float) h / w > 1.1)) {
            redrawTextBoxes();
        }

        versionPaint.setTextSize(Math.round(getHeight() / 50.0));

        refresh();
    }

    private void calcModelTextSize() {
        RectF modelTextRect = new RectF(
                innerArcRect.left + inner_outer_padding * 2,
                innerArcRect.top + inner_outer_padding * 2,
                innerArcRect.right - inner_outer_padding * 2,
                innerArcRect.bottom - inner_outer_padding * 2);
        modelTextPath = new Path();
        modelTextPath.addArc(modelTextRect, 190, 160);
        modelTextRect.bottom = modelTextRect.top + innerStrokeWidth * 1.2f;
        modelTextPaint.setTextSize(calculateFontSize(boundaryOfText, modelTextRect, mWheelModel, modelTextPaint) / 2);
    }

    private void drawTextBox(String header, String value, Canvas canvas, RectF rect, Paint paint) {
        if (header.length() > 10) {
            paint.setTextSize(Math.min(boxTextSize * 0.8f, calculateFontSize(boundaryOfText, rect, header, paint)));
        } else {
            paint.setTextSize(boxTextSize * 0.8f);
        }
        canvas.drawText(header, rect.centerX(), rect.centerY() - box_inner_padding, paint);
        paint.setTextSize(boxTextSize);
        canvas.drawText(value, rect.centerX(), rect.centerY() + boxTextHeight, paint);
    }

    public void redrawTextBoxes() {
        if (mTextBoxesBitmap == null) {
            return;
        }

        mTextBoxesBitmap.eraseColor(Color.TRANSPARENT);

        int w = getWidth();
        int h = getHeight();
        boolean landscape = w > h;

        int countBlocks = 0;
        for (ViewBlockInfo block : mViewBlocks) {
            if (block.getEnabled()) {
                countBlocks++;
            }
        }

        if (countBlocks == 0) {
            return;
        }

        int cols = 2;
        int rows = Math.round(countBlocks / (float) cols + 0.499f);
        RectF[] boxRects = new RectF[(cols + 1) * rows];

        if (landscape) {
            float boxTop = getPaddingTop();
            float boxH = (h - boxTop - getPaddingBottom()) / (float) rows - box_inner_padding;
            float boxW = (w - oaDiameter - getPaddingRight()) / (float) cols - outerStrokeWidth;

            int i = 0;
            float boxLeft = getPaddingLeft();
            float boxLeft2 = w - boxW - getPaddingRight();
            for (int row = 0; row < rows; row++) {
                boxRects[i++] = new RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH);
                boxRects[i++] = new RectF(boxLeft2, boxTop, boxLeft2 + boxW, boxTop + boxH);
                boxTop += boxH + box_inner_padding;
            }
        } else {
            float boxTop = box_top_padding + outerArcRect.top + oaDiameter / 2 + (float) (Math.cos(Math.toRadians(54)) * (oaDiameter + outerStrokeWidth) / 2);

            if (countBlocks == 1) {
                cols = 1;
            } else {
                float hh = h - boxTop - getPaddingBottom() - box_inner_padding;
                float ratio = w / hh;
                rows = (int) (Math.sqrt((float) countBlocks / ratio * 3));
                cols = Math.round(countBlocks / (float) rows + 0.499f);
                // packing
                if (countBlocks != cols * rows) {
                    for (; ; ) {
                        if (countBlocks / (float) rows <= cols) {
                            rows--;
                        } else {
                            rows++;
                            break;
                        }
                    }
                }
            }

            float boxH = (h - boxTop - getPaddingBottom()) / (float) rows - box_inner_padding;
            float boxW = (w - getPaddingRight()) / (float) cols - box_inner_padding;

            int i = 0;
            for (int row = 0; row < rows; row++) {
                float boxLeft = getPaddingLeft();
                for (int col = 0; col < cols && i < countBlocks; col++) {
                    boxRects[i++] = new RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH);
                    boxLeft += boxW + box_inner_padding;
                }
                boxTop += boxH + box_inner_padding;
            }
        }
        boxTextSize = calculateFontSize(boundaryOfText, boxRects[0], getResources().getString(R.string.top_speed) + "W", textPaint, 2) * 1.2f;
        boxTextHeight = boundaryOfText.height();

        Paint paint = new Paint(textPaint);
        paint.setColor(getContext().getResources().getColor(R.color.wheelview_text));

        try {
            int i = 0;
            for (ViewBlockInfo block : mViewBlocks) {
                if (block.getEnabled()) {
                    drawTextBox(block.getTitle(), block.getValue(), mCanvas, boxRects[i++], paint);
                }
            }
        } catch (Exception e) {
            Timber.i("Draw exception: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int currentDial;
        if (WheelLog.AppConfig.getCurrentOnDial()) {
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

        int speed = WheelLog.AppConfig.getUseMph() ? Math.round(kmToMiles(mSpeed)) : mSpeed;

        String speedString;
        if (speed < 100)
            speedString = String.format(Locale.US, "%.1f", speed / 10.0);
        else
            speedString = String.format(Locale.US, "%02d", Math.round(speed / 10.0));

        double alarm1Speed = WheelLog.AppConfig.getAlarm1Speed();
        if (!WheelLog.AppConfig.getAlteredAlarms() && (alarm1Speed * 10) > 0 && mSpeed >= (alarm1Speed * 10))
            textPaint.setColor(getContext().getResources().getColor(R.color.accent));
        else
            textPaint.setColor(getContext().getResources().getColor(R.color.wheelview_speed_text));

        textPaint.setTextSize(speedTextSize);
        canvas.drawText(speedString, outerArcRect.centerX(), speedTextRect.centerY() + (speedTextRect.height() / 2), textPaint);
        textPaint.setTextSize(speedTextKPHSize);
        textPaint.setColor(getContext().getResources().getColor(R.color.wheelview_text));


        if (WheelLog.AppConfig.getUseShortPwm() || isInEditMode()) {
            String pwm = String.format("%02.0f%% / %02.0f%%",
                    WheelData.getInstance().getCalculatedPwm(),
                    WheelData.getInstance().getMaxPwm());
            textPaint.setTextSize(speedTextKPHSize * 1.2F);
            canvas.drawText(pwm, outerArcRect.centerX(), speedTextRect.bottom + (speedTextKPHHeight * 3.3F), textPaint);
        } else {
            String metric = WheelLog.AppConfig.getUseMph() ? getResources().getString(R.string.mph) : getResources().getString(R.string.kmh);
            canvas.drawText(metric, outerArcRect.centerX(), speedTextRect.bottom + (speedTextKPHHeight * 1.1F), textPaint);
        }

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
            Boolean fixedPercents = WheelLog.AppConfig.getFixedPercents();
            if (WheelLog.AppConfig.getUseBetterPercents() || fixedPercents) {
                if (getWidth() > getHeight())
                    canvas.rotate((144 + (-3.3F * 2.25F) - 180), innerArcRect.centerX(), innerArcRect.centerY());
                else
                    canvas.rotate((144 + (-2 * 2.25F) - 180), innerArcRect.centerY(), innerArcRect.centerX());

                String batteryCalculateType = "true";
                if (fixedPercents && !WheelData.getInstance().isVoltageTiltbackUnsupported())
                    batteryCalculateType = "fixed";

                String batteryString = String.format(Locale.US, "%s", batteryCalculateType);
                canvas.drawText(batteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint);
                canvas.restore();
                canvas.save();
            }

            if (getWidth() > getHeight())
                canvas.rotate((143.5F + (currentTemperature * 2.25F)), innerArcRect.centerX(), innerArcRect.centerY());
            else
                canvas.rotate((143.5F + (currentTemperature * 2.25F)), innerArcRect.centerY(), innerArcRect.centerX());

            String temperatureString = String.format(Locale.US, "%02d℃", mTemperature);
            canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint);
            canvas.restore();
            canvas.save();

            // Max temperature
            if (getWidth() > getHeight())
                canvas.rotate(-50F, innerArcRect.centerX(), innerArcRect.centerY());
            else
                canvas.rotate(-50F, innerArcRect.centerY(), innerArcRect.centerX());
            String maxTemperatureString = String.format(Locale.US, "%02d℃", mMaxTemperature);
            canvas.drawText(maxTemperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint);
            canvas.restore();
            canvas.save();
        }

        // Wheel name
        canvas.drawTextOnPath(mWheelModel, modelTextPath, 0, 0, modelTextPaint);

        // Draw text blocks bitmap
        canvas.drawBitmap(mTextBoxesBitmap, 0, 0, null);

        refreshDisplay = currentSpeed != targetSpeed ||
                currentCurrent != targetCurrent ||
                currentBattery != targetBattery ||
                currentTemperature != targetTemperature;

        if (getWidth() * 1.2 < getHeight()) {
            canvas.drawText(versionString,
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom(),
                    versionPaint);
        }
    }

    private int updateCurrentValue(int target, int current) {
        if (target > current)
            return current + 1;
        else if (current > target)
            return current - 1;
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

    private float calculateFontSize(@NonNull Rect textBounds, @NonNull RectF textContainer, @NonNull String text, @NonNull Paint textPaint) {
        return calculateFontSize(textBounds, textContainer, text, textPaint, 1);
    }

    private float calculateFontSize(@NonNull Rect textBounds, @NonNull RectF textContainer, @NonNull String text, @NonNull Paint textPaint, int lines) {
        textPaint.setTextSize(100);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);

        int h = textBounds.height();
        float w = textPaint.measureText(text);

        float target_h = textContainer.height();
        if (lines != 1) {
            target_h /= (float) lines * 1.2;
        }
        float target_w = textContainer.width();

        float size_h = ((target_h / h) * 100f);
        float size_w = ((target_w / w) * 100f);

        float result = Math.min(size_h, size_w);
        textPaint.setTextSize(result);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        return result;
    }
}

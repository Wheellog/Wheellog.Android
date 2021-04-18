package com.cooper.wheellog.presentation.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.cooper.wheellog.R;

/**
 * Доработанный {@link androidx.preference.SeekBarPreference}, поддерживающий установку
 * значения через диалоговое окно по клику на текст с текущим значением.
 *
 * В отличие от стандартного {@link androidx.preference.SeekBarPreference} всегда
 * отображает текстовое значение с выбранным значением. Также для текстового значения
 * добавлено отображение единицы измерения.
 *
 * @author Yakushev Vladimir <ru.phoenix@gmail.com>
 */
public class SeekBarPreference extends Preference {

    private static final String TAG = "SeekBarPreference";
    private static final int DEFAULT_CURRENT_VALUE = 50;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_INTERVAL = 1;
    private static final int DEFAULT_DECIMAL_PLACES = 0;

    private int mSeekBarValue;
    private int mMin;
    private int mDecimalPlaces;
    private int mMax;
    private int mSeekBarIncrement; //FIX ME - it doesn't work!
    private boolean mTrackingTouch;
    private TextView mSeekBarValueTextView;
    private String mMeasurementUnit;
    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new OnChangeListener();

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.seekbar_view_layout);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
        try {
            mMin = a.getInt(R.styleable.SeekBarPreference_sbp_minValue, DEFAULT_MIN_VALUE);
            mSeekBarIncrement = a.getInt(R.styleable.SeekBarPreference_sbp_increment, DEFAULT_INTERVAL);
            mMax = a.getInt(R.styleable.SeekBarPreference_sbp_maxValue, DEFAULT_MAX_VALUE);
            mDecimalPlaces = a.getInt(R.styleable.SeekBarPreference_sbp_decimalPlaces, DEFAULT_DECIMAL_PLACES);
            if (mDecimalPlaces > 3)
                mDecimalPlaces = 3;
            if (mDecimalPlaces < 0)
                mDecimalPlaces = 0;
            mMeasurementUnit = a.getString(R.styleable.SeekBarPreference_sbp_measurementUnit);
            if (mMeasurementUnit == null) {
                mMeasurementUnit = "";
            }
            mSeekBarValue = attrs != null
                    ? attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_CURRENT_VALUE)
                    : DEFAULT_CURRENT_VALUE;
        } finally {
            a.recycle();
        }
    }

    public void setMin(int value) {
        mMin = value;
    }

    public int getMin() {
        return mMin;
    }

    public void setIncrement(int value) {
        mSeekBarIncrement = value;
    }

    public int getIncrement() {
        return mSeekBarIncrement;
    }

    public void setMax(int value) {
        mMax = value;
    }

    public int getMax() {
        return mMax;
    }

    public void setUnit(String value) {
        mMeasurementUnit = value;
    }

    public String getUnit() {
        return mMeasurementUnit;
    }

    public void setDecimalPlaces(int value) {
        mDecimalPlaces = value;
    }

    public int getDecimalPlaces() {
        return mDecimalPlaces;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mSeekBarValueTextView = (TextView) view.findViewById(R.id.seekbar_value);
        mSeekBarValueTextView.setOnClickListener(v ->
                new CustomValueDialog(getContext(), mMin, mMax, mSeekBarValue, mDecimalPlaces)
                        .setOnValueChangeListener(value -> setValueInternal(value, true))
                        .show());

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        if (seekBar == null) {
            Log.e(TAG, "SeekBar view is null in onBindViewHolder.");
            return;
        }

        if (mSeekBarIncrement != 0) {
            seekBar.setKeyProgressIncrement(mSeekBarIncrement);
        } else {
            mSeekBarIncrement = seekBar.getKeyProgressIncrement();
        }

        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        // If the increment is not zero, use that. Otherwise, use the default mKeyProgressIncrement
        // in AbsSeekBar when it's zero. This default increment value is set by AbsSeekBar
        // after calling setMax. That's why it's important to call setKeyProgressIncrement after
        // calling setMax() since setMax() can change the increment value.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.setMin(mMin);
        }
        seekBar.setMax(mMax);

        seekBar.setProgress(mSeekBarValue); // - mMin);
        updateLabelValue(mSeekBarValue);
        seekBar.setEnabled(isEnabled());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.mSeekBarValue = mSeekBarValue;
        myState.mMin = mMin;
        myState.mMax = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mSeekBarValue = myState.mSeekBarValue;
        mMin = myState.mMin;
        mMax = myState.mMax;
        notifyChanged();
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = 0;
        }
        setValue(getPersistedInt((Integer) defaultValue));
    }

    /**
     * Gets the current progress of the {@link SeekBar}.
     *
     * @return The current progress of the {@link SeekBar}
     */
    public int getValue() {
        return mSeekBarValue;
    }

    /**
     * Sets the current progress of the {@link SeekBar}.
     *
     * @param seekBarValue The current progress of the {@link SeekBar}
     */
    public void setValue(int seekBarValue) {
        setValueInternal(seekBarValue, true);
    }

    /**
     * Persist the {@link SeekBar}'s SeekBar value if callChangeListener returns true, otherwise
     * set the {@link SeekBar}'s value to the stored value.
     */
    private void syncValueInternal(SeekBar seekBar) {
        int seekBarValue = seekBar.getProgress();
        //int seekBarValue = mMin + seekBar.getProgress();

        if (seekBarValue != mSeekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false);
            } else {
                //seekBar.setProgress(mSeekBarValue - mMin);
                seekBar.setProgress(mSeekBarValue);
                updateLabelValue(mSeekBarValue);
            }
        }
    }

    private void setValueInternal(int seekBarValue, boolean notifyChanged) {
        if (seekBarValue < mMin) {
            seekBarValue = mMin;
        }
        if (seekBarValue > mMax) {
            seekBarValue = mMax;
        }

        if (seekBarValue != mSeekBarValue) {
            mSeekBarValue = seekBarValue;
            updateLabelValue(mSeekBarValue);
            persistInt(seekBarValue);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    /**
     * Attempts to update the TextView label that displays the current value.
     *
     * @param value the value to display next to the {@link SeekBar}
     */
    private void updateLabelValue(int value) {
        if (mSeekBarValueTextView != null) {
            //Timber.i("UPDATE<<<<<<<<<<<<<");
            if (mDecimalPlaces == 0)
                mSeekBarValueTextView.setText(String.format("%s %s", value, mMeasurementUnit));

            else if (mDecimalPlaces == 1)
                mSeekBarValueTextView.setText(String.format("%.1f %s", value/(10.0), mMeasurementUnit));
            else if (mDecimalPlaces == 2)
                mSeekBarValueTextView.setText(String.format("%.2f %s", value/(100.0), mMeasurementUnit));
            else if (mDecimalPlaces == 3)
                mSeekBarValueTextView.setText(String.format("%.3f %s", value/(1000.0), mMeasurementUnit));
        }
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state of this preference.
     *
     * <p>It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        int mSeekBarValue;
        int mMin;
        int mMax;

        SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            mSeekBarValue = source.readInt();
            mMin = source.readInt();
            mMax = source.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(mSeekBarValue);
            dest.writeInt(mMin);
            dest.writeInt(mMax);
        }
    }

    private class OnChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O && progress < mMin) {
                seekBar.setProgress(mMin);
            }

            if (fromUser && (!mTrackingTouch)) {
                syncValueInternal(seekBar);
            } else {
                // We always want to update the text while the seekbar is being dragged
                //updateLabelValue(progress + mMin);
                updateLabelValue(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTrackingTouch = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mTrackingTouch = false;
            //if (seekBar.getProgress() + mMin != mSeekBarValue) {
            if (seekBar.getProgress() != mSeekBarValue) {
                syncValueInternal(seekBar);
            }
        }
    }
}

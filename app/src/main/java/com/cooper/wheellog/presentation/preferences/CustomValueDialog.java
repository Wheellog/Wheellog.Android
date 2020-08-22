package com.cooper.wheellog.presentation.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.cooper.wheellog.R;

/**
 * Диалог для выбора значения из диапазона
 *
 * @author Yakushev Vladimir <ru.phoenix@gmail.com>
 */
class CustomValueDialog {

    private final String TAG = getClass().getSimpleName();

    private Dialog mDialog;
    private EditText mCustomValueView;
    private OnValueChangeListener mOnValueChangeListener;

    private int mMinValue;
    private int mMaxValue;
    private int mCurrentValue;
    private int mDecimalPlaces;
    /**
     * @param context      контекст текущей {@link Activity}
     * @param minValue     минимально допустимое значение
     * @param maxValue     максимально допустимое значение
     * @param currentValue текущее значение для подсказки
     */
    CustomValueDialog(Context context, int minValue, int maxValue, int currentValue, int decimalPlaces) {
        mMinValue = minValue;
        mMaxValue = maxValue;
        mCurrentValue = currentValue;
        mDecimalPlaces = decimalPlaces;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.value_selector_dialog, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        mDialog = dialogBuilder.setView(dialogView).create();

        TextView minValueView = dialogView.findViewById(R.id.minValue);
        TextView maxValueView = dialogView.findViewById(R.id.maxValue);
        mCustomValueView = dialogView.findViewById(R.id.customValue);
        if (mDecimalPlaces == 0) {
            minValueView.setText(String.valueOf(mMinValue));
            maxValueView.setText(String.valueOf(mMaxValue));
            mCustomValueView.setHint(String.valueOf(mCurrentValue));
        }
        if (mDecimalPlaces == 1) {
            minValueView.setText(String.format("%.1f", mMinValue/10.0));
            maxValueView.setText(String.format("%.1f", mMaxValue/10.0));
            mCustomValueView.setHint(String.format("%.1f", mCurrentValue/10.0));
        }
        if (mDecimalPlaces == 2) {
            minValueView.setText(String.format("%.2f", mMinValue/100.0));
            maxValueView.setText(String.format("%.2f", mMaxValue/100.0));
            mCustomValueView.setHint(String.format("%.2f", mCurrentValue/100.0));
        }
        if (mDecimalPlaces == 3) {
            minValueView.setText(String.format("%.3f", mMinValue/1000.0));
            maxValueView.setText(String.format("%.3f", mMaxValue/1000.0));
            mCustomValueView.setHint(String.format("%.3f", mCurrentValue/1000.0));
        }
        Button applyButton = dialogView.findViewById(R.id.btn_apply);
        applyButton.setOnClickListener(v -> tryApply());

        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> mDialog.dismiss());
    }

    /**
     * @param listener подписчик на изменение значения в диалоге
     * @return ссылку на диалог для его последующей настройки
     */
    CustomValueDialog setOnValueChangeListener(OnValueChangeListener listener) {
        mOnValueChangeListener = listener;
        return this;
    }

    /**
     * Запуск диалога
     */
    void show() {
        mDialog.show();
    }

    private void tryApply() {
        int value;
        float fval;
        try {
            fval = Float.parseFloat(mCustomValueView.getText().toString());
            value = Math.round(fval);
            if (mDecimalPlaces == 1)
                value = Math.round(fval*10);
            if (mDecimalPlaces == 2)
                value = Math.round(fval*100);
            if (mDecimalPlaces == 3)
                value = Math.round(fval*1000);

            if (value > mMaxValue) {
                Log.e(TAG, "wrong input( > than required): " + mCustomValueView.getText().toString());
                notifyWrongInput();
                return;
            } else if (value < mMinValue) {
                Log.e(TAG, "wrong input( < then required): " + mCustomValueView.getText().toString());
                notifyWrongInput();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "wrong input(non-integer): " + mCustomValueView.getText().toString());
            notifyWrongInput();
            return;
        }

        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onChanged(value);
            mDialog.dismiss();
        }
    }

    private void notifyWrongInput() {
        mCustomValueView.setText("");
        if (mDecimalPlaces == 0) {
            mCustomValueView.setHint(String.valueOf(mCurrentValue));
        }
        if (mDecimalPlaces == 1) {
            mCustomValueView.setHint(String.format("%.1f", mCurrentValue/10.0));
        }
        if (mDecimalPlaces == 2) {
            mCustomValueView.setHint(String.format("%.2f", mCurrentValue/100.0));
        }
        if (mDecimalPlaces == 3) {
            mCustomValueView.setHint(String.format("%.3f", mCurrentValue/1000.0));
        }

    }
}

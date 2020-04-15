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

    /**
     * @param context      контекст текущей {@link Activity}
     * @param minValue     минимально допустимое значение
     * @param maxValue     максимально допустимое значение
     * @param currentValue текущее значение для подсказки
     */
    CustomValueDialog(Context context, int minValue, int maxValue, int currentValue) {
        mMinValue = minValue;
        mMaxValue = maxValue;
        mCurrentValue = currentValue;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.value_selector_dialog, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        mDialog = dialogBuilder.setView(dialogView).create();

        TextView minValueView = dialogView.findViewById(R.id.minValue);
        minValueView.setText(String.valueOf(mMinValue));

        TextView maxValueView = dialogView.findViewById(R.id.maxValue);
        maxValueView.setText(String.valueOf(mMaxValue));

        mCustomValueView = dialogView.findViewById(R.id.customValue);
        mCustomValueView.setHint(String.valueOf(currentValue));

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

        try {
            value = Integer.parseInt(mCustomValueView.getText().toString());
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
        mCustomValueView.setHint(String.valueOf(mCurrentValue));
    }
}

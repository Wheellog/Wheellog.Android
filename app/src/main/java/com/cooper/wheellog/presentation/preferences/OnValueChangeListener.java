package com.cooper.wheellog.presentation.preferences;

/**
 * Интерфейс подписчика на изменение значения
 *
 * @author Yakushev Vladimir <ru.phoenix@gmail.com>
 */
public interface OnValueChangeListener {

    /**
     * Оповещение об изменении значения
     *
     * @param value новое значение
     */
    void onChanged(int value);
}
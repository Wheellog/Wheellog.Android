package com.cooper.wheellog.utils.veteran;

/**
 * Computes the battery percentage for Veteran wheels.
 */
public class VeteranBatteryCalculator {
    // TODO add support for 151V wheels
    private static final int VOLTAGE_THRESHOLD_PATTON = 12525;
    private static final int VOLTAGE_THRESHOLD_BETTER_PERCENTS = 10200;
    private static final int VOLTAGE_LOW_THRESHOLD = 9600;
    private static final double VOLTAGE_TO_PERCENT_HIGH = 25.5;
    private static final double VOLTAGE_TO_PERCENT_LOW = 67.5;

    public int calculateBattery(int voltage, int version, boolean useAccuratePercentages) {
        if (version < 4) {
            return useAccuratePercentages ? calculateNonPattonAccurate(voltage) : calculateNonPattonStandard(voltage);
        } else {
            return useAccuratePercentages ? calculatePattonAccurate(voltage) : calculatePattonStandard(voltage);
        }
    }

    private static int calculateNonPattonAccurate(int voltage) {
        if (voltage > 10020) return 100;
        if (voltage > 8160) return roundPercentage(voltage - 8070, 19.5);
        if (voltage > 7935) return roundPercentage(voltage - 7935, 48.75);
        return 0;
    }

    private static int calculateNonPattonStandard(int voltage) {
        if (voltage <= 7935) return 0;
        if (voltage >= 9870) return 100;
        return roundPercentage(voltage - 7935, 19.5);
    }

    private static int calculatePattonAccurate(int voltage) {
        if (voltage > VOLTAGE_THRESHOLD_PATTON) return 100;
        if (voltage > VOLTAGE_THRESHOLD_BETTER_PERCENTS) return roundPercentage(voltage - 9975, VOLTAGE_TO_PERCENT_HIGH);
        if (voltage > VOLTAGE_LOW_THRESHOLD) return roundPercentage(voltage - 9600, VOLTAGE_TO_PERCENT_LOW);
        return 0;
    }

    private static int calculatePattonStandard(int voltage) {
        if (voltage <= 9918) return 0;
        if (voltage >= 12337) return 100;
        return roundPercentage(voltage - 9918, 24.2);
    }

    private static int roundPercentage(double value, double factor) {
        return (int) Math.round(value / factor);
    }
}
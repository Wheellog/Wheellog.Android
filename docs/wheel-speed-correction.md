# Wheel speed correction

Application settings provide an optional correction from -10% to +10% in 0.5% steps.
It is disabled by default, with a stored default of 0%, and saved per wheel MAC.
Legacy integer percentages remain readable and are clamped to the new range.
New settings store integer half-percent units under a separate per-wheel key;
this avoids preference type mismatches and preserves exact 0.5% increments.
The corrected value is controller_speed * (1 + percent / 100). For example, -10%
changes a controller reading of 100 km/h to 90 km/h. GPS speed is unchanged.

Current, maximum and distance/time-derived average wheel speed getters apply the
correction. Their consumers include the dashboard, live graph, companions, logger
and software speed alarms. Firmware commands, raw controller fields and the
calculated PWM formula retain raw values. A small true indicator is drawn below
the central speed unit while correction is enabled.

New CSV rows store the corrected speed and append wheel_speed_raw and
speed_correction_percent after the pre-existing columns. Recorded rows retain the
fractional correction applied at capture time; historical files are not rewritten. The original
CSV columns keep their order, and GPS values are not calibrated by this option.

WheelDataTest checks raw versus corrected current and maximum speeds. InfoBlocksTest
checks defaults, clamping, per-wheel persistence and disabling without losing the
saved percentage. TripValuesTest checks CSV correction metadata and GPS independence.

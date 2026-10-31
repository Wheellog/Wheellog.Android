# Native CSV trips and route viewing

The existing WheelLog CSV format and Trips page remain the source of trip data.
Historical stored CSV summaries retain upstream behavior; the card duration uses
recorded intervals rather than counting hours with no samples as recording time.
extended logs use absolute timestamps for accurate midnight crossings.
The main-toolbar recording button opens Start, Pause and Stop commands and
disables commands that do not apply. Manual Start does not use the automatic
movement threshold. The button can be hidden in Main menu buttons.

Automatic recording follows the existing logging preferences and movement
threshold. Disabling location stops GPS collection. A wheel disconnection keeps
GPS movement recording but does not repeat stale wheel readings. Long gaps form
separate segments in the same trip. Stationary drift and unconfirmed GPS movement
are filtered. Manual pause is explicit; automatic standing pause starts after
five minutes. Stop completes a session and the next Start makes a new file.
Normal exit and crash recovery preserve the configured same-day continuation.

Trip cards have lightly dimmed route previews drawn in solid red, independently
of map rendering preferences. Their menus support rename, Share CSV and GPX
export. The route viewer has solid rendering by default, optional speed coloring,
a timeline, selected-point telemetry, 60x replay with Play/Pause, following and a
latest-point action. OSM attribution stays visible on the full map. Distances in
the Values tab are in kilometers. Missing PWM remains unknown; valid zero is
retained and historical wheel rows can enrich GPS samples within a bounded gap.

No second recorder or standalone map dashboard is introduced. The default order matches upstream: Main, Values, Smart BMS when available,
Graph, Trips and Events; disabled pages remain hidden. Additional dashboard ordering is in a separate change.

Tests cover CSV parsing, GPS/wheel association, pause and recovery, playback,
export, route segments, thumbnail behavior and day/night theme surfaces.
Physical-device BLE/GPS and background-service verification is still required.

## Trip cards and Values

Thumbnails are square with rounded corners. Eight existing card metrics use a new
layout beside the preview: two columns with large icons spanning value and unit
rows. Values are left aligned; captions begin below the first digit. The metric grid
is constrained to the same height as the thumbnail. Original hamburger menus and the
absence of a Trips page heading are retained.

Values contains GPS maximum, time-weighted average and moving average speed; wheel
maximum speed; maximum PWM, power, current and phase current; minimum voltage;
moving time; distance in kilometres; and maximum board and motor temperatures.
GPS moving averages use available GPS samples and skip gaps and segment boundaries.
Overall GPS average includes stationary recording time. New files append an optional
recording_elapsed_ms clock that excludes manual and automatic pauses; filtered stationary
coordinates are not required. A final timing-only row can retain a short final stop without
inventing sensor data or a map point. Older files lack pause timing, so their overall
average counts consecutive recorded intervals up to 30 seconds rather than including
unrecorded gaps. Actual recorded zero-speed samples still count as stationary time.
Stops without any samples or recording clock cannot be reconstructed from an old file.
Continuing an older file preserves its original header and column layout.
Missing optional telemetry is shown as an em dash; a recorded zero remains zero.
Speeds are presented in km/h and temperatures in Celsius on this summary screen.

With same-day continuation enabled, morning Stop closes the morning session. Lunch
Start opens a new CSV. After a normal exit, an evening Start for the same wheel
continues the lunch CSV until Stop is pressed again. NativeTrackRecordingTest covers
this complete sequence without modifying the finished morning file.

Fresh decoded wheel packets feed the CSV writer directly, independently of UI broadcasts
and cached BluetoothService state. Disconnects invalidate the cached sample; old decoded
timestamps are rejected. GPS-only files cannot supply wheel maxima, PWM or consumption:
cards display a dash rather than labeling GPS speed as wheel speed or substituting zero.
CSV summaries use availability metadata and are refreshed from version6 caches.
Extended-file energy integrates available power over valid short intervals only.
Extended-file distance retains the last available wheel odometer even when the
file ends with many GPS-only or timing-only rows. The upstream summary behavior
for historical CSV files is unchanged.
The dashboard Distance block restores the continued CSV's starting wheel odometer
after a restart, so it includes earlier rides in that file. The origin is saved for
new recordings; older files fall back to their first available wheel odometer.
Explicit Stop followed by Start creates a new distance origin. With no wheel
odometer yet, GPS-only history cannot invent one: the first live reading establishes
it. This file-distance origin is separate from live session timing/average-speed
counters and does not replay archived voltage, current or other live sensors.
Speed correction does not alter this distance or the lifetime wheel odometer.

All recorded parts of a daily file contribute to their own GPS and wheel maxima;
missing wheel telemetry is never replaced by a GPS maximum. Graphs separate line
segments across missing intervals and share a real-time horizontal range. Their
legends and popup labels list each metric once. Replay skips intervals without a
recorded route while preserving original timestamps within the recorded segments.
Marker content uses a transparent inner layout so the themed marker surface remains
visible and text contrast is evaluated against the actual surface behind each label.

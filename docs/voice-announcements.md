# Offline voice announcements

Voice announcements are optional and disabled by default. Settings expose installed
offline Russian and English voices and separate previews. Speech language follows
the app interface, including Force English. No independent language selector exists.

## Events and report contents

Voice alarms reuse existing thresholds and require the global warnings switch.
Existing beeps and vibration keep their independent settings. Connection speech
waits for real telemetry and a ready engine. Repeated discovery callbacks do not
repeat the connected phrase or restart report timing. Unexpected loss is debounced
two seconds; intentional disconnection is silent.

Reports can be triggered independently by time, every N metres of a trip, once
after N metres from activation, and once at a total-odometer target. All conditions
read the same selected contents, and coincident conditions merge. Distance defaults
to5km; saved custom intervals remain unchanged. Distances are integer km/metres,
stored per wheel. Offline activation waits for fresh telemetry where an origin is
needed. One-shot targets persist across restarts and turn off after completion.

Switches reflect persisted changes immediately, including offline edits and service
completion. A fresh odometer initializes an unedited total target for the session;
user-edited targets are retained. Reports can be restricted to movement at10km/h;
an already-due report waits for movement rather than losing its turn. Stale wheel
telemetry over5seconds is rejected. Alarms preempt reports; reports wait10seconds
after alarm activity. There is one utterance at a time and no speech backlog.

Contents are grouped as speeds, distance/time, battery/electrical/PWM, temperatures.
Current speed, average moving speed, average speed and ride time use existing wheel
calculations. Added fields are opt-in. Default contents are speed/battery/distance.
Default time interval60seconds (15-1800), alarm repeat10seconds, rate/volume100%.

## Playback and music

The offline engine synthesizes a temporary WAV in the private voice-playback cache.
A WheelLog-owned MediaPlayer then plays it with media attributes, keeping audio
focus and actual playback in the same application. Music modes are unchanged,
duck (transient MAY_DUCK), and pause (transient EXCLUSIVE). The old duck_music
preference remains compatible. Automatic voice selection prefers the engine's
installed offline language default or Google's installed local counterpart;
explicit user selections are preserved.

Focus is acquired before synthesis and released only when playback ends, fails or
is cancelled. External focus loss stops speech. A60-second watchdog covers the
whole utterance. Cancellation releases both engine work and player resources;
stale callbacks cannot restart replaced speech. Temporary files are deleted and
only owned orphan utterance WAV files are cleaned on initialization. No global
volume changes, amplifier, continuous Bluetooth keepalive or extra permission is
introduced. The player volume follows the existing10-100% speech setting.

BluetoothService owns the timer and foreground lifecycle with connectedDevice and
mediaPlayback types. It retains the original notification ID/content/buttons;
there is no second voice service or ready notification. Diagnostic logs record
focus and synthesis/player results, not spoken text.

## Compatibility and validation

Existing dashboard layout/fonts, BMS, recording, history, schemas and AppConfig are
unchanged by the voice integration. Voice preferences live separately in
voice_announcements_v1. The public branch preserves stock launcher artwork, About,
app labels, build versions and default page settings. Personal APK version/signing
configuration, launcher badges and personal change lists are not part of this tree.

The requester accepted the playback revision on their phone on29October2026 after
342tests passed on the matching personal149-based APK sources. This establishes
that reported setup, not every Android/player combination. The canonical integration
is validated separately. Tests cover voice settings/languages, priority/scheduling,
connection deduplication, distance persistence/offline edits, private file cleanup,
playback completion/preemption/focus loss and the shared foreground notification.

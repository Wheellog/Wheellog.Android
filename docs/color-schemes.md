# Appearance themes

The existing Original and AJDM theme IDs are preserved. Seven additional schemes
reuse the original dashboard layout: Contrast, Inferno, Porcelain, Polar, Jade,
Steel and Heather. Theme titles are localized in English and Russian.

Settings use their own foreground and surface colors. Dialog actions, trip tabs,
chart labels and metric series are checked against the surface they are drawn on.
Inferno gauge labels have larger outlined text; the `true` label grows less.
The dashboard version text is smaller and muted without shrinking metric tiles.

In Contrast and Inferno, speed at its configured high threshold, PWM at its
upper threshold and battery at or below the configurable color threshold use
pure red (#ff0000). The battery color threshold does not change alarms.
Connection icons distinguish connected, searching and stopped states.

Changing the selected appearance shows a manual restart notice. Notifications
continue using the active theme until restart. Launcher aliases keep their
existing original icons; no personal branding or release version changes are
included.

Run the utility palette tests and `StatsAppearanceTest` for regression coverage.
Physical-device checks are still needed for launcher behavior and gauge layout.

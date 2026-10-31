# Dashboard page order and visibility

Application settings provide screen order and visibility controls. The main
dashboard is always enabled and first. All other screens can be reordered with
accessible up/down controls or hidden. The initial order matches upstream:
Main, Values, Smart BMS when available, Graph, Trips and Events. Disabled pages
remain hidden. No page-order reset is applied to existing upstream settings.

Preferences use stable internal page keys, remove duplicates and unknown keys,
and preserve page order across Smart BMS availability changes. Existing graph,
trips and events visibility preferences seed the initial selection. Showing or
hiding a page does not control the recording service.

`TrackDashboardTest` covers persistence, normalization and BMS availability.

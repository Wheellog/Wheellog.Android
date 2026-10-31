# Trip library: filters, sorting and CSV import

The native Trips page has a compact search field, a date selector and a sort menu.
Name matching is an immediate, case-insensitive substring of the displayed (including
renamed) title or CSV filename. It does not start CSV parsing on a keystroke.

Date filtering uses the first recorded timestamp cached in the trip summary, with the
stock filename timestamp as a fallback while metadata loads. Supported selections are
one day, month and year, whole year, or an inclusive date range. Local midnight and the
midnight after the final day are computed with Calendar, including daylight-saving days.
Name and date filters combine. Clearing the name retains the date filter; the visible
date/count row clears the date only. Query and sort choices use stable preference keys.

Sorts are start date (newest first by default), distance, maximum GPS speed and displayed
duration. Each supports ascending and descending order; unavailable metrics remain last.
Metric sorting uses the values on the cards, with date and URI tie breakers.
Metadata loading runs on IO. Cached card summaries are published before any stale CSV
is reparsed, so metric sorting works immediately in large libraries. Further summaries
are published progressively (at most ten refreshes per second), applying the current
query each time. Unreadable CSVs retain their cached summaries and do not discard the
rest of the catalog. Current recording summaries are refreshed rather than kept stale.
Reattaching a page resumes an interrupted metadata refresh. The original card/map actions
remain available and deleting a filtered row removes its exact URI from the current list.

CSV import remains in Logging settings. The system chooser allows multiple files; the
content is validated as a WheelLog log. Selected files are staged in app cache and hashed
with streaming SHA-256. Exact byte duplicates of readable existing trips or earlier files
in the same selection are skipped even under different names. Changing line endings or
editing a file changes its content and is not treated as an exact duplicate.

Copies are stored under Downloads/WheelLog/manual. A collision with a filename anywhere
in the current trip list gets a separate suffix. Originals are not rewritten or removed.
Android 10+ imports use pending MediaStore items, publishing only after the copy completes;
failed writes/publication delete the new item. Legacy storage reserves a new file without
append/overwrite and removes that new file on a synchronous copy failure.
The Trips query excludes pending MediaStore items. Existing files that cannot be opened
are not claimed as compared: the result separately reports their count.

A retained AndroidViewModel runs the import off the UI thread, survives Activity recreation,
guards repeat starts and reports progress plus added/already-present/error counts.
Error filenames are listed in the result. Staging files are closed and removed on failure
or cancellation. This does not add a persistent background import or recovery after
process death. RU and EN strings and theme-aware controls accompany the feature.

The personal test build additionally uses the approved dark-blue speaker badge.
The upstream-facing sources retain the stock launcher artwork and release metadata.

package com.cooper.wheellog.track

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.PopupMenu
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.cooper.wheellog.LoggingService
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.ThemeManager

data class TrackRecordingMenuState(val start: Boolean, val stop: Boolean, val pause: Boolean)

/** Both record indicators open the same menu; unavailable commands stay visible and muted. */
object TrackRecordingControl {
    fun availability(state: TrackStatus, locationEnabled: Boolean) = TrackRecordingMenuState(
        (!state.recording && locationEnabled) || (state.recording && state.paused), state.recording, state.recording && !state.paused)
    fun title(state: TrackStatus) = when {
        !state.recording -> R.string.track_idle
        state.pauseReason == TrackPause.AUTOMATIC -> R.string.track_auto_paused
        state.paused -> R.string.track_paused
        else -> R.string.track_recording
    }
    fun icon(state: TrackStatus) = if (state.recording && state.paused) R.drawable.ic_track_pause else R.drawable.ic_track_record
    fun color(state: TrackStatus) = when {
        !state.recording -> 0xffd1d5db.toInt()
        state.paused -> 0xffffb300.toInt()
        else -> 0xffff0000.toInt()
    }
    fun showMenu(context: Context, anchor: View, state: TrackStatus, locationEnabled: Boolean, action: (String) -> Unit): PopupMenu {
        val theme = if (ThemeManager.palette != null) R.style.PalettePopup else R.style.OriginalTheme_PopupMenuStyle
        val available = availability(state, locationEnabled)
        return PopupMenu(ContextThemeWrapper(context, theme), anchor).apply {
            fun item(id: Int, text: Int, enabled: Boolean) {
                val title = if (enabled) context.getText(text) else SpannableString(context.getText(text)).apply {
                    setSpan(ForegroundColorSpan(0xff69777b.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                menu.add(0, id, id, title).isEnabled = enabled
            }
            item(1, R.string.track_start_menu, available.start)
            item(2, R.string.track_stop_menu, available.stop)
            item(3, R.string.track_pause, available.pause)
            setOnMenuItemClickListener { item ->
                if (!item.isEnabled) false else {
                    when (item.itemId) {
                        1 -> action(if (state.recording && state.paused) LoggingService.RESUME else LoggingService.START)
                        2 -> action(LoggingService.STOP)
                        3 -> action(LoggingService.PAUSE)
                    }
                    true
                }
            }
            show()
        }
    }
}

package com.cooper.wheellog.views

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import com.cooper.wheellog.R
import com.cooper.wheellog.LocaleManager
import com.cooper.wheellog.data.*
import com.cooper.wheellog.map.*
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** The controls only filter the in-memory catalog; CSV parsing never runs on a keystroke. */
class TripBrowserControls(private val context: Context, private val root: View, private val adapter: TripAdapter) {
    private val search = root.findViewById<EditText>(R.id.trip_search)
    private val status = root.findViewById<TextView>(R.id.trip_filter_status)
    private val clear = root.findViewById<ImageButton>(R.id.trip_clear_search)
    private val date = root.findViewById<ImageButton>(R.id.trip_date_filter)
    private val sort = root.findViewById<ImageButton>(R.id.trip_sort)
    private val empty = root.findViewById<TextView>(R.id.trips_empty)
    init {
        val color = context.tripBodyTextColor()
        search.setTextColor(color); search.setHintTextColor(context.tripBodyMutedColor())
        search.background = GradientDrawable().apply {
            cornerRadius = 12 * context.resources.displayMetrics.density
            setColor(context.tripSurfaceColor())
        }
        // The field has its own surface, which may differ from the screen background.
        search.setTextColor(readableStatsColor(context.tripTextColor(), context.tripSurfaceColor()))
        search.setHintTextColor(readableStatsColor(context.tripMutedColor(), context.tripSurfaceColor()))
        for (button in listOf(clear,date,sort)) button.imageTintList = ColorStateList.valueOf(
            statsHueColor(context.tripIconColor(), context.getColorEx(R.color.background)))
        status.setTextColor(color); empty.setTextColor(color)
        search.setText(adapter.query.name)
        search.doAfterTextChanged { adapter.setQuery(adapter.query.copy(name = it?.toString() ?: "")) }
        clear.setOnClickListener { search.setText("") }
        date.setOnClickListener { dateMenu() }
        sort.setOnClickListener { sortMenu() }
        adapter.changed = { refresh() }
        refresh()
    }
    private fun refresh() {
        clear.isEnabled = search.text.isNotEmpty()
        clear.alpha = if (clear.isEnabled) 1f else .35f
        val query = adapter.query
        val count = context.getString(R.string.trips_found_count, adapter.itemCount, adapter.totalCount)
        val period = query.period
        status.visibility = if (period != null || query.name.isNotBlank() || adapter.loading) View.VISIBLE else View.GONE
        status.text = listOfNotNull(period?.let { periodLabel(it) + " ×" }, count,
            context.getString(R.string.trips_loading).takeIf { adapter.loading }).joinToString("\n")
        status.contentDescription = if (period != null) context.getString(R.string.trips_clear_date) else null
        status.isClickable = period != null
        status.setOnClickListener { if (period != null) adapter.setQuery(query.copy(period = null)) }
        empty.visibility = if (adapter.itemCount == 0 && !adapter.loading) View.VISIBLE else View.GONE
        sort.scaleY = if (query.descending) 1f else -1f
    }
    private fun periodLabel(period: TripPeriod): String {
        val locale = LocaleManager.getLocale(context.resources)
        if (period == TripPeriod.year(period.first.year)) return period.first.year.toString()
        if (period == TripPeriod.month(period.first.year, period.first.month))
            return SimpleDateFormat("LLLL yyyy", locale).format(Date(period.first.millis()))
        val format = SimpleDateFormat("dd.MM.yyyy", locale)
        val first = format.format(Date(period.first.millis()))
        return if (period.first == period.last) first else first + " – " + format.format(Date(period.last.millis()))
    }
    private fun sortMenu() {
        val wrapper = ContextThemeWrapper(context, if (ThemeManager.palette != null) R.style.PalettePopup else R.style.TripPopupOriginal)
        val menu = PopupMenu(wrapper, sort)
        val titles = listOf(R.string.trips_sort_date,R.string.trips_sort_distance,R.string.trips_sort_gps,R.string.trips_sort_duration)
        TripSort.entries.forEachIndexed { index, type ->
            menu.menu.add(1,index,index,titles[index]).apply {
                isCheckable = true; isChecked = adapter.query.sort == type
            }
        }
        menu.menu.setGroupCheckable(1,true,true)
        menu.menu.add(2,10,10,R.string.trips_descending).apply { isCheckable = true; isChecked = adapter.query.descending }
        menu.menu.add(2,11,11,R.string.trips_ascending).apply { isCheckable = true; isChecked = !adapter.query.descending }
        menu.menu.setGroupCheckable(2,true,true)
        menu.setOnMenuItemClickListener {
            adapter.setQuery(if (it.groupId == 1) adapter.query.copy(sort = TripSort.entries[it.itemId])
                else adapter.query.copy(descending = it.itemId == 10))
            true
        }
        styleTripMenu(context,menu.menu); menu.show()
    }
    private fun dateMenu() {
        val items = arrayOf(R.string.trips_all_dates,R.string.trips_day,R.string.trips_month,R.string.trips_year,R.string.trips_date_range)
            .map { context.getString(it) }.toTypedArray()
        val choices = object : ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position,convertView,parent).also { (it as TextView).setTextColor(
                    readableStatsColor(context.tripTextColor(),context.tripSurfaceColor())) }
        }
        val dialog = AlertDialog.Builder(context).setTitle(R.string.trips_date_filter).setAdapter(choices) { _, which ->
            when (which) {
                0 -> adapter.setQuery(adapter.query.copy(period = null))
                1 -> pickDay(R.string.trips_day, adapter.query.period?.first) { adapter.setQuery(adapter.query.copy(period = TripPeriod(it,it))) }
                2 -> pickMonthYear(true)
                3 -> pickMonthYear(false)
                4 -> pickDay(R.string.trips_range_start, adapter.query.period?.first) { first ->
                    pickDay(R.string.trips_range_end, adapter.query.period?.last ?: first, first) { last ->
                        adapter.setQuery(adapter.query.copy(period = TripPeriod(first,last)))
                    }
                }
            }
        }.setNegativeButton(android.R.string.cancel,null).create()
        showTripBrowserDialog(context,dialog)
    }
    private fun pickDay(title: Int, seed: TripDay?, minimum: TripDay? = null, selected: (TripDay) -> Unit) {
        val now = Calendar.getInstance()
        val day = seed ?: TripDay(now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1,now.get(Calendar.DAY_OF_MONTH))
        val light = androidx.core.graphics.ColorUtils.calculateLuminance(context.tripSurfaceColor()) >= .5
        val nativeTheme = if (light) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert
        val pickerContext = ContextThemeWrapper(context,nativeTheme)
        val backing = if (light) android.graphics.Color.WHITE else 0xff303030.toInt()
        val dialog = DatePickerDialog(pickerContext, { _,year,month,date -> selected(TripDay(year,month+1,date)) },day.year,day.month-1,day.day)
        dialog.setTitle(title)
        minimum?.let { dialog.datePicker.minDate = it.millis() }
        dialog.show()
        dialog.window?.decorView?.setBackgroundColor(backing)
        dialog.datePicker.setBackgroundColor(backing)
        for (which in listOf(-1,-2)) dialog.getButton(which)?.setTextColor(
            readableStatsColor(context.tripIconColor(),backing))
    }
    private fun pickMonthYear(withMonth: Boolean) {
        val now = Calendar.getInstance()
        val seed = adapter.query.period?.first
        val row = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER; setPadding(16,8,16,8) }
        val year = NumberPicker(context).apply {
            minValue = 1900; maxValue = 2100; value = (seed?.year ?: now.get(Calendar.YEAR)).coerceIn(minValue,maxValue)
            contentDescription = context.getString(R.string.trips_year)
        }
        val month = NumberPicker(context).apply {
            minValue = 1; maxValue = 12
            displayedValues = DateFormatSymbols(LocaleManager.getLocale(context.resources)).months.take(12).toTypedArray()
            value = seed?.month ?: now.get(Calendar.MONTH)+1
            contentDescription = context.getString(R.string.trips_month)
        }
        if (withMonth) row.addView(month,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        row.addView(year,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        val foreground = readableStatsColor(context.tripTextColor(),context.tripSurfaceColor())
        if (Build.VERSION.SDK_INT >= 29) { year.textColor = foreground; month.textColor = foreground }
        val dialog = AlertDialog.Builder(context).setTitle(if(withMonth) R.string.trips_month else R.string.trips_year)
            .setView(row).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok) { _,_ ->
                // Commit any typed NumberPicker value before reading it.
                row.clearFocus()
                adapter.setQuery(adapter.query.copy(period = if(withMonth) TripPeriod.month(year.value,month.value) else TripPeriod.year(year.value)))
            }.create()
        showTripBrowserDialog(context,dialog)
    }
}

fun showTripBrowserDialog(context: Context, dialog: AlertDialog) {
    dialog.setOnShowListener {
        val surface = context.tripSurfaceColor()
        dialog.window?.decorView?.setBackgroundColor(surface)
        dialog.findViewById<View>(androidx.appcompat.R.id.parentPanel)?.setBackgroundColor(surface)
        dialog.listView?.setBackgroundColor(surface)
        fun colors(view: View) {
            if (view is TextView) view.setTextColor(readableStatsColor(context.tripTextColor(),surface))
            if (view is ViewGroup) for (index in 0 until view.childCount) colors(view.getChildAt(index))
        }
        colors(dialog.window!!.decorView)
        for (which in listOf(-1,-2,-3)) dialog.getButton(which)?.setTextColor(readableStatsColor(context.tripIconColor(),surface))
    }
    dialog.show()
}

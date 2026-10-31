package com.cooper.wheellog.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.LoggingService
import com.cooper.wheellog.map.tripSurfaceColor
import com.cooper.wheellog.map.tripTextColor
import com.cooper.wheellog.map.tripMutedColor
import com.cooper.wheellog.map.tripIconColor
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.cooper.wheellog.R
import com.cooper.wheellog.data.TripCatalog
import com.cooper.wheellog.data.TripListItem
import com.cooper.wheellog.data.TripListQuery
import com.cooper.wheellog.data.TripListPreferences
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.data.TripDataDbEntry
import com.cooper.wheellog.data.TripParser
import com.cooper.wheellog.databinding.ListTripItemBinding
import com.cooper.wheellog.map.MapActivity
import com.cooper.wheellog.utils.SomeUtil.doAsync
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.MathsUtil.kmToMilesMultiplier
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager
import com.google.common.io.ByteStreams
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Locale
import org.json.JSONObject
import androidx.core.content.FileProvider
import android.content.ClipData


class TripAdapter(var context: Context, private var tripModels: ArrayList<TripModel>) : RecyclerView.Adapter<TripAdapter.ViewHolder>(), KoinComponent {
    private val appConfig: AppConfig by inject()
    private var uploadViewVisible: Int = View.VISIBLE
    private var font = ThemeManager.getTypeface(context)
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null
    private var refreshOnAttach = false

    var uploadVisible: Boolean
        get() = uploadViewVisible == View.VISIBLE
        set(value) { uploadViewVisible = if (value) View.VISIBLE else View.GONE }

    init {
        uploadVisible = appConfig.autoUploadEc
    }

    private val listPreferences by lazy { TripListPreferences(context.getSharedPreferences("trip_list", Context.MODE_PRIVATE)) }
    var query: TripListQuery = listPreferences.load()
        private set
    private var allTrips = ArrayList(tripModels)
    private var catalog = allTrips.map { TripCatalog.item(context, it) }
    var loading: Boolean = false
        private set
    var changed: (() -> Unit)? = null
    val totalCount: Int get() = allTrips.size

    @SuppressLint("NotifyDataSetChanged")
    fun setQuery(value: TripListQuery) {
        query = value
        listPreferences.save(value)
        applyQuery()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyQuery() {
        val byId = allTrips.associateBy { it.uri.toString() }
        tripModels = ArrayList(query.apply(catalog).mapNotNull { entry -> byId[entry.id]?.apply { title = entry.name } })
        notifyDataSetChanged()
        changed?.invoke()
    }

    fun updateTrips(tripModels: ArrayList<TripModel>) {
        refreshJob?.cancel()
        allTrips = ArrayList(tripModels)
        val previous = catalog.associateBy { it.id }
        catalog = allTrips.map { model -> previous[model.uri.toString()]?.copy(name = TripCatalog.title(context, model, previous[model.uri.toString()]?.startedAt))
            ?: TripCatalog.item(context, model) }
        if (allTrips.isEmpty()) { catalog = emptyList(); loading = false; applyQuery(); return }
        loading = true
        applyQuery()
        refreshJob = refreshScope.launch {
            val models = ArrayList(allTrips)
            try {
                catalog = withContext(Dispatchers.IO) {
                    TripCatalog.load(context, models, get<TripDao>()) { available ->
                        withContext(Dispatchers.Main) {
                            catalog = available
                            applyQuery()
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { Timber.w(error, "Trip catalog could not be refreshed") }
            loading = false
            applyQuery()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (refreshOnAttach) { refreshOnAttach = false; updateTrips(ArrayList(allTrips)) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        refreshOnAttach = refreshJob?.isActive == true
        refreshJob?.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    init { updateTrips(tripModels) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListTripItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        itemBinding.root.setCardBackgroundColor(parent.context.tripSurfaceColor())
        return ViewHolder(itemBinding, font)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = tripModels[position]
        holder.bind(trip, uploadViewVisible, this)
    }

    override fun getItemCount(): Int {
        return tripModels.size
    }

    fun removeAt(position: Int) { tripModels.getOrNull(position)?.let { removeTrip(it) } }
    fun removeTrip(model: TripModel) {
        allTrips.removeAll { it.uri == model.uri }
        catalog = catalog.filterNot { it.id == model.uri.toString() }
        applyQuery()
    }

    class ViewHolder internal constructor(private val itemBinding: ListTripItemBinding, val font: Typeface) : RecyclerView.ViewHolder(itemBinding.root), KoinComponent {
        private val dao: TripDao by inject()
        private val appConfig: AppConfig by inject()
        private val context = itemBinding.root.context
        private var boundFileName = ""

        private fun showMap(tripModel: TripModel) {
            context.startActivity(Intent(context, MapActivity::class.java).apply {
                putExtra("uri", tripModel.uri.toString())
                putExtra("path", tripModel.pathLegacyAndroid)
                putExtra("title", tripModel.title)
                putExtra("file_name", tripModel.fileName)
            }, Bundle.EMPTY)
        }

        private fun uploadToEc(tripModel: TripModel) {
            val inputStream =
                get<Context>().contentResolver.openInputStream(tripModel.uri)
            if (inputStream == null) {
                Timber.i("Failed to create inputStream for %s", tripModel.title)
            } else {
                val data = ByteStreams.toByteArray(inputStream)
                inputStream.close()
                ElectroClub.instance.uploadTrack(data, tripModel.title, false) { }
            }
        }

        private fun showTrackEc(trackIdInEc: Int) {
            if (trackIdInEc != -1) {
                val browserIntent = Intent(Intent.ACTION_VIEW, ElectroClub.instance.getUrlFromTrackId(trackIdInEc))
                context.startActivity(browserIntent, Bundle.EMPTY)
            }
        }

        private fun share(tripModel: TripModel) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, tripModel.uri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent, Bundle.EMPTY)
        }

        private fun deleteFile(tripModel: TripModel, adapter: TripAdapter) {
            if (LoggingService.currentLogFileName == tripModel.fileName) return
            AlertDialog.Builder(context)
                .setTitle(R.string.trip_menu_delete_file)
                .setMessage(context.getString(R.string.trip_menu_delete_file_confirmation) + " " + tripModel.fileName)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val file = tripModel.pathLegacyAndroid?.let { File(it) }
                        if (file?.exists() == true) {
                            file.canonicalFile.delete()
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    } else {
                        get<Context>().contentResolver.delete(tripModel.uri, null, null)
                    }
                    adapter.removeTrip(tripModel)

                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        dao.apply {
                            val tripDb = getTripByFileName(tripModel.fileName)
                            if (tripDb != null) {
                                delete(tripDb)
                            }
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
                .setBlackIcon()
        }

        private fun formatMi(value: Float): String {
            return format(MathsUtil.kmToMiles(value))
        }

        private fun format(value: Float): String {
            return String.format("%.2f", value)
        }

        private fun setDescFromDb(trip: TripDataDbEntry?) {
            val available = runCatching { JSONObject(trip?.additionalJson ?: "{}") }.getOrDefault(JSONObject())
            fun metric(value: Float?, name: String) = value?.takeIf { available.optBoolean(name, true) }
            val color = context.tripTextColor()
            val muted = context.tripMutedColor()
            val locale = com.cooper.wheellog.LocaleManager.getLocale(context.resources)
            fun number(value: Float?) = value?.let { String.format(locale, "%.2f", it) } ?: "—"
            fun speed(value: Float?) = number(value?.let { if (appConfig.useMph) MathsUtil.kmToMiles(it) else it })
            val speedUnit = context.getString(if (appConfig.useMph) R.string.mph else R.string.kmh)
            val distanceUnit = context.getString(if (appConfig.useMph) R.string.miles else R.string.km)
            fun bind(view: TripMetricView, icon: String, value: String, label: String) = view.bind(icon, value, label, color, muted, font)
            bind(itemBinding.metricWheelSpeed, "🚀", speed(metric(trip?.maxSpeed, "hasWheelSpeed")), "$speedUnit · " + context.getString(R.string.trip_metric_wheel))
            bind(itemBinding.metricGpsSpeed, "📡", speed(metric(trip?.maxSpeedGps, "hasGpsSpeed")), "$speedUnit · GPS")
            bind(itemBinding.metricAverage, "♿", speed(metric(trip?.avgSpeed, "hasAverageSpeed")), "$speedUnit · " + context.getString(R.string.trip_metric_average))
            bind(itemBinding.metricPwm, "😱", number(metric(trip?.maxPwm, "hasPwm")), "% · PWM")
            bind(itemBinding.metricDuration, "⌚", trip?.let { available.optInt("activeDurationMinutes", it.duration).toString() } ?: "—", context.getString(R.string.min))
            bind(itemBinding.metricDistance, "📏", speed(trip?.distance?.div(1000f)), distanceUnit)
            bind(itemBinding.metricEnergy, "⚡", number(metric(trip?.consumptionTotal, "hasEnergy")), context.getString(R.string.wh))
            val consumption = trip?.consumptionByKm?.let { if (appConfig.useMph) it / kmToMilesMultiplier.toFloat() else it }
            bind(itemBinding.metricConsumption, "🔋", number(metric(consumption?.takeIf { (trip?.distance ?: 0) > 0 }, "hasEnergy")), context.getString(if (appConfig.useMph) R.string.whmi else R.string.whkm))
        }

        private fun setFriendlyName() {
            val locale = com.cooper.wheellog.LocaleManager.getLocale(context.resources)
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
            try {
                val dateTime = sdf.parse(itemBinding.name.text.toString())
                val now = System.currentTimeMillis()
                var skeleton = "MMMM dd, HH:mm"
                if (dateTime != null) {
                    if (DateUtils.isToday(dateTime.time)) {
                        val text = itemBinding.name.context.getText(R.string.today).toString() +
                                SimpleDateFormat(", EEE, HH:mm", locale).format(dateTime)
                        itemBinding.name.text = text
                    } else if (now - dateTime.time < 604_800_000) { // current week
                        itemBinding.name.text = SimpleDateFormat("EEEE, HH:mm", locale).format(dateTime)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Year.now().value != Year.parse(itemBinding.name.text.subSequence(0, 4).toString()).value) {
                            skeleton = "yyyy $skeleton"
                        }
                        val bestFormat = DateFormat.getBestDateTimePattern(locale, skeleton)
                        val sdfName = SimpleDateFormat(bestFormat, locale)
                        itemBinding.name.text = sdfName.format(dateTime)
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility", "SetTextI18n")
        fun bind(tripModel: TripModel, uploadViewVisible: Int, adapter: TripAdapter) {
            boundFileName = tripModel.fileName
            itemBinding.name.text = tripModel.title
            itemBinding.name.typeface = font
            setFriendlyName()
            setDescFromDb(null)
            itemBinding.name.setTextColor(context.tripTextColor())
            itemBinding.thumbnail.maximumSpeed = 1.0
            itemBinding.thumbnail.bind(tripModel, if (LoggingService.currentLogFileName == tripModel.fileName) LoggingService.status.value.points / 10 else 0)
            val titles = context.getSharedPreferences("trip_titles", Context.MODE_PRIVATE)
            titles.getString(tripModel.fileName, null)?.let { itemBinding.name.text = it }

            var trackIdInEc = -1
            var trip: TripDataDbEntry? = null
            itemView.doAsync({
                trip = dao.getTripByFileName(tripModel.fileName)
                trackIdInEc =
                    if (uploadViewVisible == View.VISIBLE && trip != null && trip!!.ecId > 0) {
                        trip!!.ecId
                    } else {
                        -1
                    }
                if (trip == null || LoggingService.currentLogFileName == tripModel.fileName || trip?.duration == 0 || runCatching {
                        JSONObject(trip?.additionalJson ?: "{}").optInt("tripStatsVersion") != TripParser.STATS_VERSION
                    }.getOrDefault(true)) {
                    trip = TripParser.parseFile(context, tripModel.fileName, tripModel.pathLegacyAndroid ?: "", tripModel.uri).second
                }
            }) {
                if (boundFileName != tripModel.fileName) return@doAsync
                setDescFromDb(trip)
                itemBinding.thumbnail.maximumSpeed = maxOf(trip?.maxSpeed?.toDouble() ?: 0.0, trip?.maxSpeedGps?.toDouble() ?: 0.0, 1.0)
                itemBinding.thumbnail.invalidate()
                val wrapper = ContextThemeWrapper(context, if (ThemeManager.palette != null)
                    R.style.PalettePopup else R.style.TripPopupOriginal)
                val ecAvailable = appConfig.ecToken != null
                val popupMenu = PopupMenu(wrapper,  itemBinding.popupButton).apply {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        menu.add(0, 0, 0, R.string.trip_menu_view_map).icon =
                            context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsMap))
                        menu.add(0, 1, 1, R.string.trip_menu_upload_to_ec).apply {
                            icon =
                                context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsUpload))
                            isVisible = trackIdInEc == -1 && ecAvailable
                        }
                        menu.add(0, 2, 2, R.string.trip_menu_open_in_ec).apply {
                            icon =
                                context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsOpenEc))
                            isVisible = trackIdInEc != -1 && ecAvailable
                        }
                        menu.add(0, 3, 3, R.string.track_share_csv).icon =
                            context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsShare))
                        menu.add(0, 4, 4, R.string.trip_menu_delete_file).icon =
                            context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsDelete))
                    }
                    menu.add(0, 6, 2, R.string.track_rename).icon = context.getDrawable(R.drawable.ic_trip_rename)
                    menu.add(0, 7, 3, R.string.track_share).icon = context.getDrawable(R.drawable.ic_trip_share_gpx)
                    val recording = LoggingService.currentLogFileName == tripModel.fileName
                    menu.findItem(4)?.isEnabled = !recording
                    if (recording) {
                        menu.add(0, 8, 4, if (LoggingService.status.value.paused) R.string.track_resume else R.string.track_pause)
                            .icon = context.getDrawable(if (LoggingService.status.value.paused) R.drawable.ic_track_record else R.drawable.ic_track_pause)
                        menu.add(0, 9, 5, R.string.track_stop_menu).icon = context.getDrawable(R.drawable.ic_track_stop)
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            0 -> showMap(tripModel)
                            1 -> uploadToEc(tripModel)
                            2 -> showTrackEc(trackIdInEc)
                            3 -> share(tripModel)
                            4 -> deleteFile(tripModel, adapter)
                            7 -> com.cooper.wheellog.track.TrackActions.shareGpx(context, tripModel)
                            8 -> if (LoggingService.currentLogFileName == tripModel.fileName)
                                LoggingService.command(context, if (LoggingService.status.value.paused) LoggingService.RESUME else LoggingService.PAUSE)
                            9 -> if (LoggingService.currentLogFileName == tripModel.fileName) LoggingService.command(context, LoggingService.STOP)
                            6 -> com.cooper.wheellog.track.TrackActions.rename(context, itemBinding.name.text.toString(), {
                                check(titles.edit().putString(tripModel.fileName, it).commit())
                            }, { adapter.updateTrips(com.cooper.wheellog.utils.FileUtil.fillTrips(context)) })
                        }
                        return@setOnMenuItemClickListener false
                    }
                    styleTripMenu(context, menu)
                    setForceShowIcon(true)
                }
                itemView.setOnTouchListener(null)
                itemView.setOnLongClickListener { popupMenu.show(); true }
                itemView.setOnClickListener { showMap(tripModel) }
                itemBinding.popupButton.setOnClickListener {
                    popupMenu.show()
                }
            }

            // Themes
            itemBinding.popupButton.setImageResource(R.drawable.ic_baseline_settings_24)
            itemBinding.popupButton.imageTintList = android.content.res.ColorStateList.valueOf(context.tripIconColor())
        }
    }
}

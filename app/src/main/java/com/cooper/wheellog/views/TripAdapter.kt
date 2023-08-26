package com.cooper.wheellog.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.data.TripDataDbEntry
import com.cooper.wheellog.data.TripParser
import com.cooper.wheellog.databinding.ListTripItemBinding
import com.cooper.wheellog.map.MapActivity
import com.cooper.wheellog.utils.SomeUtil.doAsync
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager
import com.google.common.io.ByteStreams
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Locale


class TripAdapter(var context: Context, private var tripModels: ArrayList<TripModel>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var uploadViewVisible: Int = View.VISIBLE
    private var font = ThemeManager.getTypeface(context)

    var uploadVisible: Boolean
        get() = uploadViewVisible == View.VISIBLE
        set(value) { uploadViewVisible = if (value) View.VISIBLE else View.GONE }

    init {
        uploadVisible = WheelLog.AppConfig.autoUploadEc
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTrips(tripModels: ArrayList<TripModel>) {
        this.tripModels = tripModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListTripItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding, font)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = tripModels[position]
        holder.bind(trip, uploadViewVisible, this)
    }

    override fun getItemCount(): Int {
        return tripModels.size
    }

    fun removeAt(position: Int) {
        if (tripModels.size > position) {
            tripModels.removeAt(position)
            notifyItemChanged(position)
            notifyItemRangeRemoved(position, 1)
        }
    }

    class ViewHolder internal constructor(private val itemBinding: ListTripItemBinding, val font: Typeface) : RecyclerView.ViewHolder(itemBinding.root) {
        private val context = itemBinding.root.context

//        private fun uploadInProgress(inProgress: Boolean) {
//            uploadView.visibility = if (!inProgress) View.VISIBLE else View.GONE
//            uploadProgressView.visibility = if (inProgress) View.VISIBLE else View.GONE
//        }

        private fun showMap(tripModel: TripModel) {
            startActivity(context, Intent(context, MapActivity::class.java).apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    putExtra("path", tripModel.mediaId)
                } else {
                    putExtra("uri", tripModel.uri.toString())
                }
                putExtra("title", tripModel.title)
            }, Bundle.EMPTY)
        }

        private fun uploadToEc(tripModel: TripModel) {
//            uploadInProgress(true)
            val inputStream: InputStream? =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 or less
                    FileInputStream(File(tripModel.mediaId))
                } else {
                    // Android 10+
                    WheelLog.cResolver().openInputStream(tripModel.uri)
                }
            if (inputStream == null) {
                Timber.i("Failed to create inputStream for %s", tripModel.title)
//                uploadInProgress(false)
            } else {
                val data = ByteStreams.toByteArray(inputStream)
                inputStream.close()
                ElectroClub.instance.uploadTrack(data, tripModel.title, false) {
//                    MainScope().launch {
//                        uploadInProgress(false)
//                    }
                }
            }
        }

        private fun showTrackEc(trackIdInEc: Int) {
            if (trackIdInEc != -1) {
                val browserIntent = Intent(Intent.ACTION_VIEW, ElectroClub.instance.getUrlFromTrackId(trackIdInEc))
                startActivity(context, browserIntent, Bundle.EMPTY)
            }
        }

        private fun share(tripModel: TripModel) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, tripModel.uri)
                type = "text/csv"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(context, shareIntent, Bundle.EMPTY)
        }

        private fun deleteFile(tripModel: TripModel, adapter: TripAdapter) {
            AlertDialog.Builder(context)
                .setTitle(R.string.trip_menu_delete_file)
                .setMessage(context.getString(R.string.trip_menu_delete_file_confirmation) + " " + tripModel.fileName)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val file = File(tripModel.mediaId.replace(':', '_'))
                        if (file.exists()) {
                            file.canonicalFile.delete()
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    } else {
                        WheelLog.cResolver().delete(tripModel.uri, null, null)
                    }
                    adapter.removeAt(adapterPosition)

                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        ElectroClub.instance.dao?.apply {
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

        private fun toMiles(value: Float): String {
            return String.format("%.2f", MathsUtil.kmToMiles(value))
        }

        private fun toKm(value: Float): String {
            return String.format("%.2f", value)
        }

        private fun setDescFromDb(trip: TripDataDbEntry?) {
            if (trip != null && trip.duration != 0) {
                val min = context.getString(R.string.min)
                var desc1: String
                var desc2 = "⌚ ${trip.duration} $min"
                if (WheelLog.AppConfig.useMph) {
                    val mph = context.getString(R.string.mph)
                    val miles = context.getString(R.string.miles)
                    desc1 = "\uD83D\uDE80 ${toMiles(trip.maxSpeed)} $mph" +
                            "\n\uD83D\uDCE1 ${toMiles(trip.maxSpeedGps)} $mph" +
                            "\n♿ ${toMiles(trip.avgSpeed)} $mph"
                    desc2 += "\n\uD83D\uDCCF ${toMiles(trip.distance / 1000.0f)} $miles"
                } else {
                    val kmh = context.getString(R.string.kmh)
                    val km = context.getString(R.string.km)
                    desc1 = "\uD83D\uDE80 ${toKm(trip.maxSpeed)} $kmh" +
                            "\n\uD83D\uDCE1 ${toKm(trip.maxSpeedGps)} $kmh" +
                            "\n♿ ${toKm(trip.avgSpeed)} $kmh"
                    desc2 += "\n\uD83D\uDCCF ${toKm(trip.distance / 1000.0f)} $km"
                }
                desc1 += "\n\uD83D\uDE31 ${trip.maxPwm}%"
                if (trip.ecId != 0) {
                    desc1 += "\n\uD83C\uDF10 electro.club"
                }
                desc2 += "\n⚡ ${toKm(trip.consumptionTotal)} ${context.getString(R.string.wh)}" +
                            "\n\uD83D\uDD0B ${toKm(trip.consumptionByKm)} ${context.getString(R.string.whkm)}"
                itemBinding.description.text = desc1
                itemBinding.description2.text = desc2
            }
        }

        private fun setFriendlyName() {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
            try {
                val dateTime = sdf.parse(itemBinding.name.text.toString())
                val now = System.currentTimeMillis()
                var skeleton = "MMMM dd, HH:mm"
                if (dateTime != null) {
                    if (DateUtils.isToday(dateTime.time)) {
                        val text = itemBinding.name.context.getText(R.string.today).toString() +
                                SimpleDateFormat(", EEE, HH:mm", Locale.getDefault()).format(dateTime)
                        itemBinding.name.text = text
                    } else if (now - dateTime.time < 604_800_000) { // current week
                        itemBinding.name.text = SimpleDateFormat("EEEE, HH:mm", Locale.getDefault()).format(dateTime)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Year.now().value != Year.parse(itemBinding.name.text.subSequence(0, 4).toString()).value) {
                            skeleton = "yyyy $skeleton"
                        }
                        val bestFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
                        val sdfName = SimpleDateFormat(bestFormat, Locale.getDefault())
                        itemBinding.name.text = sdfName.format(dateTime)
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility", "SetTextI18n")
        fun bind(tripModel: TripModel, uploadViewVisible: Int, adapter: TripAdapter) {
            itemBinding.name.text = tripModel.title
            itemBinding.name.typeface = font
            itemBinding.description.apply {
                text = tripModel.description
                typeface = font
            }
            itemBinding.description2.apply {
                text = ""
                typeface = font
            }
//            uploadInProgress(false)
            setFriendlyName()

            var trackIdInEc = -1
            var trip: TripDataDbEntry? = null
            itemView.doAsync({
                trip = ElectroClub.instance.dao?.getTripByFileName(tripModel.fileName)
                trackIdInEc =
                    if (uploadViewVisible == View.VISIBLE && trip != null && trip!!.ecId > 0) {
                        trip!!.ecId
                    } else {
                        -1
                    }
                if (trip == null || trip?.duration == 0) {
                    TripParser.parseFile(context, tripModel.fileName, tripModel.mediaId, tripModel.uri)
                    trip = ElectroClub.instance.dao?.getTripByFileName(tripModel.fileName)
                }
            }) {
                setDescFromDb(trip)
                val wrapper = ContextThemeWrapper(context, R.style.OriginalTheme_PopupMenuStyle)
                val ecAvailable = WheelLog.AppConfig.ecToken != null
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
                        menu.add(0, 3, 3, R.string.trip_menu_share).icon =
                            context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsShare))
                        menu.add(0, 4, 4, R.string.trip_menu_delete_file).icon =
                            context.getDrawable(ThemeManager.getId(ThemeIconEnum.TripsDelete))
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            0 -> showMap(tripModel)
                            1 -> uploadToEc(tripModel)
                            2 -> showTrackEc(trackIdInEc)
                            3 -> share(tripModel)
                            4 -> deleteFile(tripModel, adapter)
                        }
                        return@setOnMenuItemClickListener false
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setForceShowIcon(true)
                    }
                }
                val gestureDetector = GestureDetector(
                    context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onLongPress(e: MotionEvent) {
                            super.onLongPress(e)
                            popupMenu.show()
                        }
                    })
                itemView.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }
                itemBinding.popupButton.setOnClickListener {
                    popupMenu.show()
                }
            }

            // Themes
            itemBinding.popupButton.setImageResource(ThemeManager.getId(ThemeIconEnum.TripsPopupButton))
        }
    }
}
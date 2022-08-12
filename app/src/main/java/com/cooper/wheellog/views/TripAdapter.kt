package com.cooper.wheellog.views

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.*
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.data.TripDatabase
import com.cooper.wheellog.map.MapActivity
import com.cooper.wheellog.utils.ThemeIconEnum
import com.google.common.io.ByteStreams
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*

class TripAdapter(var context: Context, private var tripModels: ArrayList<TripModel>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var uploadViewVisible: Int = View.VISIBLE
    private var font = WheelLog.ThemeManager.getTypeface(context)

    var uploadVisible: Boolean
        get() = uploadViewVisible == View.VISIBLE
        set(value) { uploadViewVisible = if (value) View.VISIBLE else View.GONE }

    init {
        uploadVisible = WheelLog.AppConfig.autoUploadEc
    }

    fun updateTrips(tripModels: ArrayList<TripModel>) {
        this.tripModels = tripModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.list_trip_item, parent, false)
        return ViewHolder(view, font)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = tripModels[position]
        holder.bind(trip, uploadViewVisible, this)
    }

    override fun getItemCount(): Int {
        return tripModels.size
    }

    fun removeAt(position: Int) {
        tripModels.removeAt(position)
        notifyItemChanged(position)
        notifyItemRangeRemoved(position, 1)
    }

    class ViewHolder internal constructor(var view: View, val font: Typeface) : RecyclerView.ViewHolder(view) {
        private var nameView: TextView = view.findViewById(R.id.name)
        private var descriptionView: TextView = view.findViewById(R.id.description)
        private var popupView: ImageView = view.findViewById(R.id.popupButton)
        private val context = view.context

        private fun uploadInProgress(inProgress: Boolean) {
//            uploadView.visibility = if (!inProgress) View.VISIBLE else View.GONE
//            uploadProgressView.visibility = if (inProgress) View.VISIBLE else View.GONE
        }

        private fun uploadViewEnabled(isEnabled: Boolean) {
//            uploadView.isEnabled = isEnabled
//            uploadView.imageAlpha = if (isEnabled) 0xFF else 0x20
        }

        private fun showMap(tripModel: TripModel) {
            startActivity(context, Intent(context, MapActivity::class.java).apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    putExtra("path", tripModel.mediaId)
                } else {
                    putExtra("uri", tripModel.uri)
                }
                putExtra("title", tripModel.title)
            }, Bundle.EMPTY)
        }

        private fun uploadToEc(tripModel: TripModel) {
            uploadInProgress(true)
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
                uploadInProgress(false)
            } else {
                val data = ByteStreams.toByteArray(inputStream)
                inputStream.close()
                ElectroClub.instance.uploadTrack(data, tripModel.title, false) { success ->
                    MainScope().launch {
                        uploadInProgress(false)
                    }
                    // hide the upload button if it was successful
                    uploadViewEnabled(!success)
                }
            }
        }

        private fun share(tripModel: TripModel) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, tripModel.uri)
                type = "text/csv"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(view.context, shareIntent, Bundle.EMPTY)
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
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
                .setBlackIcon()
        }

        @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
        fun bind(tripModel: TripModel, uploadViewVisible: Int, adapter: TripAdapter) {
            nameView.text = tripModel.title
            nameView.typeface = font
            descriptionView.text = tripModel.description
            descriptionView.typeface = font
            uploadInProgress(false)

            // check upload
            if (uploadViewVisible == View.VISIBLE) {
                GlobalScope.launch {
                    // async find
                    val trip = TripDatabase.getDataBase(context).tripDao().getTripByFileName(tripModel.fileName).value
                    withContext(Dispatchers.Main) {
                        uploadViewEnabled(trip != null && trip.ecId > 0)
                    }
                }
            }

            val wrapper = ContextThemeWrapper(context, R.style.OriginalTheme_PopupMenuStyle)
            val popupMenu = PopupMenu(wrapper, popupView).apply {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    menu.add(0, 0, 0, R.string.trip_menu_view_map).icon =
                        context.getDrawable(WheelLog.ThemeManager.getId(ThemeIconEnum.TripsMap))
                    menu.add(0, 1, 1, R.string.trip_menu_upload_to_ec).apply {
                        icon = context.getDrawable(WheelLog.ThemeManager.getId(ThemeIconEnum.TripsUpload))
                        isVisible = uploadViewVisible == View.VISIBLE
                    }
                    menu.add(0, 2, 2, R.string.trip_menu_share).icon =
                        context.getDrawable(WheelLog.ThemeManager.getId(ThemeIconEnum.TripsShare))
                    menu.add(0, 3, 3, R.string.trip_menu_delete_file).icon =
                        context.getDrawable(WheelLog.ThemeManager.getId(ThemeIconEnum.TripsDelete))
                }
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        0 -> showMap(tripModel)
                        1 -> uploadToEc(tripModel)
                        2 -> share(tripModel)
                        3 -> deleteFile(tripModel, adapter)
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
            view.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
            popupView.setOnClickListener {
                popupMenu.show()
            }
            // Themes
            popupView.setImageResource(WheelLog.ThemeManager.getId(ThemeIconEnum.TripsPopupButton))
        }
    }
}
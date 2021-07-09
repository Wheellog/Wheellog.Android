package com.cooper.wheellog.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.*
import com.google.common.io.ByteStreams
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*


class TripAdapter(var context: Context, private var trips: List<Trip>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var uploadViewVisible: Int = View.VISIBLE
    private var font = WheelLog.ThemeManager.getTypeface(context)

    var uploadVisible: Boolean
        get() = uploadViewVisible == View.VISIBLE
        set(value) { uploadViewVisible = if (value) View.VISIBLE else View.GONE }

    init {
        uploadVisible = WheelLog.AppConfig.autoUploadEc
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.list_trip_item, parent, false)
        return ViewHolder(view, font)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip, uploadViewVisible)
    }

    override fun getItemCount(): Int {
        return trips.size
    }

    class ViewHolder internal constructor(var view: View, val font: Typeface) : RecyclerView.ViewHolder(view) {
        private var nameView: TextView = view.findViewById(R.id.name)
        private var descriptionView: TextView = view.findViewById(R.id.description)
        private var uploadButtonLayout: RelativeLayout = view.findViewById(R.id.uploadButtonLayout)
        private var uploadView: ImageView = view.findViewById(R.id.uploadButton)
        private var uploadProgressView: ProgressBar = view.findViewById(R.id.progressBar)
        private var shareView: ImageView = view.findViewById(R.id.shareButton)

        private fun uploadInProgress(inProgress: Boolean) {
            uploadView.visibility = if (!inProgress) View.VISIBLE else View.GONE
            uploadProgressView.visibility = if (inProgress) View.VISIBLE else View.GONE
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(trip: Trip, uploadViewVisible: Int) {
            nameView.text = trip.title
            nameView.typeface = font
            descriptionView.text = trip.description
            descriptionView.typeface = font
            uploadButtonLayout.visibility = uploadViewVisible
            val context = view.context
            uploadInProgress(false)
            val gestureDetector = GestureDetector(
                context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
                        super.onLongPress(e)
                        val wrapper = ContextThemeWrapper(context, R.style.OriginalTheme_PopupMenuStyle)
                        val popupMenu = PopupMenu(wrapper, view).apply {
                            menu.add(0, 0, 0, R.string.trip_menu_view_map)
                            menu.add(0, 1, 0, R.string.trip_menu_upload_to_ec)
                            menu.add(0, 2, 0, R.string.trip_menu_share)
                            //menu.add(0, 3, 0, R.string.trip_menu_delete_file)
                        }
                        popupMenu.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                0 -> { // show map
                                    startActivity(context, Intent(context, MapActivity::class.java).apply {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                            putExtra("path", trip.mediaId)
                                        } else {
                                            putExtra("uri", trip.uri)
                                        }
                                        putExtra("title", trip.title)
                                    }, Bundle.EMPTY)
                                }
                                1 -> { // send to ec
                                    uploadInProgress(true)
                                    val inputStream: InputStream? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        // Android 9 or less
                                        FileInputStream(File(trip.mediaId))
                                    } else {
                                        // Android 10+
                                        WheelLog.cResolver().openInputStream(trip.uri)
                                    }
                                    if (inputStream == null) {
                                        Timber.i("Failed to create inputStream for %s", trip.title)
                                        uploadInProgress(false)
                                    } else {
                                        val data = ByteStreams.toByteArray(inputStream)
                                        inputStream.close()
                                        ElectroClub.instance.uploadTrack(data, trip.title, false) {
                                            MainScope().launch {
                                                uploadInProgress(false)
                                            }
                                        }
                                    }
                                }
                                2 -> { // share
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, trip.uri)
                                        type = "text/csv"
                                    }

                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    startActivity(context, shareIntent, Bundle.EMPTY)
                                }
                                3 -> { // delete
                                    // TODO: confirmation dialog
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        context.deleteFile(trip.mediaId)
                                    } else {
                                        WheelLog.cResolver().delete(trip.uri, null, null)
                                    }
                                }
                            }
                            return@setOnMenuItemClickListener false
                        }
                        popupMenu.show()
                    }
                })
            view.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
            uploadView.setOnClickListener {
                uploadInProgress(true)
                val inputStream: InputStream? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 or less
                    FileInputStream(File(trip.mediaId))
                } else {
                    // Android 10+
                    it.context.contentResolver.openInputStream(trip.uri)
                }
                if (inputStream == null) {
                    Timber.i("Failed to create inputStream for %s", trip.title)
                    uploadInProgress(false)
                    return@setOnClickListener
                }
                val data = ByteStreams.toByteArray(inputStream)
                ElectroClub.instance.uploadTrack(data, trip.title, false) {
                    MainScope().launch {
                        uploadInProgress(false)
                    }
                }
                inputStream.close()
            }
            shareView.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, trip.uri)
                    type = "text/csv"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(it.context, shareIntent, Bundle.EMPTY)
            }
        }
    }
}
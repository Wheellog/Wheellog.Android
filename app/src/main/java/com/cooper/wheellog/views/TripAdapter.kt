package com.cooper.wheellog.views

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.google.common.io.ByteStreams
import timber.log.Timber

class TripAdapter(var context: Context, private var trips: List<Trip>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    var uploadViewVisible: Int = View.VISIBLE

    init {
        uploadViewVisible = if (WheelLog.AppConfig.autoUploadEc) View.VISIBLE else View.GONE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.list_trip_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip, uploadViewVisible)
//            if(position % 2 == 0)
//            {
//                nameView.rootView.setBackgroundResource(R.color.background);
//            }
    }

    override fun getItemCount(): Int {
        return trips.size
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        var nameView: TextView = view.findViewById(R.id.name)
        var descriptionView: TextView = view.findViewById(R.id.description)
        var uploadView: ImageView = view.findViewById(R.id.uploadButton)
        var shareView: ImageView = view.findViewById(R.id.shareButton)

        fun bind(trip: Trip, uploadViewVisible: Int) {
            nameView.text = trip.title
            descriptionView.text = trip.description
            uploadView.visibility = uploadViewVisible
            uploadView.setOnClickListener {
                val inputStream = it.context.contentResolver.openInputStream(trip.uri)
                if (inputStream == null) {
                    Timber.i("Failed to create inputStream for %s", trip.title)
                    return@setOnClickListener
                }
                val data = ByteStreams.toByteArray(inputStream)
                ElectroClub.instance.uploadTrack(data, trip.title, false)
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
package com.cooper.wheellog.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.R
import java.io.File

class TripAdapter(var context: Context, private var trips: List<Trip>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.list_trip_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.apply {
            nameView.text = trip.title
            descriptionView.text = trip.description
            uploadView.setOnClickListener { }
            shareView.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    //context.contentResolver.
                    var uri = Uri.parse("content://path/to/email/attachment")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "text/csv"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(context, shareIntent, Bundle.EMPTY)
            }
//            if(position % 2 == 0)
//            {
//                nameView.rootView.setBackgroundResource(R.color.background);
//            }
        }
    }

    override fun getItemCount(): Int {
        return trips.size
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        var nameView: TextView = view.findViewById(R.id.name)
        var descriptionView: TextView = view.findViewById(R.id.description)
        var uploadView: ImageView = view.findViewById(R.id.uploadButton)
        var shareView: ImageView = view.findViewById(R.id.shareButton)
    }
}
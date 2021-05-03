package com.cooper.wheellog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.LinkedHashMap

class MainRecyclerAdapter(private var pages: MutableList<Int>, var wd: WearData): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var pagesView = LinkedHashMap<Int, View?>()
    private var mMainTextView: TextView? = null
    private var mMainTextUnitView: TextView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(pages[viewType], parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        pagesView[pages[position]] = view
        when (pages[position]) {
            R.layout.recycler_row_main -> {
                mMainTextView = view.findViewById(R.id.text)
                mMainTextUnitView = view.findViewById(R.id.mainunit)
            }
        }
    }

    override fun getItemCount(): Int {
        return pages.count()
    }

    fun updateScreen() {
        // Main screen
        val format = if (wd.speed < 10) "%.1f" else "%.0f"
        mMainTextView?.text = String.format(Locale.US, format, wd.speed)
        mMainTextUnitView?.text = wd.mainUnit
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)
}

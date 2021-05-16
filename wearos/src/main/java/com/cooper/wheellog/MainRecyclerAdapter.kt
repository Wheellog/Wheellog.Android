package com.cooper.wheellog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.views.MainView
import java.util.*
import kotlin.collections.LinkedHashMap

class MainRecyclerAdapter(private var pages: MutableList<Int>, var wd: WearData): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var pagesView = LinkedHashMap<Int, View?>()
    private lateinit var mainView: MainView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            // create mainPage
            mainView = MainView(parent.context, null, wd)
            ViewHolder(mainView)
        } else {
            // other pages
            val inflater = LayoutInflater.from(parent.context)
            ViewHolder(inflater.inflate(pages[viewType], parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        pagesView[pages[position]] = view
        // TODO inmplement pages
        when (pages[position]) {
            R.layout.recycler_row_main -> {
            }
        }
    }

    override fun getItemCount(): Int {
        return pages.count()
    }

    fun updateScreen() {
        // Main screen
        mainView.invalidate()
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)
}

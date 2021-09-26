package com.cooper.wheellog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.databinding.MainListViewBinding
import com.cooper.wheellog.views.MainView
import com.wheellog.shared.WearPage
import com.wheellog.shared.WearPages
import kotlin.collections.LinkedHashMap

class MainRecyclerAdapter(var pages: WearPages, private var wd: WearData): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var pagesView = LinkedHashMap<WearPage, RecyclerView.ViewHolder?>()
    private lateinit var mainView: MainView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            // create mainPage
            mainView = MainView(parent.context, null, wd)
            ViewHolder(mainView)
        } else {
            // other pages
            val inflater = LayoutInflater.from(parent.context)
            val itemBinding = MainListViewBinding.inflate(inflater, parent, false)
            ItemViewHolder(itemBinding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        pagesView[pages.elementAt(position)] = holder
        when (holder) {
            is ViewHolder -> {
                // main view
                holder.setIsRecyclable(false)
            }
            is ItemViewHolder -> {
                // other pages
                holder.bind(pages.elementAt(position))
            }
        }
    }

    override fun getItemCount(): Int {
        return pages.count()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun updateScreen() {
        pagesView.forEach {
            if (it.key == WearPage.Main) {
                // Main screen
                mainView.invalidate()
            } else {
                // other pages
                (it.value as? ItemViewHolder)?.update(wd)
            }
        }
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)

    class ItemViewHolder(private val itemBinding: MainListViewBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        private lateinit var page: WearPage
        private lateinit var model: PageViewModel

        fun bind(page: WearPage) {
            this.page = page
            model = PageViewModel(page.name) // TODO localization
            itemBinding.model = model
        }

        fun update(wd: WearData) {
            model.apply {
                when (page) {
                    WearPage.Current -> {
                        value.set("${wd.current.value}")
                        min.set("${wd.current.min}")
                        max.set("${wd.current.max}")
                    }
                    WearPage.Voltage -> {
                        value.set("${wd.voltage.value}")
                        min.set("${wd.voltage.min}")
                        max.set("${wd.voltage.max}")
                    }
                    WearPage.Power -> {
                        value.set("${wd.power.value}")
                        min.set("${wd.power.min}")
                        max.set("${wd.power.max}")
                    }
                    WearPage.PWM -> {
                        value.set("${wd.pwm.value}")
                        minTitle.set("")
                        min.set("")
                        max.set("${wd.pwm.max}")
                    }
                    WearPage.Temperature -> {
                        value.set("${wd.temperature.value}℃")
                        min.set("${wd.temperature.min}℃")
                        max.set("${wd.temperature.max}℃")
                    }
                    WearPage.Distance -> {
                        value.set("${wd.distance}")
                        minTitle.set("")
                        min.set("")
                        maxTitle.set("")
                        max.set("")
                    }
                    else -> {
                    }
                }
            }
        }
    }
}

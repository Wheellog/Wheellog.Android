package com.cooper.wheellog

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.viewpagerindicator.PageIndicator
import java.util.*

internal class ViewPageAdapter(var mActivity: Activity) : PagerAdapter() {
    private val pages: ArrayList<Int> = ArrayList()
    private var pageIndicator: PageIndicator? = null

    fun setPageIndicator(value: PageIndicator?) {
        pageIndicator = value
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        return mActivity.findViewById(pages[position])
    }

    fun hidePage(pageId: Int) {
        if (pages.contains(pageId)) {
            pages.remove(pageId)
            notifyDataSetChanged()
        }
    }

    fun showPage(pageId: Int) {
        showPage(pageId, -1)
    }

    fun showPage(pageId: Int, index: Int) {
        if (!pages.contains(pageId)) {
            if (index == -1) {
                pages.add(pageId)
            } else {
                pages.add(index, pageId)
            }
            notifyDataSetChanged()
        }
    }

    fun isShowed(pageId: Int): Boolean {
        return pages.contains(pageId)
    }

    fun getPageIdByPosition(position: Int): Int {
        return pages[position]
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        pageIndicator?.notifyDataSetChanged()
        if (pageIndicator is View) {
            (pageIndicator as View).invalidate()
        }
    }

    override fun getCount(): Int {
        return pages.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}

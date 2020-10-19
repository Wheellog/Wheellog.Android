package com.cooper.wheellog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

class ViewPageAdapter extends PagerAdapter {

    Activity mActivity;

    private ArrayList<Integer> pages;

    public ViewPageAdapter(Activity activity) {
        mActivity = activity;
        pages = new ArrayList<>();
        reset(true);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        return mActivity.findViewById(pages.get(position));
    }

    public void reset(boolean init) {
        int oldSize = pages.size();
        pages.clear();
        pages.add(R.id.page_one);
        pages.add(R.id.page_two);
        pages.add(R.id.page_three);
        if (!init && oldSize != pages.size()) {
            notifyDataSetChanged();
        }
    }

    public void deletePage(int pageId) {
        if (pages.contains(pageId)) {
            pages.remove((Object) pageId);
            notifyDataSetChanged();
        }
    }

    public void addPage(int pageId) {
        if (!pages.contains(pageId)) {
            pages.add(pageId);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
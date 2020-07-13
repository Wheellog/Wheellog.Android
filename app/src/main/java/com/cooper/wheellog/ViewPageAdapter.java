package com.cooper.wheellog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.cooper.wheellog.utils.Constants;


class ViewPageAdapter extends PagerAdapter {

    Activity mActivity;

    public ViewPageAdapter(Activity activity){
        mActivity = activity;
    }

    public Object instantiateItem(ViewGroup collection, int position) {

        int resId = 0;
        switch (position) {
            case 0:
                resId = R.id.page_one;
                break;
            case 1:
                resId = R.id.page_two;
                break;
            case 2:
                resId = R.id.page_three;
                break;
            case 3:
                resId = R.id.page_four;

                break;
        }
        return mActivity.findViewById(resId);
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == (arg1);
    }
}
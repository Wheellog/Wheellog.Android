package com.cooper.wheellog.views;

import java.util.concurrent.Callable;

public class ViewBlockInfo implements Comparable {
    private String title;
    private boolean enabled;
    private Callable<String> value;
    private int index = -1;

    public ViewBlockInfo(String title, Callable<String> value, boolean enabled) {
        this.title = title;
        this.value = value;
        this.enabled = enabled;
    }

    public ViewBlockInfo(String title, Callable<String> value) {
        this.title = title;
        this.value = value;
        this.enabled = true;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() throws Exception {
        return value.call();
    }

    public void setValue(Callable<String> mValue) {
        this.value = mValue;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int value) {
        index = value;
    }

    @Override
    public int compareTo(Object o) {
        return Integer.compare(this.index, ((ViewBlockInfo)o).getIndex());
    }
}

package com.cooper.wheellog

import androidx.databinding.ObservableField

class PageViewModel(title: String) {
    var title = ObservableField<String>()
    var value = ObservableField<String>()
    var min = ObservableField<String>()
    var max = ObservableField<String>()
    var minTitle = ObservableField<String>()
    var maxTitle = ObservableField<String>()

    init {
        this.title.set(title)
        minTitle.set("MIN")
        maxTitle.set("MAX")
    }
}
package com.cooper.wheellog.views

import java.util.concurrent.Callable

class ViewBlockInfo : Comparable<ViewBlockInfo> {
    var title: String
    var enabled: Boolean
    private var value: Callable<String>
    var index = -1

    constructor(title: String, value: Callable<String>, enabled: Boolean) {
        this.title = title
        this.value = value
        this.enabled = enabled
    }

    constructor(title: String, value: Callable<String>) : this(title, value, true)

    @Throws(Exception::class)
    fun getValue(): String {
        return value.call()
    }

    fun setValue(mValue: Callable<String>) {
        value = mValue
    }

    override fun compareTo(other: ViewBlockInfo): Int {
        return index.compareTo(other.index)
    }
}
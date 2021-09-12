package com.cooper.wheellog.presentation.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.cooper.wheellog.R
import java.util.*

@SuppressLint("ResourceType")
class MultiSelectPreference(context: Context, attrs: AttributeSet?)
    : DialogPreference(context, attrs) {

    constructor(context: Context) : this(context, null)

    companion object {
        @JvmStatic
        val separator = ";"
    }

    var useSort = true
    var entries = arrayOf<CharSequence>()
    private var values: LinkedHashSet<String> = LinkedHashSet()

    init {
        if (attrs != null) {
            lateinit var a: TypedArray
            try {
                a = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectPreference)
                entries = a.getTextArray(R.styleable.MultiSelectPreference_entries)
            } finally {
                a.recycle()
            }
        }
    }

    private fun setValues(v: String) {
        setValues(v.split(separator))
    }

    fun setValues(v: List<String>) {
        values.clear()
        if (!(v.size == 1 && v[0] == "")) {
            values.addAll(v)
        }

        persistString(getValues())
        notifyChanged()
    }

    fun setValues(v: Array<String>) {
        this.setValues(v.toList())
    }

    private fun getValues(): String {
        return values.joinToString(separator)
    }

    fun getValueArray(): LinkedHashSet<String> {
        return values
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        val defaultValues = a.getTextArray(index)
        val result = LinkedHashSet<String>()
        for (defaultValue in defaultValues) {
            result.add(defaultValue.toString())
        }
        return result
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        if (defaultValue is String?) {
            setValues(getPersistedString(defaultValue))
        }
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        if (newValue is String) {
            return super.callChangeListener(newValue)
        } else if (newValue is Set<*>) {
            return super.callChangeListener(newValue.joinToString(separator))
        }
        return false
    }
}
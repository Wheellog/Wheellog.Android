package com.cooper.wheellog.presentation.preferences

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.preference.PreferenceDialogFragmentCompat
import com.cooper.wheellog.R
import java.util.*
import kotlin.math.roundToInt

class MultiSelectPreferenceDialogFragment: PreferenceDialogFragmentCompat(), CompoundButton.OnCheckedChangeListener {

    private val _saveStateValues = "MultiSelectPreferenceDialogFragment.values"
    private val _saveStateChanged = "MultiSelectPreferenceDialogFragment.changed"
    private val _saveStateEntries = "MultiSelectPreferenceDialogFragment.entries"
    private var newValues = LinkedHashSet<String>()
    private lateinit var layoutWithCheckboxes: LinearLayout
    private var preferenceChanged = false
    private var entries: Array<CharSequence>? = null
    private var useSort = true

    companion object {
        @JvmStatic
        fun newInstance(key: String): MultiSelectPreferenceDialogFragment {
            val fragment = MultiSelectPreferenceDialogFragment()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun getListPreference(): MultiSelectPreference? {
        return preference as MultiSelectPreference?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newValues.clear()
        if (savedInstanceState == null) {
            val preference = getListPreference() ?: return
            newValues = preference.getValueArray()
            preferenceChanged = false
            entries = preference.entries
        } else {
            savedInstanceState.getStringArray(_saveStateValues)?.let {
                newValues.addAll(it)
            }
            preferenceChanged = savedInstanceState.getBoolean(_saveStateChanged, false)
            entries = savedInstanceState.getCharSequenceArray(_saveStateEntries)
        }
    }

    override fun onCreateDialogView(context: Context): View {
        val paddingDP = (20 * context.resources.displayMetrics.density).roundToInt()
        layoutWithCheckboxes = LinearLayout(context).also {
            it.orientation = LinearLayout.VERTICAL
            it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setPadding(paddingDP, paddingDP, paddingDP, paddingDP)
        }
        val scrollView = ScrollView(context).also {
            it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            it.addView(layoutWithCheckboxes)
        }
        val p = getListPreference()
        if (p == null) {
            dismiss()
            return scrollView
        }
        useSort = p.useSort
        p.getValueArray().forEach { e ->
            addCheckbox(context, e, true)
        }
        p.entries?.forEach { e ->
            if (!p.getValueArray().contains(e)) {
                addCheckbox(context, e)
            }
        }
        return scrollView
    }

    private fun addCheckbox(context: Context, text: CharSequence, isChecked: Boolean = false) {
        val checkBox = CheckBox(context)
        checkBox.text = text
        checkBox.isChecked = isChecked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkBox.setTextAppearance(R.style.Stats)
        }
        val paddingDP = (5 * context.resources.displayMetrics.density).roundToInt()
        checkBox.setPadding(0, paddingDP, 0, paddingDP)
        checkBox.setOnCheckedChangeListener(this)
        layoutWithCheckboxes.addView(checkBox)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(_saveStateValues, newValues.toTypedArray())
        outState.putBoolean(_saveStateChanged, preferenceChanged)
        outState.putCharSequenceArray(_saveStateEntries, entries)
    }

    private fun changeViewOrder(view: View, newOrder: Int) {
        layoutWithCheckboxes.removeView(view)
        layoutWithCheckboxes.addView(view, newOrder)
        view.startAnimation(AnimationUtils.makeInAnimation(context, false))
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val currentIndex = layoutWithCheckboxes.indexOfChild(buttonView)
        if (isChecked) {
            newValues.add(buttonView.text.toString())
            if (useSort && currentIndex >= newValues.size) {
                changeViewOrder(buttonView, newValues.size - 1)
            }
        } else {
            newValues.remove(buttonView.text)
            if (useSort && currentIndex < newValues.size) {
                changeViewOrder(buttonView, newValues.size)
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val preference = getListPreference()
            if (preference != null && preference.callChangeListener(newValues)) {
                preference.setValues(newValues.toList())
            }
        }
        preferenceChanged = false
    }
}
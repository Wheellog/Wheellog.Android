package com.cooper.wheellog.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MapViewModel : ViewModel() {

    private val mutableSelectedItem = MutableLiveData<TripData>()

    val selectedItem: LiveData<TripData>
        get() = mutableSelectedItem

    fun selectItem(item: TripData) {
        mutableSelectedItem.value = item
    }
}
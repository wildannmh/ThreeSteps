package com.example.projekpemmob.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class LeftAction { NONE, BACK, FAVORITES }

data class HeaderConfig(
    val title: String,
    val leftAction: LeftAction = LeftAction.BACK,
    val showCart: Boolean = true
)

class HeaderViewModel : ViewModel() {
    private val _config = MutableLiveData(HeaderConfig(title = ""))
    val config: LiveData<HeaderConfig> = _config

    fun set(config: HeaderConfig) {
        _config.value = config
    }
}
package com.yk.automotive.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yk.automotive.data.CarManager
import com.yk.automotive.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(private val carManager: CarManager) : BaseViewModel() {

    companion object {
        const val MIN_SPEED = 0
        const val MAX_SPEED = 140
        val PLUS_RANGE_SPEED = 1..10
        const val MOCK_DATA_CHANGE_DURATION = 2000L
    }

    private val _speedCar = MutableLiveData(MIN_SPEED)
    val speedCar: LiveData<Int>
        get() = _speedCar

    var isRealData = true

    private var job: Job? = null

    init {
        carManager.init()
    }

    fun loadSpeedProperties() {
        compositeDisposable.addAll(
            carManager.speedCarObservable
                .subscribe {
                    if (isRealData) {
                        _speedCar.postValue(it)
                    }
                }
        )
    }

    fun startMockSpeed() {
        stopMockSpeed()
        job = viewModelScope.launch {
            while (true) {
                val randomSpeed = (PLUS_RANGE_SPEED).random()
                if (speedCar.value!! >= MAX_SPEED - randomSpeed) {
                    _speedCar.value = MIN_SPEED
                } else {
                    _speedCar.value = _speedCar.value!!.plus(randomSpeed)
                }
                delay(MOCK_DATA_CHANGE_DURATION)
            }
        }
    }

    fun stopMockSpeed() {
        job?.cancel()
        job = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMockSpeed()
        carManager.cleanCallback()
        compositeDisposable.dispose()
    }

}

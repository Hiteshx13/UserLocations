package com.printful.userlocations.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.printful.userlocations.data.repository.UserTrackerRepository
import kotlinx.coroutines.launch

class UserTrackerViewModel(application: Application) :
    AndroidViewModel(application) {
    private var mutableResponse = MutableLiveData<String>()
    private var userTrackerRepository = UserTrackerRepository()

    fun getLocationData(): MutableLiveData<String> {
        return mutableResponse
    }

    fun startServer() {
        viewModelScope.launch { userTrackerRepository.startServer(mutableResponse) }
    }

    fun stopServer() {
        userTrackerRepository.stopServer()
    }

    fun sendMessage(email: String) {
            userTrackerRepository.sendMessage(email)
    }
}
package com.printful.userlocations.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.printful.userlocations.data.repository.UserTrackerRepository

class UserTrackerViewModel(application: Application) :
    AndroidViewModel(application) {
    private var mutableResponse = MutableLiveData<String>()
    private var userTrackerRepository = UserTrackerRepository()

    fun getLocationData(): MutableLiveData<String> {
        return mutableResponse
    }

    suspend fun startServer() {
        userTrackerRepository.startServer(mutableResponse)
    }

    fun stopServer() {
        userTrackerRepository.stopServer()
    }

    fun sendMessage(email: String) {
        userTrackerRepository.sendMessage(email)

    }
}
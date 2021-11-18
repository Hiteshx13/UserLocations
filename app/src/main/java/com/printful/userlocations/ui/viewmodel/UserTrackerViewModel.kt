package com.printful.userlocations.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.printful.userlocations.data.repository.UserTrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class UserTrackerViewModel(application: Application) :
    AndroidViewModel(application) {
    private var mutableResponse = MutableLiveData<String>()
    private var userTrackerRepository = UserTrackerRepository()

    fun getLocationData(): MutableLiveData<String> {
        return mutableResponse
    }

    fun startServer() {
        CoroutineScope(IO).launch {
            userTrackerRepository.startServer(mutableResponse)
        }
    }

    fun stopServer() {
        userTrackerRepository.stopServer()
    }

    fun sendMessage(email: String) {
            userTrackerRepository.sendMessage(email)
    }
}
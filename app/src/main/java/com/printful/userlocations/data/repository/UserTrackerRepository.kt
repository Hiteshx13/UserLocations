package com.printful.userlocations.data.repository

import androidx.lifecycle.MutableLiveData
import com.printful.userlocations.data.`interface`.OnServerMessageReceived
import com.printful.userlocations.data.network.TcpClient
import com.printful.userlocations.utils.AUTHORIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTrackerRepository {

    var mTcpClient: TcpClient? = null
    suspend fun startServer(  mutableResponse: MutableLiveData<String>) {
        mTcpClient =
            TcpClient(object :
                OnServerMessageReceived {
                override fun messageReceived(message: String?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        mutableResponse.value = message
                    }
                }
            })
        mTcpClient!!.run()
    }

    fun stopServer() {
        mTcpClient?.stopClient()
    }
    fun sendMessage(email:String) {
        mTcpClient?.sendMessage("$AUTHORIZE $email")
    }
}
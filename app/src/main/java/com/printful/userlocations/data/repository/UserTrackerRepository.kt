package com.printful.userlocations.data.repository

import androidx.lifecycle.MutableLiveData
import com.printful.userlocations.data.`interface`.OnServerMessageReceived
import com.printful.userlocations.data.network.TcpClient
import com.printful.userlocations.utils.AUTHORIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class UserTrackerRepository {

    private var mTcpClient: TcpClient? = null

    /**
     * Initialization of TCP client.
     */
     fun startServer(mutableResponse: MutableLiveData<String>) {
        mTcpClient =

            TcpClient(object :
                OnServerMessageReceived {
                override fun messageReceived(message: String?) {
                    CoroutineScope(Main).launch {
                        /** sending server data to ViewModel*/
                        mutableResponse.value = message
                    }
                }
            })
        CoroutineScope(IO).launch {
            mTcpClient!!.run()
        }

    }

    fun stopServer() {
        mTcpClient?.stopClient()
    }

    fun sendMessage(email: String) {
        CoroutineScope(IO).launch {
            /** send authorization message to TCP server with email**/
            mTcpClient?.sendMessage("$AUTHORIZE $email")
        }
    }
}
package com.printful.userlocations.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.printful.userlocations.data.network.TcpClient
import com.printful.userlocations.ui.activity.UserTrackerActivity
import com.printful.userlocations.utils.AUTHORIZE
import com.printful.userlocations.ui.activity.mTcpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserTrackerViewModel(application: Application) :
    AndroidViewModel(application) {
    private var mutableResponse = MutableLiveData<String>()

    fun getLocationData(): MutableLiveData<String> {
        return mutableResponse
    }

    suspend fun  startServer( ) {
            mTcpClient =
                TcpClient(object :
                    TcpClient.OnMessageReceived {
                    //here the messageReceived method is implemented
                    override fun messageReceived(message: String?) {
                        //this method calls the onProgressUpdate
                        Log.d("TCP", "$message")
                        //publishProgress(message)
                        CoroutineScope(Main).launch{
                            mutableResponse.value = message
                        }
                    }
                })
            mTcpClient!!.run()
            Log.d("TCP"," RUN")



    }



    fun sendMessage(){
        mTcpClient?.sendMessage("$AUTHORIZE IMHR1727@gmail.com")

        Log.d("TCP"," sendMessage")
    }


    class AsyncConnectServer(val activity: UserTrackerActivity) : AsyncTask<String, String, TcpClient>() {
        override fun doInBackground(vararg message: String?): TcpClient? {

            //we create a TCPClient object
            mTcpClient =
                TcpClient(object :
                    TcpClient.OnMessageReceived {
                    //here the messageReceived method is implemented
                    override fun messageReceived(message: String?) {
                        //this method calls the onProgressUpdate
                        publishProgress(message)
                    }
                })
            mTcpClient!!.run()

            return null
        }

        override fun onProgressUpdate(vararg values: String) {
            super.onProgressUpdate(*values)
            activity.updateUI(values[0])
        }
    }
}
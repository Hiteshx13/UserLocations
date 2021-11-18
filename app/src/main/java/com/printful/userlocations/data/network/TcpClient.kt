package com.printful.userlocations.data.network

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.printful.userlocations.utils.SERVER_IP
import com.printful.userlocations.utils.SERVER_PORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetAddress
import java.net.Socket



class TcpClient(var listener: OnMessageReceived?) {
    private var mServerMessage: String? = null
    private var mRun = false
    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null
    var mutableResponse = MutableLiveData<String>()

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String) {
        val runnable = Runnable {
            if (mBufferOut != null) {
                Log.d(TAG, "Sending: $message")
                mBufferOut!!.println(message)
                mBufferOut!!.flush()
            }
        }
        val thread = Thread(runnable)
        thread.start()
    }

    /**
     * Close the connection and release the members
     */
    fun stopClient() {
        mRun = false
        if (mBufferOut != null) {
            mBufferOut!!.flush()
            mBufferOut!!.close()
        }
        listener = null
        mBufferIn = null
        mBufferOut = null
        mServerMessage = null
    }

    fun run() {
        mRun = true
        try {
            val serverAddr =
                InetAddress.getByName(SERVER_IP)
            Log.d("TCP Client", "C: Connecting...")

            val socket =
                Socket(serverAddr, SERVER_PORT)
            try {
                mBufferOut = PrintWriter(
                    BufferedWriter(OutputStreamWriter(socket.getOutputStream())),
                    true
                )
                mBufferIn =
                    BufferedReader(InputStreamReader(socket.getInputStream()))
                while (mRun) {
                    mServerMessage = mBufferIn!!.readLine()
                    if (mServerMessage != null && listener != null) {
                        //call the method messageReceived from MyActivity class
                        CoroutineScope(Main).launch{
                            listener!!.messageReceived(mServerMessage)
                        }

                    }
                }
                Log.d(
                    "RESPONSE FROM SERVER",
                    "S: Received Message: '$mServerMessage'"
                )
            } catch (e: Exception) {
                Log.e("TCP", "S: Error", e)
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            Log.e("TCP", "C: Error", e)
        }
    }

    fun updateResults(result:String?){
        listener!!.messageReceived(result)
    }

    interface OnMessageReceived {
        fun messageReceived(message: String?)
    }

    companion object {
        val TAG = TcpClient::class.java.simpleName

    }
}
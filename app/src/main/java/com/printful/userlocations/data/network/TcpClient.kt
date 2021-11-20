package com.printful.userlocations.data.network

import android.util.Log
import com.printful.userlocations.data.`interface`.OnServerMessageReceived
import com.printful.userlocations.utils.SERVER_IP
import com.printful.userlocations.utils.SERVER_PORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetAddress
import java.net.Socket

class TcpClient(var listener: OnServerMessageReceived?) {

    companion object {
        val TAG = TcpClient::class.java.simpleName
    }

    private var mServerMessage: String? = null
    private var mRun = false
    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String) {
        val runnable = Runnable {
            if (mBufferOut != null) {
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

    /**
     * starting TCP server
     */
    fun run() {
        mRun = true
        try {
            val serverAddr =
                InetAddress.getByName(SERVER_IP)

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
                        Log.d(TAG,"$mServerMessage")
                        CoroutineScope(Main).launch {
                            /** sending data back to repository*/
                            listener!!.messageReceived(mServerMessage)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "S: Error", e)
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "C: Error", e)
        }
    }

}
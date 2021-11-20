package com.printful.userlocations.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.printful.userlocations.data.`interface`.LatLngInterpolator
import java.util.*


var UPDATE = "update"
var USERLIST = "userlist"
var AUTHORIZE = "AUTHORIZE"
 const val SERVER_PORT = 6111
 const val SERVER_IP: String = "ios-test.printful.lv"

/**show toast message**/
fun showTast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

/** get address by provising LatLan**/
fun getAddressFromLatLan(
    context: Context,
    latitude: String,
    longitude: String
): String {
    val addresses: List<Address>
    val geocoder = Geocoder(context, Locale.getDefault())
    addresses = geocoder.getFromLocation(
        latitude.toDouble(),
        longitude.toDouble(),
        1
    )

    return addresses[0]
        .getAddressLine(0)
}

/** Animate marker between two locations**/
fun animateMarker(
    marker: Marker,
    finalPosition: LatLng,
    latLngInterpolator: LatLngInterpolator
) {
    val startPosition = marker.position
    val handler = Handler()
    val start = SystemClock.uptimeMillis()
    val interpolator: Interpolator = AccelerateDecelerateInterpolator()
    val durationInMs = 2000f
    handler.post(object : Runnable {
        var elapsed: Long = 0
        var t = 0f
        var v = 0f
        override fun run() {
            // Calculate progress using interpolator
            elapsed = SystemClock.uptimeMillis() - start
            t = elapsed / durationInMs
            v = interpolator.getInterpolation(t)
            marker.position = latLngInterpolator.interpolate(v, startPosition, finalPosition)
            // Repeat till progress is complete.
            if (t < 1) {
                // Post again 16ms later.
                handler.postDelayed(this, 16)
            }
        }
    })
}

/** check internet connectivity**/
fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
    val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

    return isConnected
}
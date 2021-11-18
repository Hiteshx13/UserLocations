package com.printful.userlocations.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.printful.locations.`interface`.LatLngInterpolator
import com.printful.userlocations.R
import com.printful.userlocations.data.model.UserModel
import com.printful.userlocations.data.network.TcpClient
import com.printful.userlocations.databinding.ActivityMapBinding
import com.printful.userlocations.databinding.RowInfoWindowBinding
import com.printful.userlocations.ui.viewmodel.UserTrackerViewModel
import com.printful.userlocations.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

var mTcpClient: TcpClient? = null

class UserTrackerActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        fun getIntent(context: Context, email: String): Intent {
            var intent = Intent(context, UserTrackerActivity::class.java)
            intent.putExtra(PARAM_EMAIL, email)
            return intent
        }
    }

    private var googleMap: GoogleMap? = null
    private var email = ""
    private lateinit var binding: ActivityMapBinding
    var mapMarkers = HashMap<String?, Marker?>()
    var isServerConnected = false
    private var listLocation: List<String>? = null
    private lateinit var userList: HashMap<String, UserModel>
    var isImageLoading = false
    var imageLoadingID = ""
    private lateinit var viewModel: UserTrackerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupViewModel()
        setupObserver()
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(IO).launch { initServer() }
        setupMap()
    }
    suspend fun initServer() {
        viewModel.startServer()
    }

    private fun setupUI() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        email = intent.getStringExtra(PARAM_EMAIL) ?: ""
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[UserTrackerViewModel::class.java]
    }

    private fun setupObserver() {
        viewModel.getLocationData().observe(this, Observer {
            updateUI(it)
        })
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this@UserTrackerActivity)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        isServerConnected = false
        mTcpClient?.stopClient()
    }


    fun updateUI(sResponse: String) {

        Log.d("TCP", "response: $sResponse")
        binding.progressbar.visibility = View.GONE

        if (sResponse.startsWith(USERLIST, true)) {
            googleMap?.clear()
            val response = sResponse.replace(USERLIST, "", true)
            var listUsers: List<String>? = null
            listLocation = response.split(";")
            userList = HashMap()
            for (i in listLocation!!.indices) {
                if (listLocation!![i].trim().isNotEmpty()) {

                    listUsers = listLocation!![i].split(",")
                    val model = UserModel(
                        listUsers[0], //Id
                        listUsers[1], //name
                        listUsers[2], //profile
                        listUsers[3], //lat
                        listUsers[4] //lan
                        , null, false
                    )
                    userList.put(listUsers[0].trim(), model)

                    addMarker(
                        model, false
                    )
                }
            }

            /** zoom on first marker**/
            if (!listUsers.isNullOrEmpty()) {
                val zoomLatLan = LatLng(
                    listUsers[3].toDouble(),
                    listUsers[4].toDouble()
                )
                val cameraPosition =
                    CameraPosition.Builder().target(zoomLatLan).zoom(16.0f).build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                googleMap?.moveCamera(cameraUpdate)
            }

        } else if (sResponse.startsWith(UPDATE, true)) {
            updateMarkerLocation(sResponse)
        }
    }

    /** Add markers on map**/
    fun addMarker(model: UserModel, isUpdateing: Boolean) {
        val newLat = model.lat
        val newLan = model.lan
        var marker: Marker? = null
        val view: RowInfoWindowBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.row_info_window,
            null,
            false
        ) //this.layoutInflater.inflate(R.layout.row_info_window, null, false)
        if (isUpdateing) {
            marker = mapMarkers.get(model.id.trim())
            val uDataOld: UserModel? = userList[model.id.trim()]
            model.profile = uDataOld?.profile ?: ""
            model.name = uDataOld?.name ?: ""
            model.isImageLoaded = uDataOld?.isImageLoaded ?: false
            if (uDataOld?.image != null) {
                view.ivProfile.setImageDrawable(uDataOld.image)
            }
        }


        view.tvName.text = model.name
        view.tvAddress.text = getAddressFromLatLan(this, model.lat, model.lan).replace(",", "\n")


        view.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.root.layout(0, 0, view.root.measuredWidth, view.root.measuredHeight)

        val markerOpt = MarkerOptions().position(
            LatLng(
                model.lat.toDouble(),
                model.lan.toDouble()
            )
        ).snippet(model.profile)
            .icon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view.root)))


        if (!isImageLoading && !model.isImageLoaded && imageLoadingID != model.id) {
            isImageLoading = true
            imageLoadingID = model.id
            Glide
                .with(this)
                .load(model.profile)
                .placeholder(R.drawable.ic_user)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        data: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        view.ivProfile.setImageDrawable(resource)
                        marker?.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view.root)))

                        /**Updating marker with user image **/

                        val tempUserModel: UserModel? = userList[model.id.trim()] as UserModel
                        tempUserModel?.image = resource
                        tempUserModel?.isImageLoaded = true
                        userList.remove(model.id.trim())
                        userList.put(model.id.trim(), tempUserModel!!)

//                        userList.get(model.id.trim())?.isImageLoaded = true
                        isImageLoading = false
                        imageLoadingID = ""


                        if (!isUpdateing) {
                            marker?.setIcon(
                                BitmapDescriptorFactory.fromBitmap(
                                    loadBitmapFromView(
                                        view.root
                                    )
                                )
                            )
                        } else {
                            Log.d("update_2_", "" + newLat.toDouble() + "__" + newLan.toDouble())
                        }

                        return true
                    }
                })
                .into(view.ivProfile)


        }


        if (!isUpdateing) {

            marker = googleMap?.addMarker(markerOpt)
            marker?.tag = model
            mapMarkers.put(model.id.trim(), marker)

            Log.d("#Adding Marker", "" + marker?.id?.trim())
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        model.lat.toDouble(),
                        model.lan.toDouble()
                    )
                )
            )


        } else {
            marker?.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view.root)))
            animateMarker(
                marker!!, LatLng(
                    model.lat.toDouble(),
                    model.lan.toDouble()
                ), LatLngInterpolator.Spherical()
            )
        }

    }

    fun loadBitmapFromView(v: View): Bitmap {
        val b = Bitmap.createBitmap(
            v.width,
            v.height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }


    /** update marker locations by tcp response**/
    fun updateMarkerLocation(sResponse: String) {
        Log.d("Updating..", "" + sResponse)
        val response = sResponse.replace(UPDATE, "", true)
        var listUserData: List<String>? = null
        val listLocation: List<String> = response.split(";")
        for (i in listLocation.indices) {
            if (listLocation[i].trim().isNotEmpty()) {

                listUserData = listLocation[i].split(",")
                Log.d("update_", "" + listUserData[1] + "__" + listUserData[2])
                addMarker(
                    UserModel(
                        listUserData[0], //Id
                        "", //name
                        "", //profile
                        listUserData[1], //lat
                        listUserData[2] //lan
                        , null, false
                    ), true
                )
            }
        }
    }


    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        viewModel.sendMessage()
//        mTcpClient?.sendMessage("$AUTHORIZE $email")
    }
}
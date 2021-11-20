package com.printful.userlocations.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.printful.userlocations.R
import com.printful.userlocations.data.`interface`.LatLngInterpolator
import com.printful.userlocations.data.model.UserModel
import com.printful.userlocations.data.viewmodel.UserTrackerViewModel
import com.printful.userlocations.databinding.FragmentEmployeeLocationBinding
import com.printful.userlocations.ui.adapter.CustomInfoWindow
import com.printful.userlocations.utils.AUTHORIZE
import com.printful.userlocations.utils.UPDATE
import com.printful.userlocations.utils.USERLIST
import com.printful.userlocations.utils.animateMarker
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

class EmployeeLocationFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var viewModel: UserTrackerViewModel
    private var googleMap: GoogleMap? = null
    private lateinit var binding: FragmentEmployeeLocationBinding
    private var mapMarkers = HashMap<String?, Marker?>()
    private var listLocation: List<String>? = null
    private lateinit var userList: HashMap<String, UserModel>
    private var markerClickID = ""
    private var USER_EMAIL = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_employee_location, container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            USER_EMAIL = EmployeeLocationFragmentArgs.fromBundle(it).userEmail
        }

        setupViewModel()
        setupObserver()
    }

    override fun onResume() {
        super.onResume()
        initServer()
        setupMap()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopServer()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[UserTrackerViewModel::class.java]
    }

    /** ViewModel Observer**/
    private fun setupObserver() {
        viewModel.getLocationData().observe(viewLifecycleOwner, {
            updateUI(it)
        })
    }

    /** Starting TCP server**/
    private fun initServer() {
        viewModel.startServer()
    }

    private fun setupMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    /** updating User Interface with server data **/
    private fun updateUI(sResponse: String) {
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
    private fun addMarker(model: UserModel, isUpdating: Boolean) {

        val marker: Marker?

        /** adding marker first time for user data**/
        if (!isUpdating) {
            googleMap?.setInfoWindowAdapter(CustomInfoWindow(layoutInflater, isUpdating))
            googleMap?.setOnMarkerClickListener(this)
            val markerOpt = MarkerOptions().position(
                LatLng(
                    model.lat.toDouble(),
                    model.lan.toDouble()
                )
            ).snippet(model.profile)
            marker = googleMap?.addMarker(markerOpt)
            marker?.tag = model
            mapMarkers[model.id.trim()] = marker

            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        model.lat.toDouble(),
                        model.lan.toDouble()
                    )
                )
            )

        } else {

            /** updating marker with new data **/
            marker = mapMarkers[model.id.trim()]
            val oldModel: UserModel = marker?.tag as UserModel
            oldModel.lat = model.lat
            oldModel.lan = model.lan
            marker.tag = oldModel

            /** updating current open infowindow data **/
            if (markerClickID.isNotEmpty()) {
                val markerClicked = mapMarkers[markerClickID]
                if (markerClicked?.isInfoWindowShown == true) {
                    markerClicked.showInfoWindow()
                }
            }
            animateMarker(
                marker, LatLng(
                    model.lat.toDouble(),
                    model.lan.toDouble()
                ), LatLngInterpolator.Spherical()
            )
        }
    }

    /** update marker locations by tcp response**/
    private fun updateMarkerLocation(sResponse: String) {
        val response = sResponse.replace(UPDATE, "", true)
        var listUserData: List<String>?
        val listLocation: List<String> = response.split(";")
        for (i in listLocation.indices) {
            if (listLocation[i].trim().isNotEmpty()) {

                listUserData = listLocation[i].split(",")
                addMarker(
                    UserModel(
                        listUserData[0], //Id
                        "", //name
                        "", //profile
                        listUserData[1], //lat
                        listUserData[2] //lan
                    ), true
                )
            }
        }
    }

    /** function for Map Marker click listener**/
    override fun onMarkerClick(marker: Marker): Boolean {
        val model: UserModel = marker.tag as UserModel
        markerClickID = model.id
        return false
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        Timer().schedule(1000) {

            /** send authorization message to server with email**/
            viewModel.sendMessage("$AUTHORIZE $USER_EMAIL")
        }
    }
}
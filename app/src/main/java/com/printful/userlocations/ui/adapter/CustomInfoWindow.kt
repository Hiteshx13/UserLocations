package com.printful.userlocations.ui.adapter

import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import com.printful.userlocations.R
import com.printful.userlocations.data.model.UserModel
import com.printful.userlocations.databinding.RowInfoWindowBinding
import com.printful.userlocations.utils.getAddressFromLatLan
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

internal class CustomInfoWindow(
    private var inflater: LayoutInflater,
    private var isUpdating: Boolean
) :
    InfoWindowAdapter {

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View {
        val binding = DataBindingUtil.inflate<RowInfoWindowBinding>(
            inflater,
            R.layout.row_info_window,
            null,
            false
        )

        val model: UserModel = marker.tag as UserModel
        binding.tvAddress.text = getAddressFromLatLan(binding.tvName.context, model.lat, model.lan)
            .replace(",", "\n")

        if (!isUpdating) {
            binding.tvName.text = model.name

            Picasso.get().load(model.profile)
                .placeholder(R.drawable.ic_user)
                .into(
                    binding.ivProfile,
                    object : MarkerCallback(marker) {

                    },
                )
        }
        return binding.root
    }

    internal open class MarkerCallback(private var marker: Marker?) :
        Callback {

        override fun onSuccess() {
            if (marker == null) {
                return
            }
            if (!marker!!.isInfoWindowShown) {
                return
            }

            marker!!.hideInfoWindow() /** Calling only showInfoWindow() throws an error */
            marker!!.showInfoWindow()
        }

        override fun onError(e: java.lang.Exception?) {}
    }
}
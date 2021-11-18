package com.printful.userlocations.data.model

import android.graphics.drawable.Drawable

data class UserModel(
    val id: String,
    var name: String,
    var profile: String,
    var lat: String,
    var lan: String,
    var image: Drawable?,
    var isImageLoaded: Boolean
)
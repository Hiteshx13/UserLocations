package com.printful.userlocations.data.model

import androidx.annotation.Keep

@Keep
data class UserModel(
    val id: String,
    var name: String,
    var profile: String,
    var lat: String,
    var lan: String,
)
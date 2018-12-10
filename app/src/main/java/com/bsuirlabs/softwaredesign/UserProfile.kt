package com.bsuirlabs.softwaredesign

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
        var firstName: String? = "",
        var lastName: String? = "",
        var phone: String? = "",
        var image: String? = "",
        var rssSource: String? = "")

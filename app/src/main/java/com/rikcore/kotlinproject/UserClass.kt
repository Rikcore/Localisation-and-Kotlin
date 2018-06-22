package com.rikcore.kotlinproject

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class UserClass {
    var deviceName: String? = null
    var batteryLvl: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var captureDate: String? = null
    var chargeStatus: String? = null
    var memoryAvailable: String? = null
    var networkStatus: String? = null
    var isVisible: Boolean = true
    var uptime: Int? = null

    constructor() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    constructor(deviceName: String, batteryLvl: String, latitude: Double, longitude: Double, captureDate: String, chargeStatus: String, memory : String, networkStatus : String, isVisible: Boolean, uptime: Int) {
        this.deviceName = deviceName
        this.batteryLvl = batteryLvl
        this.latitude = latitude
        this.longitude = longitude
        this.captureDate = captureDate
        this.chargeStatus = chargeStatus
        this.memoryAvailable = memory
        this.networkStatus = networkStatus
        this.isVisible = isVisible
        this.uptime = uptime
    }


}
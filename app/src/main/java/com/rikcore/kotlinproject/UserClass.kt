package com.rikcore.kotlinproject

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class UserClass {
    var deviceName: String? = null
    var deviceId: String? = null
    var pictureUrl: String? = null
    var batteryLvl: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var captureDate: String? = null
    var chargeStatus: String? = null
    var memoryAvailable: String? = null
    var networkStatus: String? = null
    var uptime: Int? = null
    var speed: Int? = null

    constructor() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    constructor(deviceId: String, deviceName: String, batteryLvl: String, latitude: Double, longitude: Double, captureDate: String, chargeStatus: String, memory : String, networkStatus : String, uptime: Int, speed : Int) {
        this.deviceId = deviceId
        this.deviceName = deviceName
        this.batteryLvl = batteryLvl
        this.latitude = latitude
        this.longitude = longitude
        this.captureDate = captureDate
        this.chargeStatus = chargeStatus
        this.memoryAvailable = memory
        this.networkStatus = networkStatus
        this.uptime = uptime
        this.speed = speed
    }
}
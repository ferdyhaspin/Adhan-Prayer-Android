package com.ferdyhaspin.adhan_prayer_android.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import com.ferdyhaspin.adhan_prayer_android.utils.AppSettings

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
class PassiveLocationChangedReceiver : BroadcastReceiver() {

    private val prayAlarmReceiver = PrayAlarmReceiver()

    override fun onReceive(context: Context, intent: Intent) {
        val key = LocationManager.KEY_LOCATION_CHANGED
        if (intent.hasExtra(key)) {
            // This update came from Passive provider, so we can extract the location
            // directly.
            val location: Location? = intent.extras!![key] as Location?
            if (location != null) {
                AppSettings.getInstance(context).latFor = location.latitude
                AppSettings.getInstance(context).lngFor = location.longitude
                if (AppSettings.getInstance(context).isAlarmSetFor(0)) {
                    prayAlarmReceiver.cancelAlarm(context)
                    prayAlarmReceiver.setAlarm(context)
                }
            }
        }
    }
}
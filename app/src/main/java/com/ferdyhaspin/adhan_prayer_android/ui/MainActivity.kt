package com.ferdyhaspin.adhan_prayer_android.ui

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.adapter.PrayerAdapter
import com.ferdyhaspin.adhan_prayer_android.model.Prayer
import com.ferdyhaspin.adhan_prayer_android.scheduler.PrayAlarmReceiver
import com.ferdyhaspin.adhan_prayer_android.utils.*
import com.ferdyhaspin.adhan_prayer_android.utils.Constants.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), Constants, LocationHelper.LocationCallback {

    private lateinit var mLocationHelper: LocationHelper
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.getInstance(this)
        //INIT APP
        if (!settings.getBoolean(AppSettings.Key.IS_INIT)) {
            for (key in KEYS) {
                settings.set(ALARM_FOR + key, 0)
            }
            settings.set(AppSettings.Key.IS_INIT, true)
        }

        setContentView(R.layout.activity_main)

        mLocationHelper = LocationHelper.newInstance()

        supportFragmentManager
            .beginTransaction()
            .add(mLocationHelper, LOCATION_FRAGMENT)
            .commit()


    }

    override fun onResume() {
        super.onResume()
        mLocationHelper.checkLocationPermissions()
    }

    private fun init() {
        val prayerTimes: LinkedHashMap<String, String> =
            PrayTime.getPrayerTimes(this, settings.latFor, settings.lngFor)

        val list = mutableListOf<Prayer>()
        for (i in 0 until prayerTimes.size) {
            val key = KEYS[i]
            val name = NAME_ID[i]
            val time = prayerTimes[key]
            val setting = settings.getInt(ALARM_FOR + key)
            list.add(Prayer(key, name, time, setting))
        }

        val mAdapter = PrayerAdapter(list)
        rv_prayers.adapter = mAdapter
        rv_prayers.layoutManager = LinearLayoutManager(this)
        rv_prayers.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        updateAlarmStatus()
    }

    private fun updateAlarmStatus() {
        if (PermissionUtil.hasSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            val prayerAlarmReceiver = PrayAlarmReceiver()
            prayerAlarmReceiver.cancelAlarm(this)
            prayerAlarmReceiver.setAlarm(this)
        }
    }

    override fun onLocationSettingsFailed() {
        Toast.makeText(this, "onLocationSettingsFailed", Toast.LENGTH_LONG).show()
    }

    override fun onLocationChanged(location: Location) {
        settings.latFor = location.latitude
        settings.lngFor = location.longitude
        init()
        setLocationName(location.latitude, location.longitude)
    }

    private fun setLocationName(lat: Double, long: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, long, 1)

        var city: String? = address[0].subAdminArea
        if (city == null)
            city = address[0].locality
        val street = address[0].subLocality

        val locationName = "$street - $city"
        tv_location.text = locationName
    }
}

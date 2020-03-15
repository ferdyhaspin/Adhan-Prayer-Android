package com.ferdyhaspin.adhan_prayer_android.ui

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.adapter.PrayerAdapter
import com.ferdyhaspin.adhan_prayer_android.model.Prayer
import com.ferdyhaspin.adhan_prayer_android.scheduler.PrayAlarmReceiver
import com.ferdyhaspin.adhan_prayer_android.utils.*
import com.ferdyhaspin.adhan_prayer_android.utils.Constants.KEYS
import com.ferdyhaspin.adhan_prayer_android.utils.Constants.LOCATION_FRAGMENT
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), Constants, LocationHelper.LocationCallback {

    private val sIsAlarmInit = false
    var mIndex = 0
    private lateinit var mLastLocation: Location

    private var mLocationHelper: LocationHelper? = null

    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.getInstance(this)
        //INIT APP
        //INIT APP
        if (!settings.getBoolean(AppSettings.Key.IS_INIT)) {
            for (key in KEYS) {
                settings.set(settings.getKeyFor(AppSettings.Key.IS_ALARM_SET, 0), true)
                settings.set(settings.getKeyFor(AppSettings.Key.IS_FAJR_ALARM_SET, 0), true)
                settings.set(settings.getKeyFor(AppSettings.Key.IS_DHUHR_ALARM_SET, 0), true)
                settings.set(settings.getKeyFor(AppSettings.Key.IS_ASR_ALARM_SET, 0), true)
                settings.set(settings.getKeyFor(AppSettings.Key.IS_MAGHRIB_ALARM_SET, 0), true)
                settings.set(settings.getKeyFor(AppSettings.Key.IS_ISHA_ALARM_SET, 0), true)
            }
            settings.set(AppSettings.Key.USE_ADHAN, true)
            settings.set(AppSettings.Key.IS_INIT, true)
        }

        setContentView(R.layout.activity_main)

        if (mLocationHelper == null) {
            mLocationHelper = LocationHelper.newInstance()
            supportFragmentManager
                .beginTransaction().add(mLocationHelper!!, LOCATION_FRAGMENT)
                .commit()
        }
    }

    private fun init() {
        val prayerTimes: LinkedHashMap<String, String> =
            PrayTime.getPrayerTimes(
                this,
                mIndex,
                mLastLocation.latitude,
                mLastLocation.longitude
            )

        val list = mutableListOf<Prayer>()
        for (i in 0 until prayerTimes.size) {
            val key = KEYS[i]
            val time = prayerTimes[key]
            list.add(Prayer(key, key, time, true))
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


    override fun onResume() {
        super.onResume()
        fetchLocation()
    }

    private fun fetchLocation() {
        if (mLocationHelper != null) {
            mLocationHelper!!.checkLocationPermissions()
        }
    }

    override fun onLocationSettingsFailed() {
    }


    override fun onDestroy() {
        super.onDestroy()
        updateAlarmStatus()
    }

    override fun onLocationChanged(location: Location) {
        mLastLocation = location
        settings.latFor = location.latitude
        settings.lngFor = location.longitude
        init()
    }
}

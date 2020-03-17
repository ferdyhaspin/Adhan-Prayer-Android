package com.ferdyhaspin.adhan_prayer_android.ui

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.format.DateFormat
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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Constants, LocationHelper.LocationCallback,
    PrayerAdapter.OnSettingChanged {

    private lateinit var mLocationHelper: LocationHelper
    private lateinit var settings: AppSettings
    private lateinit var mAdapter: PrayerAdapter

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
            if (key != SUNSET) {
                val name = NAME_ID[i]
                val time = prayerTimes[key]
                val setting = settings.getInt(ALARM_FOR + key)
                list.add(Prayer(key, name, time, setting))
            }
        }

        mAdapter = PrayerAdapter(list, this, getPrayerName(prayerTimes))
        rv_prayers.adapter = mAdapter
        rv_prayers.layoutManager = LinearLayoutManager(this)
        rv_prayers.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        updateAlarmStatus()
    }

    override fun onChanged(position: Int, setting: Int) {
        mAdapter.changeSetting(position, setting)
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

    private fun getPrayerName(prayerTimes: LinkedHashMap<String, String>): String {
        val prayerNames: List<String> = ArrayList(prayerTimes.keys)
        val now = Calendar.getInstance(TimeZone.getDefault())
        now.timeInMillis = System.currentTimeMillis()

        var then = Calendar.getInstance(TimeZone.getDefault())
        then.timeInMillis = System.currentTimeMillis()

        var nextAlarmFound = false
        var nameOfPrayerFound = ""

        for (prayer in prayerNames) {
            if (prayer != SUNRISE && prayer != SUNSET) {
                val time = prayerTimes[prayer]

                if (time != null) {
                    then = getCalendarFromPrayerTime(then, time)

                    if (then.after(now)) {
                        // this is the alarm to set
                        nameOfPrayerFound = prayer
                        nextAlarmFound = true
                        break
                    }
                }
            }

        }

        if (!nextAlarmFound) {
            for (prayer in prayerNames) {
                if (prayer != SUNRISE && prayer != SUNSET) {
                    val time = prayerTimes[prayer]

                    if (time != null) {
                        then = getCalendarFromPrayerTime(then, time)

                        if (then.before(now)) {
                            // this is the alarm to set
                            nameOfPrayerFound = prayer
                            then.add(Calendar.DAY_OF_YEAR, 1)
                            break
                        }
                    }
                }
            }
        }

        return nameOfPrayerFound
    }

    private fun getCalendarFromPrayerTime(cal: Calendar, prayerTime: String): Calendar {
        var strTime = prayerTime
        if (!DateFormat.is24HourFormat(this)) {
            val display = SimpleDateFormat("HH:mm", Locale.getDefault())
            val parse = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parse.parse(strTime)
            if (date != null) strTime = display.format(date)
        }
        val time = strTime.split(":").toTypedArray()
        cal[Calendar.HOUR_OF_DAY] = Integer.valueOf(time[0])
        cal[Calendar.MINUTE] = Integer.valueOf(time[1])
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }
}

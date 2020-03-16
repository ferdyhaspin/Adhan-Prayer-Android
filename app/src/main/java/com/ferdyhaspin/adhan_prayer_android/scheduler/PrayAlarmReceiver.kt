package com.ferdyhaspin.adhan_prayer_android.scheduler

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.ui.MainActivity
import com.ferdyhaspin.adhan_prayer_android.utils.AppSettings
import com.ferdyhaspin.adhan_prayer_android.utils.Constants
import com.ferdyhaspin.adhan_prayer_android.utils.Constants.*
import com.ferdyhaspin.adhan_prayer_android.utils.PrayTime
import com.ferdyhaspin.adhan_prayer_android.utils.Utils
import java.util.*
import kotlin.math.abs

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
class PrayAlarmReceiver : BroadcastReceiver(), Constants {

    companion object {
        private const val TAG = "PrayAlarmReceiver"
    }

    private lateinit var alarmManager: AlarmManager
//    private lateinit var alarmIntent: PendingIntent

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
        val prayerTime = intent.getLongExtra(EXTRA_PRAYER_TIME, -1)

        val timePassed =
            prayerTime != -1L && abs(System.currentTimeMillis() - prayerTime) > FIVE_MINUTES

        if (!timePassed) {
            val time = Utils.convertLongToTime(prayerTime)
            Log.e(TAG, "name: $prayerName, time: $time")
            if (prayerName != null)
                sendNotification(context, prayerName, time)
            else
                sendNotification(context, "Something when wrong", time)
//                val service = Intent(context, PraySchedulingService::class.java)
//                service.putExtra(EXTRA_PRAYER_NAME, prayerName)
//                service.putExtra(EXTRA_PRAYER_TIME, prayerTime)
//                // Start the service, keeping the device awake while it is launching.
//                context.startService(service)

            //SET THE NEXT ALARM
            setAlarm(context)
        }
    }

    fun setAlarm(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayAlarmReceiver::class.java)

        val now = Calendar.getInstance(TimeZone.getDefault())
        now.timeInMillis = System.currentTimeMillis()

        var then = Calendar.getInstance(TimeZone.getDefault())
        then.timeInMillis = System.currentTimeMillis()


        val settings = AppSettings.getInstance(context)
        val lat = settings.latFor
        val long = settings.lngFor

        val prayerTimes: LinkedHashMap<String, String> =
            PrayTime.getPrayerTimes(context, lat, long)
        val prayerNames: List<String> = ArrayList(prayerTimes.keys)

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
                            nextAlarmFound = true
                            then.add(Calendar.DAY_OF_YEAR, 1)
                            break
                        }
                    }
                }
            }
        }

        if (!nextAlarmFound) {
            Log.e(TAG, "Alarm not found")
            return
        }

        Log.e(
            TAG,
            "set alarm for $nameOfPrayerFound, time ${Utils.convertLongToTime(then.timeInMillis)}"
        )

        intent.putExtra(EXTRA_PRAYER_NAME, nameOfPrayerFound)
        intent.putExtra(EXTRA_PRAYER_TIME, then.timeInMillis)

        val alarmIntent =
            PendingIntent.getBroadcast(context, ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        when {
            Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 -> { //lollipop_mr1 is 22, this is only 23 and above
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    then.timeInMillis,
                    alarmIntent
                )
            }
            Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2 -> { //JB_MR2 is 18, this is only 19 and above.
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, then.timeInMillis, alarmIntent)
            }
            else -> { //available since api1
                alarmManager.set(AlarmManager.RTC_WAKEUP, then.timeInMillis, alarmIntent)
            }
        }

        val passiveIntent = Intent(context, PassiveLocationChangedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PASSIVE_LOCATION_ID,
            passiveIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        requestPassiveLocationUpdates(context, pendingIntent)

        val receiver = ComponentName(context, PrayBootReceiver::class.java)
        val pm = context.packageManager

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun cancelAlarm(context: Context) {
        // If the alarm has been set, cancel it.
        if (!::alarmManager.isInitialized) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

        val intent = Intent(context, PrayAlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            ALARM_ID,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        alarmManager.cancel(alarmIntent)

        //REMOVE PASSIVE LOCATION RECEIVER
        val passiveIntent = Intent(context, PassiveLocationChangedReceiver::class.java)
        val locationListenerPassivePendingIntent = PendingIntent.getActivity(
            context,
            PASSIVE_LOCATION_ID,
            passiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        removePassiveLocationUpdates(context, locationListenerPassivePendingIntent)

        Log.e(TAG, "Alarm cancel")
    }

    // END_INCLUDE(cancel_alarm)
    private fun requestPassiveLocationUpdates(context: Context, pendingIntent: PendingIntent) {
        val oneHourInMillis = 1000 * 60 * 60.toLong()
        val fiftyKinMeters: Long = 50000
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                oneHourInMillis, fiftyKinMeters.toFloat(), pendingIntent
            )
        } catch (se: SecurityException) {
            Log.w("SetAlarmReceiver", se.message, se)
        }
    }

    private fun removePassiveLocationUpdates(context: Context, pendingIntent: PendingIntent) {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.removeUpdates(pendingIntent)
        } catch (se: SecurityException) {
            Log.w("CancelAlarmReceiver", se.message, se)
            //do nothing. We should always have permision in order to reach this screen.
        }
    }

    private fun getCalendarFromPrayerTime(cal: Calendar, prayerTime: String): Calendar {
        val time = prayerTime.split(":").toTypedArray()
        cal[Calendar.HOUR_OF_DAY] = Integer.valueOf(time[0])
        cal[Calendar.MINUTE] = Integer.valueOf(time[1])
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }

    private fun sendNotification(applicationContext: Context, title: String, time: String) {
        val soundUri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.adhan_trimmed)

        val id = generateRandom()

        var intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent = Intent(applicationContext, MainActivity::class.java)

        //intent.putExtra("notification", data.get("type"));
        val resultPendingIntent = PendingIntent.getActivity(
            applicationContext, id /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val channelId = "Alarm-Pray"
        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.TRANSPARENT)
                .setAutoCancel(true)
                .setContentTitle("Waktunya $title")
                .setContentText("Akan dimulai pukul $time")
                .setSound(soundUri)
                .setContentIntent(resultPendingIntent)
        val mNotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.app_name), importance
            )
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationBuilder.setChannelId(channelId)
            mNotificationManager.createNotificationChannel(channel)
        }
        mNotificationManager.notify(id /* Request Code */, notificationBuilder.build())
    }

    private fun generateRandom(): Int {
        val random = Random()
        return random.nextInt(9999 - 1000) + 1000
    }
}
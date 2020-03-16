package com.ferdyhaspin.adhan_prayer_android.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ferdyhaspin on 15/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
object Utils {

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun calculationTime(context: Context, time: String, minute: Int): String {
        return try {
            var localTime = time
            val df = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            if (!DateFormat.is24HourFormat(context)) {
                val date12Format = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val format = date12Format.parse(localTime)
                if (format != null)
                    localTime = df.format(format)
            }
            val d = df.parse(localTime)
            val cal = Calendar.getInstance().apply {
                if (d != null)
                    this.time = d
                add(Calendar.MINUTE, minute)
            }
            df.format(cal.time)
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

    fun getPrayerName(prayerTimes: LinkedHashMap<String, String>): String {
        val settings = AppSettings.getInstance()
        val prayerNames: List<String> = ArrayList(prayerTimes.keys)
        val now = Calendar.getInstance(TimeZone.getDefault())
        now.timeInMillis = System.currentTimeMillis()

        var then = Calendar.getInstance(TimeZone.getDefault())
        then.timeInMillis = System.currentTimeMillis()

        var nextAlarmFound = false
        var nameOfPrayerFound = ""

        for (prayer in prayerNames) {
            if (prayer != Constants.SUNRISE && prayer != Constants.SUNSET && settings.getInt(
                    Constants.ALARM_FOR + prayer
                ) != 2
            ) {
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
                if (prayer != Constants.SUNRISE && prayer != Constants.SUNSET && settings.getInt(
                        Constants.ALARM_FOR + prayer
                    ) != 2
                ) {
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
        val time = prayerTime.split(":").toTypedArray()
        cal[Calendar.HOUR_OF_DAY] = Integer.valueOf(time[0])
        cal[Calendar.MINUTE] = Integer.valueOf(time[1])
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }
}
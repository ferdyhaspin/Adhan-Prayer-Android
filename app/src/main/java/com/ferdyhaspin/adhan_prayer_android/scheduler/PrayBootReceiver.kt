package com.ferdyhaspin.adhan_prayer_android.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by ferdyhaspin & ilhamelmujib on 13/03/20.
 * Copyright (c) 2020 Bank Syariah Mandiri - Super Apps All rights reserved.
 */

class PrayBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayBootReceiver"
    }

    private val prayAlarmReceiver = PrayAlarmReceiver()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null) {
            when (action) {
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.QUICKBOOT_POWERON",
                "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                    Log.e(
                        TAG,
                        "onReceive : " +
                                "\n- android.intent.action.BOOT_COMPLETED" +
                                "\n- android.intent.action.QUICKBOOT_POWERON" +
                                "\n- com.htc.intent.action.QUICKBOOT_POWERON"
                    )
                    prayAlarmReceiver.setAlarm(context)
                }
                // Our location could have changed, which means time calculations may be different
                // now so cancel the alarm and set it again.
                "android.intent.action.TIMEZONE_CHANGED",
                "android.intent.action.TIME_SET",
                "android.intent.action.MY_PACKAGE_REPLACED" -> {
                    prayAlarmReceiver.cancelAlarm(context)
                    prayAlarmReceiver.setAlarm(context)
                }
            }

        }
    }
}
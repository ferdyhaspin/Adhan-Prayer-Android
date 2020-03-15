package com.ferdyhaspin.adhan_prayer_android.scheduler

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.ui.MainActivity
import com.ferdyhaspin.adhan_prayer_android.utils.Constants
import java.util.*

/**
 * Created by ferdyhaspin & ilhamelmujib on 13/03/20.
 * Copyright (c) 2020 Bank Syariah Mandiri - Super Apps All rights reserved.
 */
class PraySchedulingService(name: String) : IntentService(name), Constants {

    // An ID used to post the notification.
// The Google home page URL from which the app fetches content.
// You can find a list of other Google domains with possible doodles here:
// http://en.wikipedia.org/wiki/List_of_Google_domains


    override fun onHandleIntent(intent: Intent?) {
        // BEGIN_INCLUDE(service_onhandle)
        if (intent != null) {
            val name = intent.getStringExtra(Constants.EXTRA_PRAYER_NAME)
            val time = intent.getStringExtra(Constants.EXTRA_PRAYER_TIME)
            if (name != null && time != null)
                sendNotification(this, name, time)
            else
                sendNotification(this, "null", "null")
        } else {
            sendNotification(this, "null", "null")
        }

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
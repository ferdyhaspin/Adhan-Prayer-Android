package com.ferdyhaspin.adhan_prayer_android.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.model.Prayer
import com.ferdyhaspin.adhan_prayer_android.scheduler.PrayAlarmReceiver
import com.ferdyhaspin.adhan_prayer_android.utils.AppSettings
import com.ferdyhaspin.adhan_prayer_android.utils.Constants

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */

class PrayerAdapter(
    private val data: List<Prayer>,
    private val onSettingChanged: OnSettingChanged,
    private val nowPrayer: String
) : RecyclerView.Adapter<PrayerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.prayer_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.init(data[position], position, onSettingChanged, nowPrayer)
    }

    fun changeSetting(position: Int, settingPosition: Int) {
        data[position].apply {
            setting = settingPosition
        }
        notifyItemChanged(position)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private lateinit var setting: AppSettings

        fun init(
            prayer: Prayer,
            position: Int,
            onSettingChanged: OnSettingChanged,
            nowPrayer: String
        ) {
            setting = AppSettings.getInstance(itemView.context)
            itemView.apply {

                if (prayer.key == nowPrayer)
                    setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightGray))


                val disable = prayer.key == Constants.SUNRISE || prayer.key == Constants.SUNSET

                findViewById<TextView>(R.id.tv_name).text = prayer.name
                findViewById<TextView>(R.id.tv_time).text = prayer.time

                findViewById<ImageView>(R.id.ib_notification).run {
                    if (disable || prayer.setting == 2) {
                        isEnabled = false
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_notification_off)
                    }
                }

                setOnClickListener {
                    if (!disable) {
                        dialog(context, prayer, position, onSettingChanged)
                    }
                }
            }
        }

        private fun dialog(
            context: Context,
            prayer: Prayer,
            position: Int,
            onSettingChanged: OnSettingChanged
        ) {
            Dialog(context).run {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setContentView(R.layout.dialog_setting_alarm_adzan)
                val metrics = context.resources.displayMetrics
                val width = metrics.widthPixels
                window!!.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)

                val mTitle = "Atur Notifikasi ${prayer.name}"
                findViewById<TextView>(R.id.tv_title).apply {
                    text = mTitle
                }

                findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                    dismiss()
                }

                when (prayer.setting) {
                    0 -> findViewById<AppCompatRadioButton>(R.id.rb_adzan).isChecked = true
                    1 -> findViewById<AppCompatRadioButton>(R.id.rb_notif).isChecked = true
                    2 -> findViewById<AppCompatRadioButton>(R.id.rb_nonaktif).isChecked = true
                }

                findViewById<RadioGroup>(R.id.rg_setting).setOnCheckedChangeListener { _, checkedId ->
                    val key = Constants.ALARM_FOR + prayer.key
                    val positionSetting =
                        when (checkedId) {
                            R.id.rb_adzan -> 0
                            R.id.rb_notif -> 1
                            else -> 2
                        }
                    setting.set(key, positionSetting)
                    onSettingChanged.onChanged(position, positionSetting)
                    dismiss()
                }

                setOnDismissListener {
                    val prayAlarmReceiver = PrayAlarmReceiver()
                    prayAlarmReceiver.setAlarm(context)
                }

                show()
            }
        }

    }

    interface OnSettingChanged {
        fun onChanged(position: Int, setting: Int)
    }
}
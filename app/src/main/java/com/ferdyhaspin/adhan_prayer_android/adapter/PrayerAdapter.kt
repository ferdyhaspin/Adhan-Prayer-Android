package com.ferdyhaspin.adhan_prayer_android.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ferdyhaspin.adhan_prayer_android.R
import com.ferdyhaspin.adhan_prayer_android.model.Prayer
import com.ferdyhaspin.adhan_prayer_android.utils.Constants

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */

class PrayerAdapter(
    private val data: List<Prayer>
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
        holder.init(data[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun init(prayer: Prayer) {
            itemView.apply {
                val disable = prayer.key == Constants.SUNRISE

                findViewById<TextView>(R.id.tv_name).text = prayer.name
                findViewById<TextView>(R.id.tv_time).text = prayer.time

                findViewById<ImageButton>(R.id.ib_notification).run {
                    if (disable) {
                        isEnabled = false
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_notification_off)
                    }
                }

                setOnClickListener {
                    if (!disable) {
                        dialog(context, prayer.name)
                    }
                }
            }
        }

        private fun dialog(context: Context, title: String) {
            Dialog(context).run {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setContentView(R.layout.dialog_setting_alarm_adzan)
                val metrics = context.resources.displayMetrics
                val width = metrics.widthPixels
                window!!.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)

                val mTitle = "Atur Notifikasi $title"
                findViewById<TextView>(R.id.tv_title).apply {
                    text = mTitle
                }

                findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                    dismiss()
                }

                show()
            }
        }

    }
}
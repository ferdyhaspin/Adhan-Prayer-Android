package com.ferdyhaspin.adhan_prayer_android.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

                findViewById<TextView>(R.id.tv_name).text = prayer.name
                findViewById<TextView>(R.id.tv_time).text = prayer.time

                findViewById<ImageButton>(R.id.ib_notification).apply {
                    if (prayer.key == Constants.SUNRISE || prayer.key == Constants.SUNSET) {
                        isEnabled = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.colorGrey)
                        }
                    }
                }
            }
        }

    }
}
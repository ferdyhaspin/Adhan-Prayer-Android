package com.ferdyhaspin.adhan_prayer_android.model

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
data class Prayer(
    var key: String = "",
    var name: String = "",
    var time: String? = "",
    var setting: Int = 0
)
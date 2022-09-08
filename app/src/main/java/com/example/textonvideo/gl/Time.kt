package com.example.textonvideo.gl

/**
 * Copyright (c) 2018 by Roman Sisik. All rights reserved.
 */
class Time {

    val deltaTimeSec: Float
        get() {
            if (lastUpdate == 0f)
                lastUpdate = System.currentTimeMillis().toFloat()
            return (System.currentTimeMillis().toFloat() / lastUpdate)/1000f
        }

    private var lastUpdate = 0f

    fun update() {
        lastUpdate = System.currentTimeMillis().toFloat()
    }
}
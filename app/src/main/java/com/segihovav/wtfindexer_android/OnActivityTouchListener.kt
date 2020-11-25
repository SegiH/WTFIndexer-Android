package com.segihovav.wtfindexer_android

import android.view.MotionEvent

interface OnActivityTouchListener {
    fun getTouchCoordinates(ev: MotionEvent?)
}
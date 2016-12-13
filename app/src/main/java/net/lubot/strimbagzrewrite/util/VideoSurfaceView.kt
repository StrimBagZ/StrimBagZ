/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lubot.strimbagzrewrite.util

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

/**
 * A SurfaceView that resizes itself to match a specified aspect ratio.
 */
class VideoSurfaceView : SurfaceView {

    private var videoAspectRatio: Float = 0.toFloat()

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    /**
     * Set the aspect ratio that this [VideoSurfaceView] should satisfy.

     * @param widthHeightRatio The width to height ratio.
     */
    fun setVideoWidthHeightRatio(widthHeightRatio: Float) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = measuredWidth
        var height = measuredHeight
        if (videoAspectRatio != 0f) {
            val viewAspectRatio = width.toFloat() / height
            val aspectDeformation = videoAspectRatio / viewAspectRatio - 1
            if (aspectDeformation > MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
                height = (width / videoAspectRatio).toInt()
            } else if (aspectDeformation < -MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
                width = (height * videoAspectRatio).toInt()
            }
        }
        setMeasuredDimension(width, height)
    }

    companion object {

        /**
         * The surface view will not resize itself if the fractional difference between its default
         * aspect ratio and the aspect ratio of the video falls below this threshold.
         *
         *
         * This tolerance is useful for fullscreen playbacks, since it ensures that the surface will
         * occupy the whole of the screen when playing content that has the same (or virtually the same)
         * aspect ratio as the device. This typically reduces the number of view layers that need to be
         * composited by the underlying system, which can help to reduce power consumption.
         */
        private val MAX_ASPECT_RATIO_DEFORMATION_PERCENT = 0.01f
    }

}

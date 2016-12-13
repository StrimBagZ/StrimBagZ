/*
 * Copyright 2016 Nicola Fäßler
 *
 * This file is part of StrimBagZ.
 *
 * StrimBagZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.lubot.strimbagzrewrite.util;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;

import java.lang.ref.WeakReference;

public class VideoController {

    private Context context;
    private PlayerActivity activity;
    private RelativeLayout controller;
    private boolean isTablet;
    private boolean showing;
    private boolean isFullScreen = false;

    private ImageButton buttonFulllscreen;
    private ImageButton buttonSRL;

    private Handler mHandler = new MessageHandler(this);
    private static final int sDefaultTimeout = 5000;
    private static final int FADE_OUT = 1;

    public VideoController(Context context, PlayerActivity activity, RelativeLayout controller, boolean isTablet) {
        this.context = context;
        this.activity = activity;
        this.controller = controller;
        this.isTablet = isTablet;

        buttonSRL = (ImageButton) controller.findViewById(R.id.btn_srl);
        buttonFulllscreen = (ImageButton) controller.findViewById(R.id.btn_fullscreen);
        buttonFulllscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doToggleFullscreen(false);
                show(sDefaultTimeout);
            }
        });
    }

    public void show() {
        show(sDefaultTimeout);
    }

    public void show(int timeout) {
        if (!showing) {
            controller.setVisibility(View.VISIBLE);
            showing = true;
        }
        //updateFullScreen();

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public void hide() {
        if (controller == null) {
            return;
        }

        try {
            controller.setVisibility(View.INVISIBLE);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        showing = false;
    }

    public boolean isShowing() {
        return showing;
    }

    public void setIsOnFullScreen(boolean s) {
        this.isFullScreen = s;
        updateFullScreen();
    }

    public void updateFullScreen() {
        if (controller == null && buttonFulllscreen == null) {
            return;
        }
        Log.d("Fullscreen", isFullScreen + "");
        if (isFullScreen) {
            Log.d("Fullscreen", "Is on fullscreen, load shrink image");
            buttonFulllscreen.setImageResource(R.drawable.ic_fullscreen_exit);
        } else {
            Log.d("Fullscreen", "Is not on fullscreen, load stretch image");
            buttonFulllscreen.setImageResource(R.drawable.ic_fullscreen);
        }
    }

    public void toogleSRLButton(boolean enabled) {
        if (enabled) {
            buttonSRL.setVisibility(View.VISIBLE);
        } else {
            buttonSRL.setVisibility(View.INVISIBLE);
        }
    }

    public void doToggleFullscreen(final boolean orienationChange) {
        if (controller == null) {
            return;
        }

        if (!isFullScreen) {
            // if player is not fullscreen, make it fullscreen
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (!orienationChange) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }
                    activity.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    activity.hideChat();
                    activity.hideIcons(true);
                }
            });
            setIsOnFullScreen(true);
        } else {
            if (!isTablet) {
                // player was in fullscreen and is phone, change orientation to portrait
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (!orienationChange) {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        }
                    }
                });
            } else {
                // player was in fullscreen and is tablet, don't change the orientation
            }
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            activity.showChat();
            activity.hideIcons(false);
            setIsOnFullScreen(false);
        }

    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoController> mView;

        MessageHandler(VideoController view) {
            mView = new WeakReference<VideoController>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoController view = mView.get();
            if (view == null) {
                return;
            }
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
            }
        }
    }
}

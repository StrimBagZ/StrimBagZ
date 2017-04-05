/*
 * Android Stopwatch Widget: turns a TextView into a timer.
 * Copyright (C) 2011 Euan Freeman <euan04@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
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
package net.lubot.strimbagzrewrite.ui.widget;

import android.os.Handler;
import android.widget.TextView;

/**
 * Turns a TextView widget into a timer, with full stop-watch
 * functionality. Uses a Handler to update at a given time
 * interval.
 *
 * The constructor takes a TextView and a time in milliseconds,
 * specifying the update interval. This implementation is
 * independent of the TextView widget.
 *
 * @author Euan Freeman
 *
 * @see TextView
 * @see Handler
 */
public class StopwatchTextView implements Runnable {
    public enum TimerState {STOPPED, PAUSED, RUNNING};

    private TextView widget;
    private long updateInterval;
    private long time;
    private long startTime;
    private TimerState state;
    private Handler handler;

    public StopwatchTextView(TextView widget, long updateInterval) {
        this.widget = widget;
        this.updateInterval = updateInterval;
        time = 0;
        startTime = 0;
        state = TimerState.STOPPED;

        handler = new Handler();
    }

    @Override
    public void run() {
        time = System.currentTimeMillis();
        long millis = time - startTime;
        long seconds = (long) (millis / 1000);

        widget.setText(String.format("%02d:%02d:%02d", seconds / 3600, (seconds - (seconds / 3600) *3600 ) / 60, seconds % 60));

        if (state == TimerState.RUNNING) {
            handler.postDelayed(this, updateInterval);
        }
    }

    /**
     * Sets the timer into a running state and
     * initialises all time values.
     */
    public void start() {
        startTime = time = System.currentTimeMillis();
        state = TimerState.RUNNING;

        handler.post(this);
    }

    public void startWithGivenTime(long s) {
        startTime = time = s;
        state = TimerState.RUNNING;

        handler.post(this);
    }

    /**
     * Resets the timer.
     */
    public void reset() {
        start();
    }

    /**
     * Puts the timer into a paused state.
     */
    public void pause() {
        handler.removeCallbacks(this);

        state = TimerState.PAUSED;
    }

    /**
     * Resumes the timer.
     */
    public void resume() {
        state = TimerState.RUNNING;

        startTime = System.currentTimeMillis() - (time - startTime);

        handler.post(this);
    }

    /**
     * Stops the timer and resets all time values.
     */
    public void stop() {
        handler.removeCallbacks(this);

        time = 0;
        startTime = 0;
        state = TimerState.STOPPED;

        widget.setText("00:00.000");
    }

    /**
     * Returns the interval (in ms) at which
     * the timer widget is updated.
     *
     * @return
     * 		Time in milliseconds
     */
    public long getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Sets the update interval for the
     * timer widget.
     *
     * @param updateInterval
     * 		Interval in milliseconds
     */
    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    /**
     * Returns the current state of the stop-watch.
     *
     * @return
     * 		State of stop-watch
     */
    public TimerState getState() {
        return state;
    }
}

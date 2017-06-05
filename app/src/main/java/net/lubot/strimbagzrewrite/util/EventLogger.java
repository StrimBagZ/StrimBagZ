package net.lubot.strimbagzrewrite.util;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.util.VerboseLogUtil;

import java.text.NumberFormat;
import java.util.Locale;

public class EventLogger implements Player.InfoListener {

    private static final String TAG = "EventLogger";
    private static final NumberFormat TIME_FORMAT;
    static {
        TIME_FORMAT = NumberFormat.getInstance(Locale.US);
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
    }

    private long sessionStartTimeMs;
    private long[] loadStartTimeMs;
    private long[] availableRangeValuesUs;

    public EventLogger() {
        loadStartTimeMs = new long[Player.RENDERER_COUNT];
    }

    public void startSession() {
        sessionStartTimeMs = SystemClock.elapsedRealtime();
        Log.d(TAG, "start [0]");
    }

    public void endSession() {
        Log.d(TAG, "end [" + getSessionTimeString() + "]");
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        Log.d(TAG, "bandwidth [" + getSessionTimeString() + ", " + bytes + ", "
                + getTimeString(elapsedMs) + ", " + bitrateEstimate + "]");
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        Log.d(TAG, "droppedFrames [" + getSessionTimeString() + ", " + count + "]");
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              long mediaStartTimeMs, long mediaEndTimeMs) {
        loadStartTimeMs[sourceId] = SystemClock.elapsedRealtime();
            Log.v(TAG, "loadStart [" + getSessionTimeString() + ", " + sourceId + ", " + type
                    + ", " + mediaStartTimeMs + ", " + mediaEndTimeMs + "]");
    }

    @Override
    public void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs) {
        Log.d(TAG, "videoFormat [" + getSessionTimeString() + ", " + format.id + ", "
                + Integer.toString(trigger) + "]");
    }

    @Override
    public void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs) {
        Log.d(TAG, "audioFormat [" + getSessionTimeString() + ", " + format.id + ", "
                + Integer.toString(trigger) + "]");
    }

    @Override
    public void onAvailableRangeChanged(int sourceId, TimeRange availableRange) {
        availableRangeValuesUs = availableRange.getCurrentBoundsUs(availableRangeValuesUs);
        Log.d(TAG, "availableRange [" + availableRange.isStatic() + ", " + availableRangeValuesUs[0]
                + ", " + availableRangeValuesUs[1] + "]");
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (VerboseLogUtil.isTagEnabled(TAG)) {
            long downloadTime = SystemClock.elapsedRealtime() - loadStartTimeMs[sourceId];
            Log.v(TAG, "loadEnd [" + getSessionTimeString() + ", " + sourceId + ", " + downloadTime
                    + "]");
        }
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {
        Log.d(TAG, "decoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]");
    }

    private String getSessionTimeString() {
        return getTimeString(SystemClock.elapsedRealtime() - sessionStartTimeMs);
    }

    private String getTimeString(long timeMs) {
        return TIME_FORMAT.format((timeMs) / 1000f);
    }

}

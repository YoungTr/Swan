package com.bomber.swan.trace.core;

import static com.bomber.swan.trace.constants.ConstantsKt.BUFFER_SIZE;
import static com.bomber.swan.trace.constants.ConstantsKt.DEFAULT_RELEASE_BUFFER_DELAY;
import static com.bomber.swan.trace.constants.ConstantsKt.TIME_UPDATE_CYCLE_MS;
import static com.bomber.swan.util.HandlersKt.getGlobalHandler;
import static com.bomber.swan.util.HandlersKt.newHandlerThread;
import static com.bomber.swan.util.StackUtilKt.getStack;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.WorkerThread;

import com.bomber.swan.util.SwanLog;

public class AppMethodBeat implements BeatLifecycle {

    private static final String TAG = "Swan.AppMethodBeat";

    /**
     * state
     */
    private static final int STATUS_DEFAULT = Integer.MAX_VALUE;
    private static final int STATUS_STARTED = 2;
    private static final int STATUS_READY = 1;
    private static final int STATUS_STOPPED = -1;
    private static final int STATUS_EXPIRED_START = -2;
    private static final int STATUS_OUT_RELEASE = -3;
    private static volatile int status = STATUS_DEFAULT;
    private static final Object statusLock = new Object();

    /**
     * mark method
     */
    private static long[] sBuffer = new long[BUFFER_SIZE];
    private static int sIndex = 0;
    private static int sLastIndex = -1;
    private static boolean assertIn = false;

    /**
     * mark time
     */
    private volatile static long sCurrentDiffTime = SystemClock.uptimeMillis();
    private volatile static long sDiffTime = sCurrentDiffTime;
    private static final long sMainThreadId = Looper.getMainLooper().getThread().getId();
    private static final HandlerThread sTimeUpdateThread = newHandlerThread("swan_time_update_thread", Thread.MIN_PRIORITY + 2);
    private static final Handler sHandler = new Handler(sTimeUpdateThread.getLooper());

    private static final int METHOD_ID_MAX = 0XFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;
    private static final Object updateTimeLock = new Object();
    private static volatile boolean isPauseUpdateTime = false;

    // 释放资源
    private static final Runnable realReleaseRunnable = AppMethodBeat::realRelease;
    private static final Runnable updateDiffTimeRunnable = AppMethodBeat::updateDiffTime;
    private static Runnable checkStartExpiredRunnable = null;


    static {
        // AppMethodBeat 类初始化 10s 还没 start，即释放 sBuffer 的资源，退出后台线程等
        getGlobalHandler().postDelayed(realReleaseRunnable, DEFAULT_RELEASE_BUFFER_DELAY);
    }

    private static void realRelease() {
        synchronized (statusLock) {
            if (status == STATUS_DEFAULT || status <= STATUS_READY) {
                SwanLog.d(TAG, "[realRelease] timestamp: %s", System.currentTimeMillis());
                sHandler.removeCallbacksAndMessages(null);
                sTimeUpdateThread.quit();
                sBuffer = null;
                status = STATUS_OUT_RELEASE;
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @WorkerThread
    private static void updateDiffTime() {
        try {
            while (true) {
                while (!isPauseUpdateTime && status > STATUS_STOPPED) {
                    sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
                    // 睡眠 5ms 更新 diff time
                    SystemClock.sleep(TIME_UPDATE_CYCLE_MS);
                }
                synchronized (updateTimeLock) {
                    updateTimeLock.wait();
                }
            }
        } catch (Exception e) {
            SwanLog.e(TAG, e.toString());
        }
    }

    @Override
    public void onStart() {
        synchronized (statusLock) {
            if (status < STATUS_STARTED && status >= STATUS_EXPIRED_START) {
                sHandler.removeCallbacks(checkStartExpiredRunnable);
                getGlobalHandler().removeCallbacks(realReleaseRunnable);
                if (sBuffer == null) {
                    throw new RuntimeException(TAG + " sBuffer == null");
                }
                SwanLog.i(TAG, "[onStart] preStatus: %s", status, getStack());
                status = STATUS_STARTED;
            }
        }
    }

    @Override
    public void onStop() {
        synchronized (statusLock) {
            if (status == STATUS_STARTED) {
                SwanLog.i(TAG, "[onStop] %s", getStack());
                status = STATUS_STOPPED;
            } else {
                SwanLog.w(TAG, "[onStop] current status: %s", status);
            }
        }
    }

    public void forceStop() {
        synchronized (statusLock) {
            status = STATUS_STOPPED;
        }
    }

    @Override
    public boolean isAlive() {
        return status >= STATUS_STARTED;
    }

    public static boolean isRealTrace() {
        return status >= STATUS_READY;
    }

    public static AppMethodBeat getInstance() {
        return Holder.INSTANCE;
    }


    private static class Holder {
        private static final AppMethodBeat INSTANCE = new AppMethodBeat();
    }
}

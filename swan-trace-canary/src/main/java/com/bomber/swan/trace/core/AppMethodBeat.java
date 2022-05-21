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
import com.bomber.swan.util.SystemInfo;

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
                    // 不用一直循环更新时间操作
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
        SystemInfo.INSTANCE.getProcStat();
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

    private static void realExecute() {
        SwanLog.i(TAG, "[realExecute] timestamp: %s", System.currentTimeMillis());
        sCurrentDiffTime = SystemClock.uptimeMillis();

        sHandler.removeCallbacksAndMessages(null);
        sHandler.postDelayed(updateDiffTimeRunnable, TIME_UPDATE_CYCLE_MS);
        sHandler.postDelayed(checkStartExpiredRunnable = () -> {
            synchronized (statusLock) {
                SwanLog.i(TAG, "[startExpired] timestamp: %s, status: %s", System.currentTimeMillis(), status);
                if (status == STATUS_DEFAULT || status == STATUS_READY) {
                    status = STATUS_EXPIRED_START;
                }
            }
        }, DEFAULT_RELEASE_BUFFER_DELAY);
    }

    private static void dispatchBegin() {
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        isPauseUpdateTime = false;

        synchronized (updateTimeLock) {
            updateTimeLock.notify();
        }
    }

    private static void dispatchEnd() {
        isPauseUpdateTime = true;
    }

    /**
     * hook method when it's called in
     *
     * @param methodId
     */
    public static void i(int methodId) {
        if (status <= STATUS_STOPPED) return;
        if (methodId >= METHOD_ID_MAX) return;
        if (status == STATUS_DEFAULT) {
            synchronized (statusLock) {
                if (status == STATUS_DEFAULT) {
                    realExecute();
                    status = STATUS_READY;
                }
            }
        }

        long threadId = Thread.currentThread().getId();
        if (threadId == sMainThreadId) {
            if (assertIn) {
                SwanLog.e(TAG, "ERROR!!! AppMethodBeat.i Recursive calls!!!");
                return;
            }
            assertIn = true;
            if (sIndex >= BUFFER_SIZE) {
                sIndex = 0;
            }
            SwanLog.d(TAG, "in " + methodId + " index: " + sIndex);
            mergeData(methodId, sIndex, true);
            ++sIndex;
            assertIn = false;
        }
    }

    /**
     * hook method when it's called out
     *
     * @param methodId
     */
    public static void o(int methodId) {
        if (status <= STATUS_STOPPED) return;
        if (methodId >= METHOD_ID_MAX) return;
        long threadId = Thread.currentThread().getId();
        if (threadId == sMainThreadId) {
            if (sIndex >= BUFFER_SIZE) {
                sIndex = 0;
            }
            SwanLog.d(TAG, "out " + methodId + " index: " + sIndex);
            mergeData(methodId, sIndex, false);
            ++sIndex;
        }
    }

    private static void mergeData(int methodId, int index, boolean isIn) {
        if (methodId == METHOD_ID_DISPATCH) {
            sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        }

        try {

            long trueId = 0L;
            if (isIn) {
                trueId |= 1L << 63;
            }
            trueId |= (long) methodId << 43;
            trueId |= sCurrentDiffTime & 0x7FFFFFFFFFFL;
            sBuffer[index] = trueId;
            checkPileup(index);
            sLastIndex = index;

        } catch (Throwable t) {
            SwanLog.e(TAG, t.getMessage());
        }
    }

    public static AppMethodBeat getInstance() {
        return Holder.INSTANCE;
    }


    private static class Holder {
        private static final AppMethodBeat INSTANCE = new AppMethodBeat();
    }

    private static IndexRecord sIndexRecordHead = null;

    public IndexRecord maskIndex(String source) {
        if (sIndexRecordHead == null) {
            SwanLog.d(TAG, "sIndexRecordHead mask index: " + sIndex);
            sIndexRecordHead = new IndexRecord(sIndex - 1);
            sIndexRecordHead.source = source;
            return sIndexRecordHead;
        } else {
            SwanLog.d(TAG, "IndexRecord mask index: " + sIndex);
            IndexRecord indexRecord = new IndexRecord(sIndex - 1);
            indexRecord.source = source;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (record != null) {
                if (indexRecord.index <= record.index) {
                    if (last == null) {
                        IndexRecord tmp = sIndexRecordHead;
                        sIndexRecordHead = indexRecord;
                        indexRecord.next = tmp;
                    } else {
                        IndexRecord tmp = last.next;
                        last.next = indexRecord;
                        indexRecord.next = tmp;
                    }
                    return indexRecord;
                }
                last = record;
                record = record.next;
            }
            return indexRecord;
        }
    }

    private static void checkPileup(int index) {
        IndexRecord indexRecord = sIndexRecordHead;
        while (indexRecord != null) {
            if (indexRecord.index == index || (indexRecord.index == -1 && sLastIndex == BUFFER_SIZE - 1)) {
                indexRecord.isValid = false;
                indexRecord = indexRecord.next;
                sIndexRecordHead = indexRecord;
            } else {
                break;
            }
        }
    }

    public static final class IndexRecord {
        public int index;
        private IndexRecord next;
        public boolean isValid = true;
        public String source;

        public IndexRecord(int index) {
            this.index = index;
        }

        public IndexRecord() {
            this.isValid = false;
        }

        /**
         * 从 sIndexRecordHead 链表中删除当前节点
         */
        public void release() {
            isValid = false;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (record != null) {
                if (record == this) {
                    if (last != null) {
                        last.next = record.next;
                    } else {
                        // 说明是该节点是头节点
                        sIndexRecordHead = record.next;
                    }
                    record.next = null;
                    break;
                }
                last = record;
                record = record.next;
            }
        }

        @Override
        public String toString() {
            return "IndexRecord{" +
                    "index=" + index +
                    ", isValid=" + isValid +
                    ", source='" + source + '\'' +
                    '}';
        }
    }

    public long[] copyData(IndexRecord startRecord) {
        return copyData(startRecord, new IndexRecord(sIndex - 1));
    }

    public long[] copyData(IndexRecord startRecord, IndexRecord endRecord) {
        long current = System.currentTimeMillis();
        long[] data = new long[0];
        try {
            if (startRecord.isValid && endRecord.isValid) {
                int len;
                int start = Math.max(0, startRecord.index);
                int end = Math.max(0, endRecord.index);

                if (end > start) {
                    len = end - start + 1;
                    data = new long[len];
                    System.arraycopy(sBuffer, start, data, 0, len);
                } else if (end < start) {
                    len = 1 + end + (sBuffer.length - start);
                    data = new long[len];
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.length - start);
                    System.arraycopy(sBuffer, 0, data, sBuffer.length - start, end + 1);
                }
                return data;
            }

            return data;
        } catch (Throwable t) {
            SwanLog.e(TAG, t.toString());
            return data;
        } finally {
            SwanLog.i(TAG, "[copyData] [%s:%s] length:%s cost:%sms", Math.max(0, startRecord.index), endRecord.index, data.length, System.currentTimeMillis() - current);

        }

    }
}

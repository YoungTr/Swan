package com.bomber.swan.trace.constants

const val BUFFER_SIZE = 100 * 10000 // 7.6M
const val DEFAULT_RELEASE_BUFFER_DELAY = 10 * 1000
const val TIME_UPDATE_CYCLE_MS = 5
const val DEFAULT_ANR = 5 * 1000
const val TARGET_EVIL_METHOD_STACK = 30
const val FILTER_STACK_MAX_COUNT = 60
const val FILTER_STACK_KEY_ALL_PERCENT = .3F
const val TIME_MILLIS_TO_NANO = 1000000
const val DEFAULT_EVIL_METHOD_THRESHOLD_MS = 700
const val DEFAULT_FPS_TIME_SLICE_ALIVE_MS = 10 * 1000
const val TIME_SECOND_TO_NANO = 1000000000
const val DEFAULT_NORMAL_LAG = 2 * 1000
const val DEFAULT_IDLE_HANDLER_LAG = 2 * 1000
const val DEFAULT_TOUCH_EVENT_LAG = 2 * 1000
const val DEFAULT_ANR_INVALID = 6 * 1000
const val DEFAULT_FRAME_DURATION = 16666667L
const val DEFAULT_DROPPED_NORMAL = 3
const val DEFAULT_DROPPED_MIDDLE = 9
const val DEFAULT_DROPPED_HIGH = 24
const val DEFAULT_DROPPED_FROZEN = 42
const val DEFAULT_STARTUP_THRESHOLD_MS_WARM = 4 * 1000
const val DEFAULT_STARTUP_THRESHOLD_MS_COLD = 10 * 1000
const val MAX_LIMIT_ANALYSE_STACK_KEY_NUM = 10
const val LIMIT_WARM_THRESHOLD_MS = 5 * 1000

enum class Type {
    NORMAL, ANR, STARTUP, LAG, SIGNAL_ANR, SIGNAL_ANR_NATIVE_BACKTRACE, LAG_IDLE_HANDLER, LAG_TOUCH, PRIORITY_MODIFIED, TIMERSLACK_MODIFIED
}
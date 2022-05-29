/*
 * Tencent is pleased to support the open source community by making wechat-swan available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bomber.mrs.plugin;


public interface IDynamicConfig {

    enum ExptEnum {
        // trace
        clicfg_swan_trace_fps_enable,
        clicfg_swan_trace_care_scene_set,
        clicfg_swan_trace_fps_time_slice,
        clicfg_swan_trace_evil_method_threshold,

        clicfg_swan_fps_dropped_normal,
        clicfg_swan_fps_dropped_middle,
        clicfg_swan_fps_dropped_high,
        clicfg_swan_fps_dropped_frozen,
        clicfg_swan_trace_evil_method_enable,
        clicfg_swan_trace_anr_enable,
        clicfg_swan_trace_startup_enable,

        clicfg_swan_trace_app_start_up_threshold,
        clicfg_swan_trace_warm_app_start_up_threshold,


        //io
        clicfg_swan_io_file_io_main_thread_enable,
        clicfg_swan_io_main_thread_enable_threshold,
        clicfg_swan_io_small_buffer_enable,
        clicfg_swan_io_small_buffer_threshold,
        clicfg_swan_io_small_buffer_operator_times,
        clicfg_swan_io_repeated_read_enable,
        clicfg_swan_io_repeated_read_threshold,
        clicfg_swan_io_closeable_leak_enable,

        //battery
        clicfg_swan_battery_detect_wake_lock_enable,
        clicfg_swan_battery_record_wake_lock_enable,
        clicfg_swan_battery_wake_lock_hold_time_threshold,
        clicfg_swan_battery_wake_lock_1h_acquire_cnt_threshold,
        clicfg_swan_battery_wake_lock_1h_hold_time_threshold,
        clicfg_swan_battery_detect_alarm_enable,
        clicfg_swan_battery_record_alarm_enable,
        clicfg_swan_battery_alarm_1h_trigger_cnt_threshold,
        clicfg_swan_battery_wake_up_alarm_1h_trigger_cnt_threshold,


        //memory
        clicfg_swan_memory_middle_min_span,
        clicfg_swan_memory_high_min_span,
        clicfg_swan_memory_threshold,
        clicfg_swan_memory_special_activities,

        //resource
        clicfg_swan_resource_detect_interval_millis,
        clicfg_swan_resource_detect_interval_millis_bg,
        clicfg_swan_resource_max_detect_times,
        clicfg_swan_resource_dump_hprof_enable,

        //thread
        clicfg_swan_thread_check_time,
        clicfg_swan_thread_check_bg_time,
        clicfg_swan_thread_limit_count,
        clicfg_swan_thread_report_time,
        clicfg_swan_thread_contain_sys,
        clicfg_swan_thread_filter_thread_set,

    }

    String get(String key, String defStr);

    int get(String key, int defInt);

    long get(String key, long defLong);

    boolean get(String key, boolean defBool);

    float get(String key, float defFloat);
}
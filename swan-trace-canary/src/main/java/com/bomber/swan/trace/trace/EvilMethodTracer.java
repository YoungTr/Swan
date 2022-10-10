/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.bomber.swan.trace.trace;

import static com.bomber.swan.trace.constants.ConstantsKt.FILTER_STACK_MAX_COUNT;
import static com.bomber.swan.trace.constants.ConstantsKt.TARGET_EVIL_METHOD_STACK;
import static com.bomber.swan.trace.constants.ConstantsKt.TIME_MILLIS_TO_NANO;
import static com.bomber.swan.trace.constants.ConstantsKt.TIME_UPDATE_CYCLE_MS;
import static com.bomber.swan.util.DeviceUtil.getProcessPriority;
import static com.bomber.swan.util.DeviceUtil.is64BitRuntime;
import static com.bomber.swan.util.HandlersKt.getGlobalHandler;
import static com.bomber.swan.util.SystemInfo.calculateCpuUsage;

import android.os.Process;

import androidx.annotation.NonNull;

import com.bomber.swan.Swan;
import com.bomber.swan.report.Issue;
import com.bomber.swan.trace.TracePlugin;
import com.bomber.swan.trace.config.SharePluginInfo;
import com.bomber.swan.trace.config.TraceConfig;
import com.bomber.swan.trace.constants.Type;
import com.bomber.swan.trace.core.AppMethodBeat;
import com.bomber.swan.trace.core.UIThreadMonitor;
import com.bomber.swan.trace.items.MethodItem;
import com.bomber.swan.trace.util.IStructuredDataFilter;
import com.bomber.swan.trace.util.TraceDataMarker;
import com.bomber.swan.util.DeviceUtil;
import com.bomber.swan.util.SwanLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;

public class EvilMethodTracer extends Tracer {

    private static final String TAG = "Swan.EvilMethodTracer";
    private final TraceConfig config;
    private AppMethodBeat.IndexRecord indexRecord;
    private long[] queueTypeCosts = new long[3];
    private long evilThresholdMs;
    private boolean isEvilMethodTraceEnable;

    public EvilMethodTracer(TraceConfig config) {
        this.config = config;
        this.evilThresholdMs = config.getEvilThresholdMs();
        this.isEvilMethodTraceEnable = config.isEvilMethodTraceEnable();
    }

    @Override
    public void onAlive() {
        super.onAlive();
        if (isEvilMethodTraceEnable) {
            UIThreadMonitor.addObserver(this);
        }

    }

    @Override
    public void onDead() {
        super.onDead();
        if (isEvilMethodTraceEnable) {
            UIThreadMonitor.removeObserver(this);
        }
    }


    @Override
    public void dispatchBegin(long beginNs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginNs, cpuBeginMs, token);
        indexRecord = AppMethodBeat.getInstance().maskIndex("EvilMethodTracer#dispatchBegin");
    }


    @Override
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
        queueTypeCosts[0] = inputCostNs;
        queueTypeCosts[1] = animationCostNs;
        queueTypeCosts[2] = traversalCostNs;
    }

    @Override
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isVsyncFrame) {
        super.dispatchEnd(beginNs, cpuBeginMs, endNs, cpuEndMs, token, isVsyncFrame);
        long start = config.isDevEnv() ? System.currentTimeMillis() : 0;
        long dispatchCost = (endNs - beginNs) / TIME_MILLIS_TO_NANO;
        try {
            if (dispatchCost >= evilThresholdMs) {
                long[] data = AppMethodBeat.getInstance().copyData(indexRecord);
                long[] queueCosts = new long[3];
                System.arraycopy(queueTypeCosts, 0, queueCosts, 0, 3);
                String scene = "Activity";
                getGlobalHandler().post(new AnalyseTask(isForeground(), scene, data, queueCosts, cpuEndMs - cpuBeginMs, dispatchCost, endNs / TIME_MILLIS_TO_NANO));
            }
        } finally {
            indexRecord.release();
            if (config.isDevEnv()) {
                String usage = calculateCpuUsage(cpuEndMs - cpuBeginMs, dispatchCost);
                SwanLog.v(TAG, "[dispatchEnd] token:%s cost:%sms cpu:%sms usage:%s innerCost:%s",
                        token, dispatchCost, cpuEndMs - cpuBeginMs, usage, System.currentTimeMillis() - start);
            }
        }
    }

    public void modifyEvilThresholdMs(long evilThresholdMs) {
        this.evilThresholdMs = evilThresholdMs;
    }

    private class AnalyseTask implements Runnable {
        long[] queueCost;
        long[] data;
        long cpuCost;
        long cost;
        long endMs;
        String scene;
        boolean isForeground;

        AnalyseTask(boolean isForeground, String scene, long[] data, long[] queueCost, long cpuCost, long cost, long endMs) {
            this.isForeground = isForeground;
            this.scene = scene;
            this.cost = cost;
            this.cpuCost = cpuCost;
            this.data = data;
            this.queueCost = queueCost;
            this.endMs = endMs;
        }

        void analyse() {

            // process
            int[] processStat = getProcessPriority(Process.myPid());
            String usage = calculateCpuUsage(cpuCost, cost);
            LinkedList<MethodItem> stack = new LinkedList();
            if (data.length > 0) {
                TraceDataMarker.structuredDataToStack(data, stack, true, endMs);
                TraceDataMarker.trimStack(stack, TARGET_EVIL_METHOD_STACK, new IStructuredDataFilter() {
                    @Override
                    public void fallback(@NonNull LinkedList<MethodItem> stack, int size) {
                        SwanLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, TARGET_EVIL_METHOD_STACK, stack);
                        Iterator<MethodItem> iterator = stack.listIterator(Math.min(size, TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                        }
                    }

                    @Override
                    public int filterMaxCount() {
                        return FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        return during < (long) filterCount * TIME_UPDATE_CYCLE_MS;
                    }
                });
            }


            StringBuilder reportBuilder = new StringBuilder();
            StringBuilder logcatBuilder = new StringBuilder();
            long stackCost = Math.max(cost, TraceDataMarker.stackToString(stack, reportBuilder, logcatBuilder));
            String stackKey = TraceDataMarker.getTreeKey(stack, (int) stackCost);

            SwanLog.w(TAG, "%s", printEvil(scene, processStat, isForeground, logcatBuilder, stack.size(), stackKey, usage, queueCost[0], queueCost[1], queueCost[2], cost)); // for logcat

            // report
            try {
                TracePlugin plugin = Swan.with().getPlugin(TracePlugin.class);
                if (null == plugin) {
                    return;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject = DeviceUtil.getDeviceInfo(jsonObject, Swan.with().getApplication());

                jsonObject.put(SharePluginInfo.ISSUE_STACK_TYPE, Type.NORMAL);
                jsonObject.put(SharePluginInfo.ISSUE_COST, stackCost);
                jsonObject.put(SharePluginInfo.ISSUE_CPU_USAGE, usage);
                jsonObject.put(SharePluginInfo.ISSUE_SCENE, scene);
                jsonObject.put(SharePluginInfo.ISSUE_TRACE_STACK, reportBuilder.toString());
                jsonObject.put(SharePluginInfo.ISSUE_STACK_KEY, stackKey);

                Issue issue = new Issue();
                issue.setTag(SharePluginInfo.TAG_PLUGIN_EVIL_METHOD);
                issue.setContent(jsonObject);
                plugin.onDetectIssue(issue);

            } catch (JSONException e) {
                SwanLog.e(TAG, "[JSONException error: %s", e);
            }

        }

        @Override
        public void run() {
            analyse();
        }

        private String printEvil(String scene, int[] processStat, boolean isForeground, StringBuilder stack, long stackSize, String stackKey, String usage, long inputCost,
                                 long animationCost, long traversalCost, long allCost) {
            StringBuilder print = new StringBuilder();
            print.append(String.format("-\n>>>>>>>>>>>>>>>>>>>>> maybe happens Jankiness!(%sms) <<<<<<<<<<<<<<<<<<<<<\n", allCost));
            print.append("|* [Status]").append("\n");
            print.append("|*\t\tScene: ").append(scene).append("\n");
            print.append("|*\t\tForeground: ").append(isForeground).append("\n");
            print.append("|*\t\tPriority: ").append(processStat[0]).append("\tNice: ").append(processStat[1]).append("\n");
            print.append("|*\t\tis64BitRuntime: ").append(is64BitRuntime()).append("\n");
            print.append("|*\t\tCPU: ").append(usage).append("\n");
            print.append("|* [doFrame]").append("\n");
            print.append("|*\t\tinputCost:animationCost:traversalCost").append("\n");
            print.append("|*\t\t").append(inputCost).append(":").append(animationCost).append(":").append(traversalCost).append("\n");
            if (stackSize > 0) {
                print.append("|*\t\tStackKey: ").append(stackKey).append("\n");
                print.append(stack.toString());
            } else {
                print.append(String.format("AppMethodBeat is close[%s].", AppMethodBeat.getInstance().isAlive())).append("\n");
            }

            print.append("=========================================================================");
            return print.toString();
        }
    }

}

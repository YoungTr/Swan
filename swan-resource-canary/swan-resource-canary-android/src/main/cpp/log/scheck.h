/*
 * Copyright (c) 2020. Kwai, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Qiushi Xue <xueqiushi@kuaishou.com> on 2020.
 *
 */

#pragma once

#include <android/log.h>
#include <errno.h>
#include "log.h"
#include <string.h>

#ifndef LOG_TAG
#define LOG_TAG "Swan.Check"
#endif

#ifndef SLOGE
#define SLOGE(assertion)                                                                           \
  LOGE("CHECK failed at %s (line: %d) - <%s>: "                                   \
                        "%s: %s",                                                                  \
                        __FILE__, __LINE__, __FUNCTION__, #assertion, strerror(errno));
#endif

#ifndef KCHECK
#define KCHECK(assertion)                                                                          \
  if (!(assertion)) {                                                                              \
    SLOGE(assertion)                                                                               \
  }
#endif

#ifndef KCHECKV
#define KCHECKV(assertion)                                                                         \
  if (!(assertion)) {                                                                              \
    SLOGE(assertion)                                                                               \
    return;                                                                                        \
  }
#endif

#ifndef SCHECKI
#define SCHECKI(assertion)                                                                         \
  if (!(assertion)) {                                                                              \
    SLOGE(assertion)                                                                               \
    return -1;                                                                                     \
  }
#endif

#ifndef SCHECKP
#define SCHECKP(assertion)                                                                         \
  if (!(assertion)) {                                                                              \
    SLOGE(assertion)                                                                               \
    return NULL;                                                                                    \
  }
#endif

#ifndef SCHECKB
#define SCHECKB(assertion)                                                                         \
  if (!(assertion)) {                                                                              \
    SLOGE(assertion)                                                                               \
    return false;                                                                                  \
  }
#endif

//#ifndef KFINISHI_FNC
//#define KFINISHI_FNC(assertion, func, ...)                                                         \
//  if (!(assertion)) {                                                                              \
//    SLOGE(assertion)                                                                               \
//    func(__VA_ARGS__);                                                                             \
//    return -1;                                                                                     \
//  }
//#endif
//
//#ifndef KFINISHP_FNC
//#define KFINISHP_FNC(assertion, func, ...)                                                         \
//  if (!(assertion)) {                                                                              \
//    SLOGE(assertion)                                                                               \
//    func(__VA_ARGS__);                                                                             \
//    return nullptr;                                                                                \
//  }
//#endif
//
//#ifndef KFINISHV_FNC
//#define KFINISHV_FNC(assertion, func, ...)                                                         \
//  if (!(assertion)) {                                                                              \
//    SLOGE(assertion)                                                                               \
//    func(__VA_ARGS__);                                                                             \
//    return;                                                                                        \
//  }
//#endif
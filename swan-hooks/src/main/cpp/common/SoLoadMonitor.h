//
// Created by YoungTr on 2022/12/15.
//

#ifndef SWAN_SOLOADMONITOR_H
#define SWAN_SOLOADMONITOR_H

#include "Macros.h"

namespace swan {
    typedef void (*so_load_callback_t)(const char *__file_name);

    EXPORT bool InstallSoLoadMonitor();
    EXPORT void AddOnSoLoadCallback(so_load_callback_t cb);
    EXPORT void PauseLoadSo();
    EXPORT void ResumeLoadSo();
}

#endif //SWAN_SOLOADMONITOR_H

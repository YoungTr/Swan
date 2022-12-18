//
// Created by YoungTr on 2022/12/15.
//

#include <vector>
#include <mutex>
#include "SoLoadMonitor.h"
#include "Log.h"
#include "xdl.h"
#include "xhook.h"
#include "ScopedCleaner.h"

#define LOG_TAG "Matrix.SoLoadMonitor"

#define TLSVAR __thread __attribute__((tls_model("initial-exec")))

namespace swan {
    static bool sInstalled = false;
    static std::mutex sInstalledMaskMutex;
    static std::mutex sSoLoadCallbacksMutex;
    static std::vector<so_load_callback_t> sSoLoadCallbacks;
    static std::mutex sDlOpenBlockerMutex;
    static std::condition_variable sDlOpenBlocker;
    static bool sDlOpenPaused = false;
    static std::recursive_mutex sDlOpenHandlerMutex;

#ifdef __LP64__
#define LINKER_NAME "linker64"
#define LINKER_NAME_PATTERN ".*/linker64$"
#else
#define LINKER_NAME "linker"
#define LINKER_NAME_PATTERN ".*/linker$"
#endif

    bool InstallSoLoadMonitor() {

        return true;
        std::lock_guard<std::mutex> lock(sInstalledMaskMutex);
        if (sInstalled) {
            return true;
        }

        int sdkVer = android_get_device_api_level();
        if (sdkVer == 24 || sdkVer == 25) {
            LOGE(LOG_TAG, "Does not support N and N_MR1 so far.");
            return false;
        }

        void *hLinker = xdl_open(LINKER_NAME, XDL_DEFAULT);
        if (nullptr == hLinker) {
            LOGE(LOG_TAG, "Fail to open %s.", LINKER_NAME);
            return false;
        }

        auto hLinkerCloser = MakeScopedCleaner([&hLinker]() {
            if (nullptr != hLinker) {
                xdl_close(hLinker);
            }
        });





    }


}
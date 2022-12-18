//
// Created by YoungTr on 2022/12/17.
//

#ifndef SWAN_PTHREADHOOK_H
#define SWAN_PTHREADHOOK_H

namespace pthread_hook {
    typedef void *(*pthread_routine_t)(void *);

    extern void SetThreadTraceEnabled(bool enabled);

    extern void SetThreadStackShrinkEnabled(bool enabled);

    extern void
    SetThreadStackShrinkIgnoredCreatorSoPatterns(const char **patterns, size_t pattern_count);

    extern void InstallHooks(bool enable_debug);

}

#endif //SWAN_PTHREADHOOK_H

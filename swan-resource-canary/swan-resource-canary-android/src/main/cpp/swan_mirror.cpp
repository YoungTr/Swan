//
// Created by YoungTr on 2022/7/3.
//

#include <string>
#include "swan_mirror.h"

// ScopedSuspendAll instance placeholder
static std::unique_ptr<char[]> ssa_instance_  = std::make_unique<char[]>(64);
// ScopedGCCriticalSection instance placeholder
static std::unique_ptr<char[]> sgc_instance_ = std::make_unique<char[]>(64);

void swan_mirror_ssa_constructor(void *handler) {
    reinterpret_cast<void (*)(void *, const char *, bool)>(handler)((void *) ssa_instance_.get(), "HprofDump", true);
}

void swan_mirror_ssa_destructor(void *handler) {
    reinterpret_cast<void (*)(void *)>(handler)((void *) ssa_instance_.get());
}

void swan_mirror_sgc_constructor(void *handler, void *self) {
    reinterpret_cast<void (*)(void *, void *, GcCause, CollectorType)>(handler)(
            (void *) sgc_instance_.get(), self, kGcCauseHprof, kCollectorTypeHprof);
}

void swan_mirror_sgc_destructor(void *handler) {
    reinterpret_cast<void (*)(void *)>(handler)((void *) sgc_instance_.get());
}

void swan_mirror_mutator_lock(void *handler) {

}

void swan_mirror_mutator_unlock(void *handler) {

}


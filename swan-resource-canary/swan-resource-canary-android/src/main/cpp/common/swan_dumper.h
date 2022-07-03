//
// Created by YoungTr on 2022/4/14.
//

#ifndef SWAN_SWAN_DUMPER_H
#define SWAN_SWAN_DUMPER_H

#ifdef __cplusplus
extern "C" {
#endif

#define SWAN_DUMP_LIB_ART ("libart.so")

#define SWAN_DUMP_LIBART_DBG_SUSPEND      "_ZN3art3Dbg9SuspendVMEv"
#define SWAN_DUMP_LIBART_DBG_RESUME       "_ZN3art3Dbg8ResumeVMEv"

#define SWAN_DUMP_LIBART_SSA_CONSTRUCTOR        "_ZN3art16ScopedSuspendAllC1EPKcb"
#define SWAN_DUMP_LIBART_SSA_DESTRUCTOR         "_ZN3art16ScopedSuspendAllD1Ev"
#define SWAN_DUMP_LIBART_SGC_CONSTRUCTOR        "_ZN3art2gc23ScopedGCCriticalSectionC1EPNS_6ThreadENS0_7GcCauseENS0_13CollectorTypeE"
#define SWAN_DUMP_LIBART_SGC_DESTRUCTOR         "_ZN3art2gc23ScopedGCCriticalSectionD1Ev"
#define SWAN_DUMP_LIBART_MUTATOR_LOCK_PTR       "_ZN3art5Locks13mutator_lock_E"
#define SWAN_DUMP_LIBART_EXCLUSIVE_LOCK         "_ZN3art17ReaderWriterMutex13ExclusiveLockEPNS_6ThreadE"
#define SWAN_DUMP_LIBART_EXCLUSIVE_UNLOCK       "_ZN3art17ReaderWriterMutex15ExclusiveUnlockEPNS_6ThreadE"


typedef void  (*swan_libart_suspend)(void);

typedef void  (*swan_libart_resumed)(void);



/**
  * Function pointer for ART Android R
  */
// art::ScopedSuspendAll::ScopedSuspendAll()
typedef void (*swan_libart_ssa_constructor_fnc_)(void *handle, const char *cause, int long_suspend);

// art::ScopedSuspendAll::~ScopedSuspendAll()
typedef void (*swan_libart_ssa_destructor_fnc_)(void *handle);

// art::gc::ScopedGCCriticalSection::ScopedGCCriticalSection()
typedef void (*swan_libart_sgc_constructor_fnc_)(void *handle, void *self, void *cause,  void *collector_type);

// art::gc::ScopedGCCriticalSection::~ScopedGCCriticalSection()
typedef void (*swan_libart_sgc_destructor_fnc_)(void *handle);

// art::Locks::mutator_lock_
typedef void **swan_libart_mutator_lock_ptr_;

// art::ReaderWriterMutex::ExclusiveLock
typedef void (*swan_libart_exclusive_lock_fnc_)(void *, void *self);

// art::ReaderWriterMutex::ExclusiveUnlock
typedef void (*swan_libart_exclusive_unlock_fnc_)(void *, void *self);


int swan_dump_init();

pid_t swan_dump_suspend(int wait);

int swan_dump_resumed(pid_t pid);


#ifdef __cplusplus
}
#endif

#endif //SWAN_SWAN_DUMPER_H

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

// What caused the GC?
enum GcCause {
    // Invalid GC cause used as a placeholder.
    kGcCauseNone,
    // GC triggered by a failed allocation. Thread doing allocation is blocked
    // waiting for GC before
    // retrying allocation.
    kGcCauseForAlloc,
    // A background GC trying to ensure there is free memory ahead of allocations.
    kGcCauseBackground,
    // An explicit System.gc() call.
    kGcCauseExplicit,
    // GC triggered for a native allocation when NativeAllocationGcWatermark is
    // exceeded.
    // (This may be a blocking GC depending on whether we run a non-concurrent
    // collector).
    kGcCauseForNativeAlloc,
    // GC triggered for a collector transition.
    kGcCauseCollectorTransition,
    // Not a real GC cause, used when we disable moving GC (currently for
    // GetPrimitiveArrayCritical).
    kGcCauseDisableMovingGc,
    // Not a real GC cause, used when we trim the heap.
    kGcCauseTrim,
    // Not a real GC cause, used to implement exclusion between GC and
    // instrumentation.
    kGcCauseInstrumentation,
    // Not a real GC cause, used to add or remove app image spaces.
    kGcCauseAddRemoveAppImageSpace,
    // Not a real GC cause, used to implement exclusion between GC and debugger.
    kGcCauseDebugger,
    // GC triggered for background transition when both foreground and background
    // collector are CMS.
    kGcCauseHomogeneousSpaceCompact,
    // Class linker cause, used to guard filling art methods with special values.
    kGcCauseClassLinker,
    // Not a real GC cause, used to implement exclusion between code cache
    // metadata and GC.
    kGcCauseJitCodeCache,
    // Not a real GC cause, used to add or remove system-weak holders.
    kGcCauseAddRemoveSystemWeakHolder,
    // Not a real GC cause, used to prevent hprof running in the middle of GC.
    kGcCauseHprof,
    // Not a real GC cause, used to prevent GetObjectsAllocated running in the
    // middle of GC.
    kGcCauseGetObjectsAllocated,
    // GC cause for the profile saver.
    kGcCauseProfileSaver,
    // GC cause for running an empty checkpoint.
    kGcCauseRunEmptyCheckpoint,
};

// Which types of collections are able to be performed.
enum CollectorType {
    // No collector selected.
    kCollectorTypeNone,
    // Non concurrent mark-sweep.
    kCollectorTypeMS,
    // Concurrent mark-sweep.
    kCollectorTypeCMS,
    // Semi-space / mark-sweep hybrid, enables compaction.
    kCollectorTypeSS,
    // Heap trimming collector, doesn't do any actual collecting.
    kCollectorTypeHeapTrim,
    // A (mostly) concurrent copying collector.
    kCollectorTypeCC,
    // The background compaction of the concurrent copying collector.
    kCollectorTypeCCBackground,
    // Instrumentation critical section fake collector.
    kCollectorTypeInstrumentation,
    // Fake collector for adding or removing application image spaces.
    kCollectorTypeAddRemoveAppImageSpace,
    // Fake collector used to implement exclusion between GC and debugger.
    kCollectorTypeDebugger,
    // A homogeneous space compaction collector used in background transition
    // when both foreground and background collector are CMS.
    kCollectorTypeHomogeneousSpaceCompact,
    // Class linker fake collector.
    kCollectorTypeClassLinker,
    // JIT Code cache fake collector.
    kCollectorTypeJitCodeCache,
    // Hprof fake collector.
    kCollectorTypeHprof,
    // Fake collector for installing/removing a system-weak holder.
    kCollectorTypeAddRemoveSystemWeakHolder,
    // Fake collector type for GetObjectsAllocated
    kCollectorTypeGetObjectsAllocated,
    // Fake collector type for ScopedGCCriticalSection
    kCollectorTypeCriticalSection,
};

/**
  * Function pointer for ART Android R
  */
// art::ScopedSuspendAll::ScopedSuspendAll()
typedef void (*swan_libart_ssa_constructor_fnc_)(void *handle, const char *cause, int long_suspend);

// art::ScopedSuspendAll::~ScopedSuspendAll()
typedef void (*swan_libart_ssa_destructor_fnc_)(void *handle);

// art::gc::ScopedGCCriticalSection::ScopedGCCriticalSection()
typedef void (*swan_libart_sgc_constructor_fnc_)(void *handle, void *self, enum GcCause,  enum CollectorType);

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

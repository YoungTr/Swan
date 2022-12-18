//
// Created by YoungTr on 2022/12/17.
//

#include <pthread.h>
#include <mutex>
#include "ReentrantPrevention.h"
#include "Log.h"
#include "ReentrantPrevention.h"


#define TAG "ReentrantPrevention"

pthread_key_t m_rp_key;

void rp_init() {
    if (!m_rp_key) {
        pthread_key_create(&m_rp_key, nullptr);
    }
}

bool rp_acquire() {
    auto counter = static_cast<size_t *>(pthread_getspecific(m_rp_key));
    if (!counter) {
        counter = new size_t;
        *counter = 0;
        pthread_setspecific(m_rp_key, counter);
    }
    if (*counter) {
        return false;
    }
    (*counter)++;
    return true;
}

void rp_release() {
    auto counter = static_cast<size_t *>(pthread_getspecific(m_rp_key));
    if (!counter) {
        LOG_ALWAYS_FATAL(TAG, "calling rp_release() before rp_acquire");
    }

    (*counter)--;
}

#undef TAG
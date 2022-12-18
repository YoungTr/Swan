//
// Created by YoungTr on 2022/12/17.
//

#ifndef SWAN_REENTRANTPREVENTION_H
#define SWAN_REENTRANTPREVENTION_H

#include "Macros.h"

EXPORT void rp_init();

EXPORT bool rp_acquire();

EXPORT void rp_release();

#endif //SWAN_REENTRANTPREVENTION_H

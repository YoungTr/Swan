//
// Created by YoungTr on 2022/6/29.
//

#ifndef SWAN_SWAN_LINK_H
#define SWAN_SWAN_LINK_H

#include <link.h>

#ifdef __cplusplus
extern "C" {
#endif

extern __attribute((weak)) int dl_iterate_phdr(int (*)(struct dl_phdr_info *, size_t, void *), void *);

void swan_iterate_phdr();

#ifdef __cplusplus
}
#endif

#endif //SWAN_SWAN_LINK_H

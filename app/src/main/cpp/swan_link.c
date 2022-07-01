//
// Created by YoungTr on 2022/6/29.
//

#include <link.h>
#include "swan_link.h"
#include "log.h"

static int callback(struct dl_phdr_info *info, size_t size, void *data) {
    char *type;
    int p_type;
    LOGD("Name: \"%s\" (%d segments)", info->dlpi_name, info->dlpi_phnum);

//    for (int j = 0; j < info->dlpi_phnum; ++j) {
//        p_type = info->dlpi_phdr[j].p_type;
//        type = (p_type == PT_LOAD) ? "PT_LOAD" :
//               (p_type == PT_DYNAMIC) ? "PT_DYNAMIC" :
//               (p_type == PT_INTERP) ? "PT_INTERP" :
//               (p_type == PT_NOTE) ? "PT_NOTE" :
//               (p_type == PT_INTERP) ? "PT_INTERP" :
//               (p_type == PT_PHDR) ? "PT_PHDR" :
//               (p_type == PT_TLS) ? "PT_TLS" :
//               (p_type == PT_GNU_EH_FRAME) ? "PT_GNU_EH_FRAME" :
//               (p_type == PT_GNU_STACK) ? "PT_GNU_STACK" :
//               (p_type == PT_GNU_RELRO) ? "PT_GNU_RELRO" : NULL;
//        LOGD("    %2d: [%14p; memsz:%7jx] flags: %#jx; ", j,
//             (void *) (info->dlpi_addr + info->dlpi_phdr[j].p_vaddr),
//             (uintmax_t) info->dlpi_phdr[j].p_memsz,
//             (uintmax_t) info->dlpi_phdr[j].p_flags);
//
//        if (NULL == type) {
//            LOGD("%s\n", type);
//        } else {
//            LOGD("[other (%#x)]", p_type);
//        }
//    }
    return 0;
}

void swan_iterate_phdr() {
    dl_iterate_phdr(callback, NULL);
}
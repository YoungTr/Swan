//
// Created by YoungTr on 2022/6/12.
//

#include "trace_common.h"

int common_api_level = 0;
JavaVM *common_vm = NULL;

int common_init(int api_level) {
    common_api_level = api_level;

    return 0;
}

void set_common_vm(JavaVM *vm) {
    common_vm = vm;
}
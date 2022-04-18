//
// Created by YoungTr on 2022/4/14.
//

#include "sn_common.h"

int sn_common_api_level = 0;

int sn_common_init(int api_level) {
    sn_common_api_level = api_level;
    return 0;
}
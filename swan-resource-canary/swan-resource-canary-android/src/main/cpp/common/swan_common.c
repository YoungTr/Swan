//
// Created by YoungTr on 2022/4/14.
//

#include "swan_common.h"

int swan_common_api_level = 0;

int swan_common_init(int api_level) {
    swan_common_api_level = api_level;
    return 0;
}
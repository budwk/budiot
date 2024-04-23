export const API_IOT_PUB_COMMON_INIT = '/iot/pub/common/init'

import request from '/@/utils/request'

export function getInit(data: object = {}) {
    return request({
        url: API_IOT_PUB_COMMON_INIT,
        method: 'GET',
        data: data
    })
}
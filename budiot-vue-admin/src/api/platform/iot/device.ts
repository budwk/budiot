export const API_IOT_DEVICE_DEVICE_LIST = '/iot/admin/device/device/list'
export const API_IOT_DEVICE_DEVICE_GET = '/iot/admin/device/device/get/'
export const API_IOT_DEVICE_DEVICE_EXPORT = '/iot/admin/device/device/export'

import request from '/@/utils/request'

export function getInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_GET + id,
        method: 'GET'
    })
}
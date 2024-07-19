export const API_IOT_DEVICE_DEVICE_LIST = '/iot/admin/device/device/list'
export const API_IOT_DEVICE_DEVICE_GETNAME = '/iot/admin/device/device/get/name/'
export const API_IOT_DEVICE_DEVICE_GETEXT = '/iot/admin/device/device/get/ext/'
export const API_IOT_DEVICE_DEVICE_EXPORT = '/iot/admin/device/device/export'

import request from '/@/utils/request'

export function getName(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_GETNAME + id,
        method: 'GET'
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_GETEXT + id,
        method: 'GET'
    })
}
export const API_IOT_DEVICE_DEVICE_LIST = '/iot/admin/device/device/list'
export const API_IOT_DEVICE_DEVICE_GETNAME = '/iot/admin/device/device/get/name/'
export const API_IOT_DEVICE_DEVICE_GETEXT = '/iot/admin/device/device/get/ext/'
export const API_IOT_DEVICE_DEVICE_EXPORT = '/iot/admin/device/device/export'
export const API_IOT_DEVICE_DEVICE_DATA_LIST = '/iot/admin/device/device/data/list'
export const API_IOT_DEVICE_DEVICE_DATA_FIELD = '/iot/admin/device/device/data/field/'
export const API_IOT_DEVICE_DEVICE_RAW_LIST = '/iot/admin/device/device/raw/list'
export const API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST = '/iot/admin/device/device/cmd/wait/list'
export const API_IOT_DEVICE_DEVICE_CMD_DONE_LIST = '/iot/admin/device/device/cmd/done/list'
export const API_IOT_DEVICE_DEVICE_CMD_CONFIG_LIST = '/iot/admin/device/device/cmd/config/list'

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

export function getField(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_DATA_FIELD + id,
        method: 'GET'
    })
}

export function getCmdConfigList(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_CMD_CONFIG_LIST,
        method: 'POST',
        data: {
            deviceId: id
        }
    })
}


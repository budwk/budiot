export const API_IOT_DEVICE_DEVICE_LIST = '/iot/admin/device/device/list'
export const API_IOT_DEVICE_DEVICE_GETNAME = '/iot/admin/device/device/get/name/'
export const API_IOT_DEVICE_DEVICE_GETEXT = '/iot/admin/device/device/get/ext/'
export const API_IOT_DEVICE_DEVICE_EXPORT = '/iot/admin/device/device/export'
export const API_IOT_DEVICE_DEVICE_DATA_LIST = '/iot/admin/device/device/data/list'
export const API_IOT_DEVICE_DEVICE_DATA_FIELD = '/iot/admin/device/device/data/field/'
export const API_IOT_DEVICE_DEVICE_RAW_LIST = '/iot/admin/device/device/raw/list'
export const API_IOT_DEVICE_DEVICE_EVENT_LIST = '/iot/admin/device/device/event/list'
export const API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST = '/iot/admin/device/device/cmd/wait/list'
export const API_IOT_DEVICE_DEVICE_CMD_DONE_LIST = '/iot/admin/device/device/cmd/done/list'
export const API_IOT_DEVICE_DEVICE_CMD_CONFIG_LIST = '/iot/admin/device/device/cmd/config/list'
export const API_IOT_DEVICE_DEVICE_CMD_CREATE = '/iot/admin/device/device/cmd/create'

import request from '/@/utils/request'

// 获取设备名称
export function getName(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_GETNAME + id,
        method: 'GET'
    })
}

// 获取设备信息
export function getInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_GETEXT + id,
        method: 'GET'
    })
}

// 获取设备上报数据字段列表
export function getField(id: string) {
    return request({
        url: API_IOT_DEVICE_DEVICE_DATA_FIELD + id,
        method: 'GET'
    })
}

// 获取指令配置
export function getCmdConfigList(data: any) {
    return request({
        url: API_IOT_DEVICE_DEVICE_CMD_CONFIG_LIST,
        method: 'POST',
        data: data
    })
}

// 创建下发指令
export function doCmdCreate(data: any) {
    return request({
        url: API_IOT_DEVICE_DEVICE_CMD_CREATE,
        method: 'POST',
        data: data
    })
}

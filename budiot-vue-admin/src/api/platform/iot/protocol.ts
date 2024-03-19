export const API_IOT_CONFIG_PROTOCOL_LIST = '/iot/admin/config/protocol/list'
export const API_IOT_CONFIG_PROTOCOL_GET = '/iot/admin/config/protocol/get/'
export const API_IOT_CONFIG_PROTOCOL_CREATE = '/iot/admin/config/protocol/create'
export const API_IOT_CONFIG_PROTOCOL_DELETE = '/iot/admin/config/protocol/delete'
export const API_IOT_CONFIG_PROTOCOL_UPDATE = '/iot/admin/config/protocol/update'

import request from '/@/utils/request'

export function getList(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_PROTOCOL_LIST,
        method: 'POST',
        data: data
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_CONFIG_PROTOCOL_GET + id,
        method: 'GET'
    })
}

export function doCreate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_PROTOCOL_CREATE,
        method: 'POST',
        data: data
    })
}

export function doUpdate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_PROTOCOL_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDelete(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_PROTOCOL_DELETE,
        method: 'POST',
        data: data
    })
}

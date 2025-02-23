export const API_IOT_CONFIG_SUPPLIER_LIST = '/iot/admin/config/supplier/list'
export const API_IOT_CONFIG_SUPPLIER_GET = '/iot/admin/config/supplier/get/'
export const API_IOT_CONFIG_SUPPLIER_CREATE = '/iot/admin/config/supplier/create'
export const API_IOT_CONFIG_SUPPLIER_DELETE = '/iot/admin/config/supplier/delete'
export const API_IOT_CONFIG_SUPPLIER_UPDATE = '/iot/admin/config/supplier/update'

import request from '/@/utils/request'

export function getList(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_SUPPLIER_LIST,
        method: 'POST',
        data: data
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_CONFIG_SUPPLIER_GET + id,
        method: 'GET'
    })
}

export function doCreate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_SUPPLIER_CREATE,
        method: 'POST',
        data: data
    })
}

export function doUpdate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_SUPPLIER_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDelete(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_SUPPLIER_DELETE,
        method: 'POST',
        data: data
    })
}

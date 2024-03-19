export const API_IOT_DEVICE_PRODUCT_INIT = '/iot/admin/device/product/init'
export const API_IOT_DEVICE_PRODUCT_LIST = '/iot/admin/device/product/list'
export const API_IOT_DEVICE_PRODUCT_GET = '/iot/admin/device/product/get/'
export const API_IOT_DEVICE_PRODUCT_CREATE = '/iot/admin/device/product/create'
export const API_IOT_DEVICE_PRODUCT_DELETE = '/iot/admin/device/product/delete/'
export const API_IOT_DEVICE_PRODUCT_UPDATE = '/iot/admin/device/product/update'

import request from '/@/utils/request'

export function getInit(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_INIT,
        method: 'GET',
        data: data
    })
}

export function getList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_LIST,
        method: 'POST',
        data: data
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_GET + id,
        method: 'GET'
    })
}

export function doCreate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_CREATE,
        method: 'POST',
        data: data
    })
}

export function doUpdate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDelete(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DELETE,
        method: 'POST',
        data: data
    })
}

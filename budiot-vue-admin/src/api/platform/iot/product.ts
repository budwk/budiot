export const API_IOT_DEVICE_PRODUCT_INIT = '/iot/admin/device/product/init'
export const API_IOT_DEVICE_PRODUCT_LIST = '/iot/admin/device/product/list'
export const API_IOT_DEVICE_PRODUCT_GET = '/iot/admin/device/product/get/'
export const API_IOT_DEVICE_PRODUCT_CREATE = '/iot/admin/device/product/create'
export const API_IOT_DEVICE_PRODUCT_DELETE = '/iot/admin/device/product/delete'
export const API_IOT_DEVICE_PRODUCT_UPDATE = '/iot/admin/device/product/update'
// 设备管理
export const API_IOT_DEVICE_PRODUCT_DEVICE_COUNT = '/iot/admin/device/product/device/count'
export const API_IOT_DEVICE_PRODUCT_DEVICE_COUNT_MORE = '/iot/admin/device/product/device/countMore'
export const API_IOT_DEVICE_PRODUCT_DEVICE_IMPORT = '/iot/admin/device/product/device/import'
export const API_IOT_DEVICE_PRODUCT_DEVICE_EXPORT = '/iot/admin/device/product/device/export'
export const API_IOT_DEVICE_PRODUCT_DEVICE_LIST = '/iot/admin/device/product/device/list'
export const API_IOT_DEVICE_PRODUCT_DEVICE_GET = '/iot/admin/device/product/device/get/'
export const API_IOT_DEVICE_PRODUCT_DEVICE_CREATE = '/iot/admin/device/product/device/create'
export const API_IOT_DEVICE_PRODUCT_DEVICE_DELETE = '/iot/admin/device/product/device/delete'
export const API_IOT_DEVICE_PRODUCT_DEVICE_DELETES = '/iot/admin/device/product/device/deletes'
export const API_IOT_DEVICE_PRODUCT_DEVICE_UPDATE = '/iot/admin/device/product/device/update'
// 上报参数
export const API_IOT_DEVICE_PRODUCT_ATTR_LIST = '/iot/admin/device/product/attr/list'
export const API_IOT_DEVICE_PRODUCT_ATTR_GET = '/iot/admin/device/product/attr/get/'
export const API_IOT_DEVICE_PRODUCT_ATTR_CREATE = '/iot/admin/device/product/attr/create'
export const API_IOT_DEVICE_PRODUCT_ATTR_DELETE = '/iot/admin/device/product/attr/delete'
export const API_IOT_DEVICE_PRODUCT_ATTR_DELETES = '/iot/admin/device/product/attr/deletes'
export const API_IOT_DEVICE_PRODUCT_ATTR_UPDATE = '/iot/admin/device/product/attr/update'
export const API_IOT_DEVICE_PRODUCT_ATTR_IMPORT = '/iot/admin/device/product/attr/import'
export const API_IOT_DEVICE_PRODUCT_ATTR_EXPORT = '/iot/admin/device/product/attr/export'
export const API_IOT_DEVICE_PRODUCT_ATTR_SORT = '/iot/admin/device/product/attr/sort'

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
        url: API_IOT_DEVICE_PRODUCT_GET + id + '?t=' + new Date().getTime(),
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

export function getDeviceCount(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_COUNT,
        method: 'POST',
        data: data
    })
}

export function getDeviceCountMore(id: string) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_COUNT_MORE,
        method: 'POST',
        data: { id: id}
    })
}

export function getDeviceList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_LIST,
        method: 'POST',
        data: data
    })
}


export function getDeviceInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_GET + id + '?t=' + new Date().getTime(),
        method: 'GET'
    })
}

export function doDeviceCreate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_CREATE,
        method: 'POST',
        data: data
    })
}

export function doDeviceUpdate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDeviceDelete(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_DELETE,
        method: 'POST',
        data: data
    })
}

export function doDeviceDeletes(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_DEVICE_DELETES,
        method: 'POST',
        data: data
    })
}

export function getAttrList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_LIST,
        method: 'POST',
        data: data
    })
}


export function getAttrInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_GET + id + '?t=' + new Date().getTime(),
        method: 'GET'
    })
}

export function doAttrCreate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_CREATE,
        method: 'POST',
        data: data
    })
}

export function doAttrUpdate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doAttrDelete(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_DELETE,
        method: 'POST',
        data: data
    })
}

export function doAttrSort(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_PRODUCT_ATTR_SORT,
        method: 'POST',
        data: data
    })
}
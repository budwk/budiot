export const API_IOT_CONFIG_CLASSIFY_LIST = '/iot/admin/config/classify/list'
export const API_IOT_CONFIG_CLASSIFY_GET = '/iot/admin/config/classify/get/'
export const API_IOT_CONFIG_CLASSIFY_CREATE = '/iot/admin/config/classify/create'
export const API_IOT_CONFIG_CLASSIFY_DELETE = '/iot/admin/config/classify/delete'
export const API_IOT_CONFIG_CLASSIFY_UPDATE = '/iot/admin/config/classify/update'
export const API_IOT_CONFIG_CLASSIFY_SORT_TREE = '/iot/admin/config/classify/get_sort_tree'
export const API_IOT_CONFIG_CLASSIFY_SORT = '/iot/admin/config/classify/sort'

import request from '/@/utils/request'

export function getList(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_LIST,
        method: 'POST',
        data: data
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_GET + id,
        method: 'GET'
    })
}

export function doCreate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_CREATE,
        method: 'POST',
        data: data
    })
}

export function doUpdate(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDelete(data: object = {}) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_DELETE,
        method: 'POST',
        data: data
    })
}

export function getSortTree() {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_SORT_TREE,
        method: 'GET'
    })
}

export function doSort(ids: string) {
    return request({
        url: API_IOT_CONFIG_CLASSIFY_SORT,
        method: 'POST',
        data: { ids: ids}
    })
}
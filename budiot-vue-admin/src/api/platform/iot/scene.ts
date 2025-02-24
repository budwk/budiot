export const API_IOT_DEVICE_SCENE_SPACE_LIST = '/iot/admin/scene/space/list'
export const API_IOT_DEVICE_SCENE_SPACE_GET = '/iot/admin/scene/space/get/'
export const API_IOT_DEVICE_SCENE_SPACE_CREATE = '/iot/admin/scene/space/create'
export const API_IOT_DEVICE_SCENE_SPACE_DELETE = '/iot/admin/scene/space/delete'
export const API_IOT_DEVICE_SCENE_SPACE_UPDATE = '/iot/admin/scene/space/update'
export const API_IOT_DEVICE_SCENE_SPACE_SORT_TREE = '/iot/admin/scene/space/get_sort_tree'
export const API_IOT_DEVICE_SCENE_SPACE_SORT = '/iot/admin/scene/space/sort'

export const API_IOT_DEVICE_SCENE_LIST = '/iot/admin/scene/list'
export const API_IOT_DEVICE_SCENE_GET = '/iot/admin/scene/get/'
export const API_IOT_DEVICE_SCENE_CREATE = '/iot/admin/scene/create'
export const API_IOT_DEVICE_SCENE_DELETE = '/iot/admin/scene/delete'
export const API_IOT_DEVICE_SCENE_UPDATE = '/iot/admin/scene/update'
export const API_IOT_DEVICE_SCENE_ENABLED_CHANGE = '/iot/admin/scene/enabled_change'
export const API_IOT_DEVICE_SCENE_PRODUCT_LIST = '/iot/admin/scene/product/list'
export const API_IOT_DEVICE_SCENE_DEVICE_LIST = '/iot/admin/scene/device/list'
export const API_IOT_DEVICE_SCENE_CMDPARAMS_LIST = '/iot/admin/scene/cmd/params'

import request from '/@/utils/request'

export function getList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_LIST,
        method: 'POST',
        data: data
    })
}

export function getInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_SCENE_GET + id,
        method: 'GET'
    })
}

export function doCreate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_CREATE,
        method: 'POST',
        data: data
    })
}

export function doUpdate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doDelete(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_DELETE,
        method: 'POST',
        data: data
    })
}


export function getSpaceList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_LIST,
        method: 'POST',
        data: data
    })
}

export function getSpaceInfo(id: string) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_GET + id,
        method: 'GET'
    })
}

export function doSpaceCreate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_CREATE,
        method: 'POST',
        data: data
    })
}

export function doSpaceUpdate(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_UPDATE,
        method: 'POST',
        data: data
    })
}

export function doSpaceDelete(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_DELETE,
        method: 'POST',
        data: data
    })
}

export function getSpaceSortTree() {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_SORT_TREE,
        method: 'GET'
    })
}

export function doSpaceSort(ids: string) {
    return request({
        url: API_IOT_DEVICE_SCENE_SPACE_SORT,
        method: 'POST',
        data: { ids: ids}
    })
}

export function getProductList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_PRODUCT_LIST,
        method: 'POST',
        data: data
    })
}

export function getCmdParamsList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_CMDPARAMS_LIST,
        method: 'POST',
        data: data
    })
}

export function doEnabledChange(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_ENABLED_CHANGE,
        method: 'POST',
        data: data
    })
}

export function getDeviceList(data: object = {}) {
    return request({
        url: API_IOT_DEVICE_SCENE_DEVICE_LIST,
        method: 'POST',
        data: data
    })
}

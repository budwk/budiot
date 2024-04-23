
<template>
    <div class="app-container">

        <el-row :gutter="10" class="mb8">
            <el-col :span="12">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                    <el-form-item>
                        <el-input
    v-model="queryParams.filedValue" placeholder="请输入" clearable style="width: 380px"
                            @keyup.enter="handleSearch" >
                            <template #prepend>
                                    <el-select v-model="queryParams.filedName" placeholder="查询条件" style="width: 180px">
                                    <el-option
                                        v-for="(el, index) in filedList"
                                        :key="index"
                                        :label="el.name"
                                        :value="el.code"
                                    />
                                </el-select>
                            </template>
                        </el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-cascader style="width: 100%;" clearable :options="classifyList" placeholder="产品分类"
                        :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children', emitPath: false }"
                        v-model="queryParams.classifyId"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                        <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
            <el-col :span="12" style="text-align: right; ">
               <div style="display: inline-flex;">
                    <import
                                v-permission="['iot.device.product.device.import']"
                                btn-text="导入设备"
                                :action="API_IOT_DEVICE_PRODUCT_DEVICE_IMPORT"
                                :data="queryParams"
                                temp-url="/tpl/template_product_device.xlsx"
                                @refresh="handleSearch"
                                style="margin-right: 12px"
                            />
                            <export
                v-permission="['iot.device.product.device.export']"
                btn-text="导出设备"
                :check-list="multipleSelection"
                :action="API_IOT_DEVICE_PRODUCT_DEVICE_EXPORT"
                :columns="columns"
                :data="{
                    productId: id
                }"
                style="margin-right: 12px"
            />
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.product.device.create']">添加设备
                    </el-button>
                    <el-button
    plain type="danger" @click="handleDeletes"
                        v-permission="['iot.device.product.device.delete']">批量删除
                    </el-button>
                    <right-toolbar :columns="columns" :quickSearchShow="false" @quickSearch="handleSearch" />

               </div>
            </el-col>
        </el-row>
        <el-row class="mb8">

            <pro-table-list 
                ref="tableRef"
                :url="API_IOT_DEVICE_PRODUCT_DEVICE_LIST" 
                :queryParams="queryParams"
                v-model:selectRows="multipleSelection"
                :pageSize="10"
                style="width: 100%;"
            >
                <template v-for="(item, idx) in columns" :key="idx">
                    <el-table-column
    :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
                        :show-overflow-tooltip="true" :align="item.align" :width="item.width">
                        <template v-if="item.prop == 'createdAt'" #default="scope">
                            <span>{{ formatTime(scope.row.createdAt) }}</span>
                        </template>
                        <template v-if="item.prop == 'lastConnectionTime'" #default="scope">
                            <span>{{ formatTime(scope.row.lastConnectionTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'online'" #default="scope">
                            <el-tag v-if="scope.row.online" type="success">在线</el-tag>
                            <el-tag v-else type="danger">离线</el-tag>
                        </template>
                    </el-table-column>
                </template>
                <el-table-column
    fixed="right" header-align="center" align="center" label="操作"
                    class-name="small-padding fixed-width" width="150">
                    <template #default="scope">
                        <div>
                            <el-tooltip content="修改" placement="top">
                                <el-button
    link type="primary" icon="View" @click="handleView(scope.row)"
                                    v-permission="['iot.device.product.device.update']"></el-button>
                            </el-tooltip>
                        </div>
                    </template>
                </el-table-column>
            </pro-table-list>
        </el-row>
    
    </div>
</template>
<script setup lang="ts" name="platform-device-device-device">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { handleTree } from '/@/utils/common';
import { getInit } from '/@/api/platform/iot/common'
const iotPlatform = ref([])
const classifyList = ref([])
const classifyListOrigin = ref([])
const protocolList = ref([])
const protocolType = ref([])
const deviceType = ref([])

const filedList = ref([
    { code: 'deviceNo', name: '设备通信号' },
    { code: 'meterNo', name: '设备编号/表号' },
    { code: 'imei', name: 'IMEI' },
    { code: 'iccid', name: 'ICCID' },
    { code: 'iotPlatformId', name: '第三方平台设备编号' },
])
const data = reactive({
    queryParams: {
        classifyId: '',
        filedName: 'deviceNo',
        filedValue: '',
        pageOrderName: '',
        pageOrderBy: '',
    }
})
const { queryParams, } = toRefs(data)
const init = async () => {
    const { data } = await getInit()
    iotPlatform.value = data.iotPlatform
    classifyList.value = handleTree(data.classifyList) as never
    classifyListOrigin.value = data.classifyList
    protocolList.value = data.protocolList
    protocolType.value = data.protocolType
    deviceType.value = data.deviceType
}

onMounted(() => {
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
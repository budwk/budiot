<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            
                <el-form :model="queryParams" shallowRef="queryRef" :inline="true" label-width="68px">
                    <el-form-item label="输入参数名称">
                        <el-input
    v-model="queryParams.name" placeholder="请输入参数名称/参数标识" clearable style="width: 380px"
                            @keyup.enter="handleSearch" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                        <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>

        </el-row>
    </div>
</template>
<script setup lang="ts" name="deviceAttr">
import { nextTick, onMounted, reactive, ref, shallowRef, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm } from 'element-plus'
import { useRoute } from "vue-router"
import { getAttrList, getAttrInfo, doAttrCreate, doAttrUpdate, doAttrDelete } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string

const createRef = shallowRef<InstanceType<typeof ElForm>>()
const updateRef = shallowRef<InstanceType<typeof ElForm>>()
const queryRef = shallowRef<InstanceType<typeof ElForm>>()

const showCreate = shallowRef(false)
const showUpdate = shallowRef(false)

const tableData = shallowRef([])
const tableLoading = shallowRef(false)
const columns = shallowRef([
    { prop: 'deviceNo', label: '设备通信号', show: true, fixed: 'left' },
    { prop: 'meterNo', label: '设备编号', show: true },
    { prop: 'iotPlatformId', label: '第三方平台设备号', show: false },
    { prop: 'imei', label: 'IMEI', show: true },
    { prop: 'iccid', label: 'ICCID', show: true },
    { prop: 'lastConnectionTime', label: '最后通信时间', show: true },
    { prop: 'online', label: '在线状态', show: true }
])
const data = reactive({
    formData: {
        id: '',
        deviceNo: '',
        meterNo: '',
        imei: '',
        iccid: '',
        productId: '',
    },
    queryParams: {
        productId: id,
        name: '',
        pageNo: 1,
        pageSize: 8,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        deviceNo: [{ required: true, message: "设备通信号不能为空", trigger: ["blur", "change"] }],
    },
})

const queryParams = shallowRef(data.queryParams)
const formData = shallowRef(data.formData)
const formRules = shallowRef(data.formRules)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        deviceNo: '',
        meterNo: '',
        imei: '',
        iccid: '',
        productId: id,
    }
    formEl?.resetFields()
}

const handleSearch = () => {
    queryParams.value.pageNo = 1
    list()
}

const resetSearch = () => {
    queryRef.value?.resetFields()
    list()
}

const list = () => {
    tableLoading.value = true
    getAttrList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
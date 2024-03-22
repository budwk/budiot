<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="12">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                    <el-form-item>
                        <el-input
    v-model="queryParams.filedValue" placeholder="请输入" clearable style="width: 380px"
                            @keyup.enter="handleSearch" >
                            <template #prepend>
                                    <el-select v-model="queryParams.filedName" placeholder="请选择查询条件" style="width: 180px">
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
                                :action="API_IOT_DEVICE_PRODUCT_DEVICE_IMPOT"
                                temp-url="/tpl/template_device.xlsx"
                                @refresh="handleSearch"
                                style="margin-right: 12px"
                            />
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.product.device.create']">添加设备
                    </el-button>
                    <el-button
    plain type="danger" @click="handleCreate"
                        v-permission="['iot.device.product.device.delete']">批量删除
                    </el-button>
               </div>
            </el-col>
        </el-row>
    </div>
</template>
<script setup lang="ts" >
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { useRoute } from "vue-router"
import { API_IOT_DEVICE_PRODUCT_DEVICE_IMPOT } from '/@/api/platform/iot/product'
import { ElForm } from 'element-plus'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()
    
const filedList = ref([
    { code: 'deviceNo', name: '设备通信号' },
    { code: 'meterNo', name: '设备编号' },
    { code: 'imei', name: 'IMEI' },
    { code: 'iccid', name: 'ICCID' },
])

const data = reactive({
    formData: {
        id: '',
        name: '',
        classifyId: '',
        deviceType: '',
        dataFormat: 'json',
        iotPlatform: '',
        protocolType: '',
        protocolId: '',
        properties: {},
        propertieList: [],
        description: '',
        payMode: 0,
    },
    queryParams: {
        filedName: 'deviceNo',
        filedValue: '',
        pageNo: 1,
        pageSize: 8,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        name: [{ required: true, message: "产品名称不能为空", trigger: ["blur", "change"] }],
        classifyId: [{ required: true, message: "设备分类不能为空", trigger: ["blur", "change"] }],
        deviceType: [{ required: true, message: "设备类型不能为空", trigger: ["blur", "change"] }],
        iotPlatform: [{ required: true, message: "接入平台不能为空", trigger: ["blur", "change"] }],
        protocolType: [{ required: true, message: "接入协议不能为空", trigger: ["blur", "change"] }],
        protocolId: [{ required: true, message: "设备协议不能为空", trigger: ["blur", "change"] }],
        dataFormat: [{ required: true, message: "数据格式不能为空", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const handleSearch = () => {
    queryParams.value.pageNo = 1
    list()
}

const resetSearch = () => {
    queryRef.value?.resetFields()
    list()
}

const list = () => {
    console.log('list')
}


const handleCreate = () => {

}
</script>
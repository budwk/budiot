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

            <el-table v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="50" />
            <template v-for="(item, idx) in columns" :key="idx">
                <el-table-column
:prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
                    :show-overflow-tooltip="true" :align="item.align" :width="item.width">
                    <template v-if="item.prop == 'createdAt'" #default="scope">
                        <span>{{ formatTime(scope.row.createdAt) }}</span>
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
link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                                v-permission="['iot.device.product.device.update']"></el-button>
                        </el-tooltip>
                        <el-tooltip content="删除" placement="top">
                            <el-button
link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                                v-permission="['iot.device.product.device.delete']"></el-button>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <pagination
:total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
            @pagination="list" />
        </el-row>

        <el-dialog title="添加设备" v-model="showCreate" width="45%">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="160px">
                <el-form-item label="设备通信号" prop="deviceNo">
                    <el-input v-model="formData.deviceNo" placeholder="请输入设备唯一设备通信号" />
                </el-form-item>
                <el-form-item label="设备编号(表号/铭牌号)" prop="meterNo">
                    <el-input v-model="formData.meterNo" placeholder="请输入设备编号" />
                </el-form-item>
                <el-form-item label="IMEI" prop="imei">
                    <el-input v-model="formData.imei" placeholder="请输入IMEI" />
                </el-form-item>
                <el-form-item label="ICCID" prop="iccid">
                    <el-input v-model="formData.iccid" placeholder="请输入ICCID" />
                </el-form-item>
                <el-form-item
v-for="(item, index) in props" :key="index" :label="item.name" :prop="item.name" class="lable-font-weight"
                    :rules="getDynamicRule(item.code, item.name, item.required)"
                >
                    <el-input v-model="formData.props[item.code]" :placeholder="'请输入' + item.name" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="create">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="修改设备" v-model="showUpdate" width="45%" :close-on-click-modal="false">
            <el-form ref="updateRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="设备通信号" prop="deviceNo">
                    <el-input v-model="formData.deviceNo" placeholder="请输入设备唯一设备通信号" />
                </el-form-item>
                <el-form-item label="设备编号(表号/铭牌号)" prop="meterNo">
                    <el-input v-model="formData.meterNo" placeholder="请输入设备编号" />
                </el-form-item>
                <el-form-item label="IMEI" prop="imei">
                    <el-input v-model="formData.imei" placeholder="请输入IMEI" />
                </el-form-item>
                <el-form-item label="ICCID" prop="iccid">
                    <el-input v-model="formData.iccid" placeholder="请输入ICCID" />
                </el-form-item>
                <el-form-item
v-for="(item, index) in props" :key="index" :label="item.name" :prop="item.name" class="lable-font-weight"
                    :rules="getDynamicRule(item.code, item.name, item.required)"
                >
                    <el-input v-model="formData.props[item.code]" :placeholder="'请输入' + item.name" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="update">确 定</el-button>
                    <el-button @click="showUpdate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts" >
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { useRoute } from "vue-router"
import { getDeviceList, API_IOT_DEVICE_PRODUCT_DEVICE_IMPORT, API_IOT_DEVICE_PRODUCT_DEVICE_EXPORT, getDeviceProp,
    getDeviceInfo, doDeviceCreate, doDeviceUpdate, doDeviceDelete, doDeviceDeletes } from '/@/api/platform/iot/product'
import { ElForm } from 'element-plus'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()

const showCreate = ref(false)
const showUpdate = ref(false)
const multipleSelection = ref([])
const props = ref({})    
const filedList = ref([
    { code: 'deviceNo', name: '设备通信号' },
    { code: 'meterNo', name: '设备编号' },
    { code: 'imei', name: 'IMEI' },
    { code: 'iccid', name: 'ICCID' },
])
const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
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
        props: {}
    },
    queryParams: {
        productId: id,
        filedName: 'deviceNo',
        filedValue: '',
        pageNo: 1,
        pageSize: 10,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        deviceNo: [{ required: true, message: "设备通信号不能为空", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        deviceNo: '',
        meterNo: '',
        imei: '',
        iccid: '',
        productId: id,
        props: {}
    }
    formEl?.resetFields()
}

const getDynamicRule = (code: string, name: string,required: boolean) =>{
    return {
        required: required,
        message: "请输入"+name,
        validator: (rule, value, callback) => {
            if (formData.value.props[code] === "" && required) {
                callback("请输入"+ name)
            } else {
                callback()
            }
        }
    }
}

const handleSearch = () => {
    queryParams.value.pageNo = 1
    list()
}

const resetSearch = () => {
    queryRef.value?.resetFields()
    list()
}

const handleSelectionChange = (val: any) => {
    multipleSelection.value = val
}

// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
    if(props.value) {
        props.value.forEach((item: any) => {
            formData.value.props[item.code] = item.defaultValue || ''
        })
    }
}

// 修改按钮
const handleUpdate = (row: any) => {
    getDeviceInfo(row.id).then((res: any) => {
        formData.value = res.data
        showUpdate.value = true
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除设备（设备通信号 ' + row.deviceNo + '）？').then(() => {
        return doDeviceDelete(row)
    }).then(() => {
        queryParams.value.pageNo = 1
        list()
        modal.msgSuccess('删除成功')
    }).catch(() => { })
}

// 批量删除
const handleDeletes = () => {
    if (multipleSelection.value.length == 0) {
        modal.msgWarning('请选择要删除的设备')
        return
    }
    const ids = multipleSelection.value.map((item: any) => item.id)
    modal.confirm('确定删除选中的设备？').then(() => {
        return doDeviceDeletes({ ids: ids})
    }).then(() => {
        queryParams.value.pageNo = 1
        list()
        modal.msgSuccess('删除成功')
    }).catch(() => { })
}

// 提交新增
const create = () => {
    if (!createRef.value) return
    createRef.value.validate((valid) => {
        if (valid) {
            doDeviceCreate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showCreate.value = false
                queryParams.value.pageNo = 1
                list()
            })
        }
    })
}

// 提交修改
const update = () => {
    if (!updateRef.value) return
    updateRef.value.validate((valid) => {
        if (valid) {
            doDeviceUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

const list = () => {
    tableLoading.value = true
    getDeviceList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

const listProp = () => {
    getDeviceProp(id).then((res) => {
        props.value = res.data
    })
}

onMounted(() => {
    list()
    listProp()
})
</script>
<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="14">
            <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                <el-form-item>
                    <el-input
    v-model="queryParams.name" placeholder="请输入参数名称/参数标识" clearable style="width: 380px"
                        @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                    <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                </el-form-item>
            </el-form>
            </el-col>
            <el-col :span="10" style="text-align: right; ">
               <div style="display: inline-flex;">
                    <import
                                v-permission="['iot.device.product.device.config']"
                                btn-text="导入"
                                :action="API_IOT_DEVICE_PRODUCT_ATTR_IMPORT"
                                :data="queryParams"
                                :cover="true"
                                temp-url="/tpl/template_product_attr.xlsx"
                                @refresh="handleSearch"
                                style="margin-right: 12px"
                            />
                            <export
                v-permission="['iot.device.product.device.config']"
                btn-text="导出"
                :check-list="multipleSelection"
                :action="API_IOT_DEVICE_PRODUCT_ATTR_EXPORT"
                :columns="columns"
                :data="{
                    productId: id
                }"
                style="margin-right: 12px"
            />
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.product.device.config']">新增
                    </el-button>

               </div>
            </el-col>
        </el-row>
        <el-row class="mb8">

<el-table v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50" />
<template v-for="(item, idx) in columns" :key="idx">
    <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
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
<el-table-column fixed="right" header-align="center" align="center" label="操作"
    class-name="small-padding fixed-width" width="150">
    <template #default="scope">
        <div>
            <el-tooltip content="修改" placement="top">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                    v-permission="['iot.device.product.device.update']"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
                <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                    v-permission="['iot.device.product.device.delete']"></el-button>
            </el-tooltip>
        </div>
    </template>
</el-table-column>
</el-table>
<pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
@pagination="list" />
</el-row>

<el-dialog title="添加设备" v-model="showCreate" width="45%">
<el-form ref="createRef" :model="formData" :rules="formRules" label-width="200px">
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
<script setup lang="ts" name="deviceAttr">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm } from 'element-plus'
import { useRoute } from "vue-router"
import { getAttrList, getAttrInfo, doAttrCreate, doAttrUpdate, doAttrDelete,
    API_IOT_DEVICE_PRODUCT_ATTR_IMPORT,
    API_IOT_DEVICE_PRODUCT_ATTR_EXPORT
 } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()

const showCreate = ref(false)
const showUpdate = ref(false)
const multipleSelection = ref([])

const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
    { prop: 'name', label: '参数名称', show: true, fixed: 'left' },
    { prop: 'code', label: '参数标识', show: true },
    { prop: 'attrType', label: '参数类型', show: true },
    { prop: 'dataType', label: '数据类型', show: true },
    { prop: 'note', label: '参数说明', show: true }
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

const { queryParams, formData, formRules } = toRefs(data)

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

const handleSelectionChange = (val: any) => {
    multipleSelection.value = val
}


// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getAttrInfo(row.id).then((res: any) => {
        formData.value = res.data
        showUpdate.value = true
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除参数（参数名称 ' + row.name + '）？').then(() => {
        return doAttrDelete(row)
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
            doAttrCreate(formData.value).then((res: any) => {
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
            doAttrUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
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
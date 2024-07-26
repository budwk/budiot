<template>
    <div>
        <el-row :gutter="10" class="mb8">
            <el-col :span="14">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                    <el-form-item>
                        <el-input
                            v-model="queryParams.name"
                            placeholder="请输入固件名称"
                            clearable
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                        <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
            <el-col :span="10" style="text-align: right">
                <div style="display: inline-flex">
                    <el-button plain type="success" @click="handleCreate" v-permission="['iot.device.product.firmware']">新增 </el-button>
                </div>
            </el-col>
        </el-row>
        <el-row class="mb8">
            <el-table ref="tableRef" v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
                <template v-for="(item, idx) in columns" :key="idx">
                    <el-table-column
                        :prop="item.prop"
                        :label="item.label"
                        :fixed="item.fixed"
                        v-if="item.show"
                        :show-overflow-tooltip="true"
                        :align="item.align"
                        :width="item.width"
                    >
                        <template v-if="item.prop == 'createdAt'" #default="scope">
                            {{ formatTime(scope.row.createdAt) }}
                        </template>
                        <template v-if="item.prop == 'path'" #default="scope">
                            <el-button link type="primary" @click="handleDownload(scope.row)" v-permission="['iot.device.product.firmware']">
                                下载
                            </el-button>
                        </template>
                        <template v-if="item.prop == 'enabled'" #default="scope">
                            <el-tag v-if="scope.row.enabled" type="success">启用</el-tag>
                            <el-tag v-else type="danger">未启用</el-tag>
                        </template>
                        <template v-if="item.prop == 'allUpgrade'" #default="scope">
                            <span v-if="scope.row.allUpgrade">是</span>
                            <span v-else>否</span>
                        </template>
                    </el-table-column>
                </template>
                <el-table-column fixed="right" header-align="center" align="right" label="操作"
        class-name="small-padding fixed-width" width="200">
        <template #default="scope">
            <div style="padding-right: 20px;">
                <el-tooltip v-if="!scope.row.allUpgrade" content="指定设备" placement="top">
                    <el-button link type="primary" icon="Tools" @click="handleDevice(scope.row)"
                        v-permission="['iot.device.product.firmware']">
                        指定设备
                    </el-button>
                </el-tooltip>
                <el-tooltip content="修改" placement="top">
                    <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                        v-permission="['iot.device.product.firmware']"></el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                    <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                        v-permission="['iot.device.product.firmware']"></el-button>
                </el-tooltip>
            </div>
        </template>
    </el-table-column>
            </el-table>
            <pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
@pagination="list" />
        </el-row>


        <el-dialog title="新增固件" v-model="showCreate" width="45%" :close-on-click-modal="false">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="110px">
                <el-form-item label="固件名称" prop="name">
                    <el-input v-model="formData.name" placeholder="ASCII字符串（不包含逗号）" />
                    <el-alert type="warning" style="margin: 5px 0;height: 35px;">不同功能分支的固件应当选用不同的固件名来区分，以免出现版本混乱、误操作、误升级</el-alert>
                </el-form-item>
                <el-form-item label="固件版本" prop="version">
                    <el-input v-model="formData.version" placeholder="xx.xx.xx" />
                </el-form-item>
                <el-form-item label="固件文件" prop="path" class="label-font-weight">
                    <el-upload
                        ref="uploadRef"
                        name="Filedata"
                        :limit="1"
                        accept=".bin"
                        :headers="upload.headers"
                        :action="upload.url"
                        :disabled="upload.isUploading"
                        :on-success="handleFileSuccess"
                        :auto-upload="true"
                        drag
                    >
                        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                        <template #tip>
                        <div class="el-upload__tip text-center">
                            <span>仅允许上传 .bin 文件。</span>
                        </div>
                        </template>
                    </el-upload>
                </el-form-item>

                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="formData.enabled">
                        <el-radio :value="true">启用</el-radio>
                        <el-radio :value="false">不启用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="升级全部设备" prop="allUpgrade">
                    <el-radio-group v-model="formData.allUpgrade">
                        <el-radio :value="true">是</el-radio>
                        <el-radio :value="false">否</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="固件说明" prop="note">
                    <el-input v-model="formData.note" placeholder="请输入固件说明" maxlength="200" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="create">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="修改固件" v-model="showUpdate" width="45%" :close-on-click-modal="false">
            <el-form ref="updateRef" :model="formData" :rules="formRules" label-width="110px">
                <el-form-item label="固件名称" prop="name">
                    <el-input v-model="formData.name" placeholder="ASCII字符串（不包含逗号）" />
                    <el-alert type="warning" style="margin: 5px 0;height: 35px;">不同功能分支的固件应当选用不同的固件名来区分，以免出现版本混乱、误操作、误升级</el-alert>
                </el-form-item>
                <el-form-item label="固件版本" prop="version">
                    <el-input v-model="formData.version" placeholder="xx.xx.xx" />
                </el-form-item>
                <el-form-item label="固件文件" prop="path" class="label-font-weight">
                    <el-upload
                        ref="uploadRef"
                        name="Filedata"
                        :limit="1"
                        accept=".bin"
                        :headers="upload.headers"
                        :action="upload.url"
                        :disabled="upload.isUploading"
                        :on-success="handleFileSuccess"
                        :auto-upload="true"
                        :file-list="fileList"
                        drag
                    >
                        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                        <template #tip>
                        <div class="el-upload__tip text-center">
                            <span>仅允许上传 .bin 文件</span>
                        </div>
                        </template>
                    </el-upload>
                </el-form-item>

                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="formData.enabled">
                        <el-radio :value="true">启用</el-radio>
                        <el-radio :value="false">不启用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="升级全部设备" prop="allUpgrade">
                    <el-radio-group v-model="formData.allUpgrade">
                        <el-radio :value="true">是</el-radio>
                        <el-radio :value="false">否</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="固件说明" prop="note">
                    <el-input v-model="formData.note" placeholder="请输入固件说明" maxlength="200" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="update">确 定</el-button>
                    <el-button @click="showUpdate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="指定设备" v-model="showDevice" width="55%" :close-on-click-modal="false">
            <el-row :gutter="10" class="mb8">
            <el-col :span="16">
                <el-form :model="queryDeviceParams" ref="queryDeviceRef" :inline="true" label-width="68px">
                    <el-form-item>
                        <el-input
    v-model="queryDeviceParams.filedValue" placeholder="请输入" clearable style="width: 320px"
                            @keyup.enter="handleDeviceSearch" >
                            <template #prepend>
                                    <el-select v-model="queryDeviceParams.filedName" placeholder="请选择查询条件" style="width: 150px">
                                    <el-option label="设备通信号" value="deviceNo"></el-option>
                                    <el-option label="IMEI" value="imei"></el-option>
                                </el-select>
                            </template>
                        </el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleDeviceSearch">搜索</el-button>
                        <el-button icon="Refresh" @click="resetDeviceSearch">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
            <el-col :span="8" style="text-align: right; ">
               <div style="display: inline-flex;">
                    <import
                                btn-text="导入设备"
                                :action="API_IOT_DEVICE_PRODUCT_FIRMWARE_DEVICE_IMPORT"
                                :data="queryDeviceParams"
                                temp-url="/tpl/template_product_firmware_device.xlsx"
                                @refresh="handleDeviceSearch"
                                style="margin-right: 12px"
                            />
                    <el-button
    plain type="success" @click="handleDeviceCreate">添加设备
                    </el-button>
                    <el-button
    plain type="danger" @click="handleDeviceDeletes">批量解绑
                    </el-button>

               </div>
            </el-col>
        </el-row>

        <pro-table-list 
                ref="tableDeviceRef"
                :url="API_IOT_DEVICE_PRODUCT_FIRMWARE_DEVICE_LIST" 
                :queryParams="queryDeviceParams"
                v-model:selectRows="multipleSelectionDevice"
                :pageSize="10"
                :lazy="true"
                style="width: 100%;"
            >
            <el-table-column
                    prop="deviceNo"
                    label="设备通信号"
                    align="center"/>
                    <el-table-column
                    prop="imei"
                    label="IMEI"
                    align="center"/>
                <el-table-column fixed="right" header-align="center" align="center" label="操作"
                class-name="small-padding fixed-width" >
                <template #default="scope">
                    <div style="padding-right: 20px;">
                        <el-tooltip content="解绑" placement="top">
                            <el-button link type="danger" icon="Delete" @click="handleDeviceDelete(scope.row)"
                            ></el-button>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>
            </pro-table-list>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable, ElUpload } from 'element-plus'
import { useRoute } from 'vue-router'
import { getFirmwareList, doFirmwareCreate, doFirmwareUpdate, doFirmwareDelete, getFirmwareInfo,
    API_IOT_DEVICE_PRODUCT_FIRMWARE_DEVICE_LIST, doFirmwareDeviceInsert, doFirmwareDeviceDelete, API_IOT_DEVICE_PRODUCT_FIRMWARE_DEVICE_IMPORT
 } from '/@/api/platform/iot/product'
import { useUserInfo } from '/@/stores/userInfo'
import { platformUploadFileUrl } from "/@/api/common"
import { usePlatformInfo } from "/@/stores/platformInfo"
import download from '/@/utils/download'
import { ElMessage, ElMessageBox } from 'element-plus'


const platformInfo = usePlatformInfo()
const userInfo = useUserInfo()
const route = useRoute()
const id = route.params.id as string
const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()
const tableRef = ref<InstanceType<typeof ElTable>>()
const uploadRef = ref<InstanceType<typeof ElUpload>>()
const queryDeviceRef = ref<InstanceType<typeof ElForm>>()
const tableDeviceRef = ref<InstanceType<typeof ElTable>>()    

const showCreate = ref(false)
const showUpdate = ref(false)
const showDevice = ref(false)

const multipleSelection = ref([])
const multipleSelectionDevice = ref([])

const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
    { prop: 'name', label: '固件名称', show: true, fixed: 'left', width: 180, align: 'center' },
    { prop: 'version', label: '版本号', show: true, align: 'center' },
    { prop: 'path', label: '固件文件', show: true, align: 'center' },
    { prop: 'enabled', label: '是否启用', show: true, align: 'center' },
    { prop: 'allUpgrade', label: '升级全部设备', show: true, align: 'center' },
    { prop: 'note', label: '固件说明', show: true, align: 'center' },
])

const tableDeviceData = ref([])
const tableDeviceLoading = ref(false)

const upload = reactive({
    // 是否禁用上传
    isUploading: false,
    // 设置上传的请求头部
    headers: { "wk-user-token": userInfo.getToken() },
    // 上传的地址
    url: import.meta.env.VITE_AXIOS_BASE_URL + platformUploadFileUrl
})
const fileList = ref([])

const data = reactive({
    formData: {
        id: '',
        name: '',
        version: '',
        path: '',
        enabled: true,
        allUpgrade: false,
        note: '',
        productId: id,
    },
    queryParams: {
        productId: id,
        name: '',
        pageNo: 1,
        pageSize: 10,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    queryDeviceParams: {
        productId: id,
        firmwareId: '',
        filedName: 'deviceNo',
        filedValue: '',
        pageNo: 1,
        pageSize: 10,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        name: [{ required: true, message: "固件名称不能为空", trigger: ["blur", "change"] }],
        enabled: [{ required: true, message: "是否启用不能为空", trigger: ["blur", "change"] }],
        allUpgrade: [{ required: true, message: "升级全部设备不能为空", trigger: ["blur", "change"] }],
        path: [{ required: true, message: "固件文件不能为空", trigger: ["blur", "change"] }],
        // 校验version，必须是数字 xx.xx.xx格式
        version: [{ required: true, message: "版本号不能为空", trigger: ["blur", "change"] },
        { pattern: /^\d{1,2}\.\d{1,2}\.\d{1,2}$/, message: "版本号格式不正确，应为xx.xx.xx", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules, queryDeviceParams } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        version: '',
        path: '',
        enabled: true,
        allUpgrade: false,
        note: '',
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


// 文件上传中处理
const handleFileUploadProgress = (event: any, file: any, fileList: any) => {
    upload.isUploading = true
}

// 文件上传成功处理
const handleFileSuccess = (response: any, file: any, fileList: any) => {
    upload.isUploading = false
    formData.value.path = response.data.url
}

const handleDownload = (row: any) => {
    download.download(platformInfo.AppFileDomain + row.path, { ...queryParams.value},`${row.name}_${row.version}.bin`);
}

// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getFirmwareInfo(row.id).then((res: any) => {
        formData.value = res.data
        showUpdate.value = true
        //从path中截取出文件名
        const path = res.data.path
        const index = path.lastIndexOf('/')
        const fileName = path.substring(index + 1)
        fileList.value = [{ name: fileName, url: path }]
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除固件（固件名称 ' + row.name + '）？').then(() => {
        return doFirmwareDelete(row)
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
            doFirmwareCreate(formData.value).then((res: any) => {
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
            doFirmwareUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

// 多选设备删除
const handleDeviceSelectionChange = (val: any) => {
    multipleSelectionDevice.value = val
}

const handleDevice = (row: any) => {
    showDevice.value = true
    queryDeviceParams.value.firmwareId = row.id
    nextTick(() => {
        tableDeviceRef.value?.query()
    })
}

const handleDeviceSearch = () => {
    queryDeviceParams.value.pageNo = 1
    tableDeviceRef.value?.query()
}

const resetDeviceSearch = () => {
    queryDeviceRef.value?.resetFields()
    tableDeviceRef.value?.query()
}

const handleDeviceDeletes = () => {
    if (multipleSelectionDevice.value.length === 0) {
        modal.msgWarning('请选择要解绑的设备')
        return
    }
    modal.confirm('确定解绑选中的设备？').then(() => {
        const ids = multipleSelectionDevice.value.map((item: any) => item.id)
        doFirmwareDeviceDelete({ ids: ids.join(',') }).then(() => {
            resetDeviceSearch()
            modal.msgSuccess('解绑成功')
        })
    })
}

const handleDeviceDelete = (row: any) => {
    modal.confirm('确定解绑设备？').then(() => {
        doFirmwareDeviceDelete({ ids: row.id }).then(() => {
            resetDeviceSearch()
            modal.msgSuccess('解绑成功')
        })
    })
}

const handleDeviceCreate = () => {
    ElMessageBox.prompt('请输入设备通信号或IMEI', '提示', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
    })
        .then(({ value }) => {
            if(!value){
                ElMessage({
                    type: 'warning',
                    message: '请输入设备通信号或IMEI',
                })
                return false
            }
            doFirmwareDeviceInsert({ productId: id, firmwareId: queryDeviceParams.value.firmwareId, input: value }).then(() => {
                resetDeviceSearch()
                modal.msgSuccess('添加成功')
            })
        })
        .catch(() => {
            ElMessage({
                type: 'info',
                message: 'Input canceled',
            })
        })
}

const list = () => {
    tableLoading.value = true
    getFirmwareList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
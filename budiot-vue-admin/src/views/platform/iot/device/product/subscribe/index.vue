<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="12">
            </el-col>
            <el-col :span="12" style="text-align: right; ">
               <div style="display: inline-flex;">
                  
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.product.sub']">添加订阅
                    </el-button>
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
                    <template v-if="item.prop == 'subType'" #default="scope">
                        <span>{{ scope.row.subType?.text }}</span>
                    </template>
                    <template v-if="item.prop == 'enabled'" #default="scope">
                <el-switch v-model="scope.row.enabled" :active-value="true" :inactive-value="false"
                            active-color="green" inactive-color="red" @change="enabledChange(scope.row)" />
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

        <el-dialog title="添加订阅" v-model="showCreate" width="45%" :close-on-click-modal="false">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="160px">
                <el-form-item label="订阅地址" prop="url">
                    <el-input v-model="formData.url" placeholder="请输入订阅地址" />
                </el-form-item>
                <el-form-item label="订阅类别" prop="subType">
                    <el-radio-group v-model="formData.subType">
                        <el-radio v-for="item in subscribeTypes" :key="item.value" :label="item.value">{{ item.text }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="formData.enabled">
                        <el-radio :value="true">启用</el-radio>
                        <el-radio :value="false">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="create">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="修改订阅" v-model="showUpdate" width="45%" :close-on-click-modal="false">
            <el-form ref="updateRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="订阅地址" prop="url">
                    <el-input v-model="formData.url" placeholder="请输入订阅地址" />
                </el-form-item>
                <el-form-item label="订阅类别" prop="subType">
                    <el-radio-group v-model="formData.subType">
                        <el-radio v-for="item in subscribeTypes" :key="item.value" :label="item.value">{{ item.text }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="formData.enabled">
                        <el-radio :value="true">启用</el-radio>
                        <el-radio :value="false">禁用</el-radio>
                    </el-radio-group>
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
import { getSubList, getSubInfo, doSubCreate, doSubUpdate, doSubDelete, doSubEnabled } from '/@/api/platform/iot/product'
import { ElForm } from 'element-plus'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()

const showCreate = ref(false)
const showUpdate = ref(false)
const multipleSelection = ref([])
const subscribeTypes = ref([
    { value: 0, text: '数据上报' },
    { value: 1, text: '事件上报' },
])
const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
    { prop: 'url', label: '订阅地址', show: true, fixed: 'left' },
    { prop: 'subType', label: '订阅类别', show: true },
    { prop: 'enabled', label: '启用状态', show: true }
])
const data = reactive({
    formData: {
        id: '',
        url: '',
        subType: 0,
        enabled: true,
        productId: id
    },
    queryParams: {
        productId: id,
        pageNo: 1,
        pageSize: 10,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        // 校验url 为 http https开头
        url: [
            { required: true, message: '请输入订阅地址', trigger: ['blur','change'] },
            { pattern: /^(http|https):\/\/.*/, message: '请输入正确的订阅地址', trigger: ['blur','change'] }
        ],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        url: '',
        subType: 0,
        enabled: true,
        productId: id
    }
    formEl?.resetFields()
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
    getSubInfo(row.id).then((res: any) => {
        formData.value = res.data
        formData.value.subType = res.data.subType.value
        showUpdate.value = true
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除订阅（订阅地址 ' + row.url + '）？').then(() => {
        return doSubDelete(row)
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
            doSubCreate(formData.value).then((res: any) => {
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
            doSubUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

const enabledChange = (row: any) => {
    doSubEnabled(row).then((res: any) => {
        modal.msgSuccess(res.msg)
        list()
    }).catch(() => {
        setTimeout(() => {
            row.enabled = !row.enabled
        }, 300)
    })
}

const list = () => {
    tableLoading.value = true
    getSubList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
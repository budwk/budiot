<template>
    <div class="app-container">
        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button plain type="primary" icon="Plus" @click="handleCreate"
                    v-permission="['iot.config.protocol.create']">新增
                </el-button>
            </el-col>
            <right-toolbar :columns="columns" @quickSearch="quickSearch" :quickSearchShow="true"
                quickSearchPlaceholder="通过名称搜索" />
        </el-row>

        <el-table v-loading="tableLoading" :data="tableData" row-key="id">
            <template v-for="(item, idx) in columns" :key="idx">
                <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
                    :show-overflow-tooltip="true" :align="item.align" :width="item.width">
                    <template v-if="item.prop == 'createdAt'" #default="scope">
                        <span>{{ formatTime(scope.row.createdAt) }}</span>
                    </template>
                </el-table-column>
            </template>
            <el-table-column fixed="right" header-align="center" align="center" label="操作"
                class-name="small-padding fixed-width" width="150">
                <template #default="scope">
                    <div>
                        <el-tooltip content="修改" placement="top">
                            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                                v-permission="['iot.config.protocol.update']"></el-button>
                        </el-tooltip>
                        <el-tooltip content="删除" placement="top">
                            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                                v-permission="['iot.config.protocol.delete']"></el-button>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <el-row>
            <pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo"
                v-model:limit="queryParams.pageSize" @pagination="list" />
        </el-row>
        <el-dialog title="新增设备协议" v-model="showCreate" width="50%">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="120px">
                <el-row :gutter="10">
                    <el-col :span="24">
                        <el-form-item label="协议名称" prop="name">
                            <el-input v-model="formData.name" placeholder="协议名称" maxlength="32" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="协议标识" prop="code">
                            <el-input v-model="formData.code" placeholder="协议标识" maxlength="32" />
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="create">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="修改设备协议" v-model="showUpdate" width="50%">
            <el-form ref="updateRef" :model="formData" :rules="formRules" label-width="120px">
                <el-row :gutter="10">
                    <el-col :span="24">
                        <el-form-item label="协议名称" prop="name">
                            <el-input v-model="formData.name" placeholder="协议名称" maxlength="32" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="协议标识" prop="code">
                            <el-input v-model="formData.code" placeholder="协议标识" maxlength="32" />
                        </el-form-item>
                    </el-col>
                </el-row>
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
<script setup lang="ts" name="platform-iot-config-protocol">
import { onMounted, reactive, ref, toRefs } from 'vue'
import { ElForm } from 'element-plus'
import { doCreate, doUpdate, getInfo, getList, doDelete, } from '/@/api/platform/iot/protocol'
import modal from '/@/utils/modal'


const queryRef = ref<InstanceType<typeof ElForm>>()
const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const tableLoading = ref(false)
const tableData = ref([])
const showCreate = ref(false)
const showUpdate = ref(false)

const data = reactive({
    formData: {
        id: '',
        name: '',
        code: ''
    },
    queryParams: {
        name: '',
        pageNo: 1,
        pageSize: 10,
        totalCount: 0,
        pageOrderName: 'createdAt',
        pageOrderBy: 'descending'
    },
    formRules: {
        name: [{ required: true, message: "协议名称", trigger: "blur" }],
        code: [{ required: true, message: "协议标识需以字母开头、字母数字_-组合，结尾不能为_-", trigger: "blur", pattern: /^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$/}],
    },
})
const { queryParams, formData, formRules } = toRefs(data)

const columns = ref([
    { prop: 'name', label: `协议名称`, show: true, fixed: false },
    { prop: 'code', label: `协议标识`, show: true, fixed: false },
    { prop: 'createdAt', label: `创建时间`, show: true, fixed: false }
])

// 重置表单
const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        code: ''
    }
    formEl?.resetFields()
}

// 查询表格
const list = () => {
    tableLoading.value = true
    getList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list as never
        queryParams.value.totalCount = res.data.totalCount
    })
}


onMounted(() => {
    list()
})

// 快速搜索&刷新
const quickSearch = (data: any) => {
    queryParams.value.name = data.keyword
    queryParams.value.pageNo = 1
    list()
}

// 重置查询
const resetSearch = () => {
    queryParams.value.pageNo = 1
    list()
}


// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getInfo(row.id).then((res: any) => {
        formData.value = res.data
        showUpdate.value = true
    })
}

// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除 ' + row.name + ' ?').then(() => {
        return doDelete(row.id)
    }).then(() => {
        list()
        modal.msgSuccess('删除成功')
    }).catch(() => { })
}


// 提交新增
const create = () => {
    if (!createRef.value) return
    createRef.value.validate((valid) => {
        if (valid) {
            doCreate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showCreate.value = false
                resetSearch()
            })
        }
    })
}

// 提交修改
const update = () => {
    if (!updateRef.value) return
    updateRef.value.validate((valid) => {
        if (valid) {
            doUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                resetSearch()
            })
        }
    })
}
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>

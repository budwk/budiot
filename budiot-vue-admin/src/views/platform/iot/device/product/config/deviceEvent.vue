<template>
    <div>
        <el-row :gutter="10" class="mb8">
            <el-col :span="14">
            <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                <el-form-item>
                    <el-input
    v-model="queryParams.name" placeholder="请输入事件名称/事件标识" clearable style="width: 380px"
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
                
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.product.config']">新增
                    </el-button>
               </div>
            </el-col>
        </el-row>
        <el-row class="mb8">

<el-table ref="tableRef" v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50" />
    <template v-for="(item, idx) in columns" :key="idx">
        <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
            :show-overflow-tooltip="true" :align="item.align" :width="item.width">
            <template v-if="item.prop == 'createdAt'" #default="scope">
                <span>{{ formatTime(scope.row.createdAt) }}</span>
            </template>
            <template v-if="item.prop == 'eventType'" #default="scope">
                {{ scope.row.eventType?.text }}
            </template>
        </el-table-column>
    </template>
    <el-table-column fixed="right" header-align="center" align="center" label="操作"
        class-name="small-padding fixed-width" width="150">
        <template #default="scope">
            <div>
                <el-tooltip content="修改" placement="top">
                    <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                        v-permission="['iot.device.product.config']"></el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                    <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                        v-permission="['iot.device.product.config']"></el-button>
                </el-tooltip>
            </div>
        </template>
    </el-table-column>
</el-table>
<pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
@pagination="list" />
</el-row>

<el-dialog title="新增事件" v-model="showCreate" width="45%" :close-on-click-modal="false">
<el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
    <el-form-item label="事件名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入事件名称" />
    </el-form-item>
    <el-form-item label="事件标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入事件唯一标识" />
    </el-form-item>
    <el-form-item label="事件类型" prop="eventType">
        <el-radio-group v-model="formData.eventType">
            <el-radio v-for="item in eventTypes" :key="item.value" :value="item.value">{{ item.text }}</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="事件说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入事件说明" />
    </el-form-item>
</el-form>
<template #footer>
    <div class="dialog-footer">
        <el-button type="primary" @click="create">确 定</el-button>
        <el-button @click="showCreate = false">取 消</el-button>
    </div>
</template>
</el-dialog>

<el-dialog title="修改事件" v-model="showUpdate" width="45%" :close-on-click-modal="false">
<el-form ref="updateRef" :model="formData" :rules="formRules" label-width="100px">
    <el-form-item label="事件名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入事件名称" />
    </el-form-item>
    <el-form-item label="事件标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入事件唯一标识" />
    </el-form-item>
    <el-form-item label="事件类型" prop="eventType">
        <el-radio-group v-model="formData.eventType">
            <el-radio v-for="item in eventTypes" :key="item.value" :value="item.value">{{ item.text }}</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="事件说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入事件说明" />
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
<script setup lang="ts" name="deviceEvent">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { useRoute } from "vue-router"
import { getEventList, getEventInfo, doEventCreate, doEventUpdate, doEventDelete
 } from '/@/api/platform/iot/product'
 import sortable from 'sortablejs'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()
const tableRef = ref<InstanceType<typeof ElTable>>()

const showCreate = ref(false)
const showUpdate = ref(false)
const showSort = ref(false)
const multipleSelection = ref([])

const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
    { prop: 'name', label: '事件名称', show: true, fixed: 'left' },
    { prop: 'code', label: '事件标识', show: true },
    { prop: 'eventType', label: '事件类型', show: true },
    { prop: 'note', label: '事件说明', show: true },
])
const eventTypes = ref([
    { value: 0, text: '信息' },
    { value: 1, text: '报警' },
    { value: 2, text: '故障' },
])

const data = reactive({
    formData: {
        id: '',
        name: '',
        code: '',
        eventType: 0,
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
    formRules: {
        name: [{ required: true, message: "事件名称不能为空", trigger: ["blur", "change"] }],
        code: [{ required: true, message: "事件标识需两个字符以上，并以字母开头、字母数字_-组合，结尾不能为_-", pattern: /^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$/, trigger: ["blur", "change"] }],
        eventType: [{ required: true, message: "事件类型不能为空", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        code: '',
        eventType: 0,
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


// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getEventInfo(row.id).then((res: any) => {
        formData.value = res.data
        formData.value.eventType = res.data.eventType.value
        showUpdate.value = true
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除事件（事件名称 ' + row.name + '）？').then(() => {
        return doEventDelete(row)
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
            doEventCreate(formData.value).then((res: any) => {
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
            doEventUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

const list = () => {
    tableLoading.value = true
    getEventList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
<template>
    <div>
        <el-row :gutter="10" class="mb8">
            <el-col :span="14">
            <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                <el-form-item>
                    <el-input
    v-model="queryParams.name" placeholder="请输入指令名称/指令标识" clearable style="width: 380px"
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
                                :action="API_IOT_DEVICE_PRODUCT_CMD_IMPORT"
                                :data="queryParams"
                                :cover="true"
                                temp-url="/tpl/template_product_cmd.json"
                                suffix="json,txt"
                                @refresh="handleSearch"
                                style="margin-right: 12px"
                            />
                            <export
                v-permission="['iot.device.product.device.config']"
                btn-text="导出"
                title="导出指令"
                :action="API_IOT_DEVICE_PRODUCT_CMD_EXPORT"
                :columns="[]"
                suffix="json"
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

<el-table ref="tableRef" v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
    <el-table-column type="expand" fixed>
        <template #default="props">
            <el-table :data="props.row.attrList" style="width: 98%;margin-left: 10px;">
                <el-table-column label="参数名称" prop="name" />
                <el-table-column label="参数标识" prop="code" />
                <el-table-column label="数据类型" prop="dataType.text" />
                <el-table-column label="默认值" prop="defaultValue" />
            </el-table>
        </template>
    </el-table-column>
    <template v-for="(item, idx) in columns" :key="idx">
        <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
            :show-overflow-tooltip="true" :align="item.align" :width="item.width">
            <template v-if="item.prop == 'createdAt'" #default="scope">
                <span>{{ formatTime(scope.row.createdAt) }}</span>
            </template>
            <template v-if="item.prop == 'enabled'" #default="scope">
                <el-switch v-model="scope.row.enabled" :active-value="true" :inactive-value="false"
                            active-color="green" inactive-color="red" @change="enabledChange(scope.row)" />
            </template>
        </el-table-column>
    </template>
    <el-table-column fixed="right" header-align="center" align="center" label="操作"
        class-name="small-padding fixed-width" width="150">
        <template #default="scope">
            <div>
                <el-tooltip content="修改" placement="top">
                    <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                        v-permission="['iot.device.product.device.config']"></el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                    <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                        v-permission="['iot.device.product.device.config']"></el-button>
                </el-tooltip>
            </div>
        </template>
    </el-table-column>
</el-table>
<pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
@pagination="list" />
</el-row>

<el-dialog title="新增指令" v-model="showCreate" width="65%" :close-on-click-modal="false">
<el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
    <el-form-item label="指令名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入指令名称" />
    </el-form-item>
    <el-form-item label="指令标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入指令唯一标识" />
    </el-form-item>
    <el-form-item label="是否启用" prop="enabled">
        <el-radio-group v-model="formData.enabled">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">禁用</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="指令参数" class="label-font-weight">
        <el-table :data="formData.attrList" row-key="_id">
            <el-table-column label="参数名称">
                <template #default="scope">
                    <el-input v-model="scope.row.name" placeholder="请输入参数名称" />
                </template>    
            </el-table-column>
            <el-table-column label="参数标识">
                <template #default="scope">
                    <el-input v-model="scope.row.code" placeholder="请输入参数标识" />
                </template>
            </el-table-column>
            <el-table-column label="数据类型">
                <template #default="scope">
                    <el-select v-model="scope.row.dataType" placeholder="请选择数据类型">
                        <el-option v-for="item in dataTypes" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </template>
            </el-table-column>
            <el-table-column label="默认值">
                <template #default="scope">
                    <el-input v-model="scope.row.defaultValue" :placeholder="scope.row.dataType==5?'请输入格式如 1=A,2=B':'请输入默认值'" />
                </template>    
            </el-table-column>
            <el-table-column fixed="right" header-align="center" align="center" label="操作"
                class-name="small-padding fixed-width" width="150">
                <template #default="scope">
                    <div>
                        <el-tooltip content="删除" placement="top">
                            <el-button link type="danger" icon="Delete" @click="handleRowDelete(scope.row)"
                                v-permission="['iot.device.product.device.config']"></el-button>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>`
        </el-table>
        <el-button plain icon="Plus" @click="handleRowAdd" style="padding: 10px 10px;margin-top: 10px;">新增参数</el-button>
    </el-form-item>
    <el-form-item label="指令说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入指令说明" />
    </el-form-item>
</el-form>
<template #footer>
    <div class="dialog-footer">
        <el-button type="primary" @click="create">确 定</el-button>
        <el-button @click="showCreate = false">取 消</el-button>
    </div>
</template>
</el-dialog>

<el-dialog title="修改指令" v-model="showUpdate" width="65%" :close-on-click-modal="false">
<el-form ref="updateRef" :model="formData" :rules="formRules" label-width="100px">
    <el-form-item label="指令名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入指令名称" />
    </el-form-item>
    <el-form-item label="指令标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入指令唯一标识" />
    </el-form-item>
    <el-form-item label="是否启用" prop="enabled">
        <el-radio-group v-model="formData.enabled">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">禁用</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="指令参数" class="label-font-weight">
        <el-table :data="formData.attrList" row-key="_id">
            <el-table-column label="参数名称">
                <template #default="scope">
                    <el-input v-model="scope.row.name" placeholder="请输入参数名称" />
                </template>    
            </el-table-column>
            <el-table-column label="参数标识">
                <template #default="scope">
                    <el-input v-model="scope.row.code" placeholder="请输入参数标识" />
                </template>
            </el-table-column>
            <el-table-column label="数据类型">
                <template #default="scope">
                    <el-select v-model="scope.row.dataType" placeholder="请选择数据类型">
                        <el-option v-for="item in dataTypes" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </template>
            </el-table-column>
            <el-table-column label="默认值">
                <template #default="scope">
                    <el-input v-model="scope.row.defaultValue" :placeholder="scope.row.dataType==5?'请输入格式如 1=A,2=B':'请输入默认值'" />
                </template>    
            </el-table-column>
            <el-table-column fixed="right" header-align="center" align="center" label="操作"
                class-name="small-padding fixed-width" width="150">
                <template #default="scope">
                    <div>
                        <el-tooltip content="删除" placement="top">
                            <el-button link type="danger" icon="Delete" @click="handleRowDelete(scope.row)"
                                v-permission="['iot.device.product.device.config']"></el-button>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>`
        </el-table>
        <el-button plain icon="Plus" @click="handleRowAdd" style="padding: 10px 10px;margin-top: 10px;">新增参数</el-button>
    </el-form-item>
    <el-form-item label="指令说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入指令说明" />
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
<script setup lang="ts" name="deviceCmd">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable, FormRules } from 'element-plus'
import { useRoute } from "vue-router"
import { getCmdList, getCmdInfo, doCmdCreate, doCmdUpdate, doCmdDelete, doCmdEnabled,
    API_IOT_DEVICE_PRODUCT_CMD_EXPORT, API_IOT_DEVICE_PRODUCT_CMD_IMPORT
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
    { prop: 'name', label: '指令名称', show: true, fixed: 'left' },
    { prop: 'code', label: '指令标识', show: true },
    { prop: 'note', label: '指令说明', show: true },
    { prop: 'enabled', label: '是否启用', show: true },
])
const dataTypes = ref([
    { value: 1, text: '整型' },
    { value: 2, text: '浮点型' },
    { value: 3, text: '字符串' },
    { value: 4, text: '布尔型' },
    { value: 5, text: '枚举型' },
    { value: 6, text: '时间戳' },
    { value: 7, text: '日期时间' },
    { value: 8, text: 'IP地址' },
])
const data = reactive({
    formData: {
        id: '',
        name: '',
        code: '',
        enabled: false,
        note: '',
        productId: id,
        attrList: [],
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
        name: [{ required: true, message: "指令名称不能为空", trigger: ["blur", "change"] }],
        code: [{ required: true, message: "指令标识需两个字符以上，并以字母开头、字母数字_-组合，结尾不能为_-", pattern: /^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$/, trigger: ["blur", "change"] }],
        enabled: [{ required: true, message: "是否启用不能为空", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        code: '',
        enabled: true,
        note: '',
        productId: id,
        attrList: [],
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

const enabledChange = (row: any) => {
    doCmdEnabled(row).then((res: any) => {
        modal.msgSuccess(res.msg)
        list()
    }).catch(() => {
        setTimeout(() => {
            row.enabled = !row.enabled
        }, 300)
    })
}

const handleRowDelete = (row: any) => {
    const index = formData.value.attrList.findIndex((item: any) => item._id === row._id)
    formData.value.attrList.splice(index, 1)
}

const handleRowAdd = () => {
    formData.value.attrList.push({ _id: new Date().getTime() , productId: id, name: '', code: '', dataType: 1, defaultValue: '' } as never)
}

// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    handleRowAdd()
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getCmdInfo(row.id).then((res: any) => {
        formData.value = res.data
        formData.value.attrList.forEach((item: any) => {
            item['_id'] = item.id
            item.dataType = item.dataType.value
        })
        console.log(formData.value)
        showUpdate.value = true
    })
}


// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除指令（指令名称 ' + row.name + '）？').then(() => {
        return doCmdDelete(row)
    }).then(() => {
        queryParams.value.pageNo = 1
        list()
        modal.msgSuccess('删除成功')
    }).catch(() => { })
}

const validatorAttrList = (attrList: any) => {
    let flag = true
    attrList.forEach((item: any) => {
        if (!item.name || !item.code ) {
            modal.msgError('指令参数：参数名称、参数标识不能为空')
            flag = false
        }
        // = 左侧为字母数组，= 右侧可以为中文
        if(item.dataType == 5 && !/^\w+=[\w\u4e00-\u9fa5]+(,\w+=[\w\u4e00-\u9fa5]+)*$/.test(item.defaultValue)){
            modal.msgError('指令参数：枚举型默认值不能为空,且格式为 1=A,2=B ')
            flag = false
        }
    })
    return flag
}

// 提交新增
const create = () => {
    if (!createRef.value) return
    createRef.value.validate((valid) => {
        if (valid) {
            if(!validatorAttrList(formData.value.attrList)){
                return
            }
            doCmdCreate(formData.value).then((res: any) => {
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
            if(!validatorAttrList(formData.value.attrList)){
                return
            }
            doCmdUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

const list = () => {
    tableLoading.value = true
    getCmdList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
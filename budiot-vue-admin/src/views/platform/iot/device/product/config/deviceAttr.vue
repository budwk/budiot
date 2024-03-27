<template>
    <div>
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
                title="导出参数"
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

<el-table ref="tableRef" class="drag_table" v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50" />
    <el-table-column width="50" fixed>
        <icon name="fa fa-arrows" size="14" style="cursor: pointer; color: gray;"/>
    </el-table-column>
    <template v-for="(item, idx) in columns" :key="idx">
        <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
            :show-overflow-tooltip="true" :align="item.align" :width="item.width">
            <template v-if="item.prop == 'createdAt'" #default="scope">
                <span>{{ formatTime(scope.row.createdAt) }}</span>
            </template>
            <template v-if="item.prop == 'attrType'" #default="scope">
                {{ scope.row.attrType?.text }}
            </template>
            <template v-if="item.prop == 'dataType'" #default="scope">
                {{ scope.row.dataType?.text }}
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

<el-dialog title="新增参数" v-model="showCreate" width="45%">
<el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
    <el-form-item label="参数名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入参数名称" />
    </el-form-item>
    <el-form-item label="参数标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入参数唯一标识" />
    </el-form-item>
    <el-form-item label="参数类型" prop="attrType">
        <el-radio-group v-model="formData.attrType">
            <el-radio v-for="item in attrTypes" :key="item.value" :value="item.value">{{ item.text }}</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="数据类型" prop="dataType">
        <el-radio-group v-model="formData.dataType" placeholder="请选择数据类型">
            <el-radio v-for="item in dataTypes" :key="item.value" :label="item.text" :value="item.value" />
        </el-radio-group>
    </el-form-item>
    <el-form-item v-if="formData.dataType==2" label="小数位数" prop="scale">
        <el-input-number v-model="formData.scale" :min="0" :precision="0" placeholder="小数位数"/>
    </el-form-item>
    <el-form-item v-if="[1, 2, 3].includes(formData.dataType)" label="数据单位" prop="unit">
        <el-select v-model="formData.unit" placeholder="请选择数据单位" clearable>
            <el-option label="无" value=""/>
            <el-option v-for="item in units" :key="item.value" :label="item.text+'('+item.value+')'" :value="item.value" />
        </el-select>
    </el-form-item>
    <el-form-item v-if="formData.dataType === 5" label="枚举类型" prop="ext" required class="label-font-weight">
        <el-row v-for="(el, index) in formData.ext" :key="index">
            <el-col :span="7">
                <el-form-item prop="ext" :rules="getDynamicRule('value', index)">
                    <el-input v-model="el.value" placeholder="参数值" />
                </el-form-item>
            </el-col>
            <el-col :span="10" :offset="1">
                <el-form-item prop="ext" :rules="getDynamicRule('text', index)">
                    <el-input v-model="el.text" placeholder="参数描述" />
                </el-form-item>
            </el-col>
            <el-col :span="5" :offset="1">
                <el-button plain @click="handleDeleteRow(index)">删除</el-button>
            </el-col>
        </el-row>
        <el-row>
            <el-button plain icon="Plus" @click="handleAppendRow()"/>
        </el-row>
    </el-form-item>
    <el-form-item label="参数说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入参数说明" />
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
    <el-form-item label="参数名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入参数名称" />
    </el-form-item>
    <el-form-item label="参数标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入参数唯一标识" />
    </el-form-item>
    <el-form-item label="参数类型" prop="attrType">
        <el-radio-group v-model="formData.attrType">
            <el-radio v-for="item in attrTypes" :key="item.value" :value="item.value">{{ item.text }}</el-radio>
        </el-radio-group>
    </el-form-item>
    <el-form-item label="数据类型" prop="dataType">
        <el-radio-group v-model="formData.dataType" placeholder="请选择数据类型">
            <el-radio v-for="item in dataTypes" :key="item.value" :label="item.text" :value="item.value" />
        </el-radio-group>
    </el-form-item>
    <el-form-item v-if="formData.dataType==2" label="小数位数" prop="scale">
        <el-input-number v-model="formData.scale" :min="0" :precision="0" placeholder="小数位数"/>
    </el-form-item>
    <el-form-item v-if="[1, 2, 3].includes(formData.dataType)" label="数据单位" prop="unit">
        <el-select v-model="formData.unit" placeholder="请选择数据单位" clearable>
            <el-option label="无" value=""/>
            <el-option v-for="item in units" :key="item.value" :label="item.text+'('+item.value+')'" :value="item.value" />
        </el-select>
    </el-form-item>
    <el-form-item v-if="formData.dataType === 5" label="枚举类型" prop="ext" required class="label-font-weight">
        <el-row v-for="(el, index) in formData.ext" :key="index">
            <el-col :span="7">
                <el-form-item prop="ext" :rules="getDynamicRule('value', index)">
                    <el-input v-model="el.value" placeholder="参数值" />
                </el-form-item>
            </el-col>
            <el-col :span="10" :offset="1">
                <el-form-item prop="ext" :rules="getDynamicRule('text', index)">
                    <el-input v-model="el.text" placeholder="参数描述" />
                </el-form-item>
            </el-col>
            <el-col :span="5" :offset="1">
                <el-button plain @click="handleDeleteRow(index)">删除</el-button>
            </el-col>
        </el-row>
        <el-row>
            <el-button plain icon="Plus" @click="handleAppendRow()"/>
        </el-row>
    </el-form-item>
    <el-form-item label="参数说明" prop="note">
        <el-input v-model="formData.note" placeholder="请输入参数说明" />
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
import { ElForm, ElTable } from 'element-plus'
import { useRoute } from "vue-router"
import { getAttrList, getAttrInfo, doAttrCreate, doAttrUpdate, doAttrDelete, doAttrSort,
    API_IOT_DEVICE_PRODUCT_ATTR_IMPORT,
    API_IOT_DEVICE_PRODUCT_ATTR_EXPORT
 } from '/@/api/platform/iot/product'
 import sortable from 'sortablejs'
import { t } from '@wangeditor/editor'

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
    { prop: 'name', label: '参数名称', show: true, fixed: 'left' },
    { prop: 'code', label: '参数标识', show: true },
    { prop: 'attrType', label: '参数类型', show: true },
    { prop: 'dataType', label: '数据类型', show: true },
    { prop: 'scale', label: '小数位数', show: false },
    { prop: 'unit', label: '单位', show: false },
    { prop: 'note', label: '参数说明', show: true },
])
const attrTypes = ref([
    { value: 0, text: '指标' },
    { value: 1, text: '状态' },
    { value: 2, text: '信息' },
    { value: 3, text: '其他' },
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
const units = ref([
    { value: 'm³', text: '立方米' },
    { value: 'mm', text: '毫升' },
    { value: 'ss', text: '秒' },
    { value: '°', text: '温度' },
    { value: 'Pa', text: '压力' },
    { value: 'N', text: '压强' },
    { value: 'V', text: '电压' },
    { value: '元', text: '金额' },
])
const validateExt = (rule: any, value: any, callback: any) => {
  if (value.length === 0 && formData.value.dataType === 5) {
    callback(new Error('请配置枚举值'))
  } else {
    callback()
  }
}
const data = reactive({
    formData: {
        id: '',
        name: '',
        code: '',
        attrType: 0,
        dataType: 0,
        scale: 0,
        unit: '',
        productId: '',
        ext: [],
        note: '',
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
        name: [{ required: true, message: "参数名称不能为空", trigger: ["blur", "change"] }],
        code: [{ required: true, message: "参数标识需两个字符以上，并以字母开头、字母数字_-组合，结尾不能为_-", pattern: /^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$/, trigger: ["blur", "change"] }],
        attrType: [{ required: true, message: "参数类型不能为空", trigger: ["blur", "change"] }],
        dataType: [{ required: true, message: "数据类型不能为空", trigger: ["blur", "change"] }],
        ext: [{ validator: validateExt, trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        code: '',
        attrType: 0,
        dataType: 1,
        scale: 0,
        unit: '',
        productId: id,
        ext: [],
        note: '',
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

const getDynamicRule = (key: string, index: number) =>{
    return {
        required: true,
        message: "请输入",
        validator: (rule, value, callback) => {
            if (formData.value.ext[index][key] === "") {
                callback("请输入")
            } else {
                callback()
            }
        }
    }
}
const handleAppendRow = () => {
   formData.value.ext.push({ value: "", text: "" })
}

const handleDeleteRow = (index: number) => {
    formData.value.ext.splice(index, 1)
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
        formData.value.attrType = res.data.attrType.value
        formData.value.dataType = res.data.dataType.value
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

const drag = () => {
    const el = tableRef.value?.$el.querySelector('.drag_table .el-table__body-wrapper tbody')
    sortable.create(el, {
        handle: '.drag_table .el-table__row',
        animation: 120,
        onEnd: (event: any) => {
            const oldIndex = event.oldIndex
            const newIndex = event.newIndex
            const movedItem = tableData.value.splice(oldIndex, 1)[0]
            tableData.value.splice(newIndex, 0, movedItem)
            const ids = tableData.value.map((item: any) => item.id)
            doAttrSort({ ids: ids, pageNo: queryParams.value.pageNo, pageSize: queryParams.value.pageSize }).then(() => {
                modal.msgSuccess('排序成功')
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
        drag()
    })
}

onMounted(() => {
    list()
})
</script>
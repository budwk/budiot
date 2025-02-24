<template>
    <div class="app-container">
        <el-row justify="space-between">
            <el-col :span="20">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="88px">
                    <el-form-item class="label-font-weight">
                        {{ props.spaceName ? props.spaceName : '全部空间' }}
                    </el-form-item>
                    <el-form-item label="场景名称" prop="name">
                        <el-input
                            v-model="queryParams.name"
                            placeholder="请输入场景名称"
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
            <el-col :span="4" style="text-align: right">
                <el-button plain type="success" @click="handleCreate" v-permission="['iot.device.scene.create']">新建场景 </el-button>
            </el-col>
        </el-row>
        <el-row :gutter="10" class="mb8">
            <pro-table-list
                ref="tableRef"
                :url="API_IOT_DEVICE_SCENE_LIST"
                :queryParams="queryParams"
                :pageSize="10"
                :lazy="false"
                style="width: 100%"
            >
                <el-table-column prop="name" label="场景名称" align="center" />
                <el-table-column prop="enabled" label="是否启用" align="center">
                    <template #default="scope">
                        <el-switch
                            v-model="scope.row.enabled"
                            :active-value="true"
                            :inactive-value="false"
                            active-color="green"
                            inactive-color="red"
                            @change="enabledChange(scope.row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="hasTimeRange" label="时段限制" align="center">
                    <template #default="scope">
                        <span v-if="scope.row.hasTimeRange"> {{ scope.row.timeStart }} 至 {{ scope.row.timeEnd }} </span>
                        <span v-else> 无 </span>
                    </template>
                </el-table-column>
                <el-table-column prop="note" label="场景说明" align="center" />

                <el-table-column fixed="right" header-align="center" align="center" label="操作" class-name="small-padding fixed-width" width="150">
                    <template #default="scope">
                        <div>
                            <el-tooltip content="查看日志" placement="top">
                                <el-button
                                    link
                                    type="primary"
                                    icon="View"
                                    @click="handleDetail(scope.row.id)"
                                    v-permission="['iot.device.scene']"
                                ></el-button>
                            </el-tooltip>
                            <el-tooltip content="修改" placement="top">
                                <el-button
                                    link
                                    type="primary"
                                    icon="Edit"
                                    @click="handleUpdate(scope.row)"
                                    v-permission="['iot.device.scene.update']"
                                ></el-button>
                            </el-tooltip>
                            <el-tooltip content="删除" placement="top">
                                <el-button
                                    link
                                    type="danger"
                                    icon="Delete"
                                    @click="handleDelete(scope.row)"
                                    v-permission="['iot.device.scene.delete']"
                                ></el-button>
                            </el-tooltip>
                        </div>
                    </template>
                </el-table-column>
            </pro-table-list>
        </el-row>

        <el-dialog title="新建场景" v-model="showCreate" width="85%">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="90px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="场景名称" prop="name">
                            <el-input v-model="formData.name" placeholder="场景名称" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="是否启用" prop="enabled">
                            <el-switch
                                v-model="formData.enabled"
                                :active-value="true"
                                :inactive-value="false"
                                active-color="green"
                                inactive-color="red"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-card style="max-width: 100%" shadow="never">
                            <template #header>
                                <div class="card-header">
                                    <span>条件</span>
                                    <el-button link type="primary" @click="addTerm" icon="Plus" style="padding: 1 5px"></el-button>
                                </div>
                            </template>
                            <el-row v-if="formData.terms && formData.terms.length>0">
                                <el-col :span="24" v-for="(t, i) in formData.terms" :key="'t_' + i">
                                    <el-form-item :label="'条件' + (i + 1)" class="label-font-weight">
                                        <el-button
                                            type="success"
                                            plain
                                            @click="handleShowSelect(t,'term',i)"
                                            style="padding-left: 5px"
                                        >
                                            <span v-if="t.productName && t.deviceName">{{ t.productName }} - {{ t.deviceName }}</span>
                                            <span v-else>选择设备</span>
                                        </el-button>
                                        <el-select v-model="t.paramName" placeholder="上报参数" filterable style="width: 180px; padding-left: 5px">
                                            <el-option v-for="c in getAttrList(t.productId)" :key="c.id" :label="getAttrLabel(c)" :value="c.code" />
                                        </el-select>
                                        <el-select v-model="t.termType" placeholder="条件" style="width: 120px; padding-left: 5px">
                                            <el-option v-for="c in SceneTermType" :key="c.value" :label="c.label" :value="c.value" />
                                        </el-select>
                                        <el-input
                                            v-if="t.termType !== 'btw' && t.termType !== 'nbtw'"
                                            v-model="t.paramValue"
                                            placeholder="值"
                                            style="width: 120px; padding-left: 5px"
                                        />
                                        <el-input
                                            v-if="t.termType == 'btw' || t.termType == 'nbtw'"
                                            v-model="t.value1"
                                            placeholder="值1"
                                            style="width: 120px; padding-left: 5px"
                                        />
                                        <span v-if="t.termType == 'btw' || t.termType == 'nbtw'" style="padding-left: 5px">至</span>
                                        <el-input
                                            v-if="t.termType == 'btw' || t.termType == 'nbtw'"
                                            v-model="t.value2"
                                            placeholder="值2"
                                            style="width: 120px; padding-left: 5px"
                                        />
                                        <span>
                                            <el-tooltip content="持续时长（单位：秒，0为不设置）" placement="top">
                                                <span style="padding-left: 2px">持续时长<i class="fa fa-question-circle-o"></i></span>
                                                <el-icon name="el-icon-info" style="color: #409eff"></el-icon>
                                            </el-tooltip>
                                        </span>
                                        <el-select
                                            v-model="t.duration"
                                            placeholder="持续时长"
                                            filterable
                                            allow-create
                                            :min="0"
                                            style="width: 120px; padding-left: 5px"
                                        >
                                            <el-option label="不设置" :value="0"></el-option>
                                            <el-option label="1分钟" :value="60"></el-option>
                                            <el-option label="3分钟" :value="180"></el-option>
                                            <el-option label="5分钟" :value="300"></el-option>
                                            <el-option label="10分钟" :value="600"></el-option>
                                            <el-option label="15分钟" :value="900"></el-option>
                                            <el-option label="20分钟" :value="1200"></el-option>
                                            <el-option label="25分钟" :value="1500"></el-option>
                                            <el-option label="30分钟" :value="1800"></el-option>
                                            <el-option label="45分钟" :value="2700"></el-option>
                                            <el-option label="1小时" :value="3600"></el-option>
                                        </el-select>
                                        <el-button
                                            type="danger"
                                            link
                                            icon="Delete"
                                            @click="() => formData.terms.splice(i, 1)"
                                            style="padding-left: 5px"
                                        ></el-button>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                            <el-row v-else>
                                <el-col :span="24">
                                    <el-form-item>
                                        <span class="el-table__empty-text">暂无条件</span>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-card>
                    </el-col>
                    <el-col :span="24" style="padding-top: 20px">
                        <el-card style="max-width: 100%" shadow="never">
                            <template #header>
                                <div class="card-header">
                                    <span>动作</span>
                                    <el-button link type="primary" @click="addAction" icon="Plus" style="padding: 1 5px"></el-button>
                                </div>
                            </template>
                            <el-row v-if="formData.actions && formData.actions.length>0">
                                <el-col :span="24" v-for="(a, i) in formData.actions" :key="'a_' + i">
                                    <el-form-item :label="'动作' + (i + 1)"  class="label-font-weight">
                                            <el-row>
                                                <el-col :span="24"> 
                                                    <el-dropdown
                                                        @command="
                                                            (command) => {
                                                                handleActionCommand(a, command)
                                                            }
                                                        "
                                                    >
                                                        <el-button type="primary" plain>
                                                            {{ getAactionLabel(a.actionType) }} <i class="el-icon-arrow-down el-icon--right"></i>
                                                        </el-button>
                                                        <template #dropdown>
                                                            <el-dropdown-item v-for="item in SceneActionType" :key="item.value" :command="item.value">
                                                                {{ item.label }}
                                                            </el-dropdown-item>
                                                        </template>
                                                    </el-dropdown>
                                                    <span v-if="a.actionType=='CMD'" style="padding-left: 5px;">
                                                        <el-button
                                                            type="success"
                                                            plain
                                                            @click="handleShowSelect(a,'action',i)"
                                                            style="padding-left: 5px"
                                                        >
                                                            <span v-if="a.productName && a.deviceName">{{ a.productName }} - {{ a.deviceName }}</span>
                                                            <span v-else>选择设备</span>
                                                        </el-button>
                                                        <el-select
                                                            v-model="a.cmdCode"
                                                            placeholder="选择指令"
                                                            @change="actionCmdChange(a)"
                                                            filterable
                                                            style="width: 180px; padding-left: 5px"
                                                        >
                                                            <el-option v-for="c in getCmdList(a.productId)" :key="c.id" :label="c.name" :value="c.code" />
                                                        </el-select>
                                                    </span>
                                                    <span v-if="a.actionType=='SMS'" style="padding-left: 5px;">
                                                        手机号码：<el-input
                                                            
                                                            v-model="a.mobile"
                                                            placeholder="手机号码"
                                                            style="width: 280px; padding-left: 5px"
                                                        />
                                                    </span>
                                                    <span v-if="a.actionType=='EMAIL'" style="padding-left: 5px;">
                                                        Email：<el-input
                                                            
                                                            v-model="a.email"
                                                            placeholder="电子邮箱"
                                                            style="width: 280px; padding-left: 5px"
                                                        />
                                                    </span>
                                                    <el-button
                                                        type="danger"
                                                        link
                                                        icon="Delete"
                                                        @click="() => formData.actions.splice(i, 1)"
                                                        style="padding-left: 5px"
                                                    ></el-button>
                                                </el-col>
                                                <el-col :span="24" v-if="a.actionType=='CMD' && a.cmdCode">
                                                    <table class="pure-table">
                                                        <thead>
                                                            <tr>
                                                                <th colspan="5" style="text-align: center">指令字段</th>
                                                            </tr>
                                                            <tr>
                                                                <th style="min-width: 80px">字段名称</th>
                                                                <th style="min-width: 100px">字段标识</th>
                                                                <th style="min-width: 70px">字段类型</th>
                                                                <th style="min-width: 80px">字段值</th>
                                                                <th>字段说明</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            <tr v-if="a.paramList.length === 0">
                                                                <td colspan="5">
                                                                    <el-empty description="暂无数据" style="height: 200px" />
                                                                </td>
                                                            </tr>
                                                            <tr v-for="el in a.paramList" :key="el.id">
                                                                <td>{{ el.name }}</td>
                                                                <td>{{ el.code }}</td>
                                                                <td>{{ el.dataType.text }}</td>
                                                                <td>
                                                                    <el-form-item
                                                                        v-if="el.dataType.value == 5"
                                                                        label-width="0px"
                                                                        :prop="`params.${el.code}`"
                                                                    >
                                                                        <el-select
                                                                            v-model="a.params[el.code]"
                                                                            placeholder="请选择值"
                                                                            style="width: 180px"
                                                                        >
                                                                            <el-option
                                                                                v-for="item in formatEnum(el.defaultValue)"
                                                                                :key="item.key"
                                                                                :label="item.value"
                                                                                :value="item.key"
                                                                            />
                                                                        </el-select>
                                                                    </el-form-item>
                                                                    <el-form-item
                                                                        v-else
                                                                        label-width="0px"
                                                                        :prop="`params.${el.code}`"
                                                                        :rules="
                                                                            el.required
                                                                                ? { required: true, trigger: 'change', message: '请输入' }
                                                                                : null
                                                                        "
                                                                    >
                                                                        <el-input v-model="a.params[el.code]" style="width: 180px" />
                                                                    </el-form-item>
                                                                </td>
                                                                <td>
                                                                    {{ el.note || '--' }}
                                                                </td>
                                                            </tr>
                                                        </tbody>
                                                    </table>
                                                </el-col>
                                            </el-row>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                            <el-row v-else>
                                <el-col :span="24">
                                    <el-form-item>
                                        <span class="el-table__empty-text">暂无动作</span>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-card>
                    </el-col>
                    <el-col :span="6" style="padding-top: 20px">
                        <el-form-item label="生效时间" prop="hasTimeRange">
                            <el-radio-group v-model="formData.hasTimeRange" @change="hasTimeRangeChange">
                                <el-radio :value="false">不限制</el-radio>
                                <el-radio :value="true">限制</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="18" v-if="formData.hasTimeRange" style="padding-top: 20px">
                        <el-form-item label="时间范围" prop="timeStart" class="label-font-weight">
                            <el-time-picker v-model="formData.timeStart" placeholder="开始时间" value-format="HH:mm:ss" />
                            <span style="padding-left: 2px">至</span>
                            <el-time-picker v-model="formData.timeEnd" placeholder="结束时间" value-format="HH:mm:ss" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24" style="padding-top: 20px">
                        <el-form-item label="场景说明" prop="note">
                            <el-input type="textarea" v-model="formData.note" placeholder="场景说明" />
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="doSave" :loading="btnLoading">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="新建场景" v-model="showDevices" width="55%">
            <el-row :gutter="10" class="mb8">
                <el-col :span="24">
                    <el-form :model="queryDeviceParams" ref="queryDeviceRef" :inline="true" label-width="68px">
                        <el-form-item label="所属产品">
                            <el-select v-model="queryDeviceParams.productId" placeholder="请选择产品" style="width: 150px" @change="handleDeviceSearch">
                                <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
                            </el-select>
                        </el-form-item> 
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
                <el-col :span="24" >
                    <pro-table-list 
                ref="tableDeviceRef"
                :url="API_IOT_DEVICE_SCENE_DEVICE_LIST" 
                :queryParams="queryDeviceParams"
                v-model:selectRows="multipleSelectionDevice"
                :pageSize="10"
                :lazy="true"
                :selection="false"
                style="width: 100%;"
            >
            <el-table-column
                    prop="deviceNo"
                    label="设备通信号"
                    align="center"/>
                    <el-table-column
                    prop="meterNo"
                    label="表号/设备名称"
                    align="center"/>
                    <el-table-column
                    prop="imei"
                    label="IMEI"
                    align="center"/>
                    <el-table-column label="操作">
                        <template #default="scope">
                            <el-button type="primary" @click="handleSelect(scope.row)">选中</el-button>
                        </template>
                    </el-table-column>
            </pro-table-list>
                </el-col>
            </el-row>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="showDevices = false">关 闭</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-scene">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import modal from '/@/utils/modal'
import { API_IOT_DEVICE_SCENE_LIST, getProductList, getDeviceList, getCmdParamsList, getInfo, doCreate, doUpdate, doDelete, doEnabledChange, API_IOT_DEVICE_SCENE_DEVICE_LIST } from '/@/api/platform/iot/scene'
import { ElForm } from 'element-plus'

const props = defineProps<{
    spaceId: string
    spaceName: string
}>()

const queryRef = ref<InstanceType<typeof ElForm>>()
const createRef = ref<InstanceType<typeof ElForm>>()
const tableRef = ref<null>()
const tableDeviceRef = ref<null>()
const showCreate = ref(false)
const btnLoading = ref(false)
const formType = ref('create')
const showDevices = ref(false)

const multipleSelectionDevice = ref([])
const queryParams = ref({
    spaceId: props.spaceId,
})
const queryDeviceParams = reactive({
    type: 'term',
    index: 0,
    productId: '',
    filedName: 'deviceNo',
    filedValue: '',
})
const formData = ref({
    spaceId: '',
    name: '',
    enabled: true,
    note: '',
    terms: [],
    actions: [],
    hasTimeRange: false,
    timeStart: '',
    timeEnd: '',
})
const formRules = reactive({
    name: [{ required: true, message: '请输入场景名称', trigger: 'blur' }],
    enabled: [{ required: true, message: '请选择是否启用', trigger: 'blur' }],
})
const SceneActionType = ref([
    { label: '下发指令', value: 'CMD' },
    { label: '发送短信', value: 'SMS' },
    { label: '发送邮件', value: 'EMAIL' },
])
const SceneTermType = ref([
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'neq' },
    { label: '大于', value: 'gt' },
    { label: '大于等于', value: 'lte' },
    { label: '小于', value: 'lt' },
    { label: '小于等于', value: 'neq' },
    { label: '在...之间', value: 'btw' },
    { label: '不在...之间', value: 'nbtw' },
])
const productList = ref([])

const handleSearch = () => {
    queryParams.value.spaceId = props.spaceId
    tableRef.value?.query()
}

const resetSearch = () => {
    queryParams.value = {
        spaceId: props.spaceId,
    }
    queryRef.value?.resetFields()
    handleSearch()
}

const handleDeviceSearch = () => {
    tableDeviceRef.value?.query()
}

const resetDeviceSearch = () => {
    queryDeviceParams.filedName = 'deviceNo'
    queryDeviceParams.filedValue = ''
    tableDeviceRef.value?.query()
}

const handleShowSelect = (row: any, type: string, index: number) => {
    showDevices.value = true
    queryDeviceParams.productId = row.productId
    queryDeviceParams.index = index
    queryDeviceParams.type = type
    tableDeviceRef.value?.query()
}

const handleSelect = (row: any) => {
    if (queryDeviceParams.type === 'term') {
        formData.value.terms[queryDeviceParams.index].productId = row.productId
        formData.value.terms[queryDeviceParams.index].deviceId = row.id
        const product = productList.value.find((p) => p.id === row.productId)
        formData.value.terms[queryDeviceParams.index].productName = product.name
        formData.value.terms[queryDeviceParams.index].deviceName = row.meterNo ? row.meterNo + ' - ' + row.deviceNo : row.deviceNo
    } else {
        formData.value.actions[queryDeviceParams.index].productId = row.productId
        formData.value.actions[queryDeviceParams.index].deviceId = row.id
        const product = productList.value.find((p) => p.id === row.productId)
        formData.value.actions[queryDeviceParams.index].productName = product.name
        formData.value.actions[queryDeviceParams.index].deviceName = row.meterNo ? row.meterNo + ' - ' + row.deviceNo : row.deviceNo
    }
    showDevices.value = false
}

const handleCreate = () => {
    if (!props.spaceId) {
        modal.msgError('请选择空间')
        return
    }
    formType.value = 'create'
    formData.value = {
        spaceId: props.spaceId,
        name: '',
        enabled: true,
        note: '',
        terms: [
            {
                productId: '',
                deviceId: '',
                termType: 'eq',
                paramName: '',
                paramValue: '',
                duration: 0,
                productName: '',
                deviceName: '',
            },
        ],
        actions: [],
        hasTimeRange: false,
        timeStart: '',
        timeEnd: '',
    } as never
    showCreate.value = true
}

const handleUpdate = (row: any) => {
    getInfo(row.id).then((res) => {
        formData.value = parseFormData(res.data) as never
        formType.value = 'update'
        showCreate.value = true
        formData.value.actions.forEach((a: any) => {
            a.paramList = []
            a.params = {}
            if(a.actionType == 'CMD'){
                getCmdParamsList({ productId: a.productId, cmdCode: a.cmdCode }).then((res) => {
                    a.paramList = res.data
                    a.params = JSON.parse(a.cmdParam)
                })
            }
        })
    })
}

const hasTimeRangeChange = (val: boolean) => {
    if (!val) {
        formData.value.timeStart = ''
        formData.value.timeEnd = ''
    }
}

const addTerm = () => {
    formData.value.terms.push({
        productId: '',
        deviceId: '',
        termType: 'eq',
        paramName: '',
        paramValue: '',
        duration: 0,
        productName: '',
        deviceName: '',
    } as never)
}

const addAction = () => {
    formData.value.actions.push({
        productId: '',
        deviceId: '',
        actionType: 'CMD',
        cmdCode: '',
        cmdParam: '',
        mobile: '',
        email: '',
        paramList: [],
        params: {},
        productName: '',
        deviceName: '',
    } as never)
}

const doSave = async () => {
    await createRef.value?.validate()
    let passed = true
    // 校验 atcions 手机号
    formData.value.actions.forEach((a: any) => {
        if(a.actionType == 'CMD'){
            // 校验指令参数
            if(!a.cmdCode){
                modal.msgError('请选择指令')
                passed = false
            }
        } else if(a.actionType == 'SMS'){
            // 校验手机号码
            if(!/^1[3456789]\d{9}$/.test(a.mobile)){
                modal.msgError('请输入正确的手机号码')
                passed = false
            }
        } else if(a.actionType == 'EMAIL'){
            // 校验有效邮箱
            if(!/^(\w-*\.*)+@(\w-?)+(\.\w{2,})+$/.test(a.email)){
                modal.msgError('请输入正确的邮箱')
                passed = false
            }
        }
    })
    if(!passed) return
    if(formType.value == 'create'){
        doCreate(processFormData(formData.value)).then((res) => {
            if (res.code === 0) {
                modal.msgSuccess('新建成功')
                showCreate.value = false
                handleSearch()
            }
        })
    } else {
        doUpdate(processFormData(formData.value)).then((res) => {
            if (res.code === 0) {
                modal.msgSuccess('修改成功')
                showCreate.value = false
                handleSearch()
            }
        })
    }
}

const handleDetail = (id: string) => {
    console.log('detail', id)
}

const handleDelete = (row: any) => {
    modal
        .confirm('确认删除该场景吗？')
        .then(() => {
            doDelete(row).then((res) => {
                if (res.code === 0) {
                    modal.msgSuccess('删除成功')
                    handleSearch()
                }
            })
        })
        .catch(() => {})
}

const enabledChange = (row: any) => {
    doEnabledChange(row).then((res) => {
        if (res.code === 0) {
            modal.msgSuccess('操作成功')
            handleSearch()
        }
    })
}

const deviceList = (productId: string) => {
    if (!productId) return []
    return productList.value.find((p) => p.id === productId)?.devices
}

const getAttrList = (productId: string) => {
    if (!productId) return []
    return productList.value.find((p) => p.id === productId)?.attrs
}

const getCmdList = (productId: string) => {
    if (!productId) return []
    return productList.value.find((p) => p.id === productId)?.cmds
}

const productChange = (term: any) => {
    term.deviceId = ''
    term.paramName = ''
    term.paramValue = ''
    term.value1 = ''
    term.value2 = ''
    term.duration = 0
}

const deviceChange = (term: any) => {
    term.paramName = ''
    term.paramValue = ''
    term.duration = 0
    term.value1 = ''
    term.value2 = ''
}

const productChange2 = (term: any) => {
    term.deviceId = ''
    term.cmdCode = ''
    term.mobile = ''
    term.email = ''
}

const deviceChange2 = (term: any) => {
    term.cmdCode = ''
    term.mobile = ''
    term.email = ''
}

const getAttrLabel = (attr: any) => {
    if (attr.dataType.value === 5) {
        // 枚举
        return attr.name + ' (' + attr.ext.map((e: any) => e.value + '-' + e.text).join(',') + ')'
    }
    return attr.name + ' (' + attr.dataType.text + ')'
}

const handleActionCommand = (a: any, cmd: string) => {
    a.actionType = cmd
    a.cmdCode = ''
    a.cmdParam = ''
    a.mobile = ''
    a.email = ''
    a.paramList = []
    a.params = {}
    a.productName = ''
    a.deviceName = ''
    a.productId = ''
    a.deviceId = ''
}

const getAactionLabel = (actionType: string) => {
    const selectedOption = SceneActionType.value.find((item) => item.value === actionType)
    if (selectedOption) {
        return selectedOption.label
    }
    return ''
}

const actionCmdChange = (action: any) => {
    action.cmdParam = ''
    action.paramList = []
    action.params = {}
    getCmdParamsList({ productId: action.productId, cmdCode: action.cmdCode }).then((res) => {
        action.paramList = res.data
    })
}

//0=开阀,1=强制关阀,2=临时关阀 格式化
const formatEnum = (str) => {
    if (!str) return ''
    const arr = str.split(',')
    return arr.map((item) => {
        const [key, value] = item.split('=')
        return { key, value }
    })
}

// 解析表单数据
const parseFormData = (data: any) => {
    data.terms.forEach((t: any) => {
        t.value1 = ''
        t.value2 = ''
        t.termType = t.termType ? t.termType.value : ''
        if(t.termType == 'btw' || t.termType == 'nbtw'){
            const arr = t.paramValue.split(',')
            t.value1 = arr[0]
            t.value2 = arr[1]
        }
        if(t.productId){
            const product = productList.value.find((p) => p.id === t.productId)
            t.productName = product?.name || ''
        }
    })
    data.actions.forEach((a: any) => {
        a.paramList = []
        a.params = {}
        a.actionType = a.actionType ? a.actionType.value : ''
        if(a.productId){
            const product = productList.value.find((p) => p.id === a.productId)
            a.productName = product?.name || ''
        }
    })
    return data
}

// 组装表单数据
const processFormData = (data: any) => {
    data.terms.forEach((t: any) => {
        if(t.termType == 'btw' || t.termType == 'nbtw'){
            t.paramValue = t.value1 + ',' + t.value2
        }
        t.duration = t.duration ? t.duration : 0
    })
    data.actions.forEach((a: any) => {
        if(a.actionType == 'CMD'){
            a.cmdParam = JSON.stringify(a.params)
        }
    })
    return data
}

onMounted(() => {
    getProductList().then((res) => {
        productList.value = res.data
    })
})

const list = () => {
    handleSearch()
}

defineExpose({
    list,
})
</script>

<style lang="scss" scoped>
.pure-table {
    margin-top: 10px;
    width: 100%;
    border-collapse: collapse;
    border: 1px solid #e4e6ea;
    line-height: 1.4
}

.pure-table th {
    background-color: #f2f3f7
}

.pure-table td,
.pure-table th {
    border-bottom: 1px solid #e4e6ea;
    text-align: left;
    padding: 10px
}

.pure-table .el-form-item {
    margin-bottom: 0;
    margin-right: 0 !important
}

.pure-table {
    width: 100%;
    font-size: 12px;

    td {
        padding: 20px 10px;
    }

    .el-form-item {
        position: relative;
        padding-left: 10px;

        &.is-required {
            &:after {
                position: absolute;
                left: 0;
                top: 10px;
                content: '*';
                color: red;
            }
        }
    }
}
</style>

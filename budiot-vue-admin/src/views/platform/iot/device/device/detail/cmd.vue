<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="18">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                    <el-form-item label="开始时间">
                        <el-date-picker v-model="queryParams.startTime" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
                    </el-form-item>
                    <el-form-item label="结束时间">
                        <el-date-picker v-model="queryParams.endTime" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                        <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
            <el-col :span="6" style="text-align: right">
                <div style="display: inline-flex">
                    <el-button
    plain type="success" @click="handleCreate"
                        v-permission="['iot.device.device.cmd']">下发指令
                    </el-button>
                    <right-toolbar :columns="columns" :quickSearchShow="false" @quickSearch="handleSearch" />
                </div>
            </el-col>
        </el-row>
        <el-row class="mb8">
            <el-button-group>
                <!-- 选中状态的按钮样式 -->
                <el-button :type="listActive=='wait'?'primary':''" @click="listActive='wait'">待执行指令</el-button>
                <el-button :type="listActive=='done'?'primary':''" @click="listActive='done'">历史指令</el-button>
            </el-button-group>
        </el-row>
        <el-row class="mb8">
            <pro-table-list v-if="listActive=='wait'" ref="tableRef" :url="API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST" :queryParams="queryParams" :pageSize="10" style="width: 100%">
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
                        <template v-if="item.prop == 'createTime'" #default="scope">
                            <span>{{ formatTime(scope.row.createTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'sendTime'" #default="scope">
                            <span>{{ formatTime(scope.row.sendTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'finishTime'" #default="scope">
                            <span>{{ formatTime(scope.row.finishTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'name'" #default="scope">
                            <span>{{ cmdCongifMap[scope.row.code] }}</span>
                        </template>
                        <template v-if="item.prop == 'status'" #default="scope">
                            <span>{{  scope.row.status?.text }}</span>
                        </template>                        
                    </el-table-column>
                </template>
            </pro-table-list>
            <pro-table-list v-if="listActive=='done'" ref="tableRef" :url="API_IOT_DEVICE_DEVICE_CMD_DONE_LIST" :queryParams="queryParams" :pageSize="10" style="width: 100%">
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
                        <template v-if="item.prop == 'createTime'" #default="scope">
                            <span>{{ formatTime(scope.row.createTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'sendTime'" #default="scope">
                            <span>{{ formatTime(scope.row.sendTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'finishTime'" #default="scope">
                            <span>{{ formatTime(scope.row.finishTime) }}</span>
                        </template>
                        <template v-if="item.prop == 'name'" #default="scope">
                            <span>{{ cmdCongifMap[scope.row.code] }}</span>
                        </template>
                        <template v-if="item.prop == 'status'" #default="scope">
                            <span>{{  cmdStatus[scope.row.status]?.label }}</span>
                        </template>    
                    </el-table-column>
                </template>
            </pro-table-list>
        </el-row>

        <el-dialog title="下发指令" v-model="createShow" width="60%" :close-on-click-modal="false">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="选择指令" prop="code">
                    <el-select v-model="formData.code" placeholder="请选择指令" clearable @change="cmdChange">
                        <el-option v-for="item in cmdConfigList" :key="item.code" :label="item.name"
                            :value="item.code" />
                    </el-select>
                </el-form-item>
                <el-form-item>
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
                            <tr v-if="fieldList.length === 0">
                                <td colspan="5">
                                    <el-empty description="暂无数据" style="height: 200px;" />
                                </td>
                            </tr>
                            <tr v-for="el in fieldList" :key="el.id">
                                <td>{{ el.name }}</td>
                                <td>{{ el.code }}</td>
                                <td>{{ el.dataType.text }}</td>
                                <td>
                                    <el-form-item v-if="el.dataType.value == 5" label-width="0px"
                                        :prop="`params.${el.code}`">
                                        <el-select v-model="formData.params[el.code]" placeholder="请选择值"
                                            style="width: 180px;">
                                            <el-option v-for="item in formatEnum(el.defaultValue)" :key="item.key"
                                                :label="item.value" :value="item.key" />
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item v-else label-width="0px" :prop="`params.${el.code}`"
                                        :rules="el.required ? { required: true, trigger: 'change', message: '请输入' } : null">
                                        <el-input v-model="formData.params[el.code]" style="width: 180px" />
                                    </el-form-item>
                                </td>
                                <td>
                                    {{ el.note || '--' }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </el-form-item>
                <el-form-item label="指令说明" prop="note">
                    <el-input type="textarea" v-model="formData.note" placeholder="下发指令说明" />
                </el-form-item>

            </el-form>
            <template #footer>
                <el-button @click="createShow=false">取 消</el-button>
                <el-button :loading="loading" type="primary" @click="handleConfirm">提 交</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs, watch } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST, API_IOT_DEVICE_DEVICE_CMD_DONE_LIST, getCmdConfigList, doCmdCreate } from '/@/api/platform/iot/device'

const route = useRoute()
import { useRoute } from 'vue-router'
const id = route.params.id as string

const columns = ref([
    { prop: 'code', label: '指令标识', show: true, fixed: 'left',align: 'center' },
    { prop: 'name', label: '指令名称', show: true, fixed: 'left',align: 'center' },
    { prop: 'status', label: '指令状态', show: true, align: 'center' },
    { prop: 'createTime', label: '创建时间', show: true,align: 'center' },
    { prop: 'sendTime', label: '下发时间', show: true, align: 'center' },
    { prop: 'finishTime', label: '完成时间', show: true, align: 'center' },
    { prop: 'respResult', label: '执行结果', show: true},
])

const queryParams = reactive({
    deviceId: id,
    startTime: '',
    endTime: '',
})

const cmdStatus = ref([
    { label: '待下发', value: 0 },
    { label: '已下发', value: 1 },
    { label: '已送达', value: 2 },
    { label: '已完成', value: 3 },
    { label: '下发失败', value: 4 },
    { label: '执行失败', value: 5 },
    { label: '已取消', value: 6 },
    { label: '超时', value: 7 },
    { label: '已过期', value: 8 },
])

const tableRef = ref(null)
const queryRef = ref(null)
const listActive = ref("wait")
const createShow = ref(false)

const cmdConfigList = ref([])
const cmdCongifMap = ref({})
const device = ref({})

const loading = ref(false)
const createRef = ref<InstanceType<typeof ElForm>>()
const formData = ref({
    deviceId: id,
    productId: '',
    deviceNo: '',
    code: '',
    params: '',
    enabled: true,
    note: '',
})

const formRules = ref({
    code: [{ required: true, message: '请选择指令', trigger: 'blur' }],
    enabled: [{ required: true, message: '是否启用', trigger: 'blur' }],
    note: [{ required: true, message: '指令说明', trigger: ['blur', 'change'] }],
})

const fieldList = ref([])

const cmdChange = (val: string) => {
    formData.value.params = {}
    fieldList.value = []
    const cmd = cmdConfigList.value.find((item: any) => item.code === val)
    fieldList.value = cmd.attrList
    
    fieldList.value.forEach((item: any) => {
        if (item.code === 'device_no' || item.code === 'deviceNo' || item.code === 'device_id' || item.code === 'deviceId') {
            formData.value.params[item.code] = device.value.deviceNo
        } else if (item.dataType.value == 5) {
            formData.value.params[item.code] = formatEnum(item.defaultValue)[0].key
        }
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

const handleConfirm = () => {
    if (!createRef.value) return
    createRef.value.validate((valid) => {
        if (valid) {
            loading.value = true
                const params = {
                    ...formData.value,
                    params: JSON.stringify(formData.value.params)
                }
                params.productId = device.value.productId
                params.deviceNo = device.value.deviceNo
                doCmdCreate(params).then(() => {
                    loading.value = false
                    modal.msgSuccess('指令保存成功')
                    resetSearch()
                }).catch(() => {
                    loading.value = false
                })
        }
    })
}

const handleSearch = () => {
    tableRef.value?.query()
}

const resetSearch = () => {
    queryParams.startTime = ''
    queryParams.endTime = ''
    queryRef.value?.resetFields()
    tableRef.value?.query()
}

const handleCreate = () => {
    createShow.value = true
}


const init = () => {
    getCmdConfigList(id).then((res) => {
        cmdConfigList.value = res.data.list
        // cmdCongifMap = cmdConfigList 的 code name 映射
        cmdCongifMap.value = res.data.list.reduce((acc, cur) => {
            acc[cur.code] = cur.name
            return acc
        }, {})
        device.value = res.data.device
    })
}

onMounted(() => {
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>

<style lang="scss" scoped>
.pure-table {
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

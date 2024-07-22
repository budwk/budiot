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
                        <template v-if="item.prop == 'deviceNo'" #default="scope">
                            <el-button type="primary" link @click="goToList(scope.row.deviceId)">{{ scope.row.deviceNo }}</el-button>
                        </template>
                    </el-table-column>
                </template>
            </pro-table-list>
        </el-row>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs, watch } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST, API_IOT_DEVICE_DEVICE_CMD_DONE_LIST, getCmdConfigList } from '/@/api/platform/iot/device'

import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const id = route.params.id as string

const columns = ref([
    { prop: 'deviceNo', label: '设备通信号', show: true, fixed: 'left',width: 150,align: 'center' },
    { prop: 'code', label: '指令标识', show: true, fixed: 'left',align: 'center' },
    { prop: 'name', label: '指令名称', show: true, fixed: 'left',align: 'center' },
    { prop: 'status', label: '指令状态', show: true, align: 'center' },
    { prop: 'createTime', label: '创建时间', show: true,align: 'center' },
    { prop: 'sendTime', label: '下发时间', show: true, align: 'center' },
    { prop: 'finishTime', label: '完成时间', show: true, align: 'center' },
    { prop: 'respResult', label: '执行结果', show: true},
])

const queryParams = reactive({
    productId: id,
    startTime: '',
    endTime: '',
})

const cmdCongifMap = ref({})

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

const handleSearch = () => {
    tableRef.value?.query()
}

const resetSearch = () => {
    queryParams.startTime = ''
    queryParams.endTime = ''
    queryRef.value?.resetFields()
    tableRef.value?.query()
}

const goToList = (deviceId: string) =>{
    router.push({ path: `/platform/iot/device/detail/${deviceId}/cmd`, query: { fpid: id } })
}

const init = () => {
    getCmdConfigList({ productId: id}).then((res) => {
        // cmdCongifMap = cmdConfigList 的 code name 映射
        cmdCongifMap.value = res.data.list.reduce((acc, cur) => {
            acc[cur.code] = cur.name
            return acc
        }, {})
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

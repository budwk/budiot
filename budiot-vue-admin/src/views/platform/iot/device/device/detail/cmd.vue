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
                    </el-table-column>
                </template>
            </pro-table-list>
        </el-row>
    </div>
</template>
<script setup lang="ts" name="cmd">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { API_IOT_DEVICE_DEVICE_CMD_WAIT_LIST, API_IOT_DEVICE_DEVICE_CMD_DONE_LIST } from '/@/api/platform/iot/device'

const route = useRoute()
import { useRoute } from 'vue-router'
const id = route.params.id as string

const columns = ref([
    { prop: 'createTime', label: '创建时间', show: true, fixed: 'left',align: 'center' },
    { prop: 'code', label: '指令标识', show: true, fixed: 'left',align: 'center' },
    { prop: 'status', label: '指令状态', show: true, fixed: 'left',align: 'center' },
    { prop: 'sendTime', label: '下发时间', show: true, fixed: 'left',align: 'center' },
    { prop: 'finishTime', label: '完成时间', show: true, fixed: 'left',align: 'center' },
    { prop: 'respResult', label: '指令结果', show: true, fixed: 'left' },
])

const queryParams = reactive({
    deviceId: id,
    startTime: '',
    endTime: '',
})

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


</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
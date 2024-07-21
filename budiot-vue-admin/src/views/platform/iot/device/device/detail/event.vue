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
            <pro-table-list ref="tableRef" :url="API_IOT_DEVICE_DEVICE_EVENT_LIST" :queryParams="queryParams" :pageSize="10" style="width: 100%">
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
                        <template v-if="item.prop == 'receive_time'" #default="scope">
                            <span>{{ formatTime(scope.row.receive_time) }}</span>
                        </template>
                        <template v-if="item.prop == 'eventType'" #default="scope">
                            <span>{{ eventType[scope.row.eventType]?.label }}</span>
                        </template>
                        <template v-if="item.prop == 'eventData'" #default="scope">
                            <el-row>
                                <el-col v-for="(el, index) in scope.row.eventData" :key="index" :span="6">
                                    <el-row>
                                        <el-col :span="8">{{ el.name }}：</el-col>
                                        <el-col :span="16">{{ el.value }}</el-col>
                                    </el-row>
                                </el-col>
                            </el-row>
                        </template>
                    </el-table-column>
                </template>
            </pro-table-list>
        </el-row>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { API_IOT_DEVICE_DEVICE_EVENT_LIST } from '/@/api/platform/iot/device'

const route = useRoute()
import { useRoute } from 'vue-router'
const id = route.params.id as string

const columns = ref([
    { prop: 'ts', label: 'TS', show: true, fixed: 'left',width: 150,align: 'center' },
    { prop: 'startTime', label: '事件时间', show: true, fixed: 'left',width: 180,align: 'center' },
    { prop: 'eventType', label: '事件类型', show: true, align: 'center' },
    { prop: 'eventName', label: '事件名称', show: true, align: 'center' },
    { prop: 'content', label: '事件内容', show: true, align: 'center' },
    { prop: 'eventData', label: '事件数据', show: true, align: 'center' },
])

const eventType = ref([
    { label: '信息', value: 0 },
    { label: '报警', value: 1 },
    { label: '故障', value: 2 },
    { label: '恢复', value: 3 },
])

const queryParams = reactive({
    deviceId: id,
    startTime: '',
    endTime: '',
})

const tableRef = ref(null)
const queryRef = ref(null)

const handleSearch = () => {
    tableRef.value?.query()
}

const resetSearch = () => {
    queryParams.startTime = ''
    queryParams.endTime = ''
    queryRef.value?.resetFields()
    tableRef.value?.query()
}

const init = () =>{
    
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
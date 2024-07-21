<template>
    <div style="padding-left: 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="18">
                <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
                    <el-form-item label="通信日期">
                        <el-date-picker v-model="queryParams.startTime" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
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
            <pro-table-list ref="tableRef" :url="API_IOT_DEVICE_DEVICE_RAW_LIST" :queryParams="queryParams" :pageSize="10" style="width: 100%">
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
                        <template v-if="item.prop == 'startTime'" #default="scope">
                            <span>{{ formatTime(scope.row.startTime) }}</span>
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
import { API_IOT_DEVICE_DEVICE_RAW_LIST } from '/@/api/platform/iot/device'
import dayjs from 'dayjs'

const route = useRoute()
import { useRoute } from 'vue-router'
const id = route.params.id as string

const columns = ref([
    { prop: 'ts', label: 'TS', show: true, fixed: 'left',width: 150,align: 'center' },
    { prop: 'startTime', label: '通信时间', show: true, fixed: 'left',width: 180,align: 'center'},
    { prop: 'type', label: '通信方向', show: true, fixed: 'left',width: 100,align: 'center' },
    { prop: 'originData', label: '原始报文', show: true },
    { prop: 'parsedData', label: '解析结果', show: true },
])

const queryParams = reactive({
    deviceId: id,
    startTime: dayjs().format('YYYY-MM-DD'),
})

const tableRef = ref(null)
const queryRef = ref(null)

const handleSearch = () => {
    tableRef.value?.query()
}

const resetSearch = () => {
    queryParams.startTime = dayjs().format('YYYY-MM-DD')
    queryRef.value?.resetFields()
    tableRef.value?.query()
}

</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
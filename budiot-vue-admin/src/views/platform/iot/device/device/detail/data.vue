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
            <pro-table-list ref="tableRef" :url="API_IOT_DEVICE_DEVICE_DATA_LIST" :queryParams="queryParams" :pageSize="10" style="width: 100%">
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
                        <template v-else #default="scope">
                            <span>{{ getValue(item.prop,scope.row[item.prop]) }}</span>
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
import { API_IOT_DEVICE_DEVICE_DATA_LIST, getField } from '/@/api/platform/iot/device'

const route = useRoute()
import { useRoute } from 'vue-router'
const id = route.params.id as string

const columns = ref([
    { prop: 'ts', label: 'TS', show: true, fixed: 'left',width: 150,align: 'center' },
    { prop: 'receive_time', label: '通信时间', show: true, fixed: 'left',width: 180,align: 'center' },
])
const filedList = ref([])

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

const getValue = (prop: string, value: any) => {
    if (value === null || value === undefined) {
        return '-'
    }
    console.log(prop,value)
    // 从 filedList 中获取对应的属性，如果是枚举类型，返回对应的 text
    const item = filedList.value.find(item => item.code === prop)
    if (item) {
        if (item.dataType.value === 5) {
            const ext = item.ext.find(ext => ext.value === '' + value)
            return ext ? ext.text : value
        } else {
            return value + (item.unit || '')  
        }
    }
    return value
}

const init = () =>{
    getField(id).then(res => {
        filedList.value = res.data
        // 追加到 columns
        res.data.forEach(item => {
            columns.value.push({
                prop: item.code,
                label: item.name,
                show: true,
            })
        })
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
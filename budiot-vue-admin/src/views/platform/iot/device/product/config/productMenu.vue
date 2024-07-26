<template>
    <div>
        <el-row class="mb8">

<el-table ref="tableRef" v-loading="tableLoading" :data="tableData" row-key="id" @selection-change="handleSelectionChange">
    <template v-for="(item, idx) in columns" :key="idx">
        <el-table-column :prop="item.prop" :label="item.label" :fixed="item.fixed" v-if="item.show"
            :show-overflow-tooltip="true" :align="item.align" :width="item.width">
            <template v-if="item.prop == 'display'" #default="scope">
                <el-radio-group v-model="scope.row.display" :disabled="scope.row.sys" @change="handleUpdate(scope.row)">
                    <el-radio :value="true">是</el-radio>
                    <el-radio :value="false">否</el-radio>
                </el-radio-group>
            </template>
        </el-table-column>
    </template>
</el-table>
</el-row>

    </div>
</template>
<script setup lang="ts" name="productMenu">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { useRoute } from "vue-router"
import { getMenuList, doMenuChange
 } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string
const queryRef = ref<InstanceType<typeof ElForm>>()
const tableRef = ref<InstanceType<typeof ElTable>>()

const multipleSelection = ref([])

const tableData = ref([])
const tableLoading = ref(false)
const columns = ref([
    { prop: 'name', label: '菜单名称', show: true, fixed: 'left', width: 180, align: 'center' },
    { prop: 'display', label: '是否显示', show: true, align: 'center' },
])

const data = reactive({
    queryParams: {
        productId: id,
        name: '',
        pageNo: 1,
        pageSize: 15,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
})

const { queryParams } = toRefs(data)

const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
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

const handleUpdate = (row: any) => {
    doMenuChange({ productId: id, menuId: row.id, display: row.display }).then(() => {
        modal.msgSuccess('操作成功')
        list()
    })
}

const list = () => {
    tableLoading.value = true
    getMenuList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list
        queryParams.value.totalCount = res.data.totalCount
    })
}

onMounted(() => {
    list()
})
</script>
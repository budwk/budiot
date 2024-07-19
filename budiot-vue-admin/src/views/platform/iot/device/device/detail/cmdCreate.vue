<template>
    <div>
        <el-dialog title="下发指令" :model-value="showDialog" width="60%" :close-on-click-modal="false" @close="handleClose">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="选择指令" prop="code">
                    <el-select v-model="formData.code" placeholder="请选择指令" @change="cmdChange">
                        <el-option v-for="item in cmdConfigList" :key="item.code" :label="item.name" :value="item.code" />
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
                                    <el-empty description="暂无数据" style="height: 200px;"/>
                                </td>
                            </tr>
                            <tr v-for="el in fieldList" :key="el.id">
                                <td>{{ el.name }}</td>
                                <td>{{ el.code }}</td>
                                <td>{{ el.dataType.text }}</td>
                                <td>
                                    <el-form-item
                                        v-if="el.dataType.value ==5"
                                        label-width="0px"
                                        :prop="`params.${el.code}`"
                                    >
                                        <el-select v-model="formData.params[el.code]" placeholder="请选择值" style="width: 180px;">
                                            <el-option v-for="item in formatEnum(el.defaultValue)" :key="item.key" :label="item.value" :value="item.key" />
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item
                                        v-else
                                        label-width="0px"
                                        :prop="`params.${el.code}`"
                                        :rules="el.required ? { required: true, trigger: 'change', message: '请输入' } : null"
                                    >
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
                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="formData.enabled">
                        <el-radio :value="true">启用</el-radio>
                        <el-radio :value="false">不启用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="指令说明" prop="note">
                    <el-input type="textarea" v-model="formData.note" placeholder="下发指令说明" /> 
                </el-form-item>
                
            </el-form>
            <template #footer>
                <el-button @click="handleClose">取 消</el-button>
                <el-button :loading="loading" type="primary" @click="handleConfirm">提 交</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { getCmdConfigList } from '/@/api/platform/iot/device'
import { de } from 'element-plus/es/locale'

const props = defineProps<{
    deviceId: string
    showCreate: boolean
}>()

const showDialog = toRefs(props).showCreate
const loading = ref(false)

const emits = defineEmits(['update:change'])

const handleClose = () => {
    emits('update:change', false)
}

const formData = ref({
    deviceId: props.deviceId,
    code: '',
    params: '',
    enabled: true,
    note: '',
})

const formRules = ref({
    code: [{ required: true, message: '请选择指令', trigger: 'blur' }],
    enabled: [{ required: true, message: '是否启用', trigger: 'blur' }],
    note: [{ required: true, message: '指令说明', trigger: ['blur','change'] }],
})

const device = ref({})
const cmdConfigList = ref([])
const fieldList = ref([])

const cmdChange = (val) => {
    const cmd = cmdConfigList.value.find((item) => item.code === val)
    fieldList.value = cmd.attrList
    formData.value.params = {}
    fieldList.value.forEach((item) => {
        if (item.code === 'device_no' || item.code === 'deviceNo' || item.code === 'device_id' || item.code === 'deviceId') {
            formData.value.params[item.code] = device.value.deviceNo
        } else if(item.dataType.value==5){
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
    loading.value = true
    const params = {
        ...formData.value,
        params: JSON.stringify(formData.value.params)
    }
    createCmd(params).then(() => {
        loading.value = false
        emits('update:change', false)
        modal.success('下发指令成功')
    }).catch(() => {
        loading.value = false
    })
}

const init = () => {
    getCmdConfigList(props.deviceId).then((res) => {
        cmdConfigList.value = res.data.list
        device.value = res.data.device
    })
}

onMounted(() => {
    init()
})
</script>
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

.pure-table td,.pure-table th {
    border-bottom: 1px solid #e4e6ea;
    text-align: left;
    padding: 10px
}

.pure-table .el-form-item {
    margin-bottom: 0;
    margin-right: 0!important
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

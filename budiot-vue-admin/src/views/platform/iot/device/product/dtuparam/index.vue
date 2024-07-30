<template>
    <div style="padding: 0 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="24" style="text-align: right">
                <div style="display: inline-flex">
                    <el-form :model="tableData" ref="queryRef" :inline="true" label-width="100px">
                        <el-form-item label="平台版本号：" class="label-font-weight">
                            <el-tag type="info">
                                {{ tableData.version }}
                            </el-tag>
                        </el-form-item>
                        <el-form-item label="是否启用">
                            <el-switch
                                v-permission="['iot.device.product.dtuparam']"
                                v-model="tableData.enabled"
                                inline-prompt
                                active-text="启用"
                                inactive-text="未启用"
                                @change="enabledChange"
                            />
                        </el-form-item>
                    </el-form>
                    <el-button plain type="success" @click="save" v-permission="['iot.device.product.dtuparam']">保存配置 </el-button>
                    <el-button plain type="primary" @click="exportTxt" v-permission="['iot.device.product.dtuparam']">导出配置 </el-button>
                    <el-button plain type="primary" v-permission="['iot.device.product.dtuparam']">导入配置 </el-button>
                </div>
            </el-col>
        </el-row>
        <el-row>
            <el-form
                ref="createRef"
                :model="formData"
                :rules="formRules"
                label-width="220px"
                label-position="left"
                style="width: 100%; padding: 0 5px"
            >
                <el-tabs v-model="activeName" tabPosition="left" style="width: 100%">
                    <el-tab-pane label="基本信息" name="0" style="padding: 0 20px">
                        <el-form-item label="模式：" prop="passon">
                            <el-radio-group v-model="formData.passon">
                                <el-radio :value="1">透传</el-radio>
                                <el-radio :value="0">单片机控制</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否加设备识别码IMEI：" prop="plate" v-if="formData.passon == 1">
                            <el-radio-group v-model="formData.plate">
                                <el-radio :value="0">不加</el-radio>
                                <el-radio :value="1">加</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="报文转换（bin -- hex）：" prop="convert">
                            <el-radio-group v-model="formData.convert">
                                <el-radio :value="1">转换</el-radio>
                                <el-radio :value="0">不换</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 如果启用数据流模板，这里选择“不换”</span>
                        </el-form-item>
                        <el-form-item label="首次登陆服务器发送注册信息：" prop="register">
                            <el-radio-group v-model="formData.register">
                                <el-radio :value="1">发送{csq:rssi,imei:imei,iccid:iccid,ver:Version}</el-radio>
                                <el-radio :value="2">发送HEX报文"13,12345,12345"</el-radio>
                                <el-radio :value="0">不发</el-radio>
                                <el-radio :value="3">自定义</el-radio>
                                <el-radio :value="4">顺序生成</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="每分钟最大串口流量(Byte)：" prop="fota">
                            <el-input v-model="formData.flow" style="width: 150px" />
                            <span class="tip">提示: 0为不启用</span>
                        </el-form-item>
                        <el-form-item label="是否启用自动更新：" prop="fota">
                            <el-radio-group v-model="formData.fota">
                                <el-radio :value="0">否</el-radio>
                                <el-radio :value="1">是</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="串口分帧超时：" prop="uartReadTime">
                            <el-input-number v-model="formData.uartReadTime" :min="10" :max="2000" />
                            <span class="tip">提示:（单位：ms 默认25ms，范围10-2000）</span>
                        </el-form-item>
                        <el-form-item label="电源模式：" prop="pwrmod">
                            <el-radio-group v-model="formData.pwrmod">
                                <el-radio value="normal">正常</el-radio>
                                <el-radio value="energy">节能</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="配置密码：" prop="password">
                            <el-input v-model="formData.password" style="width: 150px" />
                            <span class="tip">提示: 只允许包含字母数字_</span>
                        </el-form-item>
                        <el-form-item label="网络分帧超时：" prop="netReadTime">
                            <el-input-number v-model="formData.netReadTime" :min="10" :max="2000" />
                            <span class="tip">提示:（单位：ms 默认0ms，范围10-2000）</span>
                        </el-form-item>
                        <el-form-item label="日志输出：" prop="nolog">
                            <el-radio-group v-model="formData.nolog">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否打开RNDIS：" prop="isRndis">
                            <el-radio-group v-model="formData.isRndis">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否打开ipv6：" prop="isipv6">
                            <el-radio-group v-model="formData.isipv6">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 目前仅780E/700E/600E/780EX/780EG支持该选项，724/720系列/820/600ug等模组不支持</span>
                        </el-form-item>
                        <el-form-item label="是否守护全部网络通道：" prop="webProtect">
                            <el-radio-group v-model="formData.webProtect">
                                <el-radio value="0">个别</el-radio>
                                <el-radio value="1">全部</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 服务器连接断开时5分钟会重启，守护全部通道有一个断开就重启，守护个别通道在个别通道断开会重启</span>
                        </el-form-item>
                    </el-tab-pane>
                    <el-tab-pane label="串口参数" name="1" style="padding: 0 20px">
                        <el-tabs v-model="activeComm" style="width: 100%">
                            <el-tab-pane v-for="(obj, ind) in formData.uconf" :label="'串口' + (ind + 1)" :name="ind" :key="'comm_' + ind">
                                <el-radio-group v-model="commValues[ind]" @change="commChange">
                                    <el-radio :value="true">启用</el-radio>
                                    <el-radio :value="false">不启用</el-radio>
                                </el-radio-group>
                            </el-tab-pane>
                        </el-tabs>
                    </el-tab-pane>
                    <el-tab-pane label="网络通道" name="2" style="padding: 0 20px">网络通道</el-tab-pane>
                    <el-tab-pane label="预置信息" name="3" style="padding: 0 20px">预置信息</el-tab-pane>
                    <el-tab-pane label="GPIO" name="4" style="padding: 0 20px">GPIO</el-tab-pane>
                    <el-tab-pane label="GPS" name="5" style="padding: 0 20px">GPS</el-tab-pane>
                    <el-tab-pane label="数据流" name="6" style="padding: 0 20px">数据流</el-tab-pane>
                    <el-tab-pane label="预警" name="7" style="padding: 0 20px">预警</el-tab-pane>
                    <el-tab-pane label="任务" name="8" style="padding: 0 20px">任务</el-tab-pane>
                </el-tabs>
            </el-form>
        </el-row>

        <el-dialog title="导出参数" v-model="showExport" width="45%" :close-on-click-modal="false">
            <el-form>
                <el-form-item>
                    <el-input type="textarea" rows="10" v-model="config" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="showExport = false">关 闭</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable, ElUpload } from 'element-plus'
import { useRoute } from 'vue-router'
import { getDtuInfo, doDtuEnabled, doDtuSave } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const showExport = ref(false)
const activeName = ref('0')
const activeComm = ref(0)
const commValues = ref({
    0: false,
    1: false,
    2: false,
})

const tableData = ref({
    version: 0,
    enabled: false,
    config: '',
})
const config = ref('')

const data = reactive({
    formData: {
        fota: 0,
        uartReadTime: 25,
        flow: '',
        param_ver: '',
        pwrmod: 'normal',
        password: '',
        netReadTime: 0,
        passon: 1,
        nolog: '1',
        isRndis: '0',
        isipv6: '0',
        webProtect: '1',
        plate: 0,
        protectContent: [0, 0, 0, 0, 0, 0, 0],
        reg: 0,
        convert: 0,
        uconf: [[], [], []],
        conf: [[], [], [], [], [], [], []],
        preset: { number: '', delay: '', smsword: '' },
        apn: ['', '', ''],
        cmds: [[], [], []],
        pins: [],
        gps: { pio: [], fun: [] },
        upprot: ['', '', '', '', '', '', ''],
        dwprot: ['', '', '', '', '', '', ''],
        warn: { adc0: [], adc1: [], vbatt: [], gpio: [] },
    },
    formRules: {
        name: [{ required: true, message: '固件名称不能为空', trigger: ['blur', 'change'] }],
    },
})

const { formData, formRules } = toRefs(data)

const commChange = (val: boolean) => {
    console.log(val)
}

const save = () => {
    console.log(formData.value)
    createRef.value.validate((valid) => {
        if (valid) {
            doDtuSave({
                productId: id,
                config: JSON.stringify(formData.value),
            }).then((res) => {
                if (res.code === 0) {
                    modal.msgSuccess('保存成功')
                    init()
                }
            })
        }
    })
}

const enabledChange = (val: boolean) => {
    if (val) {
        modal
            .confirm('启用后产品下所有设备将请求最新版本参数')
            .then(() => {
                return doDtuEnabled({
                    productId: id,
                    enabled: val,
                })
            })
            .then((res) => {
                if (res.code === 0) {
                    tableData.value.enabled = val
                    modal.msgSuccess('操作成功')
                }
            })
            .catch(() => {
                setTimeout(() => {
                    tableData.value.enabled = !val
                }, 500)
            })
    } else {
        doDtuEnabled({
            productId: id,
            enabled: val,
        }).then((res) => {
            if (res.code === 0) {
                tableData.value.enabled = val
                modal.msgSuccess('操作成功')
            }
        })
    }
}

const exportTxt = () => {
    config.value = JSON.parse(tableData.value.config)
    showExport.value = true
}

const init = () => {
    getDtuInfo(id).then((res) => {
        tableData.value = res.data
        return
        if (res.data.config) {
            // JSON.parse(res.data.config) 值赋值给formData.value
            formData.value = JSON.parse(res.data.config) as any

            console.log(formData.value)
            // 根据uconf的数组是否为空，初始化commValues
            formData.value.uconf.forEach((item, index) => {
                if (item.length > 0) {
                    commValues.value[index] = true
                }
            })
        }
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
<style scoped>
.tip {
    color: #999;
    padding-left: 10px;
}
</style>
<template>
    <div>
        <el-card shadow="never" class="md5 description_text">
            <div class="title">
                <span>基本信息</span>
            </div>
            <el-row :gutter="20">
                <el-col :span="6">
                    <div class="term">设备通信号</div>
                    <div class="detail">{{ deviceInfo?.deviceNo }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">设备编号/表号</div>
                    <div class="detail">{{ deviceInfo?.meterNo }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">IMEI</div>
                    <div class="detail">{{ deviceInfo?.imei }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">ICCID</div>
                    <div class="detail">{{ deviceInfo?.iccid }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">设备类型</div>
                    <div class="detail">{{ deviceInfo?.deviceType?.text }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">设备分类</div>
                    <div class="detail">{{ deviceInfo?.classify?.name }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">协议标识</div>
                    <div class="detail">{{ deviceInfo?.protocolCode }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">协议名称</div>
                    <div class="detail">{{ deviceInfo?.protocol?.name }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">在线状态</div>
                    <div class="detail">{{ deviceInfo?.online ? '在线' : '离线' }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">最后通信时间</div>
                    <div class="detail">{{ formatTime(deviceInfo?.lastConnectionTime) }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">异常状态</div>
                    <div class="detail">{{ deviceInfo?.abnormal ? '异常' : '正常' }}</div>
                </el-col>
                <el-col :span="6">
                    <div class="term">阀门状态</div>
                    <div class="detail">{{ deviceInfo?.valveState?.text }}</div>
                </el-col>
            </el-row>
        </el-card>
        <el-card shadow="never" class="md5 description_text">
            <div class="title">
                <span>扩展属性</span>
            </div>
            <el-row :gutter="20">
                <el-col :key="key" :span="6" v-for="(item, key) in productPropList">
                    <div class="term">{{ item.name }}</div>
                    <div class="detail">{{ getPropValue(item.code) }}</div>
                </el-col>
            </el-row>
        </el-card>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable } from 'element-plus'
import { getInfo } from '/@/api/platform/iot/device'

import { useRoute } from 'vue-router'
const route = useRoute()

const id = route.params.id

const deviceInfo = ref(null)
const productPropList = ref([])

const getDeviceInfo = async () => {
    const res = await getInfo(id)
    deviceInfo.value = res.data.device
    productPropList.value = res.data.productPropList
}

const getPropValue = (code: string) => {
    let properties = deviceInfo.value.prop.properties
    let prop = properties[code]
    return prop ? prop : ''
}

onMounted(() => {
    getDeviceInfo()
})
</script>
<style lang="scss" scoped>
.md5 {
    margin-bottom: 15px;
}
.description_text {
    .title {
        font-weight: 700;
        font-size: 16px;
        line-height: 1.5;
        margin-bottom: 20px;
        color: rgba(0, 0, 0, 0.85);
    }
    .term {
        color: rgba(0, 0, 0, 0.85);
        font-weight: 400;
        font-size: 14px;
        line-height: 22px;
        padding-bottom: 16px;
        margin-right: 8px;
        white-space: nowrap;
        display: table-cell;
        &:after {
            content: ':';
            margin: 0 8px 0 2px;
            position: relative;
            top: -0.5px;
        }
    }
    .detail {
        font-size: 14px;
        line-height: 1.5;
        width: 100%;
        padding-bottom: 16px;
        color: rgba(0, 0, 0, 0.65);
        display: table-cell;
    }
}
</style>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
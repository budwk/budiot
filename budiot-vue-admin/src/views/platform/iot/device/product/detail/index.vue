<template>
    <div style="padding-left: 5px">
        <el-row>
            <el-col :span="24">
            {{ product?.name }}
            </el-col>
        </el-row>
        <el-row class="field-item">
            <el-col :span="2" class="field-item__label">接入平台：</el-col>
            <el-col :span="2" class="field-item__content">{{ product?.iotPlatform?.text }}</el-col>
            <el-col :span="2" class="field-item__label">网络协议：</el-col>
            <el-col :span="2" class="field-item__content">{{ product?.protocolType?.text }}</el-col>
            <el-col :span="2" class="field-item__label">数据格式：</el-col>
            <el-col :span="2" class="field-item__content">
                <span v-if="product?.dataFormat == 'json'">JSON</span>
                <span v-else>自定义/透传</span>
            </el-col>
            <el-col :span="2" class="field-item__label">设备协议：</el-col>
            <el-col :span="2" class="field-item__content">{{ product?.protocol?.name }}</el-col>
            <el-col :span="2" class="field-item__label">设备类型：</el-col>
            <el-col :span="2" class="field-item__content">{{ product?.deviceType?.text }}</el-col>
        </el-row>
        <el-row>
            <el-col :span="24">
            统计分析
            </el-col>
        </el-row>
        <el-row class="field-warp" :gutter="20">
            <el-col :span="6">
              <el-card>
                  <el-statistic :value="countData['total']">
                      <template #title>
                          <div style="display: inline-flex; align-items: center">
                              设备总数
                          </div>
                      </template>
                  </el-statistic>
              </el-card>
          </el-col>
          <el-col :span="6">
              <el-card>
                  <el-statistic :value="countData['online']">
                      <template #title>
                          <div style="display: inline-flex; align-items: center">
                              在线设备
                          </div>
                      </template>
                  </el-statistic>
              </el-card>
          </el-col>
          <el-col :span="6">
              <el-card>
                  <el-statistic :value="countData['abnormal']">
                      <template #title>
                          <div style="display: inline-flex; align-items: center">
                              异常设备
                          </div>
                      </template>
                  </el-statistic>
              </el-card>
          </el-col>
        </el-row>
        <el-row class="field-warp" :gutter="20">
            <el-col :span="12">
               <el-row>
                    <el-col :span="24">通信成功率</el-col>
                    <el-col :span="24">
                        <div id="lineChart" />
                    </el-col>
               </el-row>
            </el-col>
            <el-col :span="12">
                <el-row>
                    <el-col :span="24">设备异常分布</el-col>
                    <el-col :span="24">
                        <div id="pieChart" />
                    </el-col>
               </el-row>
            </el-col>
        </el-row>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-product-detail">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { useRoute } from "vue-router"
import { getInfo, getDeviceCountMore } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string
const product = ref(null)
const eventData = ref([])
const countData = ref({})

const getProduct =  () => {
    getInfo(id).then(res => {
        product.value = res.data
    })
    getDeviceCountMore(id).then(res => {
        countData.value = res.data
    })
}

onMounted(() => {
    setTimeout(() => {
        getProduct()
    }, 300)
})
</script>
<style scoped lang="scss">
.field-warp {
    margin: 15px 0;
}
.field-item {
    margin: 10px 0;
    padding: 20px; 
    background-color: #eff0f4;
}
.field-item__label {
    font-size: 14px;
    align-self: baseline;
    font-family: PingFang SC;
    font-weight: 500;
    flex-shrink: 0;
    text-align: right;
}
.field-item__content {
    font-size: 14px;
    color: #666;
    line-height: 21px;
}
</style>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
<template>
    <div style="padding-left: 5px">
        <el-row>
            <el-col :span="24">
            {{ product?.name }}
            </el-col>
        </el-row>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-product-detail">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { useRoute } from "vue-router"
import { getInfo } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id
const product = ref(null)

const getProduct =  () => {
    getInfo(id).then(res => {
        product.value = res.data
    })
}

onMounted(() => {
    setTimeout(() => {
        getProduct()
    }, 300)
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
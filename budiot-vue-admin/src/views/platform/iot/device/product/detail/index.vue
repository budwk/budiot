<template>
    <div class="detail-container page-container">
        <div class="sub-menu-wrap">
            <div class="sub-menu">
                <router-link v-for="el in menuList" :key="el.id" replace :to="el.identifier">{{ el.name }}</router-link>
            </div>
        </div>
        <div v-if="product" class="child-content">
            <router-view />
        </div>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-product-detail">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
const route = useRoute()
const router = useRouter()
const id = route.params.id
const product = ref(null)

const menuList = ref([
    { id: 1, name: '基本信息', identifier: '/platform/iot/device/product/'+id+'/detail' },
    { id: 2, name: '设备列表', identifier: '/platform/iot/device/product/'+id+'/device' },
    { id: 3, name: '事件列表', identifier: '/platform/iot/device/product/'+id+'/event' },
    { id: 4, name: '指令列表', identifier: '/platform/iot/device/product/'+id+'/command' },
    { id: 5, name: '订阅管理', identifier: '/platform/iot/device/product/'+id+'/subscribe' },
    { id: 6, name: '参数配置', identifier: '/platform/iot/device/product/'+id+'/config' }
])

onMounted(() => {

})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
<template>
    <div class="app-container">
        <Head :data="product"/>
        <div class="sub-menu-wrap">
            <div class="sub-menu">
                <el-menu
                :default-active="activeIndex"
                class="el-menu-demo"
                mode="horizontal"
                @select="handleMenu"
                >
                     <el-menu-item :index="el.id" v-for="el in menuList" :key="el.id" >{{ el.name }}</el-menu-item>
                </el-menu>
            </div>
        </div>
        <router-view style="width: 100%;"/>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-product-id">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
import Head from './head.vue'
import { getInfo } from '/@/api/platform/iot/product'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const product = ref(null)
const activeIndex = route.fullPath.split(`/${id}/`)[1]

const menuList = ref([])
const menuListSys = ref([
    { id: 'detail', name: '基本信息', path: '/platform/iot/device/product/'+id+'/detail' },
    { id: 'device', name: '设备列表', path: '/platform/iot/device/product/'+id+'/device' },
    { id: 'event', name: '事件列表', path: '/platform/iot/device/product/'+id+'/event' },
    { id: 'command', name: '指令列表', path: '/platform/iot/device/product/'+id+'/command' },
    { id: 'subscribe', name: '订阅管理', path: '/platform/iot/device/product/'+id+'/subscribe' },
    { id: 'dtuparam', name: 'DTU参数管理', path: '/platform/iot/device/product/'+id+'/dtuparam' },
    { id: 'firmware', name: '固件管理', path: '/platform/iot/device/product/'+id+'/firmware' },
    { id: 'config', name: '产品配置', path: '/platform/iot/device/product/'+id+'/config' }
])

const handleMenu = (key: string, keyPath: string[]) => {
    // 从menuList中找到对应的path
    const path = menuList.value.find((el) => el.id === key)?.path
    if (path) {
        router.push(path)
    }
}

const getProduct = async () => {
    const res = await getInfo(id)
    product.value = res.data
    const menus = product.value?.menus
    // 遍历 menus，如果 display 为false，则从menuList中删除
    menuList.value = menuListSys.value.filter((el) => {
        return menus.find((menu) => menu.code === el.id && menu.display)
    })

}

onMounted(() => {
    getProduct()
})
</script>

<style scoped lang="scss">

.sub-menu-wrap {
    padding: 5px 5px 0 5px;
    margin-bottom: 15px;
}
.sub-menu {
    border-bottom: 1px solid rgba($color: #d9d9d9, $alpha: 0.6);

    > a {
        padding: 0 5px 10px 5px;
        margin-bottom: -1px;
        margin-right: 30px;
        color: #333;
        font-weight: 500;
        border-bottom: 2px solid transparent;

    }
}
</style>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
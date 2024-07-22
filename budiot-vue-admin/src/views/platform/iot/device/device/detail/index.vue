<template>
    <div class="app-container">
        <Head :data="device"/>
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
<script setup lang="ts" name="platform-device-detail-id">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
import Head from './head.vue'
import { getName } from '/@/api/platform/iot/device'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const fpid = route.query.fpid
const device = ref(null)
const activeIndex = route.fullPath.split(`/${id}/`)[1].split('?')[0]

const menuList = ref([
    { id: 'base', name: '基本信息', path: '/platform/iot/device/detail/'+id+'/base'+(fpid ? `?fpid=${fpid}` : '') },
    { id: 'data', name: '数据上报', path: '/platform/iot/device/detail/'+id+'/data'+(fpid ? `?fpid=${fpid}` : '') },
    { id: 'event', name: '事件上报', path: '/platform/iot/device/detail/'+id+'/event'+(fpid ? `?fpid=${fpid}` : '') },
    { id: 'raw', name: '通信报文', path: '/platform/iot/device/detail/'+id+'/raw'+(fpid ? `?fpid=${fpid}` : '') },
    { id: 'cmd', name: '指令下发', path: '/platform/iot/device/detail/'+id+'/cmd'+(fpid ? `?fpid=${fpid}` : '') },
])

const handleMenu = (key: string, keyPath: string[]) => {
    // 从menuList中找到对应的path
    const path = menuList.value.find((el) => el.id === key)?.path
    if (path) {
        router.push(path)
    }
}

const getDeviceInfo = async () => {
    const res = await getName(id)
    device.value = res.data
}

onMounted(() => {
    getDeviceInfo()
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
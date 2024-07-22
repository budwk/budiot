<template>
    <div>
        <el-row type="flex" justify="space-between" align="middle" class="el-page-title">
            <span>
                <el-button link type="primary" @click="goToBack">
                    <icon name="fa fa-long-arrow-left" style="color: #1b81e5;margin-right: 5px;" class="form-item-icon" size="18"/> 
                    {{ data?.deviceNo }}
                </el-button>
            </span>
        </el-row>
    </div>
</template>
<script setup lang="ts" >
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const id = route.params.id
const fpid = route.query.fpid
const activeIndex = route.fullPath.split(`/${id}/`)[1].split('?')[0]

const props = defineProps({
    data: {
        type: Object,
        default: () => {}
    }
})

const goToBack = () => {
    if(fpid){
        if(activeIndex == 'event'){
            router.push(`/platform/iot/device/product/${fpid}/event`)
        }else if(activeIndex == 'cmd'){
            router.push(`/platform/iot/device/product/${fpid}/command`)
        }else {
            router.push(`/platform/iot/device/product/${fpid}/device`)
        }
    }else{
        router.push('/platform/iot/device/device')
    }
    
}
</script>
<style scoped>
.el-page-title {
    background-color: #fff;
    font-size: 16px;
    padding-left: 5px;
}
</style>
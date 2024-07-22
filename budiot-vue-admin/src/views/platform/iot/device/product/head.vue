<template>
    <div>
        <el-row type="flex" justify="space-between" align="middle" class="el-page-title">
            <span>
                <el-button link type="primary" @click="goToBack">
                    <icon name="fa fa-long-arrow-left" style="color: #1b81e5;margin-right: 5px;" class="form-item-icon" size="18"/> 
                    {{ data?.name }}
                </el-button>
            </span>
        </el-row>
    </div>
</template>
<script setup lang="ts" >
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const props = defineProps({
    data: {
        type: Object,
        default: () => {}
    }
})

const goToBack = () => {
    router.push('/platform/iot/device/product')
}
</script>
<style scoped>
.el-page-title {
    background-color: #fff;
    font-size: 16px;
    padding-left: 5px;
}
</style>
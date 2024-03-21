<template>
    <div>
        <el-row type="flex" justify="space-between" align="middle" class="el-page-title">
            <span>
                <router-link to="/platform/iot/device/product" style="">
                    <icon name="fa fa-long-arrow-left" class="form-item-icon" size="16"/>
                    {{ data?.name }}

                </router-link>
            </span>
        </el-row>
    </div>
</template>
<script setup lang="ts" >
const props = defineProps({
    data: {
        type: Object,
        default: () => {}
    }
})
</script>
<style scoped>
.el-page-title {
    background-color: #fff;
    font-size: 16px;
    padding-left: 5px;
}
</style>
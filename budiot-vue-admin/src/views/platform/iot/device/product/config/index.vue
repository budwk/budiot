<template>
    <el-row type="flex" class="column-box">
        <div class="column-box__side" style="height: calc(100vh - 220px)">
            <el-tabs v-model="activeTab" tab-position="left" style="width: 220px;" @tab-change="tabChange">
                <el-tab-pane v-for="(item,ind) in tabList" :key="'comp_'+ind" :label="item.text" :name="item.name"  :style="{ width: '500px' }"/>
            </el-tabs>
        </div>

        <div class="column-box__main">
            <component :is="activeBlock" />
        </div>
    </el-row>
</template>
<script setup lang="ts" >
import { nextTick, onMounted, reactive, ref, toRefs, shallowRef } from 'vue'
import modal from '/@/utils/modal'
import { useRoute } from "vue-router"
import deviceAttr from "./deviceAttr.vue"
import deviceProp from "./deviceProp.vue"
import deviceEvent from "./deviceEvent.vue"
import deviceCmd from "./deviceCmd.vue"

const activeTab = ref('deviceAttr')
const activeBlock = shallowRef(deviceAttr)

const tabList = ref([
    { name: 'deviceAttr', text: '参数配置' },
    { name: 'deviceProp', text: '属性配置' },
    { name: 'deviceEvent', text: '事件配置' },
    { name: 'deviceCmd', text: '指令配置' }
])

const tabChange = (val: string) => {
    console.log(val)
    switch (val) {
        case 'deviceAttr':
            activeTab.value = 'deviceAttr'
            activeBlock.value = deviceAttr
            break
        case 'deviceProp':
            activeTab.value = 'deviceProp'
            activeBlock.value = deviceProp
            break
        case 'deviceEvent':
            activeTab.value = 'deviceEvent'
            activeBlock.value = deviceEvent
            break
        case 'deviceCmd':
            activeTab.value = 'deviceCmd'
            activeBlock.value = deviceCmd
            break
    }
}
</script>
<style scoped lang="scss">

.column-box {
    display: flex;
    justify-content: space-between;
    &__side {
        flex-shrink: 0;
        min-width: 220px;
        margin-right: 15px;
        padding: 15px 0;
        background-color: #fff;
        overflow: auto;
        overflow-x: hidden;
        > .search {
            padding: 0 15px;
        }
    }
    &__main {
        flex-grow: 1;
        max-width: calc(100% - 225px);
        background-color: #fff;
        .el-card + .el-card {
            margin-top: 0;
            border-top: 15px solid #eff0f4;
        }
    }

    .el-tabs__nav {
        width: 220px;
    }

    .el-tabs__content {
        display: none;
    }

    .el-tabs__header {
        width: 100%;
    }
    .el-tabs--left .el-tabs__item {
        min-width: auto;
        width: 100%;
    }
}

</style>

<template>
    <div class="app-container">
        <el-row :gutter="20">
            <el-col :span="4">
                <el-row type="flex" justify="end" v-permission="['iot.device.scene.create']" class="role-create">
                    <el-button link type="primary" @click="spaceHandleCreate">
                        新建空间
                    </el-button>
                </el-row>
                <div class="head-container">
                    <el-input
v-model="querySpace.name" placeholder="请输入空间名称" clearable prefix-icon="Search"
                        style="margin-bottom: 20px" />
                </div>
                <div class="head-container">
                    <el-tree
:data="spaceTreeData" :props="{ label: 'name', children: 'children' }"
                        :expand-on-click-node="false" :filter-node-method="spaceFilterNode" ref="spaceTreeRef"
                        highlight-current default-expand-all @node-click="spaceNodeClick">
                        <template #default="{ node, data }" >
                            <el-row @mouseover="enter(data.id)" style="width: 100%;padding-left: -10px;">
                                <el-col :span="24" style="cursor: pointer;">
                                {{ data.name }}

                                    <span v-if="treeIndex == data.id" class="operate"  :index="data.id" style="right: 2px;position: absolute;">
                                        <el-button link type="primary" style="margin-left:2px;"  v-permission="['iot.device.scene.create']"  @click="spaceHandleCreate(data)"
                                            ><i class="fa fa-plus-square-o" />
                                        </el-button>
                                        <el-button link type="primary" style="margin-left:2px;" v-permission="['iot.device.scene.update']"  @click="spaceHandleUpdate(data)"
                                            ><i class="fa fa-pencil-square-o" />
                                        </el-button>
                                        <el-button link type="danger" style="margin-left:2px;" v-permission="['iot.device.scene.delete']"  @click="spaceHandleDelete(data)"
                                            ><i class="fa fa-trash-o" /></el-button>
                                        
                                    </span>
                                </el-col>
                            </el-row>
                        </template>
                    </el-tree>
                </div>
            </el-col>
            <el-col :span="20">
                <scene ref="sceneRef" :space-id="spaceId" :space-name="spaceName"/>
            </el-col>
        </el-row>

        <el-dialog title="新建场景空间" v-model="showSpaceCreate" width="35%">
            <el-form ref="spaceCreateRef" :model="formDataSpace" :rules="formRulesSpace" label-width="100px">
                <el-col :span="24">
                    <el-form-item label="上级空间" prop="parentId">
                        <el-tree-select v-model="formDataSpace.parentId" :data="spaceTreeData"
                            :props="{ value: 'id', label: 'name', children: 'children' }" value-key="id"
                            placeholder="选择上级空间" check-strictly :render-after-expand="false" style="width:100%"
                            />
                    </el-form-item>
                </el-col>
                <el-col :span="24">
                    <el-form-item label="空间名称" prop="name">
                        <el-input v-model="formDataSpace.name" placeholder="请输入空间名称" />
                    </el-form-item>
                </el-col>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="createSpace" :loading="btnLoading">确 定</el-button>
                    <el-button @click="showSpaceCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>    

        <el-dialog title="修改场景空间" v-model="showSpaceUpdate" width="35%">
            <el-form ref="spaceUpdateRef" :model="formDataSpace" :rules="formRulesSpace" label-width="100px">
                <el-col :span="24">
                    <el-form-item label="空间名称" prop="name">
                        <el-input v-model="formDataSpace.name" placeholder="请输入空间名称" />
                    </el-form-item>
                </el-col>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="updateSpace" :loading="btnLoading">确 定</el-button>
                    <el-button @click="showSpaceUpdate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>  
    </div>
</template>
<script setup lang="ts" name="platform-device-device-scene">
import { nextTick, onMounted, watch, ref } from 'vue'
import modal from '/@/utils/modal'
import { getSpaceList, doSpaceCreate, doSpaceDelete, getSpaceInfo, doSpaceUpdate } from '/@/api/platform/iot/scene'
import { handleTree, findOneValue } from '/@/utils/common'
import { ElForm, ElTree } from 'element-plus'
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
import scene from './scene.vue'
const sceneRef = ref<typeof scene>()
const route = useRoute()
const router = useRouter()

const querySpace = ref({
    name: ''
})
const spaceTreeRef = ref<InstanceType<typeof ElTree>>()
const spaceListData = ref([])
const spaceTreeData = ref([])
const treeIndex = ref('')
const showSpaceCreate = ref(false)
const showSpaceUpdate = ref(false)
const btnLoading = ref(false)
const spaceCreateRef =  ref<InstanceType<typeof ElForm>>()
const spaceUpdateRef =  ref<InstanceType<typeof ElForm>>()
const spaceId = ref('')  
const spaceName = ref('')  

const formDataSpace = ref({
    name: '',
    parentId: '',
    id: ''
})
const formRulesSpace = ref({
    name: [
        { required: true, message: '请输入空间名称', trigger: 'blur' }
    ]
})

// 鼠标移动
const enter = (index: string) => {
    treeIndex.value = index
}

// 筛选名称
watch(querySpace.value, val => {
    spaceTreeRef.value?.filter(val.name)
})


// 通过条件过滤节点
const spaceFilterNode = (value: any, data: any) => {
    if (!value) return true
    return data.name.indexOf(value) !== -1
}

// 空间树节点点击事件
const spaceNodeClick = (data: any) => {
    spaceId.value = data.id
    spaceName.value = data.name
    nextTick(() => {
        sceneRef.value?.list()
    })
}

// 新建空间
const spaceHandleCreate = (data: any) => {
    formDataSpace.value = {
        name: '',
        parentId: data ? data.id : '',
        id: ''
    }
    spaceCreateRef.value?.resetFields()
    showSpaceCreate.value = true
}

const createSpace = async () => {
    await spaceCreateRef.value?.validate()
    btnLoading.value = true
    const { code } = await doSpaceCreate(formDataSpace.value)
    if (code === 0) {
        modal.msgSuccess('新建成功')
        showSpaceCreate.value = false
        getSpaceListData()
    }
    btnLoading.value = false
}

// 修改空间
const spaceHandleUpdate = async (row: any) => {
    const { code, data } = await getSpaceInfo(row.id)
    if (code === 0) {
        formDataSpace.value = data
        showSpaceUpdate.value = true
    }
}

const updateSpace = async () => {
    await spaceUpdateRef.value?.validate()
    btnLoading.value = true
    const { code } = await doSpaceUpdate(formDataSpace.value)
    if (code === 0) {
        modal.msgSuccess('修改成功')
        showSpaceUpdate.value = false
        getSpaceListData()
    }
    btnLoading.value = false
}

const spaceHandleDelete = async (row: any) => {
    modal.confirm('确定删除空间（名称 ' + row.name + '）？').then(() => {
        return doSpaceDelete({id: row.id,name: row.name})
    }).then(() => {
        modal.msgSuccess('删除成功')
        init()
    }).catch(() => { })
}

const getSpaceListData = async () => {
    const {code, data} = await getSpaceList(querySpace.value)
    if (code === 0) {
        spaceListData.value = data
        spaceTreeData.value = handleTree(data)
    }
}

const init = async () => {
    getSpaceListData()
}

onMounted(() => {
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
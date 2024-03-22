
<template>
    <div class="app-container">
        <el-row>
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
                <el-form-item label="设备分类" prop="classifyId">
                    <el-cascader style="width: 100%;" clearable :options="classifyList"
                        :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children', emitPath: false }"
                        v-model="queryParams.classifyId"
                        />
                </el-form-item>
                <el-form-item label="产品名称" prop="name">
                    <el-input v-model="queryParams.name" placeholder="请输入产品名称" clearable style="width: 180px"
                        @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
                    <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                </el-form-item>
            </el-form>
        </el-row>
        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button plain type="primary" icon="Plus" @click="handleCreate"
                    v-permission="['iot.device.product.create']">新增
                </el-button>
            </el-col>
            <right-toolbar :quickSearchShow="true" quickSearchPlaceholder="通过名称搜索" v-model:showSearch="showSearch" :extendSearch="true" @quickSearch="handleSearch" />
        </el-row>
        <el-row :gutter="20">
            <el-col :lg="6" :xs="24" class="product-box" v-for="(product, idx) in tableData" :key="idx">
                <el-card class="box-card" shadow="hover" @mouseover="showButton(idx)" @mouseleave="showButton(-1)"
                    :class="{ active: showButtonIdx === idx }">
                    <template #header>
                        <div class="card-header">
                            <el-row>
                                <el-col :span="12">
                                    <el-tag :color="findClassifyColor(product?.classifyId)" effect="dark" class="type-tag">{{findClassifyName(product?.classifyId)}}</el-tag>
                                </el-col>
                                <el-col :span="12" style="text-align: right" v-show="showButtonIdx == idx">
                                    <el-tooltip content="修改" placement="top">
                                        <el-button link type="primary" icon="EditPen" @click="handleUpdate(product)"
                                            v-permission="['iot.device.product.update']"></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top">
                                        <el-button link type="danger" icon="Delete" @click="handleDelete(product)"
                                            v-permission="['iot.device.product.delete']"  style="margin-left: 5px;"></el-button>
                                    </el-tooltip>
                                </el-col>
                            </el-row>
                            <el-row @click="handleDetail(product.id)">
                                <span class="product-title">{{ product.name }}</span>
                            </el-row>
                        </div>
                    </template>
                    <el-row @click="handleDetail(product.id)">
                        <el-col>
                            <el-form-item label="设备数量：" class="product-field-item">
                                {{ product?.deviceCount }}
                            </el-form-item>
                        </el-col>
                        <el-col>
                            <el-form-item label="接入平台：" class="product-field-item">
                                {{ product?.iotPlatform?.text }}
                            </el-form-item>
                        </el-col>
                        <el-col>
                            <el-form-item label="网络协议：" class="product-field-item">
                                {{ product?.protocolType?.text }}
                            </el-form-item>
                        </el-col>
                        <el-col>
                            <el-form-item label="数据格式：" class="product-field-item">
                                <span v-if="product?.dataFormat == 'json'">JSON</span>
                                <span v-else>自定义/透传</span>
                            </el-form-item>
                        </el-col>
                        <el-col>
                            <el-form-item label="设备协议" class="product-field-item">
                                {{ findProtocol(product?.protocolId) }}
                            </el-form-item>
                        </el-col>
                        <el-col>
                            <el-form-item label="设备类型：" class="product-field-item">
                                {{ product?.deviceType?.text }}
                            </el-form-item>
                        </el-col>

                    </el-row>
                </el-card>
            </el-col>
        </el-row>
        <el-row v-show="tableData.length == 0" justify="center">
            <span class="no-data">暂无数据</span>
        </el-row>
        <pagination :total="queryParams.totalCount" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
            @pagination="list" />

        <el-dialog title="新增产品" v-model="showCreate" width="45%">
            <el-form ref="createRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="产品名称" prop="name">
                    <el-input v-model="formData.name" placeholder="请输入产品名称" />
                </el-form-item>
                <el-form-item label="设备分类" prop="classifyId">
                    <el-cascader style="width: 100%;" clearable :options="classifyList"
                        :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children',emitPath: false }"
                        v-model="formData.classifyId"/>
                </el-form-item>
                <el-form-item label="设备类型" prop="deviceType">
                    <el-select v-model="formData.deviceType" placeholder="请选择设备类型" clearable style="width: 100%;">
                        <el-option v-for="item in deviceType" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="接入平台" prop="iotPlatform" >
                    <el-select v-model="formData.iotPlatform" placeholder="请选择接入平台" @change="platformChange" clearable style="width: 100%;">
                        <el-option v-for="item in iotPlatform" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="网络协议" prop="protocolType">
                    <el-select v-model="formData.protocolType" placeholder="请选择接入协议" @change="protocolChange" clearable style="width: 100%;">
                        <el-option v-for="item in protocolType" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="formData.protocolType == 'MQTT' || formData.protocolType == 'HTTP' && formData.iotPlatform == 'AEP' || formData.protocolType == 'MQ' && formData.iotPlatform == 'AEP'"
                 label="协议认证" prop="propertieList"
                    class="label-font-weight">
                    <template v-for="(obj, idx) in formData.propertieList" :key="idx">
                        {{ obj.name }}: 
                        <el-input v-if="obj.type=='string'" v-model="obj.value" :placeholder="obj.note" />
                        <el-radio-group v-if="obj.type=='boolean'" v-model="obj.value" style="padding-left: 10px;">
                            <el-radio value="true">是</el-radio>
                            <el-radio value="false">否</el-radio>
                        </el-radio-group>
                    </template>
                </el-form-item>
                <el-form-item label="计费方式" prop="payMode" v-if="formData.deviceType=='METER'">
                    <el-radio-group v-model="formData.payMode">
                        <el-radio :value="0">无</el-radio>
                        <el-radio :value="1">表端计费</el-radio>
                        <el-radio :value="2">平台计费</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="数据格式" prop="dataFormat">
                    <el-select v-model="formData.dataFormat" placeholder="请选择数据格式" clearable style="width: 100%;">
                        <el-option label="JSON" value="json" />
                        <el-option label="自定义/透传" value="custom" />
                    </el-select>
                </el-form-item>
                <el-form-item label="设备协议" prop="protocolId">
                    <el-select v-model="formData.protocolId" placeholder="请选择设备协议" clearable style="width: 100%;">
                        <el-option v-for="item in protocolList" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="产品描述" prop="description">
                    <el-input v-model="formData.description" placeholder="" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="create">确 定</el-button>
                    <el-button @click="showCreate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="修改产品" v-model="showUpdate" width="45%">
            <el-form ref="updateRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="产品名称" prop="name">
                    <el-input v-model="formData.name" placeholder="请输入产品名称" />
                </el-form-item>
                <el-form-item label="设备分类" prop="classifyId">
                    <el-cascader style="width: 100%;" clearable :options="classifyList"
                        :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children',emitPath: false }"
                        v-model="formData.classifyId"/>
                </el-form-item>
                <el-form-item label="设备类型" prop="deviceType">
                    <el-select v-model="formData.deviceType" placeholder="请选择设备类型" clearable style="width: 100%;">
                        <el-option v-for="item in deviceType" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="接入平台" prop="iotPlatform" >
                    <el-select v-model="formData.iotPlatform" placeholder="请选择接入平台" @change="platformChange" clearable style="width: 100%;">
                        <el-option v-for="item in iotPlatform" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="网络协议" prop="protocolType">
                    <el-select v-model="formData.protocolType" placeholder="请选择接入协议" @change="protocolChange" clearable style="width: 100%;">
                        <el-option v-for="item in protocolType" :key="item.value" :label="item.text" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="formData.protocolType == 'MQTT' || formData.protocolType == 'HTTP' && formData.iotPlatform == 'AEP' || formData.protocolType == 'MQ' && formData.iotPlatform == 'AEP'"
                 label="协议认证" prop="propertieList"
                    class="label-font-weight">
                    <template v-for="(obj, idx) in formData.propertieList" :key="idx">
                        {{ obj.name }}: 
                        <el-input v-if="obj.type=='string'" v-model="obj.value" :placeholder="obj.note" />
                        <el-radio-group v-if="obj.type=='boolean'" v-model="obj.value" style="padding-left: 10px;">
                            <el-radio value="true">是</el-radio>
                            <el-radio value="false">否</el-radio>
                        </el-radio-group>
                    </template>
                </el-form-item>
                <el-form-item label="计费方式" prop="payMode" v-if="formData.deviceType=='METER'">
                    <el-radio-group v-model="formData.payMode">
                        <el-radio :value="0">无</el-radio>
                        <el-radio :value="1">表端计费</el-radio>
                        <el-radio :value="2">平台计费</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="数据格式" prop="dataFormat">
                    <el-select v-model="formData.dataFormat" placeholder="请选择数据格式" clearable style="width: 100%;">
                        <el-option label="JSON" value="json" />
                        <el-option label="自定义/透传" value="custom" />
                    </el-select>
                </el-form-item>
                <el-form-item label="设备协议" prop="protocolId">
                    <el-select v-model="formData.protocolId" placeholder="请选择设备协议" clearable style="width: 100%;">
                        <el-option v-for="item in protocolList" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="产品描述" prop="description">
                    <el-input v-model="formData.description" placeholder="" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="update">确 定</el-button>
                    <el-button @click="showUpdate = false">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts" name="platform-device-device-product">
import { nextTick, onMounted, reactive, ref } from 'vue'
import modal from '/@/utils/modal'
import { getInit, doCreate, doUpdate, getInfo, getList, doDelete, getDeviceCount } from '/@/api/platform/iot/product'
import { toRefs } from '@vueuse/core'
import { handleTree, findOneValue } from '/@/utils/common'
import { ElForm, ElUpload } from 'element-plus'
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
const route = useRoute()
const router = useRouter()

const auth_MQTT = [{name:'username',value:'',note:'用户名',type:'string'  },{name:'password',value:'',note:'密码',type:'string'  }]
const auth_AEP_HTTP = [{name:'masterKey',value:'',note:'AEP产品masterKey',type:'string' },{name:'productId',value:'',note:'AEP产品productId',type:'string'  },{name:'hasProfile',value:'true',note:'是否有Profile文件',type:'boolean'  }]
const auth_AEP_MQ = [{name:'productId',value:'',note:'AEP产品productId',type:'string'  }]

const createRef = ref<InstanceType<typeof ElForm>>()
const updateRef = ref<InstanceType<typeof ElForm>>()
const queryRef = ref<InstanceType<typeof ElForm>>()

const showSearch = ref(true)
const showCreate = ref(false)
const showUpdate = ref(false)
const refreshTable = ref(true)
const tableLoading = ref(false)
const tableData = ref([])
const showButtonIdx = ref(-1)
const classifyList = ref([])
const classifyListOrigin = ref([])
const protocolList = ref([])
const iotPlatform = ref([])
const protocolType = ref([])
const deviceType = ref([])

const data = reactive({
    formData: {
        id: '',
        name: '',
        classifyId: '',
        deviceType: '',
        dataFormat: 'json',
        iotPlatform: '',
        protocolType: '',
        protocolId: '',
        properties: {},
        propertieList: [],
        description: '',
        payMode: 0,
    },
    queryParams: {
        classifyId: '',
        name: '',
        pageNo: 1,
        pageSize: 8,
        totalCount: 0,
        pageOrderName: '',
        pageOrderBy: '',
    },
    formRules: {
        name: [{ required: true, message: "产品名称不能为空", trigger: ["blur", "change"] }],
        classifyId: [{ required: true, message: "设备分类不能为空", trigger: ["blur", "change"] }],
        deviceType: [{ required: true, message: "设备类型不能为空", trigger: ["blur", "change"] }],
        iotPlatform: [{ required: true, message: "接入平台不能为空", trigger: ["blur", "change"] }],
        protocolType: [{ required: true, message: "接入协议不能为空", trigger: ["blur", "change"] }],
        protocolId: [{ required: true, message: "设备协议不能为空", trigger: ["blur", "change"] }],
        dataFormat: [{ required: true, message: "数据格式不能为空", trigger: ["blur", "change"] }],
    },
})

const { queryParams, formData, formRules } = toRefs(data)

// 重置表单
const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    formData.value = {
        id: '',
        name: '',
        classifyId: '',
        deviceType: '',
        dataFormat: 'json',
        iotPlatform: 'DIRECT',
        protocolType: '',
        protocolId: '',
        properties: {},
        propertieList: [],
        description: '',
        payMode: 0,
    }
    formEl?.resetFields()
}

const showButton = (val: any) => {
    showButtonIdx.value = val
}

// 刷新
const handleSearch = () => {
    queryParams.value.pageNo = 1
    list()
}

// 重置搜索
const resetSearch = () => {
    queryRef.value?.resetFields()
    list()
}

// 查看详情
const handleDetail = (id: string) => {
    router.push('/platform/iot/device/product/' + id+'/detail')
}

// 查找protocol协议
const findProtocol = (val: any) => {
    return findOneValue(protocolList.value, val, 'name', 'id')
}

// 查找classify分类名称
const findClassifyName = (val: any) => {
    return findOneValue(classifyListOrigin.value, val, 'name', 'id')
}

//查找classify分类颜色 , 如果本级没有则查找父级的颜色
const findClassifyColor = (val: any) => {
    let color = findOneValue(classifyListOrigin.value, val, 'color', 'id')
    if (color) {
        return color
    } else {
        let parentId = findOneValue(classifyListOrigin.value, val, 'parentId', 'id')
        return findOneValue(classifyListOrigin.value, parentId, 'color', 'id')
    } 
}

const platformChange = (val: string) => {
    if(formData.value.iotPlatform == 'AEP') {
        if(formData.value.protocolType == 'MQ'){
            formData.value.propertieList = JSON.parse(JSON.stringify(auth_AEP_MQ))
        }
        if(formData.value.protocolType == 'HTTP'){
            formData.value.propertieList = JSON.parse(JSON.stringify(auth_AEP_HTTP))
        }
        if(formData.value.protocolType == 'MQTT') {
            formData.value.propertieList = JSON.parse(JSON.stringify(auth_MQTT))
        }
    } else if(formData.value.protocolType == 'MQTT') {
        formData.value.propertieList = JSON.parse(JSON.stringify(auth_MQTT))
    } else {
        formData.value.propertieList = []
    }
}

const protocolChange = (val: string) => {
    platformChange(formData.value.iotPlatform)
}

// 查询表格
const list = () => {
    tableLoading.value = true
    getList(queryParams.value).then((res) => {
        tableLoading.value = false
        tableData.value = res.data.list as never
        queryParams.value.totalCount = res.data.totalCount as never
        deviceCount()
    })
}

// 通过每个产品的ID查询统计产品下设备数量
const deviceCount = () => {
    let ids = tableData.value.map((item: any) => item.id)
    getDeviceCount({ids: ids}).then((res) => {
        tableData.value.forEach((item: any) => {
            item.deviceCount = res.data[item.id] || 0
        })
    })
}

const init = () => {
    getInit().then((res) => {
        iotPlatform.value = res.data.iotPlatform
        classifyList.value = handleTree(res.data.classifyList) as never
        classifyListOrigin.value = res.data.classifyList
        protocolList.value = res.data.protocolList
        protocolType.value = res.data.protocolType
        deviceType.value = res.data.deviceType
    })
}

// 刷新
const quickSearch = (data: any) => {
    refreshTable.value = false
    queryParams.value.pageNo = 1
    list()
    nextTick(() => {
        refreshTable.value = true
    })
}

// 新增按钮
const handleCreate = (row: any) => {
    resetForm(createRef.value)
    showCreate.value = true
}

// 修改按钮
const handleUpdate = (row: any) => {
    getInfo(row.id).then((res: any) => {
        formData.value = res.data
        formData.value.iotPlatform = res.data.iotPlatform.value
        formData.value.protocolType = res.data.protocolType.value
        formData.value.deviceType = res.data.deviceType.value
        platformChange(formData.value.iotPlatform)
        if(formData.value.properties) {
            let properties = formData.value.properties
            // 将map对象properties里的值赋值到propertieList
            formData.value.propertieList.forEach((item: any) => {
                item.value = properties[item.name]
            })
        }
        showUpdate.value = true
    })
}

// 删除按钮
const handleDelete = (row: any) => {
    modal.confirm('确定删除产品 ' + row.name + '？产品下设备也将被删除！').then(() => {
        return doDelete(row.id)
    }).then(() => {
        queryParams.value.pageNo = 1
        list()
        modal.msgSuccess('删除成功')
    }).catch(() => { })
}


// 提交新增
const create = () => {
    if (!createRef.value) return
    createRef.value.validate((valid) => {
        if (valid) {
            // 将properties转为map对象字符串
            let properties = {}
            formData.value.propertieList.forEach((item: any) => {
                properties[item.name] = item.value
            })
            formData.value.properties = JSON.stringify(properties)
            doCreate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showCreate.value = false
                queryParams.value.pageNo = 1
                list()
            })
        }
    })
}

// 提交修改
const update = () => {
    if (!updateRef.value) return
    updateRef.value.validate((valid) => {
        if (valid) {
            doUpdate(formData.value).then((res: any) => {
                modal.msgSuccess(res.msg)
                showUpdate.value = false
                list()
            })
        }
    })
}

onMounted(() => {
    list()
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
<style lang='scss' scoped>
.product-box {
    margin-bottom: 20px;
}

.type-tag {
    border-style: none;
}

.active {
    cursor: pointer;
}

.product-field-item {
    margin-bottom: 2px;
}

.product-title {
    color: #2476e0;
    font-size: 18px;
    font-weight: 700;
    margin-top: 8px;
    margin-bottom: 2px;
}

.no-data {
    text-align: center;
    font-size: 14px;
    color: #999;
    height: 50px;
}
</style>
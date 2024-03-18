
<template>
    <div class="app-container home">

        <el-row :gutter="20">
            <el-col :sm="12" :lg="12" style="padding-left: 15px">
              <span style="padding-right: 18px;color: darkgray;">{{ thisWeekStart }} ~ {{ thisWeekEnd }}</span>
            </el-col>
            <el-col :sm="12" :lg="12" style="padding-right: 65px;text-align: right;">
                <el-button-group>
                    <el-button plain @click="lastWeek"><el-icon class="el-icon--left"><ArrowLeft /></el-icon></el-button>
                    <el-button plain @click="thisWeek">本周</el-button>
                    <el-button plain @click="nextWeek" :disabled="nextWeekNum==0">
                    <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                    </el-button>
                </el-button-group>
            </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-row :gutter="20" class="total_css">
              <el-col :span="8">
                  <el-card>
                <div class="statistic-card">
                  <el-statistic :value="thisWeek_oreStockTotal">
                    <template #title>
                      <div style="display: inline-flex; align-items: center">
                        <el-link @click="gotoPath(-1)">原矿入库（吨）</el-link>
                        <el-tooltip
                          effect="dark"
                          content="周原矿入库吨位"
                          placement="top"
                        >
                          <el-icon style="margin-left: 4px" :size="12">
                            <Warning />
                          </el-icon>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-statistic>
                  <div class="statistic-footer">
                    <div class="footer-item">
                      <span>同比上周</span>
                      <span :class="rate(thisWeek_oreStockTotal,lastWeek_oreStockTotal)>=0?'red':'green'">
                      <span>{{rate(thisWeek_oreStockTotal,lastWeek_oreStockTotal)}}%</span>
                        <el-icon>
                          <CaretTop v-if="rate(thisWeek_oreStockTotal,lastWeek_oreStockTotal)>=0"/>
                          <CaretBottom v-else/>
                        </el-icon>
                      </span>
                    </div>
                  </div>
                </div></el-card>
              </el-col>
              <el-col :span="8">
                  <el-card>
                <div class="statistic-card">
                  <el-statistic :value="thisWeek_stockTotal">
                    <template #title>
                      <div style="display: inline-flex; align-items: center">
                        <el-link @click="gotoPath(1)">成品出库（吨）</el-link>
                        <el-tooltip
                          effect="dark"
                          content="周成品出库吨位"
                          placement="top"
                        >
                          <el-icon style="margin-left: 4px" :size="12">
                            <Warning />
                          </el-icon>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-statistic>
                  <div class="statistic-footer">
                    <div class="footer-item">
                      <span>同比上周</span>
                      <span :class="rate(thisWeek_stockTotal,lastWeek_stockTotal)>=0?'red':'green'">
                      <span>{{rate(thisWeek_stockTotal,lastWeek_stockTotal)}}%</span>
                        <el-icon>
                          <CaretTop v-if="rate(thisWeek_stockTotal,lastWeek_stockTotal)>=0"/>
                          <CaretBottom v-else/>
                        </el-icon>
                      </span>
                    </div>
                  </div>
                </div></el-card>
              </el-col>
              <el-col :span="8">
                  <el-card>
                <div class="statistic-card">
                  <el-statistic :value="thisWeek_totalProfit" title="周利润">
                    <template #title>
                      <div style="display: inline-flex; align-items: center">
                        <el-link @click="gotoPath(0)">周利润（元）</el-link>
                        <el-tooltip
                          effect="dark"
                          content="周利润配矿"
                          placement="top"
                        >
                          <el-icon style="margin-left: 4px" :size="12">
                            <Warning />
                          </el-icon>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-statistic>
                  <div class="statistic-footer">
                    <div class="footer-item">
                      <span>同比上周</span>
                      <span :class="rate(thisWeek_totalProfit,lastWeek_totalProfit)>=0?'red':'green'">
                      <span>{{rate(thisWeek_totalProfit,lastWeek_totalProfit)}}%</span>
                        <el-icon>
                          <CaretTop v-if="rate(thisWeek_totalProfit,lastWeek_totalProfit)>=0"/>
                          <CaretBottom v-else/>
                        </el-icon>
                      </span>
                    </div>
                  </div>
                </div></el-card>
              </el-col>
            </el-row>
            <el-row :gutter="20" style="padding-top: 20px;">
              <el-col :sm="12" :lg="12" style="padding-left: 15px">
                <span style="padding-right: 18px;color: darkgray;">补库提醒（未来30天）</span>
              </el-col>
            </el-row>
            <el-row>
            <el-row style="padding-top: 20px;">
                <el-row>
                    <span>配矿库存</span>
                </el-row>
                <el-table :data="viewStockList" v-loading="supplyLoading" style="width: 100%;padding-top: 10px;">
                    <el-table-column prop="supplyDate" label="日期"></el-table-column>
                    <el-table-column prop="qualityName" label="品质"></el-table-column>
                    <el-table-column prop="planName" label="成品库"></el-table-column>
                    <el-table-column prop="gapAmount" label="缺口数量">
                        <template #default="{ row }">
                            <el-tag type="danger">{{ Math.ceil(row.gapAmount) }} 吨</el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-row>
            <el-row style="padding-top: 20px;">
                <el-row>
                    <span>原矿库存</span>
                </el-row>
                <el-table :data="viewOreList"  v-loading="supplyLoading" style="padding-top: 10px;">
                    <el-table-column prop="supplyDate" label="日期"></el-table-column>
                    <el-table-column prop="oreName" label="矿种"></el-table-column>
                    <el-table-column prop="name" label="批次"></el-table-column>
                    <el-table-column prop="gapAmount" label="缺口数量">
                        <template #default="{ row }">
                            <el-tag type="danger">{{ Math.ceil(row.gapAmount) }} 吨</el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-row>
            </el-row>
          </el-col>
   </el-row>
     
        
    </div>
</template>
<script setup lang="ts" name="platform-dashboard">
import { onMounted, ref } from "vue"
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { getTotalData } from '/@/api/platform/blend/common'
import { getSupply } from '/@/api/platform/blend/supply'


const router = useRouter()

const chartRef = ref(null)
const chartRef2 = ref(null)
const chartRef3 = ref(null)
const chartRef4 = ref(null)
const today = ref(0)
const thisWeek_oreStockTotal = ref(0)
const lastWeek_oreStockTotal = ref(0)

const thisWeek_stockTotal = ref(0)
const lastWeek_stockTotal = ref(0)

const thisWeek_totalProfit = ref(0)
const lastWeek_totalProfit = ref(0)

const viewStockList = ref([])
const viewOreList = ref([])
const days = ref(30)
const supplyLoading = ref(false)

const week_ph = ref([])
const month_data = ref([])
const holder_data = ref([])
const all_ph = ref([])
const strategic1_data = ref([])

const nextWeekNum = ref(0)
const thisWeekStart = ref('')
const thisWeekEnd = ref('')
const optinalList = ref([])
const formData = ref({})
const drawerShow = ref(false)

const gotoPath = (status: any) => {

}

const gotoPathStrate = (name: string) => {

}
const thisWeek = () => {
    today.value = new Date().getTime()
    nextWeekNum.value = 0
    total()
}

const lastWeek = () => {
    today.value = today.value - 7 * 24 * 60 * 60 * 1000
    nextWeekNum.value = nextWeekNum.value + 1
    total()
}

const nextWeek = () => {
    today.value = today.value + 7 * 24 * 60 * 60 * 1000
    nextWeekNum.value = nextWeekNum.value - 1
    total()
}

const rate = (a: number,b: number) => {
    if (b==0) {
        return 0;
    }
    return ((a-b)/b*100).toFixed(2)
}

const rate2 = (a: number,b: number) => {
    if (b==0) {
        return 0
    }
    return ((a)/b*100).toFixed(2)
}


const viewPatent = (data: Object) => {
    formData.value = data
    drawerShow.value = true

}

const initChart = () => {
    const chart = echarts.init(chartRef.value)
    let data: { name: any; value: any }[] = []
    optinalList.value.forEach((item)=>{
        data.push({name:item.strategic1,value:item.total})
    })

    const option = {
        series: [
            {
                type: 'pie',
                radius: ['20%', '60%'],
                data: data,
                label: {
                    show: true,
                    position: 'inside', //位置，内部（inside）和外部（outside）
                    formatter: '{d}%\n{b}' // 显示百分比和名称
                }
            }
        ]}

    chart.setOption(option)
    // 月度新增科技成果
    const months: any[] = []
    const totals: any[] = []
    month_data.value.map(item => {
        months.push(item.month+'月')
        totals.push(item.total)
    })
    const chart2 = echarts.init(chartRef2.value)
    const option2 = {
        xAxis: {
            type: 'category',
            data: months
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                data: totals,
                type: 'line'
            }
        ]
    }
    chart2.setOption(option2)
    // 项目来源排行榜
    const holders: any[] = []
    const holderTotals: any[] = []
    holder_data.value.map(item => {
        let h = item.holder
        if(h.length>8){
            h = h.substring(0,8)+'...'
        }
        holders.push(h)
        holderTotals.push(item.total)
    })
    const chart3 = echarts.init(chartRef3.value)
    const option3 = {
        xAxis: {
            data: holders
        },
        yAxis: {},
        series: [
            {
                type: 'bar',
                data: holderTotals
            }
        ]
    }
    chart3.setOption(option3)
    // 历史完成评估排行
    const heads: any[] = []
    const headTotals: any[] = []
    all_ph.value.map(item => {
        heads.push(item.head)
        headTotals.push(item.total)
    })
    const chart4 = echarts.init(chartRef4.value)
    const option4 = {
        xAxis: {
            data: heads
        },
        yAxis: {},
        series: [
            {
                type: 'bar',
                data: headTotals
            }
        ]
    }
    chart4.setOption(option4)
}

const total = () => {
    getTotalData({today: today.value}).then((res)=>{
        if(res.code === 0){
            thisWeekEnd.value = res.data.thisWeekEnd
            thisWeekStart.value = res.data.thisWeekStart
            thisWeek_oreStockTotal.value = res.data.thisWeek_oreStockTotal
            lastWeek_oreStockTotal.value = res.data.lastWeek_oreStockTotal
            thisWeek_stockTotal.value = res.data.thisWeek_stockTotal
            lastWeek_stockTotal.value = res.data.lastWeek_stockTotal
            thisWeek_totalProfit.value = res.data.thisWeek_totalProfit
            lastWeek_totalProfit.value = res.data.lastWeek_totalProfit
            //initChart()
        }
    })
}

const viewSupply = () => {
    supplyLoading.value = true
    getSupply({days: days.value}).then((res) => {
        viewOreList.value = res.data.oreList
        viewStockList.value = res.data.stockList
        // viewOreList 先按supplyDate排序，再按name排序
        viewOreList.value.sort((a: any, b: any) => {
            if(a.supplyDate > b.supplyDate) {
                return 1
            } else if(a.supplyDate < b.supplyDate) {
                return -1
            } else {
                if(a.name > b.name) {
                    return 1
                } else if(a.name < b.name) {
                    return -1
                } else {
                    return 0
                }
            }
        })

        supplyLoading.value = false
    })
}

const init = () => {
    total()
    viewSupply()
}

onMounted(() => {
    today.value = new Date().getTime()
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>

<style scoped lang="scss">
.label_font {
    font-weight: 700;
    text-align: right;
    padding-right: 20px;
    padding-bottom: 20px;
}
.home {
    blockquote {
        padding: 10px 20px;
        margin: 0 0 20px;
        font-size: 17.5px;
        border-left: 5px solid #eee;
    }

    hr {
        margin-top: 20px;
        margin-bottom: 20px;
        border: 0;
        border-top: 1px solid #eee;
    }

    .col-item {
        margin-bottom: 20px;
    }

    ul {
        padding: 0;
        margin: 0;
    }

    font-family: "open sans",
    "Helvetica Neue",
    Helvetica,
    Arial,
    sans-serif;
    font-size: 13px;
    color: #676a6c;
    overflow-x: hidden;

    ul {
        list-style-type: none;
    }

    h4 {
        margin-top: 0px;
    }

    h2 {
        margin-top: 10px;
        font-size: 26px;
        font-weight: 100;
    }

    p {
        margin-top: 10px;

        b {
            font-weight: 700;
        }
    }
    .total_css {
        padding-right: 5px;
        margin-top: 10px;
    }
}

.el-statistic {
  --el-statistic-content-font-size: 28px;
}

.statistic-card {
  height: 110px;
  border-radius: 4px;
  background-color: var(--el-bg-color-overlay);
}

.statistic-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 16px;
}

.statistic-footer .footer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistic-footer .footer-item span:last-child {
  display: inline-flex;
  align-items: center;
  margin-left: 4px;
}

.green {
  color: var(--el-color-success);
}
.red {
  color: var(--el-color-error);
}
.scrollbar-flex-content {
  display: flex;
}
.scrollbar-item {
  width: 260px;
  height: 300px;
  border-radius: 4px;
  margin-right: 20px;
}
.li_text {
  width: 100%;
  padding-bottom: 10px;
}
.circle-button-container {
  text-align: center;
  background-color: #409eff;
}

.circle-button {
  height: 180px; /* 根据需要调整高度 */
  width: 180px;  /* 根据需要调整宽度 */
  border-radius: 50%;
  margin: 20px;

}

</style>


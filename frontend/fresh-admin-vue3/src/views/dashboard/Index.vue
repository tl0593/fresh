<template>
  <div>
    <div class="toolbar">
      <div>
        <h3 style="margin:0">今日数据</h3>
        <div class="sub">统计日 {{ today.statDate || '-' }} · 来源订单/用户实时聚合</div>
      </div>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert
      v-if="errorTip"
      type="error"
      :closable="false"
      show-icon
      :title="errorTip"
      style="margin-bottom:12px"
    />

    <div class="stat-grid" v-loading="loading">
      <div class="stat-item" v-for="item in cards" :key="item.label">
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
      </div>
    </div>

    <div class="page-card" style="margin-top:16px">
      <div class="toolbar">
        <h3 style="margin:0">商品销量排行（今日）</h3>
      </div>
      <el-table :data="sales" border stripe size="small" empty-text="今日暂无已支付订单销量">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="goodsId" label="商品ID" width="90" />
        <el-table-column prop="goodsName" label="商品名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.goodsName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="saleNum" label="销量" width="90" />
        <el-table-column prop="saleAmount" label="销售额" width="120">
          <template #default="{ row }">¥{{ formatMoney(row.saleAmount) }}</template>
        </el-table-column>
        <el-table-column prop="statDate" label="统计日" width="120" />
      </el-table>
    </div>

    <div class="page-card" style="margin-top:16px">
      <div class="toolbar">
        <h3 style="margin:0">成团 / 售后率</h3>
      </div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="订单数">{{ rate?.orderCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="成团数">{{ rate?.groupSuccessNum ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="成团率">{{ formatRate(rate?.groupSuccessRate) }}</el-descriptions-item>
        <el-descriptions-item label="售后数">{{ rate?.afterSaleNum ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="售后率">{{ formatRate(rate?.afterSaleRate) }}</el-descriptions-item>
        <el-descriptions-item label="统计日">{{ rate?.statDate ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="page-card" style="margin-top:16px">
      <div class="toolbar">
        <h3 style="margin:0">近 7 日趋势</h3>
      </div>
      <el-table :data="daily" border stripe size="small" empty-text="暂无历史统计">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="newUser" label="新增用户" width="100" />
        <el-table-column prop="activeUser" label="活跃用户" width="100" />
        <el-table-column prop="orderCount" label="订单数" width="90" />
        <el-table-column prop="orderAmount" label="成交额" width="120">
          <template #default="{ row }">¥{{ formatMoney(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column prop="groupSuccessNum" label="成团" width="80" />
        <el-table-column prop="afterSaleNum" label="售后" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDailyList, getGoodsSales, getGroupRate, getTodayStat } from '@/api/data'

const loading = ref(false)
const today = ref({})
const sales = ref([])
const rate = ref(null)
const daily = ref([])
const errorTip = ref('')

const cards = computed(() => [
  { label: '新增用户', value: today.value.newUser ?? 0 },
  { label: '活跃用户', value: today.value.activeUser ?? 0 },
  { label: '订单数', value: today.value.orderCount ?? 0 },
  { label: '成交额', value: `¥${formatMoney(today.value.orderAmount)}` },
  { label: '成团数', value: today.value.groupSuccessNum ?? 0 },
  { label: '售后数', value: today.value.afterSaleNum ?? 0 }
])

function formatMoney(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

function formatRate(v) {
  if (v == null || v === '') return '0%'
  const n = Number(v)
  if (Number.isNaN(n)) return '0%'
  return `${(n * 100).toFixed(2)}%`
}

function range7() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  const fmt = (d) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }
  return { startDate: fmt(start), endDate: fmt(end) }
}

async function load() {
  loading.value = true
  errorTip.value = ''
  try {
    // 串行拉取，避免同一时刻三次并发聚合互相抢写
    const t = await getTodayStat()
    today.value = t || {}

    const s = await getGoodsSales({ limit: 10 })
    sales.value = Array.isArray(s) ? s : (s?.records || [])

    const r = await getGroupRate()
    rate.value = r

    const d = await getDailyList(range7())
    daily.value = Array.isArray(d) ? d : []

    const empty =
      !Number(today.value.orderCount) &&
      !Number(today.value.orderAmount) &&
      sales.value.length === 0
    if (empty) {
      errorTip.value = '今日暂无已支付订单数据；有成交后刷新即可看到销量与成交额。'
    }
  } catch (e) {
    errorTip.value = e?.message || '看板数据加载失败，请确认已登录且 data 服务可用'
    today.value = {}
    sales.value = []
    rate.value = null
    daily.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.sub {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
</style>

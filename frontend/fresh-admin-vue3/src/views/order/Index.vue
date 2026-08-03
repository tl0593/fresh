<template>
  <div class="page-card">
    <div class="toolbar">
      <span>订单管理</span>
      <div class="toolbar-right">
        <el-radio-group v-model="status" size="small" @change="load">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待支付</el-radio-button>
          <el-radio-button :label="5">待配送</el-radio-button>
          <el-radio-button :label="1">待自提</el-radio-button>
          <el-radio-button :label="2">已完成</el-radio-button>
          <el-radio-button :label="3">已取消</el-radio-button>
          <el-radio-button :label="4">售后中</el-radio-button>
        </el-radio-group>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>
    <el-alert
      v-if="pendingAfterSale > 0"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:12px"
    >
      <template #title>
        有 {{ pendingAfterSale }} 条售后待审核，请前往
        <el-button link type="primary" @click="$router.push('/after-sale')">售后管理</el-button>
        处理。
      </template>
    </el-alert>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="流程：支付后「待配送」→ 点「到站」变为「待自提」→ 用户取货后点「核销」变为「已完成」。"
      style="margin-bottom:12px"
    />
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="payAmount" label="实付" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收货人" width="90" />
      <el-table-column prop="receiverPhone" label="电话" width="120" />
      <el-table-column label="自提点" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ `${row.community || ''}${row.detailAddress || ''}` }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 5"
            link
            type="warning"
            @click="onArrive(row)"
          >到站</el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="success"
            @click="onComplete(row)"
          >核销</el-button>
          <el-button
            v-if="row.status === 4"
            link
            type="danger"
            @click="$router.push('/after-sale')"
          >去处理售后</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="订单详情" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order?.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.order?.status) }}</el-descriptions-item>
          <el-descriptions-item label="实付">¥{{ detail.order?.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detail.order?.userId }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.order?.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.order?.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">
            {{ `${detail.order?.community || ''}${detail.order?.detailAddress || ''}` }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items || []" border size="small" style="margin-top:12px">
          <el-table-column prop="goodsName" label="商品" min-width="140" />
          <el-table-column prop="price" label="单价" width="90" />
          <el-table-column prop="num" label="数量" width="70" />
          <el-table-column prop="subTotal" label="小计" width="90" />
          <el-table-column label="活动" width="80">
            <template #default="{ row }">{{ activityText(row.activityType) }}</template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          v-if="detail?.order?.status === 5"
          type="warning"
          :loading="completing"
          @click="onArrive(detail.order)"
        >确认到站</el-button>
        <el-button
          v-if="detail?.order?.status === 1"
          type="success"
          :loading="completing"
          @click="onComplete(detail.order)"
        >核销完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterSalePendingCount } from '@/api/afterSale'
import { orderArrive, orderComplete, orderDetail, orderList } from '@/api/order'

const list = ref([])
const loading = ref(false)
const status = ref(null)
const visible = ref(false)
const detail = ref(null)
const completing = ref(false)
const pendingAfterSale = ref(0)

const STATUS_MAP = {
  0: '待支付',
  5: '待配送',
  1: '待自提',
  2: '已完成',
  3: '已取消',
  4: '售后中'
}

function statusText(s) {
  return STATUS_MAP[s] ?? String(s ?? '-')
}

function activityText(t) {
  if (t === 2) return '团购'
  if (t === 3) return '秒杀'
  return '普通'
}

async function loadPending() {
  try {
    const data = await afterSalePendingCount()
    pendingAfterSale.value = Number(data?.count || 0)
  } catch {
    pendingAfterSale.value = 0
  }
}

async function load() {
  loading.value = true
  try {
    const params = status.value === null || status.value === undefined ? {} : { status: status.value }
    list.value = (await orderList(params)) || []
    await loadPending()
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detail.value = await orderDetail(row.orderNo)
  visible.value = true
}

async function onArrive(row) {
  await ElMessageBox.confirm(`确认订单 ${row.orderNo} 已配送到自提点？`, '配送到站')
  completing.value = true
  try {
    await orderArrive(row.orderNo)
    ElMessage.success('已标记到站，用户可自提')
    visible.value = false
    await load()
  } finally {
    completing.value = false
  }
}

async function onComplete(row) {
  await ElMessageBox.confirm(`确认核销订单 ${row.orderNo}？核销后将变为已完成。`, '核销自提')
  completing.value = true
  try {
    await orderComplete(row.orderNo)
    ElMessage.success('核销成功')
    visible.value = false
    await load()
  } finally {
    completing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>

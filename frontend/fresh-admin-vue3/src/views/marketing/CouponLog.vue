<template>
  <div class="page-card">
    <div class="toolbar">
      <span>用券记录</span>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="templateId" label="模板ID" width="90" />
      <el-table-column label="券模板" min-width="140">
        <template #default="{ row }">{{ couponNameOf(row.templateId) }}</template>
      </el-table-column>
      <el-table-column prop="userCouponId" label="用户券ID" width="100" />
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="deductMoney" label="抵扣金额" width="100" />
      <el-table-column prop="createTime" label="时间" width="160" />
    </el-table>
    <div style="margin-top:12px;text-align:right">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { couponList, couponLog } from '@/api/goods'

const list = ref([])
const coupons = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

const couponMap = computed(() => {
  const map = {}
  coupons.value.forEach((c) => { map[c.id] = c })
  return map
})

function couponNameOf(id) {
  return couponMap.value[id]?.couponName || (id ? `#${id}` : '-')
}

async function load() {
  loading.value = true
  try {
    const [data, cps] = await Promise.all([
      couponLog({ pageNum: pageNum.value, pageSize }),
      coupons.value.length ? Promise.resolve(coupons.value) : couponList().catch(() => [])
    ])
    list.value = data?.records || []
    total.value = Number(data?.total || 0)
    if (!coupons.value.length) coupons.value = cps || []
  } finally { loading.value = false }
}

onMounted(load)
</script>

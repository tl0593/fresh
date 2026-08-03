<template>
  <div class="page-card">
    <div class="toolbar">
      <span>AI 识图日志</span>
      <div style="display:flex;gap:8px">
        <el-input v-model="afterSaleId" placeholder="售后ID" clearable style="width:140px" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="afterSaleId" label="售后ID" width="90" />
      <el-table-column prop="imgUrl" label="图片" min-width="160" show-overflow-tooltip />
      <el-table-column prop="rawResult" label="识别结果" min-width="180" show-overflow-tooltip />
      <el-table-column prop="damageLevel" label="损坏等级" width="90" />
      <el-table-column prop="createTime" label="时间" width="160" />
    </el-table>
    <div style="margin-top:12px;text-align:right">
      <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="pageNum" :page-size="pageSize" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { imageLogPage } from '@/api/ai'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const afterSaleId = ref('')

function search() { pageNum.value = 1; load() }

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (afterSaleId.value) params.afterSaleId = afterSaleId.value
    const data = await imageLogPage(params)
    list.value = data?.records || []
    total.value = Number(data?.total || 0)
  } finally { loading.value = false }
}

onMounted(load)
</script>

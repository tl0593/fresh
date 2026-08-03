<template>
  <div class="page-card">
    <div class="toolbar">
      <span>消息模板（只读，后端 CRUD 待补齐）</span>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="templateId" label="模板ID" min-width="140" />
      <el-table-column prop="templateType" label="类型" width="80" />
      <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { templateList } from '@/api/message'

const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await templateList()
    list.value = Array.isArray(data) ? data : (data?.records || data || [])
  } finally { loading.value = false }
}

onMounted(load)
</script>

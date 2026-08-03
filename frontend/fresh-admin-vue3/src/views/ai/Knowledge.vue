<template>
  <div class="page-card">
    <div class="toolbar">
      <span>AI 知识库</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="question" label="问题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑知识' : '新增知识'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="问题"><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="答案"><el-input v-model="form.answer" type="textarea" rows="5" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeList, knowledgeSave, knowledgeUpdate, knowledgeDelete } from '@/api/ai'

const empty = () => ({ id: null, question: '', answer: '', sort: 0, status: 1 })
const list = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive(empty())

async function load() {
  loading.value = true
  try { list.value = (await knowledgeList()) || [] } finally { loading.value = false }
}
function openEdit(row) { Object.assign(form, empty(), row || {}); visible.value = true }
async function onSave() {
  saving.value = true
  try {
    if (form.id) await knowledgeUpdate({ ...form })
    else await knowledgeSave({ ...form })
    ElMessage.success('保存成功'); visible.value = false; load()
  } finally { saving.value = false }
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await knowledgeDelete(row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

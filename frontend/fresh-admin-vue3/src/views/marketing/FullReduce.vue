<template>
  <div class="page-card">
    <div class="toolbar">
      <span>满减活动</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="activityName" label="名称" min-width="120" />
      <el-table-column prop="fullAmount" label="满额" width="90" />
      <el-table-column prop="reduceAmount" label="减免" width="90" />
      <el-table-column label="范围" width="100">
        <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
      </el-table-column>
      <el-table-column label="指定分类" min-width="140">
        <template #default="{ row }">{{ catNamesOf(row.targetCatIds) }}</template>
      </el-table-column>
      <el-table-column prop="stackCoupon" label="叠券" width="70">
        <template #default="{ row }">{{ row.stackCoupon === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始" min-width="150" />
      <el-table-column prop="endTime" label="结束" min-width="150" />
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

    <el-dialog v-model="visible" :title="form.id ? '编辑满减' : '新增满减'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.activityName" /></el-form-item>
        <el-form-item label="满额"><el-input-number v-model="form.fullAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="减免"><el-input-number v-model="form.reduceAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="范围类型">
          <el-select v-model="form.targetType" style="width:100%">
            <el-option :value="1" label="1 - 全场" />
            <el-option :value="2" label="2 - 指定分类" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 2" label="指定分类">
          <el-select v-model="selectedCatIds" multiple filterable clearable placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="`${c.id} - ${c.catName}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="叠券"><el-switch v-model="form.stackCoupon" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryList, fullReduceList, fullReduceSave, fullReduceDelete } from '@/api/goods'

const TARGET_MAP = { 1: '全场', 2: '指定分类' }
const targetTypeLabel = (t) => TARGET_MAP[t] || String(t ?? '-')

const empty = () => ({
  id: null, activityName: '', fullAmount: 0, reduceAmount: 0, targetType: 1,
  targetCatIds: '', startTime: '', endTime: '', stackCoupon: 0, status: 1
})

const list = ref([])
const categories = ref([])
const selectedCatIds = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive(empty())

const categoryMap = computed(() => {
  const map = {}
  categories.value.forEach((c) => { map[c.id] = c })
  return map
})

function catNamesOf(ids) {
  if (!ids) return '-'
  return String(ids).split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((id) => categoryMap.value[id]?.catName || `#${id}`)
    .join('、') || '-'
}

function parseCatIds(text) {
  if (!text) return []
  return String(text).split(',')
    .map((s) => Number(s.trim()))
    .filter((n) => !Number.isNaN(n) && n > 0)
}

watch(selectedCatIds, (ids) => {
  form.targetCatIds = (ids || []).join(',')
})

async function load() {
  loading.value = true
  try {
    const [acts, cats] = await Promise.all([
      fullReduceList().catch(() => []),
      categoryList().catch(() => [])
    ])
    list.value = acts || []
    categories.value = cats || []
  } finally { loading.value = false }
}

function openEdit(row) {
  Object.assign(form, empty(), row || {})
  selectedCatIds.value = parseCatIds(form.targetCatIds)
  visible.value = true
}

async function onSave() {
  if (form.targetType === 2 && selectedCatIds.value.length === 0) {
    ElMessage.warning('指定分类模式下请至少选择一个分类')
    return
  }
  if (form.targetType === 1) {
    form.targetCatIds = ''
    selectedCatIds.value = []
  } else {
    form.targetCatIds = selectedCatIds.value.join(',')
  }
  saving.value = true
  try {
    await fullReduceSave({ ...form })
    ElMessage.success('保存成功'); visible.value = false; load()
  } finally { saving.value = false }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await fullReduceDelete(row.id); ElMessage.success('已删除'); load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <span>分类列表</span>
      <el-button type="primary" @click="openEdit()">新增分类</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="parentId" label="父级ID" width="90" />
      <el-table-column label="父级名称" min-width="120">
        <template #default="{ row }">{{ parentNameOf(row.parentId) }}</template>
      </el-table-column>
      <el-table-column prop="catName" label="分类名" />
      <el-table-column label="图标" width="90">
        <template #default="{ row }">
          <el-image v-if="row.icon" :src="row.icon" style="width:40px;height:40px" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑分类' : '新增分类'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="父级分类">
          <div class="id-with-name">
            <el-select
              v-model="form.parentId"
              filterable
              clearable
              placeholder="选择父级（0=顶级）"
              style="flex:1"
            >
              <el-option :value="0" label="0 - 顶级分类" />
              <el-option
                v-for="c in parentOptions"
                :key="c.id"
                :label="`${c.id} - ${c.catName}`"
                :value="c.id"
              />
            </el-select>
            <span class="name-hint">{{ parentHint }}</span>
          </div>
        </el-form-item>
        <el-form-item label="分类名"><el-input v-model="form.catName" /></el-form-item>
        <el-form-item label="图标">
          <div style="display:flex;flex-direction:column;gap:8px;width:100%">
            <div style="display:flex;gap:8px">
              <el-input v-model="form.icon" placeholder="上传后自动填入可访问 URL" />
              <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
                <el-button>上传</el-button>
              </el-upload>
            </div>
            <el-image v-if="form.icon" :src="form.icon" style="width:64px;height:64px" fit="cover" />
          </div>
        </el-form-item>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryList, categorySave, categoryDelete, uploadImage } from '@/api/goods'

const list = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, parentId: 0, catName: '', icon: '', sort: 0, status: 1 })

const categoryMap = computed(() => {
  const map = {}
  list.value.forEach((c) => { map[c.id] = c })
  return map
})

/** 不能把自己或子孙设为父级；编辑时排除自身 */
const parentOptions = computed(() =>
  list.value.filter((c) => c.id !== form.id)
)

function parentNameOf(parentId) {
  if (parentId === 0 || parentId == null) return '顶级'
  return categoryMap.value[parentId]?.catName || `未知(#${parentId})`
}

const parentHint = computed(() => {
  const pid = form.parentId
  if (pid === 0 || pid == null || pid === '') return '当前：顶级分类'
  const name = categoryMap.value[pid]?.catName
  return name ? `当前：${name}` : `当前：未找到 ID=${pid}`
})

async function load() {
  loading.value = true
  try {
    list.value = (await categoryList()) || []
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, row || { id: null, parentId: 0, catName: '', icon: '', sort: 0, status: 1 })
  if (form.parentId == null) form.parentId = 0
  visible.value = true
}

async function onUpload({ file }) {
  const data = await uploadImage(file, 'category')
  form.icon = typeof data === 'string' ? data : (data?.url || '')
  ElMessage.success('上传成功')
}

async function onSave() {
  if (form.parentId && form.parentId === form.id) {
    ElMessage.warning('父级不能是自己')
    return
  }
  saving.value = true
  try {
    await categorySave({ ...form, parentId: form.parentId ?? 0 })
    ElMessage.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除分类「${row.catName}」？`, '提示')
  await categoryDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.id-with-name {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.name-hint {
  color: #606266;
  white-space: nowrap;
  font-size: 13px;
}
</style>

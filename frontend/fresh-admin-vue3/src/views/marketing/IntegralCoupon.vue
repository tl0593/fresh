<template>
  <div class="page-card">
    <div class="toolbar">
      <span>积分兑券</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="templateId" label="券模板ID" width="100" />
      <el-table-column label="券模板" min-width="140">
        <template #default="{ row }">{{ couponNameOf(row.templateId) }}</template>
      </el-table-column>
      <el-table-column prop="costIntegral" label="消耗积分" width="100" />
      <el-table-column prop="dailyLimit" label="日限" width="80" />
      <el-table-column prop="totalStock" label="库存" width="80" />
      <el-table-column prop="usedNum" label="已兑" width="80" />
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

    <el-dialog v-model="visible" :title="form.id ? '编辑' : '新增'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="券模板" required>
          <div class="id-with-name">
            <el-select v-model="form.templateId" filterable clearable placeholder="选择优惠券模板" style="flex:1">
              <el-option
                v-for="c in coupons"
                :key="c.id"
                :label="`${c.id} - ${c.couponName}`"
                :value="c.id"
              />
            </el-select>
            <span class="name-hint">{{ templateHint }}</span>
          </div>
        </el-form-item>
        <el-form-item label="消耗积分"><el-input-number v-model="form.costIntegral" :min="1" /></el-form-item>
        <el-form-item label="日限"><el-input-number v-model="form.dailyLimit" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.totalStock" :min="0" /></el-form-item>
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
import { couponList, integralCouponList, integralCouponSave, integralCouponDelete } from '@/api/goods'

const empty = () => ({ id: null, templateId: null, costIntegral: 100, dailyLimit: 1, totalStock: 100, status: 1 })
const list = ref([])
const coupons = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive(empty())

const couponMap = computed(() => {
  const map = {}
  coupons.value.forEach((c) => { map[c.id] = c })
  return map
})

function couponNameOf(id) {
  return couponMap.value[id]?.couponName || (id ? `#${id}` : '-')
}

const templateHint = computed(() => {
  if (!form.templateId) return '请选择券模板'
  const name = couponMap.value[form.templateId]?.couponName
  return name ? `当前：${name}` : `当前：未找到 ID=${form.templateId}`
})

async function load() {
  loading.value = true
  try {
    const [rows, cps] = await Promise.all([
      integralCouponList().catch(() => []),
      couponList().catch(() => [])
    ])
    list.value = rows || []
    coupons.value = cps || []
  } finally { loading.value = false }
}

function openEdit(row) { Object.assign(form, empty(), row || {}); visible.value = true }

async function onSave() {
  if (!form.templateId) {
    ElMessage.warning('请选择券模板')
    return
  }
  saving.value = true
  try {
    await integralCouponSave({ ...form })
    ElMessage.success('保存成功'); visible.value = false; load()
  } finally { saving.value = false }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await integralCouponDelete(row.id); ElMessage.success('已删除'); load()
}

onMounted(load)
</script>

<style scoped>
.id-with-name { display:flex; align-items:center; gap:12px; width:100%; }
.name-hint { color:#606266; white-space:nowrap; font-size:13px; }
</style>

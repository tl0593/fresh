<template>
  <div class="page-card">
    <div class="toolbar">
      <span>抽奖奖品</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="奖励类型" width="110">
        <template #default="{ row }">{{ rewardTypeLabel(row.rewardType) }}</template>
      </el-table-column>
      <el-table-column prop="rewardIntegral" label="奖励积分" width="100" />
      <el-table-column prop="rewardCouponId" label="奖励券ID" width="100" />
      <el-table-column label="奖励券" min-width="140">
        <template #default="{ row }">{{ couponNameOf(row.rewardCouponId) }}</template>
      </el-table-column>
      <el-table-column prop="weight" label="权重" width="80" />
      <el-table-column prop="costIntegral" label="消耗积分" width="100" />
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
        <el-form-item label="奖励类型">
          <el-select v-model="form.rewardType" style="width:100%">
            <el-option :value="1" label="1 - 积分奖品" />
            <el-option :value="2" label="2 - 优惠券奖品" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.rewardType === 1" label="奖励积分">
          <el-input-number v-model="form.rewardIntegral" :min="0" />
        </el-form-item>
        <el-form-item v-if="form.rewardType === 2" label="奖励券" required>
          <div class="id-with-name">
            <el-select v-model="form.rewardCouponId" filterable clearable placeholder="选择优惠券模板" style="flex:1">
              <el-option
                v-for="c in coupons"
                :key="c.id"
                :label="`${c.id} - ${c.couponName}`"
                :value="c.id"
              />
            </el-select>
            <span class="name-hint">{{ couponHint }}</span>
          </div>
        </el-form-item>
        <el-form-item label="权重"><el-input-number v-model="form.weight" :min="1" /></el-form-item>
        <el-form-item label="消耗积分"><el-input-number v-model="form.costIntegral" :min="0" /></el-form-item>
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
import { couponList, lotteryList, lotterySave, lotteryDelete } from '@/api/goods'

const REWARD_MAP = { 1: '积分奖品', 2: '优惠券奖品' }
const rewardTypeLabel = (t) => REWARD_MAP[t] || String(t ?? '-')

const empty = () => ({ id: null, rewardType: 1, rewardIntegral: 0, rewardCouponId: 0, weight: 1, costIntegral: 10, status: 1 })
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
  if (!id || Number(id) === 0) return '-'
  return couponMap.value[id]?.couponName || `#${id}`
}

const couponHint = computed(() => {
  if (!form.rewardCouponId) return '请选择券模板'
  const name = couponMap.value[form.rewardCouponId]?.couponName
  return name ? `当前：${name}` : `当前：未找到 ID=${form.rewardCouponId}`
})

async function load() {
  loading.value = true
  try {
    const [rows, cps] = await Promise.all([
      lotteryList().catch(() => []),
      couponList().catch(() => [])
    ])
    list.value = rows || []
    coupons.value = cps || []
  } finally { loading.value = false }
}

function openEdit(row) { Object.assign(form, empty(), row || {}); visible.value = true }

async function onSave() {
  if (form.rewardType === 2 && !form.rewardCouponId) {
    ElMessage.warning('请选择奖励券模板')
    return
  }
  if (form.rewardType === 1) {
    form.rewardCouponId = 0
  }
  saving.value = true
  try {
    await lotterySave({ ...form })
    ElMessage.success('保存成功'); visible.value = false; load()
  } finally { saving.value = false }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await lotteryDelete(row.id); ElMessage.success('已删除'); load()
}

onMounted(load)
</script>

<style scoped>
.id-with-name { display:flex; align-items:center; gap:12px; width:100%; }
.name-hint { color:#606266; white-space:nowrap; font-size:13px; }
</style>

<template>
  <div class="page-card">
    <div class="toolbar">
      <span>优惠券模板</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="couponName" label="名称" min-width="120" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ couponTypeLabel(row.couponType) }}</template>
      </el-table-column>
      <el-table-column prop="fullAmount" label="满额" width="90" />
      <el-table-column prop="reduceAmount" label="减免" width="90" />
      <el-table-column prop="totalCount" label="总量" width="80" />
      <el-table-column prop="usedCount" label="已用" width="80" />
      <el-table-column prop="validDay" label="有效天" width="80" />
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

    <el-dialog v-model="visible" :title="form.id ? '编辑优惠券' : '新增优惠券'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.couponName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.couponType" style="width:100%">
            <el-option :value="1" label="1 - 无门槛" />
            <el-option :value="2" label="2 - 满减" />
            <el-option :value="3" label="3 - 品类专享" />
            <el-option :value="4" label="4 - 团购专用" />
          </el-select>
        </el-form-item>
        <el-form-item label="满额"><el-input-number v-model="form.fullAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="减免"><el-input-number v-model="form.reduceAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="总量"><el-input-number v-model="form.totalCount" :min="0" /></el-form-item>
        <el-form-item label="有效天"><el-input-number v-model="form.validDay" :min="1" /></el-form-item>
        <el-form-item label="开始">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="限领规则">
          <el-select v-model="form.limitType" style="width:100%">
            <el-option :value="1" label="1 - 单人限领 N 张" />
            <el-option :value="2" label="2 - 不限" />
          </el-select>
        </el-form-item>
        <el-form-item label="限领数量"><el-input-number v-model="form.limitNum" :min="0" /></el-form-item>
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
import { couponList, couponSave, couponDelete } from '@/api/goods'

const TYPE_MAP = { 1: '无门槛', 2: '满减', 3: '品类专享', 4: '团购专用' }
const couponTypeLabel = (t) => TYPE_MAP[t] || String(t ?? '-')

const empty = () => ({
  id: null, couponName: '', couponType: 2, fullAmount: 0, reduceAmount: 0,
  totalCount: 100, validDay: 7, startTime: '', endTime: '', limitType: 1, limitNum: 1, status: 1
})
const list = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive(empty())

async function load() {
  loading.value = true
  try { list.value = (await couponList()) || [] } finally { loading.value = false }
}
function openEdit(row) { Object.assign(form, empty(), row || {}); visible.value = true }
async function onSave() {
  saving.value = true
  try {
    await couponSave({ ...form })
    ElMessage.success('保存成功'); visible.value = false; load()
  } finally { saving.value = false }
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await couponDelete(row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

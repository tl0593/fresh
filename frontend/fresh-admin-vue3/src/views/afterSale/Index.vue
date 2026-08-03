<template>
  <div class="page-card">
    <div class="toolbar">
      <span>售后管理</span>
      <div class="toolbar-right">
        <el-radio-group v-model="auditStatus" size="small" @change="load">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待审核</el-radio-button>
          <el-radio-button :label="1">已通过</el-radio-button>
          <el-radio-button :label="2">已驳回</el-radio-button>
        </el-radio-group>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="pendingCount > 0"
      type="warning"
      :closable="false"
      show-icon
      :title="`有 ${pendingCount} 条售后待审核，请尽快处理`"
      style="margin-bottom:12px"
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      title="暂无待审核售后"
      style="margin-bottom:12px"
    />

    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="工单ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="goodsName" label="商品" min-width="140" show-overflow-tooltip />
      <el-table-column label="损坏图" width="100">
        <template #default="{ row }">
          <div class="img-row" v-if="imgList(row.damageImg).length">
            <el-image
              v-for="(url, i) in imgList(row.damageImg).slice(0, 2)"
              :key="i"
              :src="url"
              :preview-src-list="imgList(row.damageImg)"
              fit="cover"
              class="thumb"
            />
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="AI损坏" width="90">
        <template #default="{ row }">{{ damageLevelText(row.aiDamageLevel) }}</template>
      </el-table-column>
      <el-table-column prop="aiRate" label="损坏%" width="80" />
      <el-table-column prop="aiRefundMoney" label="AI建议退款" width="100" />
      <el-table-column prop="actualRefundMoney" label="实际退款" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="auditTag(row.auditStatus)" size="small">{{ auditText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.auditStatus === 0"
            link
            type="primary"
            @click="openAudit(row)"
          >审核</el-button>
          <el-button v-else link type="info" @click="openAudit(row, true)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="readonly ? '售后详情' : '售后审核'" width="520px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ current.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ current.goodsName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="AI损坏等级">{{ damageLevelText(current.aiDamageLevel) }}</el-descriptions-item>
          <el-descriptions-item label="AI建议退款">¥{{ current.aiRefundMoney ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请备注">{{ current.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户凭证">
            <span v-if="!imgList(current.damageImg).length">未上传</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="imgList(current.damageImg).length" class="preview-imgs">
          <div class="preview-label">用户上传凭证（点击可放大）</div>
          <el-image
            v-for="(url, i) in imgList(current.damageImg)"
            :key="i"
            :src="url"
            :preview-src-list="imgList(current.damageImg)"
            fit="cover"
            class="preview"
          />
        </div>
        <el-form v-if="!readonly" :model="form" label-width="100px" style="margin-top:16px">
          <el-form-item label="审核结果" required>
            <el-radio-group v-model="form.auditStatus">
              <el-radio :label="1">通过并退款</el-radio>
              <el-radio :label="2">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.auditStatus === 1" label="实际退款">
            <el-input-number v-model="form.actualRefundMoney" :min="0" :precision="2" :step="1" />
            <span class="hint">不填则使用 AI 建议金额</span>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="300" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSaleAudit, afterSaleList, afterSalePendingCount } from '@/api/afterSale'

const list = ref([])
const loading = ref(false)
const auditStatus = ref(0)
const pendingCount = ref(0)
const visible = ref(false)
const readonly = ref(false)
const current = ref(null)
const saving = ref(false)
const form = ref({
  id: null,
  auditStatus: 1,
  actualRefundMoney: undefined,
  remark: ''
})

function auditText(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return String(s ?? '-')
}

function auditTag(s) {
  if (s === 0) return 'warning'
  if (s === 1) return 'success'
  if (s === 2) return 'info'
  return ''
}

function damageLevelText(level) {
  if (level === 1) return '轻微'
  if (level === 2) return '中度'
  if (level === 3) return '重度'
  return level == null ? '未识别' : String(level)
}

function splitDamageImgs(damageImg) {
  const raw = String(damageImg || '').trim()
  if (!raw) return []
  // 新格式：| 分隔
  if (raw.includes('|')) {
    return raw.split('|').map((s) => s.trim()).filter(Boolean)
  }
  // 仅当存在「多个完整 http URL」时才按逗号拆
  // 避免把 .../upload/afterSale,afterSale/2026-... 拆成两张失败图
  if (/https?:\/\/.+\s*,\s*https?:\/\//i.test(raw)) {
    return raw.split(/,\s*(?=https?:\/\/)/i).map((s) => s.trim()).filter(Boolean)
  }
  return [raw]
}

function toPreviewSrc(url) {
  let u = String(url || '').trim()
  if (!u || /^https?:\/\/tmp\//i.test(u) || u.startsWith('wxfile://')) return ''

  // 网关绝对地址 -> 相对路径，走 vite /api 代理
  const local = u.match(/^https?:\/\/(?:127\.0\.0\.1|localhost):8080(\/api\/.+)$/i)
  if (local) u = local[1]
  else if (!u.startsWith('http://') && !u.startsWith('https://') && !u.startsWith('/')) u = `/${u}`

  // 历史脏目录名含逗号（afterSale,afterSale）时保留原样，不要 encode（Spring 按字面逗号找文件）
  return u
}

function imgList(damageImg) {
  return splitDamageImgs(damageImg).map(toPreviewSrc).filter(Boolean)
}

async function loadPending() {
  try {
    const data = await afterSalePendingCount()
    pendingCount.value = Number(data?.count || 0)
  } catch {
    pendingCount.value = 0
  }
}

async function load() {
  loading.value = true
  try {
    const params = auditStatus.value === null || auditStatus.value === undefined
      ? {}
      : { auditStatus: auditStatus.value }
    list.value = (await afterSaleList(params)) || []
    await loadPending()
  } finally {
    loading.value = false
  }
}

function openAudit(row, viewOnly = false) {
  current.value = row
  readonly.value = viewOnly || row.auditStatus !== 0
  form.value = {
    id: row.id,
    auditStatus: 1,
    actualRefundMoney: row.aiRefundMoney != null ? Number(row.aiRefundMoney) : undefined,
    remark: ''
  }
  visible.value = true
}

async function submitAudit() {
  if (!form.value.auditStatus) {
    ElMessage.warning('请选择审核结果')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.value.id,
      auditStatus: form.value.auditStatus,
      remark: form.value.remark || undefined
    }
    if (form.value.auditStatus === 1 && form.value.actualRefundMoney != null) {
      payload.actualRefundMoney = form.value.actualRefundMoney
    }
    await afterSaleAudit(payload)
    ElMessage.success(form.value.auditStatus === 1 ? '已通过并记录退款' : '已驳回')
    visible.value = false
    await load()
  } finally {
    saving.value = false
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
.img-row {
  display: flex;
  gap: 4px;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}
.preview-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  align-items: flex-start;
}
.preview-label {
  width: 100%;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
.preview {
  width: 80px;
  height: 80px;
  border-radius: 4px;
}
.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

<template>
  <div class="page-card">
    <div class="toolbar">
      <span>补货提醒</span>
      <div class="toolbar-right">
        <span class="hint">预警阈值</span>
        <el-input-number v-model="threshold" :min="0" :max="9999" size="small" @change="load" />
        <el-checkbox v-model="onlyOnSale" @change="load">仅上架商品</el-checkbox>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="outCount > 0"
      type="error"
      :closable="false"
      show-icon
      :title="`有 ${outCount} 个规格已缺货，请立即补货`"
      style="margin-bottom:12px"
    />
    <el-alert
      v-else-if="list.length > 0"
      type="warning"
      :closable="false"
      show-icon
      :title="`有 ${list.length} 个规格库存 ≤ ${threshold}，建议及时补货`"
      style="margin-bottom:12px"
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      title="当前无缺货/低库存商品"
      style="margin-bottom:12px"
    />

    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="goodsId" label="商品ID" width="80" />
      <el-table-column label="商品" min-width="180">
        <template #default="{ row }">
          <div class="goods-cell">
            <el-image v-if="row.goodsImg" :src="row.goodsImg" class="thumb" fit="cover" />
            <span>{{ row.goodsName || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="specName" label="规格" width="120" show-overflow-tooltip />
      <el-table-column prop="stock" label="规格库存" width="90">
        <template #default="{ row }">
          <span :class="row.stock <= 0 ? 'danger' : 'warn'">{{ row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalStock" label="商品总库存" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.level === 'OUT' ? 'danger' : 'warning'" size="small">
            {{ row.level === 'OUT' ? '缺货' : '库存不足' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上架" width="70">
        <template #default="{ row }">{{ row.goodsStatus === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRestock(row)">补货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="商品补货" width="420px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="商品">{{ current.goodsName }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ current.specName }}</el-descriptions-item>
          <el-descriptions-item label="当前库存">{{ current.stock }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="90px" style="margin-top:16px">
          <el-form-item label="补货数量" required>
            <el-input-number v-model="addNum" :min="1" :max="999999" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRestock">确认补货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { stockAlertList, stockRestock } from '@/api/goods'

const list = ref([])
const loading = ref(false)
const threshold = ref(10)
const onlyOnSale = ref(true)
const visible = ref(false)
const current = ref(null)
const addNum = ref(50)
const saving = ref(false)

const outCount = computed(() => list.value.filter((i) => i.level === 'OUT' || i.stock <= 0).length)

async function load() {
  loading.value = true
  try {
    list.value = (await stockAlertList({
      threshold: threshold.value,
      onlyOnSale: onlyOnSale.value
    })) || []
  } finally {
    loading.value = false
  }
}

function openRestock(row) {
  current.value = row
  addNum.value = Math.max(50, (threshold.value || 10) * 5)
  visible.value = true
}

async function submitRestock() {
  if (!current.value?.specId) {
    ElMessage.warning('缺少规格信息')
    return
  }
  if (!addNum.value || addNum.value <= 0) {
    ElMessage.warning('请填写补货数量')
    return
  }
  saving.value = true
  try {
    await stockRestock({ specId: current.value.specId, addNum: addNum.value })
    ElMessage.success(`已补货 ${addNum.value}`)
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
.hint {
  color: #909399;
  font-size: 13px;
}
.goods-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  flex-shrink: 0;
}
.danger {
  color: #f56c6c;
  font-weight: 600;
}
.warn {
  color: #e6a23c;
  font-weight: 600;
}
</style>

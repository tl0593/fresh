<template>
  <div class="page-card">
    <div class="toolbar">
      <span>商品列表</span>
      <el-button type="primary" @click="openEdit()">发布商品</el-button>
    </div>

    <el-alert
      v-if="lowStockCount > 0"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:12px"
    >
      <template #title>
        有 {{ lowStockCount }} 个上架商品规格库存偏低，请前往
        <el-button link type="primary" @click="$router.push('/stock-alert')">补货提醒</el-button>
        处理。
      </template>
    </el-alert>

    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="goodsName" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="分类" min-width="100">
        <template #default="{ row }">{{ catNameOf(row.catId) }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="售价" width="100">
        <template #default="{ row }">¥{{ formatMoney(row.salePrice) }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="总库存" width="80" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="活动" width="100">
        <template #default="{ row }">
          <span v-if="row.isGroup === 1">团</span>
          <span v-if="row.isSeckill === 1">{{ row.isGroup === 1 ? '/秒' : '秒' }}</span>
          <span v-if="row.isGroup !== 1 && row.isSeckill !== 1">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer
      v-model="visible"
      :title="form.id ? '编辑商品' : '发布商品'"
      size="720px"
      destroy-on-close
    >
      <div class="drawer-body" v-loading="detailLoading">
        <h4 class="section-title">基础信息</h4>
        <el-form :model="form" label-width="96px">
          <el-form-item label="分类" required>
            <el-select v-model="form.catId" filterable clearable placeholder="选择分类" style="width:100%">
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="`${c.id} - ${c.catName}`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="名称" required>
            <el-input v-model="form.goodsName" placeholder="商品名称" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="主图">
            <div class="img-row">
              <el-input v-model="form.goodsImg" placeholder="上传后自动填入 URL" />
              <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
                <el-button>上传</el-button>
              </el-upload>
            </div>
            <el-image v-if="form.goodsImg" :src="form.goodsImg" class="preview" fit="cover" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.goodsDesc" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="计量单位" required>
            <el-select
              v-model="form.unit"
              filterable
              allow-create
              default-first-option
              placeholder="斤 / 份 / 盒"
              style="width:160px"
            >
              <el-option v-for="u in unitOptions" :key="u" :label="u" :value="u" />
            </el-select>
            <el-text type="info" size="small" style="margin-left:8px">仅展示，如 ¥12.8/斤</el-text>
          </el-form-item>
          <el-form-item label="原价参考">
            <el-input-number v-model="form.originPrice" :min="0" :precision="2" />
            <el-text type="info" size="small" style="margin-left:8px">划线价，可售价以规格为准</el-text>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="活动标记">
            <el-checkbox v-model="form.isGroup" :true-value="1" :false-value="0">可参与团购</el-checkbox>
            <el-checkbox v-model="form.isSeckill" :true-value="1" :false-value="0">可参与秒杀</el-checkbox>
          </el-form-item>
        </el-form>

        <div class="section-head">
          <h4 class="section-title" style="margin:0">销售规格</h4>
          <el-radio-group v-model="specMode" size="small" @change="onSpecModeChange">
            <el-radio-button value="single">单规格</el-radio-button>
            <el-radio-button value="multi">多规格</el-radio-button>
          </el-radio-group>
        </div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="规格 = 可售 SKU（价格/库存挂在这里）。规格名写份量型号（约500g、大份），不要重复写单位。"
          style="margin-bottom:12px"
        />

        <!-- 单规格 -->
        <el-form v-if="specMode === 'single' && specs[0]" :model="specs[0]" label-width="96px">
          <el-form-item label="规格名">
            <el-input v-model="specs[0].specName" placeholder="默认：标准" style="width:200px" />
          </el-form-item>
          <el-form-item label="售价" required>
            <el-input-number v-model="specs[0].specPrice" :min="0" :precision="2" />
            <span class="unit-suffix">/ {{ form.unit || '份' }}</span>
          </el-form-item>
          <el-form-item label="库存" required>
            <el-input-number v-model="specs[0].stock" :min="0" />
          </el-form-item>
        </el-form>

        <!-- 多规格 -->
        <div v-else>
          <el-table :data="specs" border size="small">
            <el-table-column label="规格名" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.specName" placeholder="如 约500g" />
              </template>
            </el-table-column>
            <el-table-column label="价格" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.specPrice" :min="0" :precision="2" controls-position="right" style="width:120px" />
              </template>
            </el-table-column>
            <el-table-column label="库存" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.stock" :min="0" controls-position="right" style="width:100px" />
              </template>
            </el-table-column>
            <el-table-column label="默认" width="70" align="center">
              <template #default="{ row, $index }">
                <el-radio
                  v-model="defaultIndex"
                  :value="$index"
                  @change="onDefaultChange"
                >&nbsp;</el-radio>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button
                  link
                  type="danger"
                  :disabled="specs.length <= 1"
                  @click="removeSpec($index)"
                >删</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button style="margin-top:10px" @click="addSpec">+ 添加规格</el-button>
        </div>

        <div class="preview-bar">
          前台展示预览：
          <strong>{{ previewText }}</strong>
          <span class="muted">（总库存 {{ totalStockPreview }}）</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  categoryList,
  goodsList,
  goodsDetail,
  goodsSaveWithSpecs,
  goodsDelete,
  stockAlertCount,
  uploadImage
} from '@/api/goods'

const unitOptions = ['份', '斤', '盒', '袋', '个', '串', '瓶', '箱']

const emptyGoods = () => ({
  id: null,
  catId: null,
  goodsName: '',
  goodsImg: '',
  goodsDesc: '',
  originPrice: 0,
  salePrice: 0,
  totalStock: 0,
  unit: '份',
  status: 1,
  isGroup: 0,
  isSeckill: 0
})

const emptySpec = (overrides = {}) => ({
  id: null,
  goodsId: null,
  specName: '标准',
  specPrice: 0,
  stock: 0,
  isDefault: 1,
  ...overrides
})

const list = ref([])
const categories = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const lowStockCount = ref(0)
const visible = ref(false)
const saving = ref(false)
const form = reactive(emptyGoods())
const specs = ref([emptySpec()])
const specMode = ref('single')
const defaultIndex = ref(0)

const categoryMap = computed(() => {
  const map = {}
  categories.value.forEach((c) => { map[c.id] = c })
  return map
})

const defaultSpec = computed(() => {
  if (!specs.value.length) return null
  const idx = Math.min(Math.max(defaultIndex.value, 0), specs.value.length - 1)
  return specs.value[idx]
})

const totalStockPreview = computed(() =>
  specs.value.reduce((sum, s) => sum + (Number(s.stock) || 0), 0)
)

const previewText = computed(() => {
  const s = defaultSpec.value
  if (!s) return '-'
  const name = (s.specName || '标准').trim()
  const unit = (form.unit || '份').trim()
  const price = formatMoney(s.specPrice)
  return `${name} · ¥${price}/${unit}`
})

function catNameOf(catId) {
  if (!catId) return '-'
  return categoryMap.value[catId]?.catName || `未知(#${catId})`
}

function formatMoney(v) {
  const n = Number(v)
  return Number.isNaN(n) ? '0.00' : n.toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const [goods, cats, stock] = await Promise.all([
      goodsList().catch(() => []),
      categoryList().catch(() => []),
      stockAlertCount({ onlyOnSale: true }).catch(() => null)
    ])
    list.value = goods || []
    categories.value = cats || []
    lowStockCount.value = Number(stock?.count || 0)
  } finally {
    loading.value = false
  }
}

function resetEditor() {
  Object.assign(form, emptyGoods())
  specs.value = [emptySpec()]
  specMode.value = 'single'
  defaultIndex.value = 0
}

async function openEdit(row) {
  resetEditor()
  visible.value = true
  if (!row?.id) return

  detailLoading.value = true
  try {
    const data = await goodsDetail(row.id)
    const g = data?.goods || row
    Object.assign(form, emptyGoods(), g)
    const listSpecs = Array.isArray(data?.specs) && data.specs.length
      ? data.specs
      : [emptySpec({
          goodsId: g.id,
          specPrice: g.salePrice || 0,
          stock: g.totalStock || 0
        })]
    specs.value = listSpecs.map((s, i) => emptySpec({
      ...s,
      isDefault: s.isDefault === 1 ? 1 : 0
    }))
    let dIdx = specs.value.findIndex((s) => s.isDefault === 1)
    if (dIdx < 0) dIdx = 0
    defaultIndex.value = dIdx
    specs.value[dIdx].isDefault = 1
    specMode.value = specs.value.length > 1 ? 'multi' : 'single'
  } catch (e) {
    ElMessage.error(e?.message || '加载商品详情失败')
    visible.value = false
  } finally {
    detailLoading.value = false
  }
}

function onSpecModeChange(mode) {
  if (mode === 'single') {
    const keep = specs.value[defaultIndex.value] || specs.value[0] || emptySpec()
    keep.isDefault = 1
    if (!keep.specName) keep.specName = '标准'
    specs.value = [keep]
    defaultIndex.value = 0
  } else if (specs.value.length < 2) {
    // 切到多规格时预留第二行，方便录入
    if (!specs.value.length) {
      specs.value = [emptySpec()]
    }
    specs.value[0].isDefault = 1
    defaultIndex.value = 0
  }
}

function addSpec() {
  specs.value.push(emptySpec({
    goodsId: form.id,
    specName: '',
    specPrice: defaultSpec.value?.specPrice || form.salePrice || 0,
    stock: 0,
    isDefault: 0
  }))
}

function removeSpec(index) {
  if (specs.value.length <= 1) return
  specs.value.splice(index, 1)
  if (defaultIndex.value >= specs.value.length) {
    defaultIndex.value = 0
  }
  onDefaultChange()
}

function onDefaultChange() {
  specs.value.forEach((s, i) => {
    s.isDefault = i === defaultIndex.value ? 1 : 0
  })
}

async function onUpload({ file }) {
  const data = await uploadImage(file, 'goods')
  form.goodsImg = typeof data === 'string' ? data : (data?.url || '')
  ElMessage.success('上传成功')
}

function validate() {
  if (!form.catId) {
    ElMessage.warning('请选择分类')
    return false
  }
  if (!form.goodsName?.trim()) {
    ElMessage.warning('请填写商品名称')
    return false
  }
  if (!form.unit?.trim()) {
    ElMessage.warning('请填写计量单位')
    return false
  }
  if (!specs.value.length) {
    ElMessage.warning('至少需要一个销售规格')
    return false
  }
  onDefaultChange()
  const names = new Set()
  for (const s of specs.value) {
    const name = (s.specName || '').trim()
    if (!name) {
      ElMessage.warning('规格名称不能为空')
      return false
    }
    const key = name.toLowerCase()
    if (names.has(key)) {
      ElMessage.warning(`规格名称重复：${name}`)
      return false
    }
    names.add(key)
    if (s.specPrice == null || Number(s.specPrice) < 0) {
      ElMessage.warning('请填写有效的规格价格')
      return false
    }
  }
  return true
}

async function onSave() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = {
      goods: {
        ...form,
        goodsName: form.goodsName.trim(),
        unit: form.unit.trim(),
        salePrice: defaultSpec.value?.specPrice || 0,
        totalStock: totalStockPreview.value
      },
      specs: specs.value.map((s, i) => ({
        id: s.id || null,
        goodsId: form.id || null,
        specName: s.specName.trim(),
        specPrice: s.specPrice,
        stock: Number(s.stock) || 0,
        isDefault: i === defaultIndex.value ? 1 : 0
      }))
    }
    await goodsSaveWithSpecs(payload)
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.goodsName}」？`, '提示')
  await goodsDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 12px;
}
.drawer-body {
  padding-right: 8px;
}
.img-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.preview {
  width: 80px;
  height: 80px;
  margin-top: 8px;
  border-radius: 4px;
}
.unit-suffix {
  margin-left: 8px;
  color: #909399;
}
.preview-bar {
  margin-top: 20px;
  padding: 12px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
}
.preview-bar .muted {
  margin-left: 8px;
  color: #909399;
}
</style>

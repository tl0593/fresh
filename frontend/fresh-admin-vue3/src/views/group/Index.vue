<template>
  <div class="page-card">
    <div class="toolbar">
      <span>团购活动</span>
      <div>
        <el-button @click="openAi">AI 生成文案</el-button>
        <el-button type="primary" @click="openEdit()">新增</el-button>
      </div>
    </div>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="开启团购活动前，请先在「商品管理」中把对应商品的「团购」开关打开（仅标记可参与）。"
      style="margin-bottom:12px"
    />
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="goodsId" label="商品ID" width="90" />
      <el-table-column label="商品" min-width="120">
        <template #default="{ row }">{{ goodsName(row.goodsId) }}</template>
      </el-table-column>
      <el-table-column prop="specId" label="规格ID" width="90" />
      <el-table-column prop="groupPrice" label="团购价" width="90" />
      <el-table-column prop="groupNum" label="成团人数" width="90" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="startTime" label="开始" min-width="150" />
      <el-table-column prop="endTime" label="结束" min-width="150" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">{{ row.status === 1 ? '进行中' : '关闭' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑团购' : '新增团购'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品" required>
          <el-select
            v-model="form.goodsId"
            filterable
            clearable
            placeholder="仅显示已标记可参与团购的商品"
            style="width:100%"
            @change="onGoodsChange"
          >
            <el-option
              v-for="g in groupGoodsOptions"
              :key="g.id"
              :label="`${g.id} - ${g.goodsName}`"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.status === 1 && groupGoodsOptions.length === 0" label=" ">
          <el-text type="warning">暂无可用商品，请先到商品管理开启「团购」标记</el-text>
        </el-form-item>
        <el-form-item label="规格" required>
          <el-select
            v-model="form.specId"
            filterable
            clearable
            placeholder="先选商品，再选规格"
            style="width:100%"
            :disabled="!form.goodsId"
            @change="onSpecChange"
          >
            <el-option
              v-for="s in specOptions"
              :key="s.id"
              :label="`${s.id} - ${s.specName}（¥${s.specPrice} / 库存${s.stock}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.goodsId && specOptions.length === 0" label=" ">
          <el-text type="warning">该商品暂无规格，请先在商品管理点「规格」添加</el-text>
        </el-form-item>
        <el-form-item label="团购价"><el-input-number v-model="form.groupPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="成团人数"><el-input-number v-model="form.groupNum" :min="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.groupDesc" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option :value="1" label="进行中" /><el-option :value="0" label="关闭" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 生成团购文案" width="520px">
      <el-form :model="aiForm" label-width="90px">
        <el-form-item label="商品" required>
          <div class="id-with-name">
            <el-select
              v-model="aiForm.goodsId"
              filterable
              clearable
              placeholder="选择商品"
              style="flex:1"
              @change="onAiGoodsChange"
            >
              <el-option
                v-for="g in allGoods"
                :key="g.id"
                :label="`${g.id} - ${g.goodsName}`"
                :value="g.id"
              />
            </el-select>
            <span class="name-hint">{{ aiGoodsHint }}</span>
          </div>
        </el-form-item>
        <el-form-item label="商品名"><el-input v-model="aiForm.goodsName" placeholder="可手动微调" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="aiForm.spec" /></el-form-item>
        <el-form-item label="产地"><el-input v-model="aiForm.origin" /></el-form-item>
        <el-form-item label="补充"><el-input v-model="aiForm.extraInfo" type="textarea" rows="2" /></el-form-item>
      </el-form>
      <el-input v-if="aiResult" v-model="aiResult" type="textarea" rows="4" style="margin-top:8px" readonly />
      <template #footer>
        <el-button @click="aiVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="onGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { goodsList, groupList, groupSave, groupDelete, specListByGoods } from '@/api/goods'
import { generateGroupText } from '@/api/ai'

const empty = () => ({
  id: null, goodsId: null, specId: null, groupPrice: 0, groupNum: 2, stock: 0,
  startTime: '', endTime: '', groupDesc: '', status: 1
})

const list = ref([])
const allGoods = ref([])
const specOptions = ref([])
const loading = ref(false)
const visible = ref(false)
const aiVisible = ref(false)
const saving = ref(false)
const form = reactive(empty())
const aiForm = reactive({ goodsId: null, goodsName: '', spec: '', origin: '', extraInfo: '' })
const aiResult = ref('')

const groupGoodsOptions = computed(() =>
  allGoods.value.filter((g) => Number(g.isGroup) === 1)
)

const goodsMap = computed(() => {
  const map = {}
  allGoods.value.forEach((g) => { map[g.id] = g })
  return map
})

function goodsName(id) {
  return goodsMap.value[id]?.goodsName || '-'
}

const aiGoodsHint = computed(() => {
  const id = aiForm.goodsId
  if (!id) return '请选择商品'
  const name = goodsMap.value[id]?.goodsName
  return name ? `当前：${name}` : `当前：未找到 ID=${id}`
})

function onAiGoodsChange(goodsId) {
  const g = goodsMap.value[goodsId]
  if (g) {
    aiForm.goodsName = g.goodsName || ''
    if (!aiForm.spec && g.unit) {
      aiForm.spec = g.unit
    }
  } else {
    aiForm.goodsName = ''
  }
}

async function loadSpecs(goodsId) {
  if (!goodsId) {
    specOptions.value = []
    return
  }
  try {
    specOptions.value = (await specListByGoods(goodsId)) || []
  } catch {
    specOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const [groups, goods] = await Promise.all([
      groupList().catch(() => []),
      goodsList().catch(() => [])
    ])
    list.value = groups || []
    allGoods.value = goods || []
  } finally {
    loading.value = false
  }
}

async function openEdit(row) {
  Object.assign(form, empty(), row || {})
  await loadSpecs(form.goodsId)
  visible.value = true
}

async function onGoodsChange(goodsId) {
  form.specId = null
  await loadSpecs(goodsId)
  const g = goodsMap.value[goodsId]
  if (g && (form.groupPrice == null || form.groupPrice === 0)) {
    form.groupPrice = Number(g.salePrice || 0)
  }
  // 默认选中默认规格
  const def = specOptions.value.find((s) => Number(s.isDefault) === 1) || specOptions.value[0]
  if (def) {
    form.specId = def.id
    onSpecChange(def.id)
  }
}

function onSpecChange(specId) {
  const s = specOptions.value.find((item) => item.id === specId)
  if (!s) return
  if (form.groupPrice == null || form.groupPrice === 0) {
    form.groupPrice = Number(s.specPrice || 0)
  }
  if (form.stock == null || form.stock === 0) {
    form.stock = Number(s.stock || 0)
  }
}

function openAi() {
  aiResult.value = ''
  aiVisible.value = true
}

function assertCanOpen() {
  if (!form.goodsId) {
    ElMessage.warning('请选择商品')
    return false
  }
  if (!form.specId) {
    ElMessage.warning('请选择规格')
    return false
  }
  if (form.status !== 1) return true
  const g = goodsMap.value[form.goodsId]
  if (!g || Number(g.isGroup) !== 1) {
    ElMessage.warning('该商品未标记「可参与团购」，请先在商品管理中开启团购标记')
    return false
  }
  return true
}

async function onSave() {
  if (!assertCanOpen()) return
  saving.value = true
  try {
    await groupSave({ ...form })
    ElMessage.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onGenerate() {
  if (!aiForm.goodsId) {
    ElMessage.warning('请选择商品')
    return
  }
  saving.value = true
  try {
    const data = await generateGroupText({ ...aiForm })
    if (typeof data === 'string') {
      aiResult.value = data
    } else if (Array.isArray(data?.texts)) {
      aiResult.value = data.texts.join('\n\n')
    } else {
      aiResult.value = JSON.stringify(data)
    }
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除该团购活动？', '提示')
  await groupDelete(row.id)
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

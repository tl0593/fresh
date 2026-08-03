<template>
  <div class="page-card">
    <div class="toolbar">
      <span>秒杀活动</span>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="开启秒杀活动前，请先在「商品管理」中把对应商品的「秒杀」开关打开（仅标记可参与）。"
      style="margin-bottom:12px"
    />
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="goodsId" label="商品ID" width="90" />
      <el-table-column label="商品" min-width="120">
        <template #default="{ row }">{{ goodsName(row.goodsId) }}</template>
      </el-table-column>
      <el-table-column prop="specId" label="规格ID" width="90" />
      <el-table-column prop="seckillPrice" label="秒杀价" width="90" />
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

    <el-dialog v-model="visible" :title="form.id ? '编辑秒杀' : '新增秒杀'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品" required>
          <el-select
            v-model="form.goodsId"
            filterable
            clearable
            placeholder="仅显示已标记可参与秒杀的商品"
            style="width:100%"
            @change="onGoodsChange"
          >
            <el-option
              v-for="g in seckillGoodsOptions"
              :key="g.id"
              :label="`${g.id} - ${g.goodsName}`"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.status === 1 && seckillGoodsOptions.length === 0" label=" ">
          <el-text type="warning">暂无可用商品，请先到商品管理开启「秒杀」标记</el-text>
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
        <el-form-item label="秒杀价"><el-input-number v-model="form.seckillPrice" :min="0" :precision="2" /></el-form-item>
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
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option :value="1" label="进行中" /><el-option :value="0" label="关闭" /></el-select>
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
import { goodsList, seckillList, seckillSave, seckillDelete, specListByGoods } from '@/api/goods'

const empty = () => ({
  id: null, goodsId: null, specId: null, seckillPrice: 0, stock: 0,
  startTime: '', endTime: '', status: 1
})

const list = ref([])
const allGoods = ref([])
const specOptions = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive(empty())

const seckillGoodsOptions = computed(() =>
  allGoods.value.filter((g) => Number(g.isSeckill) === 1)
)

const goodsMap = computed(() => {
  const map = {}
  allGoods.value.forEach((g) => { map[g.id] = g })
  return map
})

function goodsName(id) {
  return goodsMap.value[id]?.goodsName || '-'
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
    const [acts, goods] = await Promise.all([
      seckillList().catch(() => []),
      goodsList().catch(() => [])
    ])
    list.value = acts || []
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
  if (g && (form.seckillPrice == null || form.seckillPrice === 0)) {
    form.seckillPrice = Number(g.salePrice || 0)
  }
  const def = specOptions.value.find((s) => Number(s.isDefault) === 1) || specOptions.value[0]
  if (def) {
    form.specId = def.id
    onSpecChange(def.id)
  }
}

function onSpecChange(specId) {
  const s = specOptions.value.find((item) => item.id === specId)
  if (!s) return
  if (form.seckillPrice == null || form.seckillPrice === 0) {
    form.seckillPrice = Number(s.specPrice || 0)
  }
  if (form.stock == null || form.stock === 0) {
    form.stock = Number(s.stock || 0)
  }
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
  if (!g || Number(g.isSeckill) !== 1) {
    ElMessage.warning('该商品未标记「可参与秒杀」，请先在商品管理中开启秒杀标记')
    return false
  }
  return true
}

async function onSave() {
  if (!assertCanOpen()) return
  saving.value = true
  try {
    await seckillSave({ ...form })
    ElMessage.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除该秒杀活动？', '提示')
  await seckillDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

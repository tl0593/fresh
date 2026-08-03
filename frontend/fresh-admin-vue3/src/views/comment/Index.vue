<template>
  <div class="page-card">
    <div class="toolbar">
      <span>评价管理</span>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="goodsId" label="商品ID" width="90" />
      <el-table-column prop="orderNo" label="订单号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="score" label="评分" width="70" />
      <el-table-column prop="content" label="内容" min-width="140" show-overflow-tooltip />
      <el-table-column label="图片" width="140">
        <template #default="{ row }">
          <div class="img-row" v-if="imgList(row.images).length">
            <el-image
              v-for="(url, i) in imgList(row.images).slice(0, 3)"
              :key="`${row.id}-${i}`"
              :src="url"
              :preview-src-list="imgList(row.images)"
              :initial-index="i"
              fit="cover"
              class="thumb"
              preview-teleported
            />
          </div>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column label="回复" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reply?.replyContent || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="info" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openReply(row)">回复</el-button>
          <el-button link type="warning" @click="onHide(row)">隐藏</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:12px;text-align:right">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="detailVisible" title="评价详情" width="560px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="评价ID">{{ current.id }}</el-descriptions-item>
          <el-descriptions-item label="商品ID">{{ current.goodsId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ current.score }} 星</el-descriptions-item>
          <el-descriptions-item label="内容">{{ current.content || '（无文字）' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ current.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商家回复">{{ current.reply?.replyContent || '未回复' }}</el-descriptions-item>
          <el-descriptions-item label="图片">
            <span v-if="!imgList(current.images).length">未上传</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="imgList(current.images).length" class="preview-imgs">
          <div class="preview-label">用户上传图片（点击可放大）</div>
          <el-image
            v-for="(url, i) in imgList(current.images)"
            :key="i"
            :src="url"
            :preview-src-list="imgList(current.images)"
            :initial-index="i"
            fit="cover"
            class="preview"
            preview-teleported
          />
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openReplyFromDetail">回复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="visible" title="回复评价" width="520px">
      <div v-if="currentImages.length" class="reply-imgs">
        <div class="reply-imgs-label">用户图片</div>
        <el-image
          v-for="(url, i) in currentImages"
          :key="i"
          :src="url"
          :preview-src-list="currentImages"
          :initial-index="i"
          fit="cover"
          class="thumb large"
          preview-teleported
        />
      </div>
      <el-input v-model="replyContent" type="textarea" rows="4" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commentPage, commentHide, commentReply } from '@/api/goods'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const visible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const replyContent = ref('')
const currentId = ref(null)
const currentImages = ref([])
const current = ref(null)

/** 网关绝对地址转相对路径，走 vite /api 代理 */
function toPreviewSrc(url) {
  let u = String(url || '').trim()
  if (!u || /^https?:\/\/tmp\//i.test(u) || u.startsWith('wxfile://') || u.startsWith('file://')) return ''
  const local = u.match(/^https?:\/\/(?:127\.0\.0\.1|localhost)(?::\d+)?(\/api\/.+)$/i)
  if (local) u = local[1]
  else if (!u.startsWith('http://') && !u.startsWith('https://') && !u.startsWith('/')) u = `/${u}`
  // 历史脏目录 comment,comment
  u = u.replace(/\/(afterSale|comment|goods),\1\//g, '/$1/')
  return u
}

function imgList(images) {
  if (Array.isArray(images)) {
    return [...new Set(images.map(toPreviewSrc).filter(Boolean))]
  }
  const raw = String(images || '').trim()
  if (!raw) return []
  if (raw.includes('|')) {
    return [...new Set(raw.split('|').map((s) => toPreviewSrc(s.trim())).filter(Boolean))]
  }
  const one = toPreviewSrc(raw)
  return one ? [one] : []
}

async function load() {
  loading.value = true
  try {
    const data = await commentPage({ pageNum: pageNum.value, pageSize })
    list.value = data?.records || []
    total.value = Number(data?.total || 0)
  } finally { loading.value = false }
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function openReply(row) {
  currentId.value = row.id
  currentImages.value = imgList(row.images)
  replyContent.value = ''
  visible.value = true
}

function openReplyFromDetail() {
  if (!current.value) return
  detailVisible.value = false
  openReply(current.value)
}

async function onReply() {
  saving.value = true
  try {
    await commentReply({
      commentId: currentId.value,
      adminId: userStore.userInfo?.id,
      replyContent: replyContent.value
    })
    ElMessage.success('回复成功')
    visible.value = false
    load()
  } finally { saving.value = false }
}

async function onHide(row) {
  await ElMessageBox.confirm('确认隐藏该评价？', '提示')
  await commentHide(row.id)
  ElMessage.success('已隐藏')
  load()
}

onMounted(load)
</script>

<style scoped>
.img-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
}
.thumb.large {
  width: 72px;
  height: 72px;
}
.muted {
  color: #c0c4cc;
}
.reply-imgs {
  margin-bottom: 12px;
}
.reply-imgs-label,
.preview-label {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}
.reply-imgs .thumb {
  margin-right: 8px;
  margin-bottom: 4px;
}
.preview-imgs {
  margin-top: 12px;
}
.preview {
  width: 96px;
  height: 96px;
  margin-right: 8px;
  margin-bottom: 8px;
  border-radius: 6px;
}
</style>

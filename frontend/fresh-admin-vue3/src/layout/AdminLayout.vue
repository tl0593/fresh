<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">Fresh 管理后台</div>
      <el-menu :default-active="route.path" router background-color="#1f2d3d" text-color="#bfcbd9" active-text-color="#fff">
        <el-menu-item index="/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/order">订单管理</el-menu-item>
        <el-menu-item index="/after-sale">
          <span>售后管理</span>
          <el-badge v-if="pendingAfterSale > 0" :value="pendingAfterSale" class="menu-badge" />
        </el-menu-item>
        <el-sub-menu index="goods-group">
          <template #title>
            <span>商品中心</span>
            <el-badge v-if="stockAlertCount > 0" :value="stockAlertCount" class="menu-badge" />
          </template>
          <el-menu-item index="/category">分类管理</el-menu-item>
          <el-menu-item index="/goods">商品管理</el-menu-item>
          <el-menu-item index="/stock-alert">
            <span>补货提醒</span>
            <el-badge v-if="stockAlertCount > 0" :value="stockAlertCount" class="menu-badge" />
          </el-menu-item>
          <el-menu-item index="/group">团购活动</el-menu-item>
          <el-menu-item index="/seckill">秒杀活动</el-menu-item>
          <el-menu-item index="/comment">评价管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="market-group">
          <template #title>营销中心</template>
          <el-menu-item index="/coupon">优惠券模板</el-menu-item>
          <el-menu-item index="/fullreduce">满减活动</el-menu-item>
          <el-menu-item index="/integral-coupon">积分兑券</el-menu-item>
          <el-menu-item index="/seckill-coupon">整点抢券</el-menu-item>
          <el-menu-item index="/lottery">抽奖奖品</el-menu-item>
          <el-menu-item index="/coupon-log">用券记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="ai-group">
          <template #title>AI 中心</template>
          <el-menu-item index="/ai/knowledge">知识库</el-menu-item>
          <el-menu-item index="/ai/chat-log">对话日志</el-menu-item>
          <el-menu-item index="/ai/image-log">识图日志</el-menu-item>
          <el-menu-item index="/ai/group-text-log">文案日志</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/message/template">消息模板</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ route.meta.title || '管理后台' }}</span>
        <div class="right">
          <el-badge v-if="pendingAfterSale > 0" :value="pendingAfterSale" class="header-badge">
            <el-button type="warning" plain size="small" @click="router.push('/after-sale')">待审售后</el-button>
          </el-badge>
          <el-badge v-if="stockAlertCount > 0" :value="stockAlertCount" class="header-badge">
            <el-button type="danger" plain size="small" @click="router.push('/stock-alert')">待补货</el-button>
          </el-badge>
          <span>{{ userStore.displayName }}</span>
          <el-button link type="danger" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { afterSalePendingCount } from '@/api/afterSale'
import { stockAlertCount as fetchStockAlertCount } from '@/api/goods'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const pendingAfterSale = ref(0)
const stockAlertCount = ref(0)
let timer = null

async function refreshPending() {
  if (!userStore.token) {
    pendingAfterSale.value = 0
    stockAlertCount.value = 0
    return
  }
  try {
    const [afterSale, stock] = await Promise.all([
      afterSalePendingCount().catch(() => null),
      fetchStockAlertCount({ onlyOnSale: true }).catch(() => null)
    ])
    pendingAfterSale.value = Number(afterSale?.count || 0)
    stockAlertCount.value = Number(stock?.count || 0)
  } catch {
    // ignore silent poll errors
  }
}

function onLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  refreshPending()
  timer = setInterval(refreshPending, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

watch(() => route.path, () => {
  refreshPending()
})
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #1f2d3d;
  overflow-y: auto;
}
.brand {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  border-bottom: 1px solid #2b3a4b;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.menu-badge {
  margin-left: 8px;
}
.header-badge {
  line-height: 1;
}
.main {
  padding: 16px;
}
</style>
